package com.transportation.dispatch.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Weight2Dispatch {
    double WEIGHT_TIME ;          // 行驶时间权重
    double WEIGHT_WASTED_LOAD ;   // 浪费载重权重（可能需要标准化）
    double WEIGHT_WASTED_IDLE ;
    boolean UseWeight;
}
