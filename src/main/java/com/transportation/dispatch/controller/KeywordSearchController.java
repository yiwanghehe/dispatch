package com.transportation.dispatch.controller;

import com.transportation.dispatch.model.common.Keyword;
import com.transportation.dispatch.model.common.Result;
import com.transportation.dispatch.service.KeywordSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Tag(name = "搜索Poi")
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api")
public class KeywordSearchController {

    @Autowired
    KeywordSearchService keywordSearchService;

    @Operation(summary = "根据关键字搜索Poi")
    @PostMapping("/keywordsearch")
    public Result keywordSearch(@RequestBody Keyword keyword) {
        return keywordSearchService.keywordSearch(keyword);
    }
}
