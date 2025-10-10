package com.transportation.dispatch.controller;

import com.transportation.dispatch.enumeration.VehicleStatus;
import com.transportation.dispatch.model.common.Result;
import com.transportation.dispatch.model.dto.VehicleDto;
import com.transportation.dispatch.model.entity.Vehicle;
import com.transportation.dispatch.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "车辆操作")
@RestController
@RequestMapping("/api/vehicle")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @Operation(summary = "获取所有车辆的实时状态和路径")
    @GetMapping("/get")
    public Result getAllVehicleStatus(@RequestParam(name="status" , required = false) VehicleStatus status) {
        List<VehicleDto> vehicles = vehicleService.getVehicles(status);
        return Result.success(vehicles);
    }
    @Operation(summary = "更新指定车辆的速度")
    @PutMapping("/updateSpeed")
    public Result updateSpeed(@RequestParam Long vehicleId, @RequestParam Double speed) {
        vehicleService.updateVehicleSpeed(vehicleId, speed);
        return Result.success("已将车辆"+vehicleId+"的速度更新为"+speed);
    }
}
