package com.transportation.dispatch.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略JSON中我们不需要的字段
public class AmapRouteResponse {
    private String status;
    private String info;
    private RouteInfo route;
}
