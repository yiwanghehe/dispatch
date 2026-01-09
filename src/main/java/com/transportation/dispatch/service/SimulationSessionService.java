package com.transportation.dispatch.service;

import com.transportation.dispatch.model.entity.SimulationSession;
import java.util.List;
import java.util.Optional;

/**
 * 仿真会话查询服务接口。
 * 暴露查询仿真历史记录和详细信息的方法。
 */
public interface SimulationSessionService {

    /**
     * 查询所有历史仿真会话记录，按启动时间倒序排列。
     * @return 所有的历史会话记录列表。
     */
    List<SimulationSession> findAllSessions();

}