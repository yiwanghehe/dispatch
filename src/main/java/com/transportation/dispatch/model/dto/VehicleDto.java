package com.transportation.dispatch.model.dto;

import com.transportation.dispatch.enumeration.VehicleStatus;
import lombok.Data;


import java.time.LocalDateTime;
@Data
public class VehicleDto {
    // --- 数据库持久化字段 ---
    private Long id;
    private String plateNumber;
    private Long typeId;
    private VehicleStatus status;
    private String currentLng;
    private String currentLat;
    private Long currentDemandId;
    private LocalDateTime lastUpdateTime;


    /**
     * [NEW - Transient] 当前车辆已经过的路径的所有坐标点 ("lng,lat;lng,lat;...")
     * 返回给前端，用于绘制车辆尾迹。
     */
    private transient String traveledPolyline;

    private transient int routeDistance;

    /**
     * [Transient] 当前路径的总预估时间（秒）
     */
    private transient int routeDuration;

    /**
     * [Transient] 开始当前行为（如移动、装货）的仿真时间点（秒）
     */
    private transient long actionStartTime;
}
