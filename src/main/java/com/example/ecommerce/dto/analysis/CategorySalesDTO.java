package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品品类销量 / 销售额 DTO
 */
@Data
public class CategorySalesDTO {

    /**
     * 品类名称
     */
    private String categoryName;

    /**
     * 订单数量（包含该品类的订单次数）
     */
    private long orderCount;

    /**
     * 销售额
     */
    private BigDecimal totalAmount;
}


