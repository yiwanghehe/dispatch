package com.transportation.dispatch;

import com.transportation.dispatch.model.entity.WeatherInfo;
import com.transportation.dispatch.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class WeatherServiceTest {

    @Autowired
    private WeatherService weatherService;

    @Test
    public void testGetWeatherByLocation() {
        // 测试获取天气信息
        String location = "116.4074,39.9042"; // 北京坐标
        WeatherInfo weatherInfo = weatherService.getWeatherByLocation(location);
        
        assertNotNull(weatherInfo);
        assertNotNull(weatherInfo.getCondition());
        assertNotNull(weatherInfo.getLocation());
        assertNotNull(weatherInfo.getTimestamp());
        assertTrue(weatherInfo.getTemperature() >= 10.0 && weatherInfo.getTemperature() <= 30.0);
        assertTrue(weatherInfo.getHumidity() >= 40.0 && weatherInfo.getHumidity() <= 80.0);
        assertTrue(weatherInfo.getWindSpeed() >= 1.0 && weatherInfo.getWindSpeed() <= 11.0);
        
        System.out.println("测试结果 - 获取天气信息:");
        System.out.println("位置: " + weatherInfo.getLocation());
        System.out.println("天气状况: " + weatherInfo.getCondition());
        System.out.println("温度: " + weatherInfo.getTemperature());
        System.out.println("湿度: " + weatherInfo.getHumidity());
        System.out.println("风速: " + weatherInfo.getWindSpeed());
    }

    @Test
    public void testGetSpeedFactorByLocation() {
        // 测试获取速度系数
        String location = "121.4737,31.2304"; // 上海坐标
        double speedFactor = weatherService.getSpeedFactorByLocation(location);
        
        assertTrue(speedFactor >= 0.4 && speedFactor <= 1.0);
        
        System.out.println("测试结果 - 获取速度系数:");
        System.out.println("位置: " + location);
        System.out.println("速度系数: " + speedFactor);
    }

    @Test
    public void testSimulateWeatherChange() {
        // 测试模拟天气变化
        String location = "113.2644,23.1291"; // 广州坐标
        WeatherInfo initialWeather = weatherService.getWeatherByLocation(location);
        System.out.println("初始天气: " + initialWeather.getCondition());
        
        WeatherInfo changedWeather = weatherService.simulateWeatherChange(location);
        System.out.println("变化后天气: " + changedWeather.getCondition());
        
        assertNotNull(changedWeather);
        assertNotNull(changedWeather.getCondition());
        assertEquals(location, changedWeather.getLocation());
    }

    @Test
    public void testWeatherCache() {
        // 测试天气缓存
        String location = "104.0668,30.5728"; // 成都坐标
        WeatherInfo firstWeather = weatherService.getWeatherByLocation(location);
        System.out.println("第一次获取天气: " + firstWeather.getCondition());
        
        // 再次获取，应该返回缓存的天气
        WeatherInfo secondWeather = weatherService.getWeatherByLocation(location);
        System.out.println("第二次获取天气: " + secondWeather.getCondition());
        
        assertNotNull(firstWeather);
        assertNotNull(secondWeather);
        assertEquals(firstWeather.getCondition(), secondWeather.getCondition());
    }
}