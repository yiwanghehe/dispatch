package com.transportation.dispatch.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * 高德地图Web服务-POI搜索API V5版本的响应体封装类。
 * 用于映射最外层的JSON结构。
 */
@Data
public class AmapPoiResponse {

    /**
     * 状态码
     * 1：成功；0：失败
     */
    @JsonProperty("status")
    private String status;

    /**
     * 状态说明
     * 当 status 为 0 时，info 会返回错误原因；否则返回“OK”。
     */
    @JsonProperty("info")
    private String info;

    /**
     * 状态码
     * 值为 10000 时代表OK
     */
    @JsonProperty("infocode")
    private String infocode;

    /**
     * 搜索结果总数
     * 注意：这是一个字符串类型
     */
    @JsonProperty("count")
    private String count;

    /**
     * POI信息列表
     * 包含了具体的地点信息
     */
    @JsonProperty("pois")
    private List<AmapPoiDetail> pois;
}

