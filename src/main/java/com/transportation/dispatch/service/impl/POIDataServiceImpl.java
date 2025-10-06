package com.transportation.dispatch.service.impl;

import com.transportation.dispatch.enumeration.PoiSimType;
import com.transportation.dispatch.mapper.PoiMapper;
import com.transportation.dispatch.model.common.RawPOI;
import com.transportation.dispatch.model.common.Result;
import com.transportation.dispatch.model.entity.Poi;
import com.transportation.dispatch.service.POIDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class POIDataServiceImpl implements POIDataService {

    @Autowired
    private PoiMapper poiMapper;

    @Override
    public Result insert(RawPOI rawPOI, PoiSimType simType) {

        Poi poi = new Poi();

        poi.setAmapId(rawPOI.getId());
        poi.setName(rawPOI.getName());
        poi.setAddress(rawPOI.getAddress());

        String[] location = rawPOI.getLocation().split(",");
        poi.setLng(location[0]);
        poi.setLat(location[1]);

        poi.setPname(rawPOI.getPname());
        poi.setCityname(rawPOI.getCityname());
        poi.setAdname(rawPOI.getAdname());
        poi.setType(rawPOI.getType());
        poi.setTypecode(rawPOI.getTypecode());
        poi.setSimType(simType);
        poi.setStatus(1);

        // 先查看数据库内有没有这个poi，没有再插入
        Poi DB_poi = poiMapper.findById(poi.getId());
        if(DB_poi == null){
            poiMapper.insert(poi);
            return Result.success();
        }
        else return Result.success("Already existed");
    }

    @Override
    public Result findById(Long id) {
        Poi poi = poiMapper.findById(id);
        if(poi != null){
            return Result.success(poi);
        }
        else return Result.error("Cannot find the poi");
    }

    @Override
    public Result findBySimType(PoiSimType simType) {
        List<Poi> Pois = poiMapper.findBySimType(simType);
        if(Pois == null) return Result.error("错误，数据库为空");
        else return Result.success(Pois);
    }

    @Override
    public Result update(Poi poi) {
        int res = poiMapper.update(poi);
        if(res > 0) return Result.success("Updated");
        else return Result.error("Cannot update");
    }

    @Override
    public Result delete(Long id) {
        int res = poiMapper.delete(id);
        if(res > 0) return Result.success("Deleted");
        else return Result.error("Cannot delete");
    }

    @Override
    public Result addPOIs(List<RawPOI> rawPOIs, PoiSimType simType){
        for(RawPOI poi : rawPOIs){
            Result result = insert(poi, simType);
            if(result.getCode() != 200) return Result.error("添加失败");
        }
        return Result.success();
    }


    @Override
    public Result getAll(){
        List<Poi> allPOI = poiMapper.findAll();
        if(allPOI == null) return Result.error("错误，数据库为空");
        else return Result.success(allPOI);
    }

    @Override
    public List<PoiSimType> getAllSimType() {
        return Arrays.asList(PoiSimType.values());
    }


}
