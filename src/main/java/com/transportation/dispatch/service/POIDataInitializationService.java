package com.transportation.dispatch.service;

import com.transportation.dispatch.enumeration.PoiSimType;
import com.transportation.dispatch.mapper.PoiMapper;
import com.transportation.dispatch.mapper.SupplyChainMapper;
import com.transportation.dispatch.model.dto.AmapPoiDetail;
import com.transportation.dispatch.model.dto.AmapPoiResponse;
import com.transportation.dispatch.model.entity.Poi;
import com.transportation.dispatch.model.entity.SupplyChainStage;
import com.transportation.dispatch.model.entity.SupplyChainTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.EnumMap;
import java.util.Map;

@Service
@Slf4j
public class POIDataInitializationService {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private PoiMapper poiMapper;
    @Autowired
    private SupplyChainMapper supplyChainMapper;

    // 从 application.properties 文件中注入API Key
    @Value("${api.key}")
    private String amapApiKey;

    /**
     * 主入口方法，执行所有数据初始化任务
     */
    public void initializeData() {
        log.info("开始执行数据初始化任务...");
        setupSupplyChains();
        fetchAndSavePois();
        log.info("数据初始化任务完成。");
    }

    /**
     * 第一步：检查并创建供应链模板和阶段数据
     */
    private void setupSupplyChains() {
        if (supplyChainMapper.countTemplates() > 0) {
            log.info("供应链数据已存在，跳过创建。");
            return;
        }
        log.info("数据库中无供应链数据，开始创建...");

        // 1. 创建家具供应链
        SupplyChainTemplate furnitureTemplate = new SupplyChainTemplate("高端实木家具供应链", "从原木到成品家具的完整流程");
        supplyChainMapper.insertTemplate(furnitureTemplate);
        Long furnitureTemplateId = furnitureTemplate.getId();

        supplyChainMapper.insertStage(new SupplyChainStage(furnitureTemplateId, 1, PoiSimType.LUMBER_YARD, PoiSimType.SAWMILL, "优质原木", 30.0, 25.0));
        supplyChainMapper.insertStage(new SupplyChainStage(furnitureTemplateId, 2, PoiSimType.SAWMILL, PoiSimType.FURNITURE_FACTORY, "加工后木材", 20.0, 18.0));
        supplyChainMapper.insertStage(new SupplyChainStage(furnitureTemplateId, 3, PoiSimType.FURNITURE_FACTORY, PoiSimType.FURNITURE_MARKET, "成品高档家具", 15.0, 40.0));
        log.info("成功创建 '高端实木家具供应链' 模板。");

        // 2. 创建五金供应链
        SupplyChainTemplate hardwareTemplate = new SupplyChainTemplate("通用五金件供应链", "从铁矿到五金成品的完整流程");
        supplyChainMapper.insertTemplate(hardwareTemplate);
        Long hardwareTemplateId = hardwareTemplate.getId();

        supplyChainMapper.insertStage(new SupplyChainStage(hardwareTemplateId, 1, PoiSimType.IRON_MINE, PoiSimType.STEEL_MILL, "铁矿石", 50.0, 15.0));
        supplyChainMapper.insertStage(new SupplyChainStage(hardwareTemplateId, 2, PoiSimType.STEEL_MILL, PoiSimType.HARDWARE_FACTORY, "标准钢材", 40.0, 10.0));
        log.info("成功创建 '通用五金件供应链' 模板。");
    }

    /**
     * 第二步：根据供应链中定义的POI类型，从高德API抓取并存储POI数据
     */
    private void fetchAndSavePois() {
        // 定义POI业务类型与高德搜索关键词的映射关系
        Map<PoiSimType, String> keywordMap = new EnumMap<>(PoiSimType.class);
        keywordMap.put(PoiSimType.LUMBER_YARD, "林场");
        keywordMap.put(PoiSimType.SAWMILL, "木材厂");
        keywordMap.put(PoiSimType.FURNITURE_FACTORY, "家具厂");
        keywordMap.put(PoiSimType.FURNITURE_MARKET, "家具城");

        keywordMap.put(PoiSimType.IRON_MINE, "钢材");
        keywordMap.put(PoiSimType.STEEL_MILL, "钢铁");
        keywordMap.put(PoiSimType.HARDWARE_FACTORY, "五金");

        log.info("开始从高德API抓取POI数据...");
        for (Map.Entry<PoiSimType, String> entry : keywordMap.entrySet()) {
            fetchAllPagesForType(entry.getKey(), entry.getValue());
        }
    }

    private void fetchAllPagesForType(PoiSimType simType, String keyword) {
        int pageNum = 1;
        int totalPoisFetched = 0;
        log.info("正在抓取类型为 '{}', 关键词为 '{}' 的POI...", simType, keyword);

        while (true) {
            String url = String.format(
                    "https://restapi.amap.com/v5/place/text?key=%s&keywords=%s&region=徐州市&page_size=25&page_num=%d",
                    amapApiKey, keyword, pageNum
            );

            try {
                AmapPoiResponse response = restTemplate.getForObject(url, AmapPoiResponse.class);

                if (response == null || response.getPois() == null || response.getPois().isEmpty()) {
                    break; // 没有更多数据了，退出当前关键词的抓取
                }

                for (AmapPoiDetail poiDetail : response.getPois()) {
                    // 没添加过的才进行添加
                    if (poiMapper.findByAmapId(poiDetail.getId()) == null) {
                        Poi poi = convertToPoiEntity(poiDetail, simType);
                        poiMapper.insert(poi);
                        totalPoisFetched++;
                    }
                }

                if (response.getPois().size() < 25) {
                    // 如果返回的POI数量小于请求的页面大小，说明这是最后一页
                    break;
                }

                pageNum++;
                // 礼貌性停顿，避免QPS超限
                Thread.sleep(150);

            } catch (Exception e) {
                log.error("抓取POI数据时发生错误 (类型: {}, page: {}): {}", simType, pageNum, e.getMessage());
                break; // 出现错误，停止抓取
            }
        }
        log.info("类型 '{}' 抓取完成, 共新增 {} 个POI。", simType, totalPoisFetched);
    }

    private Poi convertToPoiEntity(AmapPoiDetail detail, PoiSimType simType) {
        Poi poi = new Poi();
        poi.setAmapId(detail.getId());
        poi.setName(detail.getName());
        poi.setAddress(detail.getAddress());

        String[] location = detail.getLocation().split(",");
        poi.setLng(location[0]);
        poi.setLat(location[1]);

        poi.setPname(detail.getPname());
        poi.setCityname(detail.getCityname());
        poi.setAdname(detail.getAdname());
        poi.setType(detail.getType());
        poi.setTypecode(detail.getTypecode());
        poi.setSimType(simType);
        poi.setStatus(1);
        return poi;
    }
}
