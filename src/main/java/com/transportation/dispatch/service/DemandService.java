package com.transportation.dispatch.service;

import com.transportation.dispatch.model.entity.TransportDemand;

public interface DemandService {
    /**
     * 根据概率，随机生成新的运输任务链的起点
     */
    void generateDemands();

    /**
     * 当一个任务完成时，触发其在供应链中的下一个任务
     * @param completedDemand 已完成的任务
     */
    void triggerNextDemand(TransportDemand completedDemand);
    int getCompletedDemandCount();

    void deleteAll();
}
