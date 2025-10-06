package com.transportation.dispatch.controller;

import com.transportation.dispatch.model.common.Result;
import com.transportation.dispatch.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "仿真控制")
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    @Autowired
    private SimulationService simulationService;

    @Operation(summary = "启动仿真引擎")
    @PostMapping("/start")
    public Result startSimulation() {
        if (simulationService.isRunning()) {
            return Result.error("仿真已在运行中");
        }
        simulationService.start();
        return Result.success("仿真引擎已启动");
    }

    @Operation(summary = "停止仿真引擎")
    @PostMapping("/stop")
    public Result stopSimulation() {
        if (!simulationService.isRunning()) {
            return Result.error("仿真当前未运行");
        }
        simulationService.stop();
        return Result.success("仿真引擎已停止");
    }
}

