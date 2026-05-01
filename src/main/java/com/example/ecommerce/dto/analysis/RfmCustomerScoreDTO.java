package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 单个客户的 RFM 得分信息
 */
@Data
public class RfmCustomerScoreDTO {

    /**
     * 客户ID
     * 注意：实际存储的是 customer_unique_id（真实用户ID），而不是系统内部的 customer_id。
     * 因为同一个真实用户可能在不同时间/地点下单，系统会分配不同的 customer_id，
     * 但 customer_unique_id 是唯一的，用于识别同一真实用户。
     */
    private String customerId;

    /**
     * 最近一次下单日期（Recency 基础）
     */
    private LocalDate lastOrderDate;

    /**
     * 最近一次购买距参考日期的天数（Recency Days）
     */
    private Integer recencyDays;

    /**
     * 订单次数（Frequency）
     */
    private int orderCount;

    /**
     * 累计消费金额（Monetary）
     */
    private BigDecimal totalAmount;

    /**
     * R 值（1-5分，五分位数，数值越小越近得分越高）
     */
    private int rScore;

    /**
     * F 值（1-5分，五分位数）
     */
    private int fScore;

    /**
     * M 值（1-5分，五分位数）
     */
    private int mScore;

    /**
     * RFM 综合评分（可按业务规则加权）
     */
    private double rfmScore;

    /**
     * 用户分层标签（如：重要价值客户、一般保持客户等）
     */
    private String segmentLabel;
}


