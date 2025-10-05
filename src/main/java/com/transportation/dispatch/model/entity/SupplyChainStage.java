package com.transportation.dispatch.model.entity;

import java.math.BigDecimal;

import com.transportation.dispatch.enumeration.PoiSimType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // 添加无参构造函数
public class SupplyChainStage {
    private Long id;
    private Long templateId;
    private Integer stageOrder;
    private PoiSimType originPoiType;
    private PoiSimType destinationPoiType;
    private String cargoName;
    private BigDecimal cargoWeight;
    private BigDecimal cargoVolume;

    public SupplyChainStage(Long templateId, Integer stageOrder, PoiSimType originPoiType, PoiSimType destinationPoiType, String cargoName, double cargoWeight, double cargoVolume) {
        this.templateId = templateId;
        this.stageOrder = stageOrder;
        this.originPoiType = originPoiType;
        this.destinationPoiType = destinationPoiType;
        this.cargoName = cargoName;
        this.cargoWeight = new BigDecimal(cargoWeight);
        this.cargoVolume = new BigDecimal(cargoVolume);
    }
}
