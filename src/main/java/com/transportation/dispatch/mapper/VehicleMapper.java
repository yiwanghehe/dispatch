package com.transportation.dispatch.mapper;

import com.transportation.dispatch.enumeration.VehicleStatus;
import com.transportation.dispatch.model.entity.Vehicle;
import com.transportation.dispatch.model.entity.VehicleType;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VehicleMapper {
    @Insert("INSERT INTO vehicle_type (name, max_load_weight, max_load_volume, carbon_emission_factor) " +
            "VALUES (#{name}, #{maxLoadWeight}, #{maxLoadVolume}, #{carbonEmissionFactor})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertType(VehicleType vehicleType);

    @Select("SELECT * FROM vehicle_type WHERE id = #{id}")
    VehicleType findTypeById(Long id);

    @Select("SELECT * FROM vehicle_type")
    List<VehicleType> findAllTypes();

    @Insert("INSERT INTO vehicle (plate_number, type_id, status, current_lng, current_lat, last_update_time) " +
            "VALUES (#{plateNumber}, #{typeId}, #{status}, #{currentLng}, #{currentLat}, NOW())")
    void insert(Vehicle vehicle);

    @Select("SELECT * FROM vehicle")
    List<Vehicle> findAll();

    /**
     * 【新增】根据状态查询车辆列表
     */
    @Select("SELECT * FROM vehicle WHERE status = #{status}")
    List<Vehicle> findByStatus(VehicleStatus status);

    /**
     * 【新增】更新车辆的状态和位置信息
     */
    @Update("UPDATE vehicle SET status = #{status}, current_lng = #{currentLng}, current_lat = #{currentLat}, " +
            "current_demand_id = #{currentDemandId}, last_update_time = NOW() ,  speed=#{speed} , last_reached_path_index=#{lastReachedPathIndex} WHERE id = #{id}")
    void update(Vehicle vehicle);
}
