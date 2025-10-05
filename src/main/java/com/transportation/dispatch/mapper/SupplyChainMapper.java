package com.transportation.dispatch.mapper;

import com.transportation.dispatch.model.entity.SupplyChainStage;
import com.transportation.dispatch.model.entity.SupplyChainTemplate;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SupplyChainMapper {

    @Insert("INSERT INTO supply_chain_template (name, description) VALUES (#{name}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertTemplate(SupplyChainTemplate template);

    @Insert("INSERT INTO supply_chain_stage (template_id, stage_order, origin_poi_type, destination_poi_type, cargo_name, cargo_weight, cargo_volume) " +
            "VALUES (#{templateId}, #{stageOrder}, #{originPoiType}, #{destinationPoiType}, #{cargoName}, #{cargoWeight}, #{cargoVolume})")
    void insertStage(SupplyChainStage stage);

    @Select("SELECT COUNT(*) FROM supply_chain_template")
    long countTemplates();

    @Select("SELECT * FROM supply_chain_stage ORDER BY template_id, stage_order")
    List<SupplyChainStage> findAllStages();
}
