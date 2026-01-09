package com.transportation.dispatch.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 仿真会话/沙箱实体类。
 * 用于记录每一次仿真运行的配置、生命周期和总体统计结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationSession {

    // 主键
    private Long id;

    // --- 运行配置字段 (来自 Weight2Dispatch) ---
    /**
     * 运行名称/描述，方便后续分析
     */
    private String sessionName;

    /**
     * 启动时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间 (仿真完成时更新)
     */
    private LocalDateTime endTime;

    /**
     * 是否使用加权策略
     */
    private Boolean useWeight;

    /**
     * 行驶时间权重
     */
    private Double weightTime;

    /**
     * 浪费载重权重
     */
    private Double weightWastedLoad;

    /**
     * 浪费空闲时间权重
     */
    private Double weightWastedIdle;

    // --- 总体结果指标 (Simulation KPIs) ---

    /**
     * 车辆平均空载行驶距离 (米)
     */
    private Double avgNoLoadDistance;

    /**
     * 车辆平均装载行驶距离 (米)
     */
    private Double avgLoadDistance;

    /**
     * 车辆平均总行驶时间 (秒)
     */
    private Double avgTotalDuration;

    /**
     * 车辆平均等待/空闲时间 (秒)
     */
    private Double avgWaitingDuration;

    /**
     * 完成的任务总数
     */
    private Integer totalDemandsCompleted;

    /**
     * 总浪费载重 (车辆最大载重 - 实际载重)
     */
    private Double totalWastedCapacity;

    /**
     * 备注或错误信息
     */
    private String notes;
    /**
     * 随机事件
     */


    public SimulationSession(Long id, String sessionName, LocalDateTime startTime, LocalDateTime endTime, boolean useWeight, double weightTime, double weightWastedLoad, double weightWastedIdle, Double avgNoLoadDistance, Double avgLoadDistance, Double avgTotalDuration, Double avgWaitingDuration, Integer totalDemandsCompleted, Double totalWastedCapacity, String notes) {
        this.id = id;
        this.sessionName = sessionName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.useWeight = useWeight;
        this.weightTime = weightTime;
        this.weightWastedLoad = weightWastedLoad;
        this.weightWastedIdle = weightWastedIdle;
        this.avgNoLoadDistance = avgNoLoadDistance;
        this.avgLoadDistance = avgLoadDistance;
        this.avgTotalDuration = avgTotalDuration;
        this.avgWaitingDuration = avgWaitingDuration;
        this.totalDemandsCompleted = totalDemandsCompleted;
        this.totalWastedCapacity = totalWastedCapacity;
        this.notes = notes;
    }
}