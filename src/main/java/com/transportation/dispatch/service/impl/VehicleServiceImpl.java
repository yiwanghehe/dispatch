package com.transportation.dispatch.service.impl;

import com.transportation.dispatch.enumeration.DemandStatus;
import com.transportation.dispatch.enumeration.VehicleStatus;
import com.transportation.dispatch.mapper.RouteCacheMapper;
import com.transportation.dispatch.mapper.TransportDemandMapper;
import com.transportation.dispatch.mapper.VehicleMapper;
import com.transportation.dispatch.model.dto.VehicleDto;
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
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<Long, Vehicle> runtimeVehicleCache = new ConcurrentHashMap<>();

    private static final long LOADING_DURATION_SECONDS = 5 * 60; // 5分钟
    private static final long UNLOADING_DURATION_SECONDS = 5 * 60; // 5分钟

    @Override
    public List<VehicleDto> getVehicles(VehicleStatus status) {
        // 此方法逻辑保持不变，继续为前端提供数据
        List<Vehicle> vehicles = runtimeVehicleCache.values().stream()
                .filter(v -> status == null || v.getStatus() == status)
                .collect(Collectors.toList());

        if (vehicles.stream().anyMatch(v -> v.getCurrentDemandId() != null)) {
            List<Long> demandIds = vehicles.stream()
                    .map(Vehicle::getCurrentDemandId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (!demandIds.isEmpty()) {
                Map<Long, TransportDemand> demandMap = transportDemandMapper.findByIds(demandIds)
                        .stream().collect(Collectors.toMap(TransportDemand::getId, d -> d));
                for (Vehicle vehicle : vehicles) {
                    if (vehicle.getCurrentDemandId() != null) {
                        TransportDemand demand = demandMap.get(vehicle.getCurrentDemandId());
                        if (demand != null) {
                            // POI 坐标也应该标准化，确保和缓存键一致
                            String normalizedOrigin = routeService.normalizeCoords(transportDemandMapper.findPoiCoordsById(demand.getOriginPoiId()));
                            String normalizedDest = routeService.normalizeCoords(transportDemandMapper.findPoiCoordsById(demand.getDestinationPoiId()));

                            if (normalizedOrigin != null && normalizedDest != null) {

                                String rawStart = vehicle.getStatus() == VehicleStatus.IN_TRANSIT ? normalizedOrigin : vehicle.getCurrentLng() + "," + vehicle.getCurrentLat();
                                String routeEnd = vehicle.getStatus() == VehicleStatus.IN_TRANSIT ? normalizedDest : normalizedOrigin;

                                // 【关键修改点】：标准化查询键
                                String normalizedRouteStart = routeService.normalizeCoords(rawStart);

                                RouteCache route = routeCacheMapper.findByOriginAndDestination(normalizedRouteStart, routeEnd); // routeEnd已经是标准化后的POI坐标

                                if (route != null) {
                                    vehicle.setRoutePolyline(route.getPolyline());
                                }
                            }
                        }
                    }
                }
            }
        }
        return vehicles.stream()
                .map(v -> {
                    VehicleDto dto = new VehicleDto();
                    dto.setId(v.getId());
                    dto.setPlateNumber(v.getPlateNumber());
                    dto.setTypeId(v.getTypeId());
                    dto.setStatus(v.getStatus());
                    dto.setCurrentLng(v.getCurrentLng());
                    dto.setCurrentLat(v.getCurrentLat());

                    // 【核心修正】：新增对 traveledPolyline 的赋值
                    dto.setTraveledPolyline(v.getTraveledPolyline());
                    dto.setRouteDistance(v.getRouteDistance());
                    dto.setRouteDuration(v.getRouteDuration());
                    dto.setActionStartTime(v.getActionStartTime());
                    dto.setCurrentDemandId(v.getCurrentDemandId());
                    return dto;
                }).collect(Collectors.toList());
    }


    @Override
    public void updateAllVehiclesState(long simulationTime, int timeStep) {
        // 1. 从数据库获取最新状态列表（仅用于同步新增/修改）
        List<Vehicle> allVehiclesDb = vehicleMapper.findAll();

        // 2. 将数据库最新数据同步到内存缓存中（确保新的车辆或状态更新被捕获）
        for (Vehicle dbVehicle : allVehiclesDb) {
            Vehicle cacheVehicle = runtimeVehicleCache.get(dbVehicle.getId());

            if (cacheVehicle == null) {
                runtimeVehicleCache.put(dbVehicle.getId(), dbVehicle);
            } else {

                String[] ignoreProperties = {
                        "parsedPolyline",
                        "routeDuration",
                        "routeDistance",
                        "routePolyline",
                        "traveledPolyline",
                        "currentLng",
                        "currentLat",
                        "lastReachedPathIndex"
                };

                org.springframework.beans.BeanUtils.copyProperties(
                        dbVehicle,
                        cacheVehicle,
                        ignoreProperties
                );

            }
        }

        // 3. 遍历【内存缓存】进行状态更新
        List<Vehicle> activeVehicles = new ArrayList<>(runtimeVehicleCache.values());
        for (Vehicle vehicle : activeVehicles) { // 遍历内存中的 vehicle

            // 只有 MOVING, LOADING, UNLOADING 状态才需要更新
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
                    // 如果车辆变为空闲，在下面进行清理
                    break;
            }


        }

        // 5. 【新增】清理不再需要的 IDLE 车辆对象，减少内存占用
        runtimeVehicleCache.entrySet().removeIf(entry ->
                entry.getValue().getStatus() == VehicleStatus.IDLE
        );
    }

    @Override
    public void updateVehicleSpeed(Long vehicleId, double speed) {
        Vehicle vehicle = runtimeVehicleCache.get(vehicleId);
        vehicle.setSpeed(speed);
        vehicleMapper.update(vehicle);
        log.info("车辆 #{} 的速度已更新为 {} m/s", vehicleId, speed);
    }

    private void updateMovingVehiclePosition(Vehicle vehicle, long simulationTime) {
        if (vehicle.getRoutePolyline() == null) {
            initializeVehicleRoute(vehicle, simulationTime);

            if (vehicle.getParsedPolyline() == null)
                return;
        }
        long timeStepSeconds = 60;
        double distanceTraveledInThisStep = vehicle.getSpeed() * timeStepSeconds;
        boolean reachedDestination = updateTraveledPathAndPositionByDistance(
                vehicle,
                distanceTraveledInThisStep
        );
        // 【到达终点的判断】
        if (reachedDestination) {
            String[] finalCoords = vehicle.getParsedPolyline().get(vehicle.getParsedPolyline().size() - 1);
            vehicle.setCurrentLng(finalCoords[0]);
            vehicle.setCurrentLat(finalCoords[1]);
            vehicle.setTraveledPolyline(vehicle.getRoutePolyline());

            // ... (状态切换逻辑保持不变) ...
            if (vehicle.getStatus() == VehicleStatus.MOVING_TO_PICKUP) {
                vehicle.setStatus(VehicleStatus.LOADING);
                vehicle.setActionStartTime(simulationTime);
                vehicle.setSpeed(0.0);
                vehicle.setLastReachedPathIndex(null);
                log.info("车辆 #{} 到达装货点，开始装货。", vehicle.getId());
                TransportDemand demand = transportDemandMapper.findByIds(List.of(vehicle.getCurrentDemandId())).get(0);
                demand.setPickupTime(LocalDateTime.now());
                transportDemandMapper.update(demand);
            } else {
                vehicle.setStatus(VehicleStatus.UNLOADING);
                vehicle.setActionStartTime(simulationTime);
                vehicle.setSpeed(0.0);
                vehicle.setLastReachedPathIndex(null);
                log.info("车辆 #{} 到达卸货点，开始卸货。", vehicle.getId());
            }
        } else {
//            log.info("车辆 #{} 正在移动中。当前坐标为 #{} , #{}", vehicle.getId(), vehicle.getCurrentLng(), vehicle.getCurrentLat());
        }

        // 每次更新都进行数据库同步
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
            vehicle.setSpeed(0.0);
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
            // 【关键修改点】：使用标准化方法处理车辆当前位置
            String rawOrigin = vehicle.getCurrentLng() + "," + vehicle.getCurrentLat();
            originCoords = routeService.normalizeCoords(rawOrigin);
            // 目标 POI 坐标也应该标准化，以确保目标 POI 的缓存键一致
            String rawDest = transportDemandMapper.findPoiCoordsById(demand.getOriginPoiId());
            destCoords = routeService.normalizeCoords(rawDest);
            log.info("车辆 #{} 正在前往装货点 {}，当前位置 {}", vehicle.getId(), originCoords, rawOrigin);

        } else { // IN_TRANSIT
            // 假设 POI 坐标已标准化或在 RouteService.getRoute 中会被标准化
            originCoords = transportDemandMapper.findPoiCoordsById(demand.getOriginPoiId());
            destCoords = transportDemandMapper.findPoiCoordsById(demand.getDestinationPoiId());

            // 再次强制标准化，防止 POI 数据库中数据精度不一致
            originCoords = routeService.normalizeCoords(originCoords);
            destCoords = routeService.normalizeCoords(destCoords);
        }

        if (originCoords != null && destCoords != null) {
            RouteCache route = routeService.getRoute(originCoords, destCoords);
            if (route != null) {
                vehicle.setRoutePolyline(route.getPolyline());
                vehicle.setParsedPolyline(parsePolyline(route.getPolyline()));
                System.out.println("parsedPolyline: " + route.getPolyline());
                vehicle.setRouteDistance(route.getDistance());
                vehicle.setRouteDuration(route.getDuration());
                vehicle.setActionStartTime(simulationTime);
                vehicle.setTraveledPolyline(originCoords); // 初始化已行驶轨迹为起点
                vehicle.setSpeed(10.0);
                vehicleMapper.update(vehicle);
                log.info("车辆 #{} 初始化路径 ({} -> {}) 成功，总距离 {}，总时长 {}", vehicle.getId(), originCoords, destCoords, route.getDistance(), route.getDuration());
            } else {
                log.error("无法为车辆 #{} 初始化路径 ({} -> {}), 路径服务返回null!", vehicle.getId(), originCoords, destCoords);
            }
        }
    }

    /**
     * [CORE UPGRADE] 根据行驶进度，更新已行驶轨迹和当前精确位置
     */
    /**
     * 根据车辆在当前Tick内行驶的距离，更新其位置和已行驶轨迹。
     *
     * @param vehicle 车辆对象
     * @return boolean 是否到达终点
     */
    private boolean updateTraveledPathAndPositionByDistance(Vehicle vehicle, double distanceToCover) {
        List<String[]> fullPath = vehicle.getParsedPolyline();
        if (fullPath == null || fullPath.size() < 2) return false;

        // 1. 获取起点索引 (基于上一次移动的结果)
        // 如果是第一次移动，它会是 null 或 0
        int startIndex = (vehicle.getLastReachedPathIndex() == null || vehicle.getLastReachedPathIndex() < 0)
                ? 0
                : vehicle.getLastReachedPathIndex();
        if (startIndex >= fullPath.size() - 1)
            return true;
        // 2. 初始化本次移动的起始精确坐标（就是上一次 Tick 停止的精确坐标）
        double currentLng = Double.parseDouble(vehicle.getCurrentLng());
        double currentLat = Double.parseDouble(vehicle.getCurrentLat());

        double remainingDistance = distanceToCover;
        StringBuilder newTraveledPolyline = new StringBuilder(vehicle.getTraveledPolyline());

        // 3. 循环从上一个已知节点 i 开始
        // i 是当前路段的起点节点索引
        for (int i = startIndex; i < fullPath.size() - 1; i++) {

            String[] endPointStr = fullPath.get(i + 1);
            double endLng = Double.parseDouble(endPointStr[0]);
            double endLat = Double.parseDouble(endPointStr[1]);

            // 计算当前路段 (从车辆当前精确位置到下一个路径点 i+1) 的剩余距离
            double distanceToNextNode = calculateDistance(currentLng, currentLat, endLng, endLat);

            // 【容错处理】防止 i == startIndex 且 distanceToNextNode 接近于零的情况
            // 这种情况意味着上一次 Tick 已经将车辆位置精确更新为 fullPath.get(i+1)
            if (distanceToNextNode < 0.1) {
                // 车辆已在节点 i+1 上，更新 startIndex，继续下一个路段
                startIndex = i + 1;
                vehicle.setLastReachedPathIndex(i + 1);
                continue;
            }

            if (remainingDistance >= distanceToNextNode) {

                remainingDistance -= distanceToNextNode;


                currentLng = endLng;
                currentLat = endLat;

                vehicle.setLastReachedPathIndex(i + 1);


                newTraveledPolyline.append(";").append(endPointStr[0]).append(",").append(endPointStr[1]);

                // 检查是否到达终点... (逻辑不变)

            } else {
                // 3b. 车辆停在路段 (currentPos -> i+1) 的中间

                double ratio = remainingDistance / distanceToNextNode;


                currentLng = currentLng + (endLng - currentLng) * ratio;
                currentLat = currentLat + (endLat - currentLat) * ratio;
                newTraveledPolyline.append(";").append(endPointStr[0]).append(",").append(endPointStr[1]);


                break; // 跳出循环
            }
        }


        vehicle.setCurrentLng(String.valueOf(currentLng));
        vehicle.setCurrentLat(String.valueOf(currentLat));

        if (newTraveledPolyline.length() > vehicle.getTraveledPolyline().length()) {
            vehicle.setTraveledPolyline(newTraveledPolyline.toString());
        }

        vehicleMapper.update(vehicle);
        return false;
    }

    private void resetToIdle(Vehicle vehicle) {
        vehicle.setStatus(VehicleStatus.IDLE);
        vehicle.setCurrentDemandId(null);
        vehicle.setRoutePolyline(null);
        vehicle.setParsedPolyline(null);
        vehicle.setTraveledPolyline(null);
        vehicle.setSpeed(0.0);
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

    /**
     * 安全地将高德 Polyline 字符串解析为 List<String[]> 坐标列表。
     * 该方法包含多重过滤，以处理字符串首尾多余的分号或空坐标段。
     *
     * @param polyline 原始 Polyline 字符串
     * @return 解析后的坐标列表，如果解析失败或结果为空则返回 null
     */
    private List<String[]> parsePolyline(String polyline) {
        if (polyline == null || polyline.isEmpty()) {
            log.error("致命错误：Polyline 字符串为空！");
            return null;
        }

        try {
            List<String[]> parsedPolyline = Arrays.stream(polyline.split(";"))
                    // 关键过滤 1: 过滤掉空字符串（如结尾的 ";" 产生的 ""，或中间的 ";;" 产生的 ""）
                    .filter(s -> !s.isEmpty())

                    // 关键过滤 2: 过滤掉不包含逗号的段（确保是坐标）
                    .filter(s -> s.contains(","))

                    .map(s -> s.split(","))

                    // 关键过滤 3: 过滤掉长度不为 2 的数组（确保是完整的 lng,lat）
                    .filter(coords -> coords.length == 2)

                    .collect(Collectors.toList());

            if (parsedPolyline.isEmpty()) {
                log.error("致命错误：Polyline 字符串 {} 包含有效路径点，但解析后列表为空，或所有段格式均不正确。", polyline);
                return null;
            }
            List<String[]> cleanPath = new ArrayList<>();
            cleanPath.add(parsedPolyline.get(0));

            for (int i = 1; i < parsedPolyline.size(); i++) {
                String[] currentPoint = parsedPolyline.get(i);
                String[] lastPoint = cleanPath.get(cleanPath.size() - 1);

                // 使用新的辅助方法判断点是否重复
                if (!isPointEqual(currentPoint, lastPoint)) {
                    cleanPath.add(currentPoint);
                }
            }

            if (cleanPath.size() < 2) {
                // 如果去重后只剩一个点，说明整个路径是无效的
                log.warn("警告：Polyline 经去重后路径点少于 2 个。原始点数: {}, 去重后点数: {}",
                        parsedPolyline.size(), cleanPath.size());
                return null;
            }

            return cleanPath; // 返回去重后的路径
        } catch (Exception e) {
            // 捕获任何意外异常，并记录原始 Polyline
            log.error("解析 Polyline 失败，抛出异常: {}。原始 Polyline 值: {}", e.getMessage(), polyline, e);
            return null;
        }
    }

    /**
     * 安全地找到车辆当前坐标在 fullPath 中的索引。
     *
     * @param fullPath 完整的解析路径 (List<String[]>)
     * @return 匹配的索引，如果找不到则返回 -1
     */
    private int findCurrentPathIndex(List<String[]> fullPath, String currentLngStr, String currentLatStr) {
        if (fullPath == null || fullPath.isEmpty()) {
            log.error("致命错误：fullPath 为空！");
            return -1;
        }

        try {
            // 解析车辆当前的精确位置
            double currentLng = Double.parseDouble(currentLngStr);
            double currentLat = Double.parseDouble(currentLatStr);

            // 遍历 fullPath 中的所有坐标点
            for (int i = 0; i < fullPath.size(); i++) {
                String[] pathPoint = fullPath.get(i);

                if (pathPoint.length != 2) continue; // 跳过格式不正确的点

                // 解析路径点
                double pathLng = Double.parseDouble(pathPoint[0]);
                double pathLat = Double.parseDouble(pathPoint[1]);

                // 【核心修正】：使用容差值进行浮点数比较
                if (isEqualWithTolerance(pathLng, currentLng) && isEqualWithTolerance(pathLat, currentLat)) {
                    return i;
                }
            }
        } catch (NumberFormatException e) {
            log.error("在查找索引时解析坐标字符串失败: {}", e.getMessage());
            return -1;
        }
        log.debug("未找到匹配的坐标点。");
        return -1;
    }

    /**
     * 辅助方法：判断两个 double 值是否在容差范围内相等。
     */
    private boolean isEqualWithTolerance(double val1, double val2) {
        // 假设 COORDINATE_TOLERANCE 已定义为 0.000001
        return Math.abs(val1 - val2) < 0.000001;
    }

    private boolean isPointEqual(String[] p1, String[] p2) {
        if (p1.length != 2 || p2.length != 2) return false;
        try {
            double lng1 = Double.parseDouble(p1[0]);
            double lat1 = Double.parseDouble(p1[1]);
            double lng2 = Double.parseDouble(p2[0]);
            double lat2 = Double.parseDouble(p2[1]);

            // 使用您已定义的浮点数容差（例如 0.000001，与您现有的逻辑保持一致）
            final double COORD_TOLERANCE = 0.000001;
            return Math.abs(lng1 - lng2) < COORD_TOLERANCE && Math.abs(lat1 - lat2) < COORD_TOLERANCE;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

