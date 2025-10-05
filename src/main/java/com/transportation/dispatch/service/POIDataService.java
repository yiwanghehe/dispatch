package com.transportation.dispatch.service;

import com.transportation.dispatch.enumeration.PoiSimType;
import com.transportation.dispatch.model.common.RawPOI;
import com.transportation.dispatch.model.common.Result;
import com.transportation.dispatch.model.entity.Poi;

import java.util.List;


public interface POIDataService {
    Result insert(RawPOI rawPOI);

    Result findById(Long id);

    Result findBySimType(PoiSimType simType);

    Result update(Poi poi);

    Result delete(Long id);

    Result addPOIs(List<RawPOI> rawPOIs);

    Result getAll();

}
