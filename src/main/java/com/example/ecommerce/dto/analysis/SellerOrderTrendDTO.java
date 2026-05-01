package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.time.LocalDate;

/**
 * 卖家订单量趋势 DTO（按月）
 */
@Data
public class SellerOrderTrendDTO {
    private String sellerId;
    private String month; // 格式：YYYY-MM
    private long orderCount; // 该月订单数
}

