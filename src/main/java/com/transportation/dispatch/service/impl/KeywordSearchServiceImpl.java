package com.transportation.dispatch.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.transportation.dispatch.model.common.Keyword;
import com.transportation.dispatch.model.common.Result;
import com.transportation.dispatch.service.KeywordSearchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class KeywordSearchServiceImpl implements KeywordSearchService {

    @Value("${api.key}")
    private String key;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Result keywordSearch(Keyword keyword) {
        String url = String.format("https://restapi.amap.com/v5/place/text?key=%s&keywords=%s&region=%s&page_size=%d&page_num=%d",
                key, keyword.getKeywords(), keyword.getRegion(), keyword.getPage_size(), keyword.getPage_num());
        String response = restTemplate.getForObject(url, String.class);

        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            // 假设Result类有一个构造函数接受JsonNode作为参数
            return Result.success(jsonNode);
        } catch (Exception e) {
            // 处理异常，例如网络错误或JSON解析错误
            return Result.error("Error occurred while searching path");
        }


    }
}
