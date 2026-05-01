package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 卖家平均客单价 DTO
 */
@Data
public class SellerAverageOrderValueDTO {
    private String sellerId;
    private BigDecimal averageOrderValue; // 平均客单价
    private long orderCount; // 订单数
    private BigDecimal totalGmv; // 总GMV
}

