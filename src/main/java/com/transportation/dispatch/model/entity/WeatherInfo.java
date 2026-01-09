package com.transportation.dispatch.model.entity;

import com.transportation.dispatch.enumeration.WeatherCondition;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WeatherInfo {
    private String location; // 位置（经纬度或地区名称）
    private WeatherCondition condition; // 天气状况
    private double temperature; // 温度
    private double humidity; // 湿度
    private double windSpeed; // 风速
    private LocalDateTime timestamp; // 天气时间戳

    public WeatherInfo(String location, WeatherCondition condition) {
        this.location = location;
        this.condition = condition;
        this.temperature = 20.0; // 默认温度
        this.humidity = 50.0; // 默认湿度
        this.windSpeed = 5.0; // 默认风速
        this.timestamp = LocalDateTime.now();
    }

    public WeatherInfo() {
    }
}