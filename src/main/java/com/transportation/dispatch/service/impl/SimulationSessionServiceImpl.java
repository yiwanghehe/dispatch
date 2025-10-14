package com.transportation.dispatch.service.impl;

import com.transportation.dispatch.mapper.SimulationSessionMapper;
import com.transportation.dispatch.model.entity.SimulationSession;
import com.transportation.dispatch.service.SimulationSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 仿真会话查询服务的实现类。
 * 职责：提供对 SimulationSession 数据的只读访问。
 */
@Service
public class SimulationSessionServiceImpl implements SimulationSessionService {

    @Autowired
    private SimulationSessionMapper sessionMapper;

    /**
     * 查询所有历史仿真会话记录。
     * 依赖 Mapper 的 findAll() 方法。
     */
    @Override
    public List<SimulationSession> findAllSessions() {
        // Mapper.findAll() 已经包含了 ORDER BY start_time DESC 逻辑
        return sessionMapper.findAll();
    }


}