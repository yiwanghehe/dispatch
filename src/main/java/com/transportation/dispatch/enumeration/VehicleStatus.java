package com.transportation.dispatch.enumeration;

/**
 * 定义车辆在仿真过程中的所有可能状态
 */
public enum VehicleStatus {
    IDLE,               // 空闲，等待任务
    MOVING_TO_PICKUP,   // 空载移动中，前往装货点
    LOADING,            // 正在装货
    IN_TRANSIT,         // 满载运输中，前往卸货点
    UNLOADING,          // 正在卸货
    MAINTENANCE,        // 维护保养中，不可用
    REFUSED             // 离线/收工
}
