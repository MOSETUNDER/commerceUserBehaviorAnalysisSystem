package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 卖家维度统计 DTO：订单数、GMV、好评率
 */
@Data
public class SellerStatsDTO {

    private String sellerId;

    /**
     * 订单数量（包含该卖家的订单次数）
     */
    private long orderCount;

    /**
     * GMV（该卖家相关订单金额）
     */
    private BigDecimal totalGmv;

    /**
     * 好评率（4-5星评价占比，0-1）
     */
    private double positiveRate;
}


