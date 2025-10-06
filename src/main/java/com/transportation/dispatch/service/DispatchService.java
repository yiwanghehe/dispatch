package com.transportation.dispatch.service;

public interface DispatchService{

    /**
     * 调度主方法：为所有待处理的任务分配车辆（无干预策略）
     */
    void dispatchPendingDemands();
}
