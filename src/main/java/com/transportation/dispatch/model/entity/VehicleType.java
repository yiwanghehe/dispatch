package com.transportation.dispatch.model.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VehicleType {
    private Long id;
    private String name;
    private BigDecimal maxLoadWeight;
    private BigDecimal maxLoadVolume;
    private BigDecimal carbonEmissionFactor;
}

