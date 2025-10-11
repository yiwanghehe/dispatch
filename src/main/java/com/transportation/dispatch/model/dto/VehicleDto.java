package com.transportation.dispatch.model.dto;

import com.transportation.dispatch.enumeration.VehicleStatus;
import lombok.Data;


import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VehicleDto {

    private Long id; // 车辆id
    private String plateNumber; // 车牌
    private Long typeId; // 车辆类型
    private VehicleStatus status; // 车辆状态
    private String currentLng; // 当前经度
    private String currentLat; // 当前纬度
    private Long currentDemandId; // 当前任务id
    private LocalDateTime lastUpdateTime; // 最新更新时间

    private double totalShippingWeight; // 累计运输重量
    private double totalShippingVolume; // 累计运输体积


    /**
     * 当前车辆已经过的路径的所有坐标点 ("lng,lat;lng,lat;...")
     * 返回给前端，用于绘制车辆尾迹。
     */
    private transient String traveledPolyline; // 当前车辆已经过的路径的所有坐标点

    /**
     * [Transient] 当前路径的总预估距离和时间（秒）
     */
    private transient int routeDistance; // 当前路径的总预估距离
    private transient int routeDuration; // 当前路径的总预估时间

    /**
     * [Transient] 开始当前行为的仿真变量
     */
    private transient double maxLoadWeight; // 车辆最大载重
    private transient double currentLoad; // 当前载重
    private transient double wastedLoad; // 浪费载重
    private transient double waitingDuration; // 等待时间
    private transient double noLoadDistance; // 空载里程
    private transient double noLoadDuration; // 空载时间
    private transient double loadDistance; // 载重里程
    private transient double loadDuration; // 载重时间

}
