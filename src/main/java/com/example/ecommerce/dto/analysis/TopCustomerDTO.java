package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 高价值 / 高频客户排行 DTO
 */
@Data
public class TopCustomerDTO {

    private String customerId;

    /**
     * 订单次数
     */
    private int orderCount;

    /**
     * 累计消费金额
     */
    private BigDecimal totalAmount;

    /**
     * 客单价 = totalAmount / orderCount
     */
    private BigDecimal averageOrderValue;
}


