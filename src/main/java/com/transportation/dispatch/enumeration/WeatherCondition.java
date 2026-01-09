package com.transportation.dispatch.enumeration;

public enum WeatherCondition {
    SUNNY(1.0),        // 晴天，速度系数1.0
    CLOUDY(0.9),       // 多云，速度系数0.9
    RAINY(0.7),        // 下雨，速度系数0.7
    HEAVY_RAIN(0.5),   // 大雨，速度系数0.5
    SNOWY(0.6),        // 下雪，速度系数0.6
    FOGGY(0.6),        //  起雾，速度系数0.6
    THUNDERSTORM(0.4); // 雷暴，速度系数0.4

    private final double speedFactor;

    WeatherCondition(double speedFactor) {
        this.speedFactor = speedFactor;
    }

    public double getSpeedFactor() {
        return speedFactor;
    }
}