package com.transportation.dispatch.model.entity;

import com.transportation.dispatch.enumeration.VehicleStatus;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ToString(exclude = {"parsedPolyline", "traveledPolyline"}) // 避免日志过长
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
    private Double speed;
    private Integer lastReachedPathIndex;
    private BigDecimal totalShippingWeight;
    private BigDecimal totalShippingVolume;

    // --- 以下为仿真运行时，仅在内存中使用的字段 ---

    /**
     * [Transient] 当前任务的完整路径坐标点 ("lng,lat;lng,lat;...")
     * 当车辆状态为IDLE时，此字段为null。
     */
    private transient String routePolyline;

    /**
     * [NEW - Transient] 当前车辆已经过的路径的所有坐标点 ("lng,lat;lng,lat;...")
     * 返回给前端，用于绘制车辆尾迹。
     */
    private transient String traveledPolyline;

    /**
     * [Transient] 将 routePolyline 解析后的坐标点数组，方便计算
     */
    private transient List<String[]> parsedPolyline;

    /**
     * [Transient] 当前路径的总距离（米）
     */
    private transient int routeDistance;

    /**
     * [Transient] 当前路径的总预估时间（秒）
     */
    private transient int routeDuration;

    /**
     * [Transient] 开始当前行为（如移动、装货）的仿真时间点（秒）
     */
    private transient long actionStartTime;

    private transient double noLoadDistance;
    private transient double noLoadDuration;
    private transient double loadDistance;
    private transient double loadDuration;

    private transient double waitingDuration;
    private transient double wastedLoad;
}

