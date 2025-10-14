package com.transportation.dispatch.mapper;

import com.transportation.dispatch.model.entity.SimulationSession;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 仿真会话 (SimulationSession) 数据库操作接口。
 * 用于记录每次仿真运行的配置和最终结果。
 */
@Mapper
public interface SimulationSessionMapper {

    /**
     * 插入一个新的仿真会话记录。
     * 必须在仿真开始时调用。
     * @param session 包含配置信息 (权重, sessionName) 的会话对象。
     * @return 影响的行数。
     */
    @Insert({
            "INSERT INTO simulation_session (",
            "session_name, start_time, use_weight, weight_time, weight_wasted_load, weight_wasted_idle",
            ") VALUES (",
            "#{sessionName}, NOW(), #{useWeight}, #{weightTime}, #{weightWastedLoad}, #{weightWastedIdle}",
            ")"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SimulationSession session);

    /**
     * 根据ID查询会话记录。
     * @param id 会话ID。
     * @return 匹配的 SimulationSession 对象。
     */
    @Select("SELECT * FROM simulation_session WHERE id = #{id}")
    SimulationSession findById(Long id);

    /**
     * 在仿真结束后，更新统计指标和结束时间。
     * @param session 包含更新后统计结果的会话对象。
     * @return 影响的行数。
     */
    @Update({
            "UPDATE simulation_session SET",
            "end_time = NOW(),",
            "session_name=#{sessionName},",
            "avg_no_load_distance = #{avgNoLoadDistance},",
            "avg_load_distance = #{avgLoadDistance},",
            "avg_total_duration = #{avgTotalDuration},",
            "avg_waiting_duration = #{avgWaitingDuration},",
            "total_demands_completed = #{totalDemandsCompleted},",
            "total_wasted_capacity = #{totalWastedCapacity},",
            "notes = #{notes}",
            "WHERE id = #{id}"
    })
    int update(SimulationSession session);

    /**
     * 查询所有历史仿真会话记录。
     * @return 所有的会话记录列表。
     */
    @Select("SELECT * FROM simulation_session ORDER BY start_time DESC")
    List<SimulationSession> findAll();
    @Select("SELECT * FROM simulation_session WHERE end_time IS NULL ORDER BY start_time DESC LIMIT 1")
    SimulationSession findLatestUnfinishedSession();

}