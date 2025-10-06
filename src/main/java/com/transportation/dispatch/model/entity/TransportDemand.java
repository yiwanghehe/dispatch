package com.transportation.dispatch.model.entity;

import com.transportation.dispatch.enumeration.DemandStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransportDemand {
    private Long id;
    private Long originPoiId;
    private Long destinationPoiId;
    private String cargoName;
    private BigDecimal cargoWeight;
    private BigDecimal cargoVolume;
    private DemandStatus status;
    private Long assignedVehicleId;
    private LocalDateTime creationTime;
    private LocalDateTime assignmentTime;
    private LocalDateTime pickupTime;
    private LocalDateTime completionTime;
    private Long templateId;
    private Integer stageOrder;
}
