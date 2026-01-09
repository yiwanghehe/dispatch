package com.transportation.dispatch.service.impl;

import com.transportation.dispatch.enumeration.DemandStatus;
import com.transportation.dispatch.enumeration.PoiSimType;
import com.transportation.dispatch.mapper.PoiMapper;
import com.transportation.dispatch.mapper.SupplyChainMapper;
import com.transportation.dispatch.mapper.TransportDemandMapper;
import com.transportation.dispatch.model.entity.Poi;
import com.transportation.dispatch.model.entity.SupplyChainStage;
import com.transportation.dispatch.model.entity.SupplyChainTemplate;
import com.transportation.dispatch.model.entity.TransportDemand;
import com.transportation.dispatch.service.DemandService;
import com.transportation.dispatch.service.VehicleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class DemandServiceImpl implements DemandService {
    @Autowired
    private SupplyChainMapper supplyChainMapper;
    @Autowired
    private PoiMapper poiMapper;
    @Autowired
    private TransportDemandMapper transportDemandMapper;


    private final Random random = new Random();

    /**
     * 根据概率，随机生成新的运输任务链的起点
     */
    public void generateDemands() {
        // 假设每个tick有20%的概率生成一个新的供应链任务
        if (transportDemandMapper.findByStatus(DemandStatus.PENDING).size()<=40)
        {
            if (random.nextDouble() < 0.2)
            {
                List<SupplyChainTemplate> templates = supplyChainMapper.findAllTemplates();
                if (templates.isEmpty()) return;
                for (int i = 0; i < 10; i++)
                {
                    // 随机选择一个供应链模板
                    SupplyChainTemplate template = templates.get(random.nextInt(templates.size()));
                    SupplyChainStage firstStage = supplyChainMapper.findStageByTemplateIdAndOrder(template.getId(), 1);

                    if (firstStage != null)
                    {
                        createDemandFromStage(firstStage, null);
                        log.info("成功生成新的运输任务链, 模板: {}", template.getName());
                    }
                }


        }

        }
        else
        {
            log.info("当前任务数过多，请等待");
        }
    }

    /**
     * 当一个任务完成时，触发其在供应链中的下一个任务
     * @param completedDemand 已完成的任务
     */
    public void triggerNextDemand(TransportDemand completedDemand) {
        if (completedDemand.getTemplateId() == null) return;

        int nextStageOrder = completedDemand.getStageOrder() + 1;
        SupplyChainStage nextStage = supplyChainMapper.findStageByTemplateIdAndOrder(completedDemand.getTemplateId(), nextStageOrder);

        if (nextStage != null) {
            createDemandFromStage(nextStage, completedDemand.getDestinationPoiId());
            log.info("供应链任务已触发下一环: TemplateID={}, Stage={}", completedDemand.getTemplateId(), nextStageOrder);
        } else {
            log.info("供应链任务链已全部完成: TemplateID={}", completedDemand.getTemplateId());
        }
    }

    @Override
    public int getCompletedDemandCount() {
        transportDemandMapper.findByStatus(DemandStatus.COMPLETED);
        return transportDemandMapper.findByStatus(DemandStatus.COMPLETED).size();
    }

    @Override
    public void deleteAll() {
        transportDemandMapper.deleteAll();
    }

    private void createDemandFromStage(SupplyChainStage stage, Long fixedOriginPoiId) {
        Poi originPoi;
        // 如果上一环节的终点是固定的，就用它作为本环节的起点
        if (fixedOriginPoiId != null) {
            originPoi = poiMapper.findById(fixedOriginPoiId);
        } else {
            originPoi = getRandomPoiByType(stage.getOriginPoiType());
        }

        Poi destPoi = getRandomPoiByType(stage.getDestinationPoiType());
        if (originPoi != null && destPoi != null && !originPoi.getId().equals(destPoi.getId())) {
            TransportDemand demand = new TransportDemand();
            demand.setOriginPoiId(originPoi.getId());
            demand.setDestinationPoiId(destPoi.getId());
            demand.setCargoName(stage.getCargoName());
            demand.setCargoWeight(stage.getCargoWeight());
            demand.setCargoVolume(stage.getCargoVolume());
            demand.setStatus(DemandStatus.PENDING);
            demand.setTemplateId(stage.getTemplateId());
            demand.setStageOrder(stage.getStageOrder());
            transportDemandMapper.insert(demand);
    }
}

    private Poi getRandomPoiByType(PoiSimType simType) {
        List<Poi> pois = poiMapper.findBySimType(simType);
        if (pois.isEmpty()) return null;
        return pois.get(random.nextInt(pois.size()));
    }

}
