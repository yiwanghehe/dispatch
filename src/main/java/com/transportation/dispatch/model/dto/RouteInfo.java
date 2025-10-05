package com.transportation.dispatch.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RouteInfo {
    private String origin;
    private String destination;
    private List<PathInfo> paths;
}
