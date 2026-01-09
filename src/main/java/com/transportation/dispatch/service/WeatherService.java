package com.transportation.dispatch.service;

import com.transportation.dispatch.enumeration.WeatherCondition;
import com.transportation.dispatch.model.entity.WeatherInfo;

public interface WeatherService {
    /**
     * 根据位置获取天气信息
     * @param location 位置（经纬度格式："lng,lat"）
     * @return 天气信息
     */
    WeatherInfo getWeatherByLocation(String location);

    /**
     * 获取指定位置的天气对车辆速度的影响系数
     * @param location 位置（经纬度格式："lng,lat"）
     * @return 速度影响系数
     */
    double getSpeedFactorByLocation(String location);

    /**
     * 模拟天气变化
     * @param location 位置
     * @return 更新后的天气信息
     */
    WeatherInfo simulateWeatherChange(String location);
}