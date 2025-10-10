package com.transportation.dispatch.service.impl;

import com.transportation.dispatch.mapper.PoiMapper;
import com.transportation.dispatch.mapper.RouteCacheMapper;
import com.transportation.dispatch.mapper.SupplyChainMapper;
import com.transportation.dispatch.model.dto.AmapRouteResponse;
import com.transportation.dispatch.model.dto.PathInfo;
import com.transportation.dispatch.model.dto.StepInfo;
import com.transportation.dispatch.model.entity.Poi;
import com.transportation.dispatch.model.entity.RouteCache;
import com.transportation.dispatch.model.entity.SupplyChainStage;
import com.transportation.dispatch.service.RouteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RouteServiceImpl implements RouteService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RouteCacheMapper routeCacheMapper;

    @Autowired
    private PoiMapper poiMapper;

    @Autowired
    private SupplyChainMapper supplyChainMapper;

    @Value("${api.key}")
    private String amapApiKey;

    private static final long API_CALL_DELAY_MS = 360; // 每次API调用后暂停一定毫秒，确保不超过3次/秒的QPS

    /**
     * 获取两点之间的驾驶路径。
     * 优先从本地数据库缓存中读取，如果缓存不存在，则调用高德API获取并存入缓存。
     * @param originCoords 起点坐标 "lng,lat"
     * @param destinationCoords 终点坐标 "lng,lat"
     * @return 包含路径信息的RouteCache对象，如果失败则返回null。
     */
    @Override
    public RouteCache getRoute(String originCoords, String destinationCoords) {
        String normalizedOrigin = normalizeCoords(originCoords);
        String normalizedDestination = normalizeCoords(destinationCoords);
        // 1. 优先查询数据库缓存
        RouteCache cachedRoute = routeCacheMapper.findByOriginAndDestination(normalizedOrigin, normalizedDestination);
        if (cachedRoute != null) {
            log.info("路径缓存命中: {} -> {}", normalizedOrigin, normalizedDestination);
            return cachedRoute;
        }

        // 2. 缓存未命中，调用高德API，并将标准化坐标作为参数传入内部方法
        log.info("路径缓存未命中，正在调用高德API: {} -> {}", normalizedOrigin, normalizedDestination);

        // 调用 API 时，使用原始/高精度坐标，但传入标准化后的坐标用于缓存存储
        return callAmapApiAndCache(originCoords, destinationCoords, normalizedOrigin, normalizedDestination);
    }

    private RouteCache callAmapApiAndCache(  String originCoords,
                                             String destinationCoords,
                                             String normalizedOrigin,
                                             String normalizedDestination) {
        String url = String.format(
                "https://restapi.amap.com/v5/direction/driving?key=%s&origin=%s&destination=%s&show_fields=polyline,cost",
                amapApiKey, originCoords, destinationCoords
        );

        try {
            AmapRouteResponse response = restTemplate.getForObject(url, AmapRouteResponse.class);

            // 校验API返回结果的有效性
            if (response == null || !"1".equals(response.getStatus()) || response.getRoute() == null || response.getRoute().getPaths() == null || response.getRoute().getPaths().isEmpty()) {
                log.error("高德API返回无效数据或错误。Info: {}", response != null ? response.getInfo() : "Response is null");
                return null;
            }

            // 3. 【关键处理】只选用第一条推荐路径
            PathInfo firstPath = response.getRoute().getPaths().get(0);


            // 4. 【关键处理】拼接所有分段的polyline
            String polyline = firstPath.getSteps().stream()
                    .map(StepInfo::getPolyline)
                    .collect(Collectors.joining(";"));

            // 5. 【关键处理】将请求的终点坐标尾插到最后，确保路径闭合到精确终点
            String finalPolyline = polyline + ";" + destinationCoords;

            // 6. 创建新的缓存对象并存入数据库
            RouteCache newRoute = new RouteCache();
            newRoute.setOriginCoords(normalizedOrigin);
            newRoute.setDestinationCoords(normalizedDestination);
            // V5版本的distance和duration是字符串，需要转换为Integer
            newRoute.setDistance(Integer.parseInt(firstPath.getDistance()));
            newRoute.setDuration(Integer.parseInt(firstPath.getCost().getDuration()));
            newRoute.setPolyline(finalPolyline);

            routeCacheMapper.insert(newRoute);
            log.info("成功获取新路径并已存入缓存: {} -> {}", originCoords, destinationCoords);

            return newRoute;

        } catch (Exception e) {
            log.error("调用高德路径规划API时发生异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 为所有供应链阶段预热并缓存路径
     */
    @Override
    public void initializeAllRoutes() {
        log.info("===== 开始执行路径缓存预热任务 =====");
        List<SupplyChainStage> allStages = supplyChainMapper.findAllStages();

        if (allStages.isEmpty()) {
            log.warn("数据库中没有找到任何供应链阶段，无法预热路径。");
            return;
        }

        log.info("共找到 {} 个供应链阶段需要处理。", allStages.size());

        for (SupplyChainStage stage : allStages) {
            processStage(stage);
        }

        log.info("===== 所有路径缓存预热任务执行完毕 =====");
    }

    /**
     * 处理单个供应链阶段，获取其所有可能的路径
     * @param stage 待处理的供应链阶段
     */
    private void processStage(SupplyChainStage stage) {
        log.info("--- 开始处理阶段: 从 {} 到 {} ---", stage.getOriginPoiType(), stage.getDestinationPoiType());

        // 1. 获取该阶段所有的起点和终点POI
        List<Poi> originPois = poiMapper.findBySimType(stage.getOriginPoiType());
        List<Poi> destinationPois = poiMapper.findBySimType(stage.getDestinationPoiType());

        if (originPois.isEmpty() || destinationPois.isEmpty()) {
            log.warn("阶段 [{} -> {}] 的起点或终点POI列表为空，跳过此阶段。", stage.getOriginPoiType(), stage.getDestinationPoiType());
            return;
        }

        int totalPairs = originPois.size() * destinationPois.size();
        log.info("该阶段需要处理 {} x {} = {} 条路径。", originPois.size(), destinationPois.size(), totalPairs);
        int processedCount = 0;

        // 2. 遍历所有起点和终点的组合
        for (Poi origin : originPois) {
            for (Poi dest : destinationPois) {
                // 跳过起点和终点是同一个POI的情况
                if (origin.getId().equals(dest.getId())) {
                    continue;
                }

                // 3. 格式化坐标并调用RouteService
                String originCoords = origin.getLng() + "," + origin.getLat();
                String destCoords = dest.getLng() + "," + dest.getLat();
                String normalizedOrigin = normalizeCoords(originCoords);
                String normalizedDest = normalizeCoords(destCoords);

                // 使用不延时版本的getRoute
                RouteCache cachedRoute = routeCacheMapper.findByOriginAndDestination(normalizedOrigin, normalizedDest);
                if (cachedRoute != null) {
                    log.info("路径缓存命中: {} -> {}", normalizedOrigin, normalizedDest);
                } else {
                    // 缓存未命中，调用高德API并延时
                    log.info("路径缓存未命中，正在调用高德API: {} -> {}", normalizedOrigin, normalizedDest);
                    callAmapApiAndCache(originCoords, destCoords, normalizedOrigin, normalizedDest);

                    // 强制延时，保护API的QPS（仅在调用API时延时）
                    try {
                        Thread.sleep(API_CALL_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("路径预热任务被中断。", e);
                        return; // 任务被中断，提前退出
                    }
                }

                processedCount++;
                if (processedCount % 100 == 0) {
                    log.info("阶段 [{} -> {}]: 已处理 {} / {} 条路径。", stage.getOriginPoiType(), stage.getDestinationPoiType(), processedCount, totalPairs);
                }
            }
        }
        log.info("--- 阶段 [{} -> {}] 处理完成 ---", stage.getOriginPoiType(), stage.getDestinationPoiType());
    }
    /**
     * 统一标准化经纬度字符串，用于确保缓存键的一致性。
     * @param coords 原始坐标字符串，格式为 "lng,lat"
     * @return 格式化后的坐标字符串，如 "117.183943,34.344782" (统一为小数点后6位)
     */
    @Override
    public String normalizeCoords(String coords) {
        if (coords == null) return null;

        // 假设输入格式为 "lng,lat"
        String[] parts = coords.split(",");
        if (parts.length != 2) return coords; // 格式不正确则返回原值

        try {
            double lng = Double.parseDouble(parts[0]);
            double lat = Double.parseDouble(parts[1]);

            return String.format("%.6f,%.6f", lng, lat);
        } catch (NumberFormatException e) {
            log.error("坐标格式化失败: {}", coords);
            return coords;
        }
    }
}
