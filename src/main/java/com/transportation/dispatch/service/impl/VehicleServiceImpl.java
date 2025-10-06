package com.transportation.dispatch.service.impl;

import com.transportation.dispatch.enumeration.DemandStatus;
import com.transportation.dispatch.enumeration.VehicleStatus;
import com.transportation.dispatch.mapper.RouteCacheMapper;
import com.transportation.dispatch.mapper.TransportDemandMapper;
import com.transportation.dispatch.mapper.VehicleMapper;
import com.transportation.dispatch.model.entity.RouteCache;
import com.transportation.dispatch.model.entity.TransportDemand;
import com.transportation.dispatch.model.entity.Vehicle;
import com.transportation.dispatch.service.DemandService;
import com.transportation.dispatch.service.RouteService;
import com.transportation.dispatch.service.VehicleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VehicleServiceImpl implements VehicleService {

    @Autowired
    private VehicleMapper vehicleMapper;
    @Autowired
    private TransportDemandMapper transportDemandMapper;
    @Autowired
    private RouteCacheMapper routeCacheMapper;
    @Autowired
    private DemandService demandService; // 注入需求服务用于触发下一环

    @Autowired
    private RouteService routeService;

    // 模拟装卸货所需的时间（秒）
    private static final long LOADING_DURATION_SECONDS = 5 * 60; // 5分钟
    private static final long UNLOADING_DURATION_SECONDS = 5 * 60; // 5分钟

    /**
     * 【核心】更新所有车辆的状态和位置
     * @param simulationTime 当前仿真总时间（秒）
     * @param timeStep 本次tick的时间步长（秒）
     */
    @Override
    public void updateAllVehiclesState(long simulationTime, int timeStep) {
        List<Vehicle> allVehicles = vehicleMapper.findAll();

        for (Vehicle vehicle : allVehicles) {
            switch (vehicle.getStatus()) {
                case MOVING_TO_PICKUP:
                case IN_TRANSIT:
                    updateMovingVehiclePosition(vehicle, simulationTime);
                    break;
                case LOADING:
                    updateLoadingVehicle(vehicle, simulationTime);
                    break;
                case UNLOADING:
                    updateUnloadingVehicle(vehicle, simulationTime);
                    break;
                default:
                    // IDLE, MAINTENANCE, OFFLINE等状态暂时不处理
                    break;
            }
        }
    }

    private void updateMovingVehiclePosition(Vehicle vehicle, long simulationTime) {
        // 首次进入移动状态时，需要初始化路径信息
        if (vehicle.getParsedPolyline() == null) {
            initializeVehicleRoute(vehicle, simulationTime);
            if(vehicle.getParsedPolyline() == null) return; // 初始化失败则跳过
        }

        long timeElapsed = simulationTime - vehicle.getActionStartTime();

        // 检查是否到达目的地
        if (timeElapsed >= vehicle.getRouteDuration()) {
            // 已到达
            String[] finalCoords = vehicle.getParsedPolyline().get(vehicle.getParsedPolyline().size() - 1);
            vehicle.setCurrentLng(finalCoords[0]);
            vehicle.setCurrentLat(finalCoords[1]);

            if (vehicle.getStatus() == VehicleStatus.MOVING_TO_PICKUP) {
                // 到达装货点
                vehicle.setStatus(VehicleStatus.LOADING);
                vehicle.setActionStartTime(simulationTime); // 重置行为开始时间
                log.info("车辆 #{} 到达装货点，开始装货。", vehicle.getId());
                // 更新任务的pickup_time
                TransportDemand demand = transportDemandMapper.findByIds(List.of(vehicle.getCurrentDemandId())).get(0);
                demand.setPickupTime(LocalDateTime.now());
                transportDemandMapper.update(demand);
            } else { // IN_TRANSIT
                // 到达卸货点
                vehicle.setStatus(VehicleStatus.UNLOADING);
                vehicle.setActionStartTime(simulationTime);
                log.info("车辆 #{} 到达卸货点，开始卸货。", vehicle.getId());
            }
        } else {
            // 仍在途中，计算当前位置
            double progress = (double) timeElapsed / vehicle.getRouteDuration();
            long traveledDistance = (long) (vehicle.getRouteDistance() * progress);

            // 根据行驶距离计算在路径上的新坐标点 (这是一个简化的线性插值)
            String[] newCoords = calculatePositionOnPolyline(vehicle.getParsedPolyline(), traveledDistance);
            vehicle.setCurrentLng(newCoords[0]);
            vehicle.setCurrentLat(newCoords[1]);
        }
        vehicleMapper.update(vehicle);
    }

    private void updateLoadingVehicle(Vehicle vehicle, long simulationTime) {
        if (simulationTime - vehicle.getActionStartTime() >= LOADING_DURATION_SECONDS) {
            vehicle.setStatus(VehicleStatus.IN_TRANSIT);
            // 重新初始化路径，这次是去往终点
            initializeVehicleRoute(vehicle, simulationTime);
            log.info("车辆 #{} 装货完成，开始前往目的地。", vehicle.getId());
            vehicleMapper.update(vehicle);
        }
    }

    private void updateUnloadingVehicle(Vehicle vehicle, long simulationTime) {
        if (simulationTime - vehicle.getActionStartTime() >= UNLOADING_DURATION_SECONDS) {
            TransportDemand demand = transportDemandMapper.findByIds(List.of(vehicle.getCurrentDemandId())).get(0);

            // 任务完成
            demand.setStatus(DemandStatus.COMPLETED);
            demand.setCompletionTime(LocalDateTime.now());
            transportDemandMapper.update(demand);

            // 车辆变回空闲
            vehicle.setStatus(VehicleStatus.IDLE);
            vehicle.setCurrentDemandId(null);
            vehicle.setRoutePolyline(null); // 清理路径信息
            vehicle.setParsedPolyline(null);
            log.info("车辆 #{} 卸货完成，任务 #{} 结束，车辆变为空闲。", vehicle.getId(), demand.getId());
            vehicleMapper.update(vehicle);

            // 【关键】触发供应链下一环
            demandService.triggerNextDemand(demand);
        }
    }

    private void initializeVehicleRoute(Vehicle vehicle, long simulationTime) {
        TransportDemand demand = transportDemandMapper.findByIds(List.of(vehicle.getCurrentDemandId())).get(0);
        String originCoords, destCoords;

        if (vehicle.getStatus() == VehicleStatus.MOVING_TO_PICKUP) {
            // 去装货点
            originCoords = vehicle.getCurrentLng() + "," + vehicle.getCurrentLat();
            destCoords = transportDemandMapper.findPoiCoordsById(demand.getOriginPoiId());
        } else { // IN_TRANSIT
            // 去卸货点
            originCoords = transportDemandMapper.findPoiCoordsById(demand.getOriginPoiId());
            destCoords = transportDemandMapper.findPoiCoordsById(demand.getDestinationPoiId());
        }

        if (originCoords != null && destCoords != null) {
            RouteCache route = routeService.getRoute(originCoords, destCoords);
            if (route != null) {
                vehicle.setRoutePolyline(route.getPolyline());
                vehicle.setParsedPolyline(Arrays.stream(route.getPolyline().split(";")).map(s -> s.split(",")).collect(Collectors.toList()));
                vehicle.setRouteDistance(route.getDistance());
                vehicle.setRouteDuration(route.getDuration());
                vehicle.setActionStartTime(simulationTime); // 设置行为开始时间
            } else {
                log.error("无法为车辆 #{} 初始化路径 ({} -> {}), 路径缓存未命中!", vehicle.getId(), originCoords, destCoords);
            }
        }
    }

    // 简化版位置计算，实际项目中可使用更精确的地理库
    private String[] calculatePositionOnPolyline(List<String[]> polyline, long traveledDistance) {
        // 这是一个简化实现，它返回路径中间点的坐标
        // 实际项目需要根据距离比例在分段上做线性插值，会更复杂
        if (polyline == null || polyline.isEmpty()) return new String[]{"0","0"};
        return polyline.get(polyline.size() / 2); // 简化处理
    }

    /**
     * 获取所有车辆的当前实时状态，并为其动态关联路径信息。(此方法保持不变，用于前端展示)
     */
    @Override
    public List<Vehicle> getAllVehicles() {
        // 1. 从数据库获取所有车辆的基本信息
        List<Vehicle> vehicles = vehicleMapper.findAll();
        // ... (此处代码与之前版本相同，为了简洁省略)
        if (vehicles.stream().anyMatch(v -> v.getCurrentDemandId() != null)) {
            List<Long> demandIds = vehicles.stream()
                    .filter(v -> v.getCurrentDemandId() != null)
                    .map(Vehicle::getCurrentDemandId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if(!demandIds.isEmpty()) {
                Map<Long, TransportDemand> demandMap = transportDemandMapper.findByIds(demandIds)
                        .stream().collect(Collectors.toMap(TransportDemand::getId, d -> d));
                for (Vehicle vehicle : vehicles) {
                    if (vehicle.getCurrentDemandId() != null) {
                        TransportDemand demand = demandMap.get(vehicle.getCurrentDemandId());
                        if (demand != null) {
                            String originCoords = transportDemandMapper.findPoiCoordsById(demand.getOriginPoiId());
                            String destCoords = transportDemandMapper.findPoiCoordsById(demand.getDestinationPoiId());
                            if (originCoords != null && destCoords != null) {
                                RouteCache route = routeCacheMapper.findByOriginAndDestination(originCoords, destCoords);
                                if (route != null) {
                                    vehicle.setRoutePolyline(route.getPolyline());
                                }
                            }
                        }
                    }
                }
            }
        }
        return vehicles;
    }
}
