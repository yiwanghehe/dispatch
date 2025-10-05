package com.transportation.dispatch.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StepInfo {
    private String instruction;
    private String polyline;
}

