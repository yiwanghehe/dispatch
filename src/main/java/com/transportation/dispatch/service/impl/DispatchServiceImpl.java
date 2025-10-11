package com.transportation.dispatch.service.impl;

import com.transportation.dispatch.enumeration.DemandStatus;
import com.transportation.dispatch.enumeration.VehicleStatus;
import com.transportation.dispatch.mapper.TransportDemandMapper;
import com.transportation.dispatch.mapper.VehicleMapper;
import com.transportation.dispatch.model.entity.TransportDemand;
import com.transportation.dispatch.model.entity.Vehicle;
import com.transportation.dispatch.model.entity.VehicleType;
import com.transportation.dispatch.service.DispatchService;
import com.transportation.dispatch.service.VehicleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
public class DispatchServiceImpl implements DispatchService {
    @Autowired
    private TransportDemandMapper transportDemandMapper;
    @Autowired
    private VehicleMapper vehicleMapper;
    @Autowired
    private VehicleService vehicleService;

    /**
     * 调度主方法：为所有待处理的任务分配车辆（无干预策略）
     */
    @Override
    public void dispatchPendingDemands() {
        List<TransportDemand> pendingDemands = transportDemandMapper.findByStatus(DemandStatus.PENDING);
        if (pendingDemands.isEmpty()) return;

        List<Vehicle> idleVehicles = vehicleMapper.findByStatus(VehicleStatus.IDLE);
        if (idleVehicles.isEmpty()) return;

        // 预加载所有车型信息，避免循环查询
        Map<Long, VehicleType> vehicleTypeMap = vehicleMapper.findAllTypes().stream()
                .collect(Collectors.toMap(VehicleType::getId, Function.identity()));

        log.info("开始调度... 待处理任务: {}个, 空闲车辆: {}辆", pendingDemands.size(), idleVehicles.size());

        for (TransportDemand demand : pendingDemands) {

            for (Vehicle vehicle : idleVehicles) {
                VehicleType type = vehicleTypeMap.get(vehicle.getTypeId());
                if (type == null) continue;

                boolean canHandle = type.getMaxLoadWeight().compareTo(demand.getCargoWeight()) >= 0 &&
                        type.getMaxLoadVolume().compareTo(demand.getCargoVolume()) >= 0;

                if (canHandle) {
                    // 分配任务
                    assignDemandToVehicle(demand, vehicle);
                    // 将此车辆从空闲列表中移除，防止被再次分配
                    idleVehicles.remove(vehicle);
                    break; // 跳出内层循环，处理下一个任务
                }
            }
        }
    }

    private void assignDemandToVehicle(TransportDemand demand, Vehicle vehicle) {
        // 更新任务状态
        demand.setStatus(DemandStatus.ASSIGNED);
        demand.setAssignedVehicleId(vehicle.getId());
        demand.setAssignmentTime(LocalDateTime.now());
        transportDemandMapper.update(demand);

        // 更新车辆状态
        vehicle.setStatus(VehicleStatus.MOVING_TO_PICKUP);
        vehicle.setCurrentDemandId(demand.getId());
        vehicle.setSpeed(10.0);
        vehicleMapper.update(vehicle);

        log.info("任务 #{} 已成功分配给车辆 #{} ({})", demand.getId(), vehicle.getId(), vehicle.getPlateNumber());
    }
}
