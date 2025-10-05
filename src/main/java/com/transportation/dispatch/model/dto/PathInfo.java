package com.transportation.dispatch.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PathInfo {
    // 高德V5版本返回的distance和duration是字符串，需要注意
    private String distance;
    private CostInfo cost;
    private List<StepInfo> steps;
}
