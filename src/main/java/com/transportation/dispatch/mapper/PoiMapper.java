package com.transportation.dispatch.mapper;

import com.transportation.dispatch.enumeration.PoiSimType;
import com.transportation.dispatch.model.entity.Poi;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * POI 数据访问接口 (Mapper)。
 * 使用 MyBatis 来定义与 `poi` 表相关的数据库操作。
 * 你需要在Spring Boot启动类上添加 @MapperScan("你的mapper包路径")
 */
@Mapper
public interface PoiMapper {
    /**
     * 根据主键id查询POI信息
     * @param id
     * @return
     */
    @Select("select * from poi where id = #{id}")
    Poi findById(Long id);

    /**
     * 根据高德地图的Amap ID查询唯一的POI信息。
     * 用于在插入前检查数据是否已存在。
     *
     * @param amapId 高德POI的唯一ID
     * @return 对应的POI实体；如果不存在，则返回null。
     */
    @Select("SELECT * FROM poi WHERE amap_id = #{amapId}")
    Poi findByAmapId(String amapId);

    /**
     * 插入一条新的POI记录到数据库。
     *
     * @param poi 包含完整信息的POI实体对象
     * @return 返回影响的行数，通常为1表示成功。
     */
    @Insert("INSERT INTO poi(amap_id, name, address, lng, lat, pname, cityname, adname, type, typecode, sim_type, status, create_time) " +
            "VALUES(#{amapId}, #{name}, #{address}, #{lng}, #{lat}, #{pname}, #{cityname}, #{adname}, #{type}, #{typecode}, #{simType}, #{status}, NOW())")
    int insert(Poi poi);

    /**
     * 查询数据库中所有已存储的POI点。
     *
     * @return 包含所有POI实体的列表。
     */
    @Select("SELECT * FROM poi")
    List<Poi> findAll();

    /**
     * 根据我们自定义的仿真业务类型查询POI点。
     *
     * @param simType 仿真业务类型枚举
     * @return 符合该类型的POI实体列表。
     */
    @Select("SELECT * FROM poi WHERE sim_type = #{simType}")
    List<Poi> findBySimType(PoiSimType simType);

    /**
     * 更新POI点
     * @param poi
     * @return 返回影响的行数，通常为1表示成功。
     */
    int update(Poi poi);

    /**
     * 删除POI点
     * @param id
     * @return 返回影响的行数，通常为1表示成功。
     */
    @Delete("delete from poi where id = #{id}")
    int delete(Long id);
}
