package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 关键运营指标概览 DTO
 */
@Data
public class KpiOverviewDTO {

    /**
     * 客户总数
     */
    private long customerCount;

    /**
     * 订单总数
     */
    private long orderCount;

    /**
     * 有效订单总数（如已支付/已交付）
     */
    private long validOrderCount;

    /**
     * GMV 总额
     */
    private BigDecimal totalGmv;

    /**
     * 客单价 = GMV / 订单数
     */
    private BigDecimal averageOrderValue;
}


