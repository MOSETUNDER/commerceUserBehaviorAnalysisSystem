package com.example.ecommerce.dto.analysis;

import lombok.Data;

/**
 * 订单评价分数分布 DTO
 */
@Data
public class ReviewScoreDistributionDTO {

    /**
     * 评价分数（1-5）
     */
    private int score;

    /**
     * 对应评价数量
     */
    private long count;
}


