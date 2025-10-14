package com.transportation.dispatch.controller;

import com.transportation.dispatch.model.common.Result;
import com.transportation.dispatch.model.entity.Weight2Dispatch;
import com.transportation.dispatch.service.SimulationService;
import com.transportation.dispatch.service.SimulationSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "仿真控制")
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/simulation")
public class SimulationController {

    @Autowired
    private SimulationService simulationService;
    @Autowired
    private SimulationSessionService simulationSessionService;

    @Operation(summary = "启动仿真引擎")
    @PostMapping("/start")
    public Result startSimulation(@RequestBody Weight2Dispatch weight2Dispatch) {
        if (simulationService.isRunning()) {
            return Result.error("仿真已在运行中");
        }
        simulationService.start(weight2Dispatch);
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
    @Operation(summary = "查询所有历史仿真会话")
    @GetMapping("/sessions")
    public Result findAllSessions() {
        return Result.success(simulationSessionService.findAllSessions());
    }
}

