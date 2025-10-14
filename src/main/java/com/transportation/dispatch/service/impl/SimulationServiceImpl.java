package com.transportation.dispatch.service.impl;

import com.transportation.dispatch.mapper.SimulationSessionMapper;
import com.transportation.dispatch.model.entity.SimulationSession;
import com.transportation.dispatch.model.entity.Weight2Dispatch;
import com.transportation.dispatch.service.DemandService;
import com.transportation.dispatch.service.DispatchService;
import com.transportation.dispatch.service.SimulationService;
import com.transportation.dispatch.service.VehicleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class SimulationServiceImpl implements SimulationService {
    // 使用原子布尔值确保线程安全的启停控制
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private ScheduledExecutorService executorService;

    // 仿真世界的每一步是60秒（1分钟）
    private static final int TIME_STEP_SECONDS = 60;
    // 真实世界的每1秒，执行一次仿真步进
    private static final int TICK_INTERVAL_MS = 1000;

    // 仿真世界经过的总秒数
    private long simulationTime = 0L;

    @Autowired
    private DemandService demandService;
    @Autowired
    private DispatchService dispatchService;
    @Autowired
    private VehicleService vehicleService;
    @Autowired
    private SimulationSessionMapper   sessionMapper;
    private Weight2Dispatch weight2dispatch;
    private LocalDateTime startTime;
    private Long currentSessionId;
    private  ExecutorService dispatchExecutor;
    // 用于跟踪当前是否有一个调度任务正在运行
    private Future<?> dispatchFuture = null;

    /**
     * 启动仿真
     */
    // SimulationServiceImpl.java

    @Override
    public void start(Weight2Dispatch weight2Dispatch) {
        if (isRunning.compareAndSet(false, true)) {
            this.weight2dispatch = weight2Dispatch;
            dispatchExecutor=Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors() // 使用可用的处理器核心数
            );

            // --- 【核心修改：检查并重用沙箱】 ---

            SimulationSession sessionToUse = sessionMapper.findLatestUnfinishedSession();

            if (sessionToUse != null) {
                // 情况 1: 发现未完成的沙箱，重用它
                log.warn("检测到上次仿真（ID: {}）未正常停止，将继续使用该会话。", sessionToUse.getId());
                this.currentSessionId = sessionToUse.getId();
                this.startTime = sessionToUse.getStartTime(); // 使用旧的开始时间

                // 重新加载权重配置，以防与本次传入的参数不一致（可选，取决于业务需求）
                this.weight2dispatch = new Weight2Dispatch(
                        sessionToUse.getWeightTime(),
                        sessionToUse.getWeightWastedLoad(),
                        sessionToUse.getWeightWastedIdle(),
                        sessionToUse.getUseWeight()
                );

                // 确保名称更新为最终格式（如果之前没有成功命名）
                String finalSessionName = "数据沙箱 #" + this.currentSessionId;
                if (!finalSessionName.equals(sessionToUse.getSessionName())) {
                    sessionToUse.setSessionName(finalSessionName);
                    sessionMapper.update(sessionToUse); // 使用专用的名称更新方法
                }

            } else {
                // 情况 2: 未发现未完成的沙箱，创建新沙箱
                this.startTime = LocalDateTime.now();
                SimulationSession newSession = new SimulationSession(
                        null,
                        "Simulation Run", // 临时名称，稍后更新为最终名称
                        this.startTime,
                        null,
                        weight2Dispatch.isUseWeight(),
                        weight2Dispatch.getWEIGHT_TIME(),
                        weight2Dispatch.getWEIGHT_WASTED_LOAD(),
                        weight2Dispatch.getWEIGHT_WASTED_IDLE(),
                        null, null, null, null, null, null, null
                );

                sessionMapper.insert(newSession); // MyBatis回填ID
                this.currentSessionId = newSession.getId();

                // 立即更新为最终名称
                String finalSessionName = "数据沙箱 #" + this.currentSessionId;
                newSession.setSessionName(finalSessionName);
                sessionMapper.update(newSession); // 使用专用的名称更新方法

                log.info("已创建新的仿真会话。会话ID: {}", this.currentSessionId);
            }

            // --- 核心修改结束 ---
            executorService = Executors.newSingleThreadScheduledExecutor();

            executorService.scheduleAtFixedRate(this::tick, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);

            log.info("仿真引擎已启动。仿真时间流速: {}秒/真实秒", TIME_STEP_SECONDS);

        } else {

            log.warn("仿真引擎已在运行中，请勿重复启动。");

        }

    }
    /**
     * 停止仿真
     */
    // SimulationServiceImpl.java

    /**
     * 停止仿真
     */
    @Override
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            if (executorService != null) {
                executorService.shutdown();
                dispatchExecutor.shutdown();
                log.info("仿真引擎已停止。");


                if (this.currentSessionId != null) {
                    // 1. 根据ID加载记录
                    SimulationSession session = sessionMapper.findById(this.currentSessionId);

                    if (session != null) {
                        // 2. 收集统计数据
                        session.setEndTime(LocalDateTime.now());
                        session.setUseWeight(weight2dispatch.isUseWeight());
                        session.setWeightTime(weight2dispatch.getWEIGHT_TIME());
                        session.setWeightWastedLoad(weight2dispatch.getWEIGHT_WASTED_LOAD());
                        session.setWeightWastedIdle(weight2dispatch.getWEIGHT_WASTED_IDLE());
                        session.setAvgNoLoadDistance(vehicleService.getTotalNoLoadDistance());
                        session.setAvgLoadDistance(vehicleService.getTotalLoadDistance());
                        session.setAvgTotalDuration(vehicleService.getTotalDuration());
                        session.setAvgWaitingDuration(vehicleService.getTotalWaitingDuration());
                        session.setTotalDemandsCompleted(demandService.getCompletedDemandCount());
                        session.setTotalWastedCapacity(vehicleService.getTotalWastedCapacity());

                        // 3. 构建并设置最终名称
                        String finalSessionName = "数据沙箱 #" + this.currentSessionId;
                        session.setSessionName(finalSessionName);

                        // 4. 更新数据库
                        sessionMapper.update(session);
                        log.info("仿真会话 #{} 结果已保存并命名为: {}", this.currentSessionId, finalSessionName);
                    } else {
                        log.error("尝试停止仿真时，未找到ID为 {} 的会话记录！", this.currentSessionId);
                    }
                } else {
                    log.warn("currentSessionId 为 null，未保存本次仿真结果。");
                }
                // --- 优化 2.1 结束 ---

                // 2. 清空 TransportDemand 表
                demandService.deleteAll();

                // 3. 批量重置所有车辆
                vehicleService.resetAllVehicles();

                // 4. 清理会话状态
                this.currentSessionId = null;
            }
        } else {
            log.warn("仿真引擎当前未运行。");
        }
    }
    /**
     * 仿真核心循环（心跳）
     * 这里是所有动态事件的入口
     */
    private void tick() {
        try {
            simulationTime += TIME_STEP_SECONDS;
            log.info("--- Simulation Tick! Current Time: {}s ---", simulationTime);

            // 【1. 需求生成】
            demandService.generateDemands();

            // 【2. 异步调度分配】
            // 只有当上一个调度任务没有在运行 (isDone) 且当前有待分配任务时，才提交新的调度任务
            // 注意：dispatchFuture 在 start() 中初始化为 null
            if (dispatchFuture == null || dispatchFuture.isDone()) {

                // 提交任务的逻辑保持不变 (使用 dispatchExecutor 提交异步任务)
                Runnable dispatchTask = () -> {
                    try {
                        if (weight2dispatch.isUseWeight()) {
                            // 调用基于成本的调度
                            dispatchService.dispatchPendingDemandsByCost(weight2dispatch);
                        } else {
                            // 调用默认调度
                            dispatchService.dispatchPendingDemands();
                        }
                        log.debug("异步调度任务完成。");
                    } catch (Exception e) {
                        log.error("异步调度任务执行失败:", e);
                    }
                };

                dispatchFuture = dispatchExecutor.submit(dispatchTask);
                log.debug("已提交异步调度任务。");

            } else {
                // 这是正常的，调度是一个后台任务，允许跨越多个 tick
                log.debug("上一个调度任务仍在运行中，等待完成。");
            }


            // 【3. 状态更新】
            // 车辆到达终点的逻辑必须在这里同步执行，这是时间步进的核心。
            vehicleService.updateAllVehiclesState(simulationTime, TIME_STEP_SECONDS);

        } catch (Exception e) {
            log.error("仿真tick发生严重错误，引擎将停止: {}", e.getMessage(), e);
            stop(); // 发生未知异常时自动停止
        }
    }
    @Override
    public boolean isRunning() {
        return isRunning.get();
    }
}
