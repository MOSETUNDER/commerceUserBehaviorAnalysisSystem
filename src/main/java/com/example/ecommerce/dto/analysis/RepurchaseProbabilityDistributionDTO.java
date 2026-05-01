package com.example.ecommerce.dto.analysis;

import lombok.Data;

/**
 * 复购概率分布 DTO（用于直方图）
 */
@Data
public class RepurchaseProbabilityDistributionDTO {

    /**
     * 概率区间（例如：0.0-0.1, 0.1-0.2, 0.2-0.3 等）
     */
    private String probabilityRange;

    /**
     * 该区间内的客户数量
     */
    private long count;
}

