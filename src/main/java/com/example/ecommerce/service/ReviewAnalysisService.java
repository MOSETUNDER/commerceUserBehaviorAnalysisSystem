package com.example.ecommerce.service;

import com.example.ecommerce.dto.analysis.ReviewScoreDistributionDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 订单评价相关分析服务
 */
public interface ReviewAnalysisService {

    /**
     * 订单评价得分分布（1-5 星），用于柱状图/条形图
     */
    List<ReviewScoreDistributionDTO> getReviewScoreDistribution(LocalDate startDate, LocalDate endDate);
}


