package com.transportation.dispatch.service;


import com.transportation.dispatch.model.common.Keyword;
import com.transportation.dispatch.model.common.Result;

public interface KeywordSearchService {

    Result keywordSearch(Keyword keyword);
}
