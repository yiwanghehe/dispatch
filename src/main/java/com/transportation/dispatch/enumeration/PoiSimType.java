package com.transportation.dispatch.enumeration;

/**
 * 用于定义POI点在仿真业务中的具体角色/类型。
 * 这是一个自定义的枚举，用于将高德地图宽泛的POI类型映射到我们自己的业务场景中。
 */
public enum PoiSimType {
    LUMBER_YARD,        // 林场 (产出原木)
    SAWMILL,            // 锯木厂 (消耗原木, 产出木材)
    FURNITURE_FACTORY,  // 家具厂 (消耗木材, 产出家具)
    FURNITURE_MARKET,   // 家具市场 (最终销售点)

    IRON_MINE,          // 铁矿 (产出铁矿石)
    STEEL_MILL,         // 钢铁厂 (消耗铁矿石, 产出钢材)
    HARDWARE_FACTORY,   // 五金厂 (消耗钢材, 产出五金件)
    // ... 可以根据需要添加更多角色
}
