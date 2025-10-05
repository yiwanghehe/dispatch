package com.transportation.dispatch.controller;

import com.transportation.dispatch.model.common.Result;
import com.transportation.dispatch.model.entity.RouteCache;
import com.transportation.dispatch.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "路径操作")
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/route")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @Operation(summary = "根据起终点获取路径")
    @GetMapping("/get")
    public Result getRoute(String originCoords, String destinationCoords) {
        try {
            RouteCache route = routeService.getRoute(originCoords, destinationCoords);
            return Result.success("获取路径成功", route);
        } catch(Exception e) {
            return Result.error("获取失败" + e.getMessage());
        }
    }

    @Operation(summary = "基于当前的Poi向高德api获取全部的路径")
    @GetMapping("/initial")
    public Result initialAllRoute() {
        try {
            routeService.initializeAllRoutes();
            return Result.success("获取全部路径成功，并且全部存入数据库");
        } catch(Exception e) {
            return Result.error("获取失败" + e.getMessage());
        }
    }
}
