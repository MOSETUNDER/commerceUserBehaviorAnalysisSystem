package com.example.ecommerce.dto.analysis;

import lombok.Data;

/**
 * 消费金额分布直方图 DTO（用户金额分桶）
 */
@Data
public class CustomerAmountDistributionDTO {
    /**
     * 金额区间（例如：0-100, 100-500, 500-1000 等）
     */
    private String amountRange;

    /**
     * 该区间内的客户数量
     */
    private long customerCount;
}

