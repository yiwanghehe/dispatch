package com.transportation.dispatch.model.entity;

import com.transportation.dispatch.enumeration.VehicleStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Vehicle {
    // --- 数据库持久化字段 ---
    private Long id;
    private String plateNumber;
    private Long typeId;
    private VehicleStatus status;
    private String currentLng;
    private String currentLat;
    private Long currentDemandId;
    private LocalDateTime lastUpdateTime;

    // --- 以下为仿真运行时，仅在内存中使用的字段 ---

    /**
     * 当前任务的完整路径坐标点 ("lng,lat;lng,lat;...")
     * 当车辆状态为IDLE时，此字段为null。
     */
    private String routePolyline;

    /**
     * 【新增】将 routePolyline 解析后的坐标点数组，方便计算
     */
    private transient List<String[]> parsedPolyline;

    /**
     * 【新增】当前路径的总距离（米）
     */
    private transient int routeDistance;

    /**
     * 【新增】当前路径的总预估时间（秒）
     */
    private transient int routeDuration;

    /**
     * 【新增】开始当前行为（如移动、装货）的仿真时间点（秒）
     */
    private transient long actionStartTime;
}
