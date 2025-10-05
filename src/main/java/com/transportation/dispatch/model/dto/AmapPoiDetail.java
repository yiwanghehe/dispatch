package com.transportation.dispatch.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 高德POI信息详情实体类。
 * 用于映射 AmapPoiResponse 中 pois 数组里的每一个POI对象。
 */
@Data
public class AmapPoiDetail {

    /**
     * POI的唯一ID
     */
    @JsonProperty("id")
    private String id;

    /**
     * POI点名称
     */
    @JsonProperty("name")
    private String name;

    /**
     * POI类型
     * 例如："公司企业;工厂;工厂"
     */
    @JsonProperty("type")
    private String type;

    /**
     * POI类型编码
     */
    @JsonProperty("typecode")
    private String typecode;

    /**
     * POI地址
     */
    @JsonProperty("address")
    private String address;

    /**
     * POI的经纬度
     * 格式为 "经度,纬度" 的字符串
     */
    @JsonProperty("location")
    private String location;

    /**
     * POI所在省份名称
     */
    @JsonProperty("pname")
    private String pname;

    /**
     * POI所在城市名称
     */
    @JsonProperty("cityname")
    private String cityname;

    /**
     * POI所在区域名称
     */
    @JsonProperty("adname")
    private String adname;

    /**
     * 区域编码
     */
    @JsonProperty("adcode")
    private String adcode;

    /**
     * 省份编码
     */
    @JsonProperty("pcode")
    private String pcode;

    /**
     * 城市编码
     */
    @JsonProperty("citycode")
    private String citycode;

    /**
     * 父POI的ID（如果存在）
     */
    @JsonProperty("parent")
    private String parent;

    /**
     * 距离（在周边搜索时有值）
     */
    @JsonProperty("distance")
    private String distance;
}

