package com.transportation.dispatch.controller;

import com.transportation.dispatch.enumeration.VehicleStatus;
import com.transportation.dispatch.model.dto.VehicleDto;
import com.transportation.dispatch.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@Controller
@CrossOrigin(origins = "*", maxAge = 3600)
public class VehicleSocketController {

    private final VehicleService vehicleService;

    @Autowired
    public VehicleSocketController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * 处理客户端的订阅请求。
     * 当客户端订阅 "/app/vehicles/{status}" 这个目的地时，此方法会被调用。
     * @SubscribeMapping 非常适合用于在订阅发生时，立即向客户端发送一次性的初始数据。
     *
     * @param status 路径变量，由客户端在订阅地址中指定 (例如 "all", "moving", "idle")。
     * @return 返回的车辆列表将直接发送给刚刚订阅的那个客户端。
     */
    @SubscribeMapping("/vehicles/{status}")
    public List<VehicleDto> getVehicleStatus(@DestinationVariable String status) {
        System.out.println("客户端订阅了 /vehicles/" + status);
        // 根据传入的status字符串解析成枚举或null
        VehicleStatus vehicleStatus = "all".equalsIgnoreCase(status) ? null : VehicleStatus.valueOf(status.toUpperCase());
        // 调用业务层获取初始数据
        return vehicleService.getVehicles(vehicleStatus);
    }
}
