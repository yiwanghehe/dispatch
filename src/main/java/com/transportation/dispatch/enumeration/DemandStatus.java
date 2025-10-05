package com.transportation.dispatch.enumeration;

/**
 * 定义运输需求（任务）的生命周期状态
 */
public enum DemandStatus {
    PENDING,            // 待分配，新创建的任务
    ASSIGNED,           // 已分配，已被调度系统指派给车辆
    COMPLETED,          // 已完成
    CANCELLED           // 已取消
}

