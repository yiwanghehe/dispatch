package com.transportation.dispatch.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.transportation.dispatch.enumeration.PoiSimType;
import lombok.Data;

/**
 * POI数据库实体类 (Point of Interest)。
 * 这个类对应数据库中的 `poi` 表，用于持久化存储从高德API获取的地理位置信息。
 */
@Data
public class Poi {

    /**
     * 数据库主键ID
     */
    private Long id;

    /**
     * 高德地图返回的唯一POI ID
     */
    private String amapId;

    /**
     * POI点名称
     */
    private String name;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 经度 (Longitude)
     */
    private String lng;

    /**
     * 纬度 (Latitude)
     */
    private String lat;

    /**
     * POI所在省份名称
     */
    private String pname;

    /**
     * POI所在城市名称
     */
    private String cityname;

    /**
     * POI所在区域名称
     */
    private String adname;

    /**
     * 高德返回的类型描述
     */
    private String type;

    /**
     * 高德返回的类型编码
     */
    private String typecode;

    /**
     * 我们自定义的仿真业务类型
     */
    private PoiSimType simType;

    /**
     * Poi点的状态
     */
    private int status;

    /**
     * 记录创建时间
     */
    private LocalDateTime createTime;
}
