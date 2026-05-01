package com.example.ecommerce.dto.analysis;

import lombok.Data;

/**
 * RFM 总分分布 DTO（用于柱状图）
 */
@Data
public class RfmScoreDistributionDTO {

    /**
     * 总分区间（例如：0-1, 1-2, 2-3 等）
     */
    private String scoreRange;

    /**
     * 该区间内的客户数量
     */
    private long count;
}

