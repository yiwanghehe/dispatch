package com.transportation.dispatch.service;

import com.transportation.dispatch.model.entity.RouteCache;

public interface RouteService {
    /**
     * 获取两点之间的驾驶路径。
     * 优先从本地数据库缓存中读取，如果缓存不存在，则调用高德API获取并存入缓存。
     * @param originCoords 起点坐标 "lng,lat"
     * @param destinationCoords 终点坐标 "lng,lat"
     * @return 包含路径信息的RouteCache对象，如果失败则返回null。
     */
    RouteCache getRoute(String originCoords, String destinationCoords);

    /**
     * 为所有供应链阶段预热并缓存路径
     */
    void initializeAllRoutes();

    String normalizeCoords(String coords);
}
