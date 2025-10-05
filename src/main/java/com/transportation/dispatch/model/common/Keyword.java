package com.transportation.dispatch.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Keyword {

    private String keywords;
    private String region;
    private int page_size;
    private int page_num;
}
