package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 客单价趋势 DTO（按月统计）
 */
@Data
public class AverageOrderValueDTO {
    /**
     * 月份（格式：YYYY-MM）
     */
    private String month;

    /**
     * 该月的客单价（平均订单金额）
     */
    private BigDecimal averageOrderValue;

    /**
     * 该月的订单总数
     */
    private long orderCount;

    /**
     * 该月的GMV总额
     */
    private BigDecimal totalGmv;
}

