package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品销售 Top N DTO
 */
@Data
public class ProductSalesDTO {
    /**
     * 商品ID
     */
    private String productId;

    /**
     * 商品类别名称
     */
    private String categoryName;

    /**
     * 销售量（订单项数量）
     */
    private long salesCount;

    /**
     * 销售额
     */
    private BigDecimal salesAmount;
}

