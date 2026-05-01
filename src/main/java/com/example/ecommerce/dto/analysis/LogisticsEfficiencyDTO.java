package com.example.ecommerce.dto.analysis;

import lombok.Data;

/**
 * 物流时效分布 DTO
 */
@Data
public class LogisticsEfficiencyDTO {
    /**
     * 州名
     */
    private String state;

    /**
     * 平均物流天数
     */
    private double averageDeliveryDays;

    /**
     * 订单数量（用于统计）
     */
    private long orderCount;
}

