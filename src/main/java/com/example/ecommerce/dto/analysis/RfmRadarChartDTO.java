package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.util.List;

/**
 * RFM 雷达图数据 DTO（后端统一计算，前端直接用于绘制雷达图）
 */
@Data
public class RfmRadarChartDTO {

    /**
     * 指标定义（用于 ECharts radar.indicator）
     */
    private List<RadarIndicator> indicators;

    /**
     * 雷达图数据（一般只有一个点：当前客户）
     */
    private List<RadarDataItem> data;

    @Data
    public static class RadarIndicator {
        /**
         * 维度名称，例如 R、F、M
         */
        private String name;

        /**
         * 该维度的最大值（用于缩放）
         */
        private double max;
    }

    @Data
    public static class RadarDataItem {
        /**
         * 数据名称，例如“当前客户”
         */
        private String name;

        /**
         * 各维度的值，顺序与 indicators 对应
         */
        private List<Double> value;
    }
}


