package com.transportation.dispatch.service.impl;

import com.transportation.dispatch.enumeration.DemandStatus;
import com.transportation.dispatch.enumeration.VehicleStatus;
import com.transportation.dispatch.mapper.PoiMapper;
import com.transportation.dispatch.mapper.TransportDemandMapper;
import com.transportation.dispatch.mapper.VehicleMapper;
import com.transportation.dispatch.model.entity.*;
import com.transportation.dispatch.service.DispatchService;
import com.transportation.dispatch.service.VehicleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
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
    @Autowired
    private PoiMapper poiMapper;
    public record AssignmentCandidate(
            TransportDemand demand,
            Vehicle vehicle,
            double weightedCost // 成本，例如：行驶时间 + 惩罚项
    ) {}

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

    @Override
    public void dispatchPendingDemandsByCost(Weight2Dispatch weight2Dispatch) {
        List<TransportDemand> pendingDemands = transportDemandMapper.findByStatus(DemandStatus.PENDING);
        if (pendingDemands.isEmpty()) return;

        List<Vehicle> idleVehicles = vehicleMapper.findByStatus(VehicleStatus.IDLE);
        if (idleVehicles.isEmpty()) return;

        Map<Long, VehicleType> vehicleTypeMap = vehicleMapper.findAllTypes().stream()
                .collect(Collectors.toMap(VehicleType::getId, Function.identity()));

        log.info("开始干预调度 (最小综合成本)... 待处理任务: {}个, 空闲车辆: {}辆", pendingDemands.size(), idleVehicles.size());
        List<AssignmentCandidate> candidates = new ArrayList<>();


        final double WEIGHT_TIME = weight2Dispatch.getWEIGHT_TIME();          // 行驶时间权重
        final double WEIGHT_WASTED_LOAD = weight2Dispatch.getWEIGHT_WASTED_LOAD();   // 浪费载重权重（可能需要标准化）
        final double WEIGHT_WASTED_IDLE = weight2Dispatch.getWEIGHT_WASTED_IDLE();
        final double MAX_REF_LOAD = vehicleMapper.findAllTypes().stream()
                .mapToDouble(type -> type.getMaxLoadWeight().doubleValue())
                .max().orElse(10000.0);

        // --- 阶段 1: 找出所有合格的候选者并计算综合成本 ---
        for (TransportDemand demand : pendingDemands) {

            for (Vehicle vehicle : idleVehicles) {
                double distance1 = calculateDistance(vehicle.getCurrentLng(), vehicle.getCurrentLat(),poiMapper.findById(demand.getOriginPoiId()).getLng(),poiMapper.findById(demand.getDestinationPoiId()).getLat());
                double distance2 = calculateDistance(poiMapper.findById(demand.getOriginPoiId()).getLng(),poiMapper.findById(demand.getDestinationPoiId()).getLng(),poiMapper.findById(demand.getOriginPoiId()).getLat(),poiMapper.findById(demand.getDestinationPoiId()).getLat());
                VehicleType type = vehicleTypeMap.get(vehicle.getTypeId());
                if (type == null) continue;

                // **载重/体积检查**
                boolean canHandle = type.getMaxLoadWeight().compareTo(demand.getCargoWeight()) >= 0 &&
                        type.getMaxLoadVolume().compareTo(demand.getCargoVolume()) >= 0;

                if (canHandle) {

                        // 1. 计算时间成本 (秒)
                        double timeCost = (distance2+distance1)/10;

                        // 2. 计算浪费载重成本 (需要标准化)
                        double wastedLoad = type.getMaxLoadWeight().subtract(demand.getCargoWeight()).doubleValue();

                        // 将浪费载重标准化（除以最大参考载重），并应用权重
                        double wastedLoadCost = (wastedLoad / MAX_REF_LOAD) * WEIGHT_WASTED_LOAD * 3600; // 乘以 3600 将其转换为小时级别的影响
                    wastedLoadCost+=(type.getMaxLoadWeight().doubleValue()/MAX_REF_LOAD)*WEIGHT_WASTED_LOAD*3600;

                        // 3. 计算综合成本 (秒 + 浪费载重折算成的秒)
                        double totalCost = timeCost* WEIGHT_TIME + wastedLoadCost-vehicle.getWaitingDuration()*WEIGHT_WASTED_IDLE;

                        candidates.add(new AssignmentCandidate(demand, vehicle, totalCost));
                    }
                }
            }


        // --- 阶段 2: 排序并分配任务 (逻辑与之前相同) ---

        // 1. 按成本升序排序 (最小成本优先)
        candidates.sort(Comparator.comparing(AssignmentCandidate::weightedCost));

        Set<Long> assignedDemands = new HashSet<>();
        Set<Long> assignedVehicles = new HashSet<>();

        for (AssignmentCandidate candidate : candidates) {
            TransportDemand demand = candidate.demand();
            Vehicle vehicle = candidate.vehicle();

            if (!assignedDemands.contains(demand.getId()) && !assignedVehicles.contains(vehicle.getId())) {

                assignDemandToVehicle(demand, vehicle);

                assignedDemands.add(demand.getId());
                assignedVehicles.add(vehicle.getId());

                log.info("✅ 调度成功 (干预策略): 任务 #{} 分配给车辆 #{}，综合成本: {}。",
                        demand.getId(), vehicle.getId(), String.format("%.2f", candidate.weightedCost()));
            }
        }
    }
    private double calculateDistance( String lng1, String lat1, String lng2, String lat2) {
        double lat3 = Double.parseDouble(lat1);
        double lng3 = Double.parseDouble(lng1);
        double lat4 = Double.parseDouble(lat2);
        double lng4 = Double.parseDouble(lng2);
        double R = 6371000; // 地球半径, 米
        double latDistance = Math.toRadians(lat4 - lat3);
        double lonDistance = Math.toRadians(lng4 - lng3);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat3)) * Math.cos(Math.toRadians(lat4))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

}
