package com.example.ecommerce.dto.analysis;

import lombok.Data;

/**
 * 卖家复购率 DTO
 */
@Data
public class SellerRepurchaseRateDTO {
    private String sellerId;
    private double repurchaseRate; // 复购率（0-1）
    private long totalCustomers; // 总客户数
    private long repurchaseCustomers; // 复购客户数
}

