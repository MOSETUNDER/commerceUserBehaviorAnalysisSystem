package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 品类各州销售地图 DTO
 */
@Data
public class CategoryStateSalesDTO {
    /**
     * 品类名称
     */
    private String categoryName;

    /**
     * 州名
     */
    private String state;

    /**
     * 销售额
     */
    private BigDecimal salesAmount;

    /**
     * 订单数量
     */
    private long orderCount;
}

