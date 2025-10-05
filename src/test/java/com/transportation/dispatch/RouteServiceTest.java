package com.transportation.dispatch;


import com.transportation.dispatch.model.entity.RouteCache;
import com.transportation.dispatch.service.RouteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RouteServiceTest {

    @Autowired
    private RouteService routeService;

    @Test
    public void testPlanRouteForVehicle() {
        String start = "104.065735,30.659462"; // 天府广场
        String end = "104.08342,30.658603";   // 春熙路

        RouteCache route = routeService.getRoute(start, end);

        if (route != null) {
            System.out.println("获取路径成功！");
            System.out.println("总距离: " + route.getDistance() + " 米");
            System.out.println("预计时间: " + route.getDuration() + " 秒");
             System.out.println("路径坐标点: " + route.getPolyline());
        } else {
            System.out.println("获取路径失败！");
        }
    }
}
