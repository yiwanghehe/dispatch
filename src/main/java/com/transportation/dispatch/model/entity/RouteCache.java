package com.transportation.dispatch.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RouteCache {
    private Long id;
    private String originCoords;
    private String destinationCoords;
    private Integer distance;
    private Integer duration;
    private String polyline;
    private LocalDateTime createdAt;
}
