package com.transportation.dispatch.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Weight2Dispatch {
    @JsonProperty("WEIGHT_TIME")
    private double weightTime ;          // 行驶时间权重
    @JsonProperty("WEIGHT_WASTED_LOAD")
    private double weightWastedLoad ;   // 浪费载重权重（可能需要标准化）
    @JsonProperty("WEIGHT_WASTED_IDLE")
    private double weightWastedIdle ;
    @JsonProperty("UseWeight")
    private boolean useWeight;
}
