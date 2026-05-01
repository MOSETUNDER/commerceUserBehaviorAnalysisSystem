package com.example.ecommerce.dto.analysis;

import lombok.Data;

/**
 * 订单状态漏斗图 DTO
 */
@Data
public class OrderFunnelDTO {
    /**
     * 阶段名称
     */
    private String stageName;

    /**
     * 订单数量
     */
    private long orderCount;

    /**
     * 转化率（相对于上一阶段，百分比）
     */
    private double conversionRate;
}

