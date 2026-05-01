package com.example.ecommerce.dto.analysis;

import lombok.Data;

/**
 * 卖家销售额分布直方图 DTO（销售额分桶）
 */
@Data
public class SellerSalesDistributionDTO {
    /**
     * 销售额区间（例如：0-1000, 1000-5000, 5000-10000 等）
     */
    private String salesRange;

    /**
     * 该区间内的卖家数量
     */
    private long sellerCount;
}

