package com.transportation.dispatch.controller;

import com.transportation.dispatch.enumeration.VehicleStatus;
import com.transportation.dispatch.model.common.Result;
import com.transportation.dispatch.model.dto.VehicleDto;
import com.transportation.dispatch.model.entity.Vehicle;
import com.transportation.dispatch.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
