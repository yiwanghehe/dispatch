package com.transportation.dispatch.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // 添加无参构造函数
public class SupplyChainTemplate {
    private Long id;
    private String name;
    private String description;

    public SupplyChainTemplate(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
