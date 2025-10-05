package com.transportation.dispatch.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RawPOI {
    private String parent;
    private String address;
    private String distance;
    private String pcode;
    private String adcode;
    private String pname;
    private String cityname;
    private String type;
    private String typecode;
    private String adname;
    private String citycode;
    private String name;
    private String location;
    private String id;
}
