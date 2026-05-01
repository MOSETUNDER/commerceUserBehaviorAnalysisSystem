package com.example.ecommerce.dto.analysis;

import lombok.Data;

import java.util.List;

/**
 * 卖家评分分布 DTO（用于箱线图）
 */
@Data
public class SellerReviewDistributionDTO {
    private String sellerId;
    private List<Integer> reviewScores; // 该卖家的所有评分列表
    private double averageScore; // 平均评分
    private int minScore; // 最低评分
    private int maxScore; // 最高评分
    private int medianScore; // 中位数评分
}

