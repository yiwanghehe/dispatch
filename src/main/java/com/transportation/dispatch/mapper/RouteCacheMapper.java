package com.transportation.dispatch.mapper;

import com.transportation.dispatch.model.entity.RouteCache;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RouteCacheMapper {

    /**
     * 根据起点和终点坐标查询缓存的路径。
     * @param originCoords 起点坐标 "lng,lat"
     * @param destinationCoords 终点坐标 "lng,lat"
     * @return 如果找到，返回RouteCache对象；否则返回null。
     */
    @Select("SELECT * FROM route_cache WHERE origin_coords = #{originCoords} AND destination_coords = #{destinationCoords}")
    RouteCache findByOriginAndDestination(@Param("originCoords") String originCoords, @Param("destinationCoords") String destinationCoords);

    /**
     * 向数据库中插入一条新的路径缓存记录。
     * @param routeCache 待插入的路径对象
     */
    @Insert("INSERT INTO route_cache (origin_coords, destination_coords, distance, duration, polyline) " +
            "VALUES (#{originCoords}, #{destinationCoords}, #{distance}, #{duration}, #{polyline})")
    void insert(RouteCache routeCache);
}

