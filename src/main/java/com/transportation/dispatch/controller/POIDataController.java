package com.transportation.dispatch.controller;

import com.transportation.dispatch.enumeration.PoiSimType;
import com.transportation.dispatch.model.common.RawPOI;
import com.transportation.dispatch.model.common.Result;
import com.transportation.dispatch.model.entity.Poi;
import com.transportation.dispatch.service.POIDataInitializationService;
import com.transportation.dispatch.service.POIDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name = "Poi操作")
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/poi")
public class POIDataController {

    @Autowired
    private POIDataService poiDataService;

    @Autowired
    private POIDataInitializationService poiDataInitializationService;

    @Operation(summary = "添加完整信息")
    @PostMapping("/insert")
    public Result insert(@RequestBody RawPOI rawPOI, @RequestBody PoiSimType simType) {
        return poiDataService.insert(rawPOI, simType);
    }

    @Operation(summary = "查询id对应的信息")
    @GetMapping("/findById/{id}")
    public Result findById(@PathVariable Long id) {
        return poiDataService.findById(id);
    }

    @Operation(summary = "根据自定义类型simType查询Poi信息")
    @GetMapping("/findBySimType/{simType}")
    public Result findBySimType(@PathVariable PoiSimType simType) {
        return poiDataService.findBySimType(simType);
    }

    @Operation(summary = "查询所有自定义类型simType")
    @GetMapping("/getAllSimType")
    public Result getAllSimType() {
        try {
            List<PoiSimType> list = poiDataService.getAllSimType();
            return Result.success(list);
        } catch(Exception e) {
            return Result.error("获取PoiSimType失败");
        }

    }

    @Operation(summary = "更新信息")
    @PutMapping("/update")
    public Result update(@RequestBody Poi poi) {
        return poiDataService.update(poi);
    }

    @Operation(summary = "删除指定id的信息")
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        return poiDataService.delete(id);
    }

    @Operation(summary = "批量添加poi")
    @PostMapping("/addpois")
    public Result addPOIs(@RequestBody List<RawPOI> rawPOIs, @RequestParam PoiSimType simType) {
        return poiDataService.addPOIs(rawPOIs, simType);
    }

    @Operation(summary = "返回所有poi")
    @GetMapping("/getAll")
    public Result getAll(){
        return poiDataService.getAll();
    }

    @Operation(summary = "初始化POI，存入数据库中")
    @GetMapping("/initial")
    public Result initialPoiData() {
        try {
            poiDataInitializationService.initializeData();
            return Result.success("初始化成功，并将预先设定好的Poi点存入数据库！");
        } catch(Exception e) {
            return Result.error("初始化Poi失败" + e.getMessage());
        }
    }


}
