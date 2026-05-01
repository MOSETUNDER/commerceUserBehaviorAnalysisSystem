package com.example.ecommerce.service;

import com.example.ecommerce.dto.analysis.*;

import java.util.List;

/**
 * 地理维度分析服务（基于 customers + geolocation + orders）
 */
public interface GeoAnalysisService {

    /**
     * 各州销售额热力图（用于销售地图）
     */
    List<StateSalesHeatmapDTO> getStateSalesHeatmap();

    /**
     * 各州订单量柱状图（Top 10）
     */
    List<StateOrderCountDTO> getStateOrderCount(int topN);

    /**
     * 各州平均客单价柱状图（Top 10）
     */
    List<StateAverageOrderValueDTO> getStateAverageOrderValue(int topN);

    /**
     * 各州复购率柱状图（Top 10）
     */
    List<StateRepurchaseRateDTO> getStateRepurchaseRate(int topN);

    /**
     * 物流时效分布直方图（各州平均物流天数）
     */
    List<LogisticsEfficiencyDTO> getLogisticsEfficiency();

    /**
     * 地域用户增长折线图（按月统计）
     */
    List<GeoUserGrowthDTO> getGeoUserGrowth();
}

