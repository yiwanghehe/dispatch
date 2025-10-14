package com.transportation.dispatch.service;

import com.transportation.dispatch.enumeration.VehicleStatus;
import com.transportation.dispatch.model.dto.VehicleDto;
import com.transportation.dispatch.model.entity.Vehicle;

import java.util.List;

public interface VehicleService {

    /**
     * 获取所有车辆的当前实时状态。
     * 在仿真过程中，这个方法会返回每辆车的最新位置、状态以及当前任务的完整路径。
     * @return 包含所有车辆信息的列表
     */
    List<VehicleDto> getVehicles(VehicleStatus vehicleStatus);

    /**
     * 更新所有车辆的当前实时状态。
     */
    void updateAllVehiclesState(long simulationTime, int timeStep);
    void updateVehicleSpeed(Long vehicleId, double speed);
    Double getTotalWastedCapacity();

    Double getTotalLoadDistance();


    Double getTotalWaitingDuration();
     Double getTotalNoLoadDistance();
     Double getTotalDuration();

    void resetAllVehicles();
}
