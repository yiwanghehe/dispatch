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
import java.util.ArrayList;
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
    private DemandService demandService;
    @Autowired
    private RouteService routeService;

    private static final long LOADING_DURATION_SECONDS = 5 * 60; // 5分钟
    private static final long UNLOADING_DURATION_SECONDS = 5 * 60; // 5分钟

    @Override
    public List<Vehicle> getAllVehicles() {
        // 此方法逻辑保持不变，继续为前端提供数据
        List<Vehicle> vehicles = vehicleMapper.findAll();
        if (vehicles.stream().anyMatch(v -> v.getCurrentDemandId() != null)) {
            List<Long> demandIds = vehicles.stream()
                    .map(Vehicle::getCurrentDemandId)
                    .filter(Objects::nonNull)
                    .distinct()
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
                                // 根据车辆当前状态，决定要显示哪段路径
                                String routeStart = vehicle.getStatus() == VehicleStatus.IN_TRANSIT ? originCoords : vehicle.getCurrentLng() + "," + vehicle.getCurrentLat();
                                String routeEnd = vehicle.getStatus() == VehicleStatus.IN_TRANSIT ? destCoords : originCoords;
                                RouteCache route = routeCacheMapper.findByOriginAndDestination(routeStart, routeEnd);
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
                    break;
            }
        }
    }

    private void updateMovingVehiclePosition(Vehicle vehicle, long simulationTime) {
        if (vehicle.getParsedPolyline() == null) {
            initializeVehicleRoute(vehicle, simulationTime);
            if (vehicle.getParsedPolyline() == null) return;
        }

        long timeElapsed = simulationTime - vehicle.getActionStartTime();

        if (timeElapsed >= vehicle.getRouteDuration()) {
            String[] finalCoords = vehicle.getParsedPolyline().get(vehicle.getParsedPolyline().size() - 1);
            vehicle.setCurrentLng(finalCoords[0]);
            vehicle.setCurrentLat(finalCoords[1]);
            vehicle.setTraveledPolyline(vehicle.getRoutePolyline());

            if (vehicle.getStatus() == VehicleStatus.MOVING_TO_PICKUP) {
                vehicle.setStatus(VehicleStatus.LOADING);
                vehicle.setActionStartTime(simulationTime);
                log.info("车辆 #{} 到达装货点，开始装货。", vehicle.getId());
                TransportDemand demand = transportDemandMapper.findByIds(List.of(vehicle.getCurrentDemandId())).get(0);
                demand.setPickupTime(LocalDateTime.now());
                transportDemandMapper.update(demand);
            } else {
                vehicle.setStatus(VehicleStatus.UNLOADING);
                vehicle.setActionStartTime(simulationTime);
                log.info("车辆 #{} 到达卸货点，开始卸货。", vehicle.getId());
            }
        } else {
            double progress = (double) timeElapsed / vehicle.getRouteDuration();
            updateTraveledPathAndPosition(vehicle, progress);
        }
        vehicleMapper.update(vehicle);
    }

    private void updateLoadingVehicle(Vehicle vehicle, long simulationTime) {
        if (simulationTime - vehicle.getActionStartTime() >= LOADING_DURATION_SECONDS) {
            vehicle.setStatus(VehicleStatus.IN_TRANSIT);
            initializeVehicleRoute(vehicle, simulationTime);
            log.info("车辆 #{} 装货完成，开始前往目的地。", vehicle.getId());
            vehicleMapper.update(vehicle);
        }
    }

    private void updateUnloadingVehicle(Vehicle vehicle, long simulationTime) {
        if (simulationTime - vehicle.getActionStartTime() >= UNLOADING_DURATION_SECONDS) {
            TransportDemand demand = transportDemandMapper.findByIds(List.of(vehicle.getCurrentDemandId())).get(0);
            demand.setStatus(DemandStatus.COMPLETED);
            demand.setCompletionTime(LocalDateTime.now());
            transportDemandMapper.update(demand);
            log.info("车辆 #{} 卸货完成，任务 #{} 结束，车辆变为空闲。", vehicle.getId(), demand.getId());

            vehicle.setStatus(VehicleStatus.IDLE);
            vehicle.setCurrentDemandId(null);
            vehicle.setRoutePolyline(null);
            vehicle.setParsedPolyline(null);
            vehicle.setTraveledPolyline(null);
            vehicleMapper.update(vehicle);

            demandService.triggerNextDemand(demand);
        }
    }

    private void initializeVehicleRoute(Vehicle vehicle, long simulationTime) {
        // [FIX] 增加健壮性检查，防止因ID为null导致后续崩溃
        if (vehicle.getCurrentDemandId() == null) {
            log.error("车辆 #{} 状态为 {} 但没有关联的任务ID！重置为空闲状态。", vehicle.getId(), vehicle.getStatus());
            resetToIdle(vehicle);
            return;
        }

        List<TransportDemand> demands = transportDemandMapper.findByIds(List.of(vehicle.getCurrentDemandId()));
        // [FIX] 检查返回的列表是否为空，防止IndexOutOfBoundsException
        if (demands.isEmpty()) {
            log.error("在数据库中找不到ID为 {} 的任务，车辆 #{} 将重置为空闲状态。", vehicle.getCurrentDemandId(), vehicle.getId());
            resetToIdle(vehicle);

            return;
        }
        TransportDemand demand = demands.get(0);

        String originCoords, destCoords;

        if (vehicle.getStatus() == VehicleStatus.MOVING_TO_PICKUP) {
            originCoords = vehicle.getCurrentLng() + "," + vehicle.getCurrentLat();
            destCoords = transportDemandMapper.findPoiCoordsById(demand.getOriginPoiId());
        } else { // IN_TRANSIT
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
                vehicle.setActionStartTime(simulationTime);
                vehicle.setTraveledPolyline(originCoords); // 初始化已行驶轨迹为起点
            } else {
                log.error("无法为车辆 #{} 初始化路径 ({} -> {}), 路径服务返回null!", vehicle.getId(), originCoords, destCoords);
            }
        }
    }

    /**
     * [CORE UPGRADE] 根据行驶进度，更新已行驶轨迹和当前精确位置
     */
    private void updateTraveledPathAndPosition(Vehicle vehicle, double progress) {
        List<String[]> fullPath = vehicle.getParsedPolyline();
        if (fullPath == null || fullPath.size() < 2) return;

        double totalDistance = vehicle.getRouteDistance();
        double distanceCovered = totalDistance * progress;

        List<String[]> newTraveledPath = new ArrayList<>();
        newTraveledPath.add(fullPath.get(0));

        double cumulativeDistance = 0;

        for (int i = 0; i < fullPath.size() - 1; i++) {
            String[] startPointStr = fullPath.get(i);
            String[] endPointStr = fullPath.get(i + 1);

            double startLng = Double.parseDouble(startPointStr[0]);
            double startLat = Double.parseDouble(startPointStr[1]);
            double endLng = Double.parseDouble(endPointStr[0]);
            double endLat = Double.parseDouble(endPointStr[1]);

            double segmentDistance = calculateDistance(startLng, startLat, endLng, endLat);

            if (cumulativeDistance + segmentDistance <= distanceCovered) {
                newTraveledPath.add(endPointStr);
                cumulativeDistance += segmentDistance;
                // 如果这是路径的最后一段，并且刚好走完，将车辆位置设置为终点
                if (i == fullPath.size() - 2) {
                    vehicle.setCurrentLng(endPointStr[0]);
                    vehicle.setCurrentLat(endPointStr[1]);
                }
            } else {
                double remainingDistance = distanceCovered - cumulativeDistance;
                double fraction = (segmentDistance > 0) ? remainingDistance / segmentDistance : 0;

                double currentLng = startLng + (endLng - startLng) * fraction;
                double currentLat = startLat + (endLat - startLat) * fraction;

                String[] currentPos = {String.format("%.6f", currentLng), String.format("%.6f", currentLat)};
                newTraveledPath.add(currentPos);

                vehicle.setCurrentLng(currentPos[0]);
                vehicle.setCurrentLat(currentPos[1]);
                break;
            }
        }

        vehicle.setTraveledPolyline(newTraveledPath.stream()
                .map(p -> p[0] + "," + p[1])
                .collect(Collectors.joining(";")));
    }

    private void resetToIdle(Vehicle vehicle) {
        vehicle.setStatus(VehicleStatus.IDLE);
        vehicle.setCurrentDemandId(null);
        vehicle.setRoutePolyline(null);
        vehicle.setParsedPolyline(null);
        vehicle.setTraveledPolyline(null);
        vehicleMapper.update(vehicle);
    }

    private double calculateDistance(double lng1, double lat1, double lng2, double lat2) {
        double R = 6371000; // 地球半径, 米
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}

