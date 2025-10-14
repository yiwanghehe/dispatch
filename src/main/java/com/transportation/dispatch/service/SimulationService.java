package com.transportation.dispatch.service;

import com.transportation.dispatch.model.entity.Weight2Dispatch;

public interface SimulationService {
    /**
     * 启动仿真
     */
    void start(Weight2Dispatch weight2Dispatch);

    /**
     * 停止仿真
     */
    void stop();

    /**
     * 是否在运行
     */
    boolean isRunning();
}
