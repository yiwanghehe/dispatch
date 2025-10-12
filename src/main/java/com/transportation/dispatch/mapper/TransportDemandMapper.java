package com.transportation.dispatch.mapper;

import com.transportation.dispatch.enumeration.DemandStatus;
import com.transportation.dispatch.model.entity.TransportDemand;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface TransportDemandMapper {

    @Insert("INSERT INTO transport_demand (origin_poi_id, destination_poi_id, cargo_name, cargo_weight, cargo_volume, status, template_id, stage_order, creation_time) " +
            "VALUES (#{originPoiId}, #{destinationPoiId}, #{cargoName}, #{cargoWeight}, #{cargoVolume}, #{status}, #{templateId}, #{stageOrder}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(TransportDemand demand);

    /**
     * 根据任务ID列表批量查询任务信息。
     */
    @Select("<script>" +
            "SELECT * FROM transport_demand WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<TransportDemand> findByIds(@Param("ids") List<Long> ids);

    /**
     * 根据POI ID查询其坐标。
     */
    @Select("SELECT CONCAT(lng, ',', lat) FROM poi WHERE id = #{poiId}")
    String findPoiCoordsById(@Param("poiId") Long poiId);

    /**
     * 【新增】根据状态查询任务列表
     */
    @Select("SELECT * FROM transport_demand WHERE status = #{status}")
    List<TransportDemand> findByStatus(DemandStatus status);

    /**
     * 【新增】更新任务的状态和分配信息
     */
    @Update("UPDATE transport_demand SET status = #{status}, assigned_vehicle_id = #{assignedVehicleId}, assignment_time = #{assignmentTime}, " +
            "pickup_time = #{pickupTime}, completion_time = #{completionTime} WHERE id = #{id}")
    void update(TransportDemand demand);
    @Select("SELECT cargo_weight FROM transport_demand WHERE id=#{id}")
    BigDecimal findCargoWeightById(Long id);
}
