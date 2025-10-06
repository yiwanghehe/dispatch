package com.transportation.dispatch.service;

public interface SimulationService {
    /**
     * 启动仿真
     */
    void start();

    /**
     * 停止仿真
     */
    void stop();

    /**
     * 是否在运行
     */
    boolean isRunning();
}
