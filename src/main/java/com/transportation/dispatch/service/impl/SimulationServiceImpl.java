package com.transportation.dispatch.service.impl;

import com.transportation.dispatch.service.DemandService;
import com.transportation.dispatch.service.DispatchService;
import com.transportation.dispatch.service.SimulationService;
import com.transportation.dispatch.service.VehicleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    /**
     * 启动仿真
     */
    @Override
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            executorService = Executors.newSingleThreadScheduledExecutor();
            // scheduleAtFixedRate 确保了任务执行的稳定性
            executorService.scheduleAtFixedRate(this::tick, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
            log.info("仿真引擎已启动。仿真时间流速: {}秒/真实秒", TIME_STEP_SECONDS);
        } else {
            log.warn("仿真引擎已在运行中，请勿重复启动。");
        }
    }

    /**
     * 停止仿真
     */
    @Override
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            if (executorService != null) {
                executorService.shutdown();
                log.info("仿真引擎已停止。");
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

            // 【2. 调度分配】
            dispatchService.dispatchPendingDemands();

            // 【3. 状态更新】
            vehicleService.updateAllVehiclesState(simulationTime, TIME_STEP_SECONDS);

        } catch (Exception e) {
            log.error("仿真tick发生严重错误，引擎将停止: {}", e.getMessage(), e);
            stop(); // 发生未知异常时自动停止，防止系统崩溃
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }
}
