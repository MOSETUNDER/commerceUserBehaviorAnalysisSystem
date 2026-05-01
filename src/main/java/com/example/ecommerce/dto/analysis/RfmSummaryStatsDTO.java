package com.example.ecommerce.dto.analysis;

import lombok.Data;

/**
 * RFM 分析汇总统计 DTO
 */
@Data
public class RfmSummaryStatsDTO {

    /**
     * 总客户数
     */
    private long totalCustomers;

    /**
     * 高价值客户数（Champions + Loyal Customers）
     */
    private long highValueCount;

    /**
     * 潜力客户数（Potential Loyalists）
     */
    private long potentialCount;

    /**
     * 一般客户数（Other + Recent Customers）
     */
    private long normalCount;

    /**
     * 流失客户数（Lost + Hibernating + At Risk）
     */
    private long churnedCount;

    /**
     * R 维度平均得分
     */
    private double avgRScore;

    /**
     * F 维度平均得分
     */
    private double avgFScore;

    /**
     * M 维度平均得分
     */
    private double avgMScore;
}

