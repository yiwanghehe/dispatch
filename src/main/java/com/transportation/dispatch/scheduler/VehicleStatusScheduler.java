package com.transportation.dispatch.scheduler;

import com.transportation.dispatch.enumeration.VehicleStatus;
import com.transportation.dispatch.model.dto.VehicleDto;
import com.transportation.dispatch.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class VehicleStatusScheduler {

    private final VehicleService vehicleService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public VehicleStatusScheduler(VehicleService vehicleService, SimpMessagingTemplate messagingTemplate) {
        this.vehicleService = vehicleService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 定时任务
     * 用于向所有订阅的客户端推送最新的车辆状态。
     * 注意：请确保你的主启动类上有 @EnableScheduling 注解。
     */
    @Scheduled(fixedRate = 5000)
    public void pushVehicleUpdates() {
        // 1. 获取所有车辆的最新状态
        List<VehicleDto> allVehicles = vehicleService.getVehicles(null);
        // 通过 messagingTemplate 将数据广播到 "/topic/vehicles/all" 这个目的地
        // 所有订阅了这个目的地的客户端都会收到消息
        messagingTemplate.convertAndSend("/topic/vehicles/all", allVehicles);

        // 2. 你也可以为不同的状态创建不同的推送主题
        List<VehicleDto> movingVehicles = vehicleService.getVehicles(VehicleStatus.MOVING_TO_PICKUP);
        messagingTemplate.convertAndSend("/topic/vehicles/MOVING_TO_PICKUP", movingVehicles);

    }
}
