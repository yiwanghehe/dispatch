package com.transportation.dispatch.service.impl;

import com.transportation.dispatch.enumeration.WeatherCondition;
import com.transportation.dispatch.model.entity.WeatherInfo;
import com.transportation.dispatch.service.WeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@Slf4j
public class WeatherServiceImpl implements WeatherService {

    private final Map<String, WeatherInfo> weatherCache = new HashMap<>();
    private final Random random = new Random();
    private static final long CACHE_DURATION_MINUTES = 30;

    @Override
    public WeatherInfo getWeatherByLocation(String location) {
        WeatherInfo cachedWeather = weatherCache.get(location);
        if (cachedWeather != null && isCacheValid(cachedWeather)) {
            return cachedWeather;
        }

        // 模拟天气数据（实际项目中应调用真实的天气API）
        WeatherInfo weatherInfo = generateRandomWeather(location);
        weatherCache.put(location, weatherInfo);
        log.info("为位置 {} 生成天气信息: {}", location, weatherInfo.getCondition());
        return weatherInfo;
    }

    @Override
    public double getSpeedFactorByLocation(String location) {
        WeatherInfo weatherInfo = getWeatherByLocation(location);
        return weatherInfo.getCondition().getSpeedFactor();
    }

    @Override
    public WeatherInfo simulateWeatherChange(String location) {
        WeatherInfo newWeather = generateRandomWeather(location);
        weatherCache.put(location, newWeather);
        log.info("位置 {} 的天气已更新为: {}", location, newWeather.getCondition());
        return newWeather;
    }

    private WeatherInfo generateRandomWeather(String location) {
        WeatherCondition[] conditions = WeatherCondition.values();
        WeatherCondition randomCondition = conditions[random.nextInt(conditions.length)];
        
        double temperature = 10.0 + random.nextDouble() * 20.0; // 10-30度
        double humidity = 40.0 + random.nextDouble() * 40.0; // 40-80%
        double windSpeed = 1.0 + random.nextDouble() * 10.0; // 1-11 m/s

        WeatherInfo weatherInfo = new WeatherInfo();
        weatherInfo.setLocation(location);
        weatherInfo.setCondition(randomCondition);
        weatherInfo.setTemperature(temperature);
        weatherInfo.setHumidity(humidity);
        weatherInfo.setWindSpeed(windSpeed);
        weatherInfo.setTimestamp(LocalDateTime.now());

        return weatherInfo;
    }

    private boolean isCacheValid(WeatherInfo weatherInfo) {
        return weatherInfo.getTimestamp().plusMinutes(CACHE_DURATION_MINUTES).isAfter(LocalDateTime.now());
    }
}