package com.example.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ecommerce.dto.analysis.ReviewScoreDistributionDTO;
import com.example.ecommerce.entity.OrderReview;
import com.example.ecommerce.mapper.OrderReviewMapper;
import com.example.ecommerce.service.ReviewAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单评价相关分析实现
 */
@Service
@RequiredArgsConstructor
public class ReviewAnalysisServiceImpl implements ReviewAnalysisService {

    private final OrderReviewMapper orderReviewMapper;

    // 评价分数分布缓存
    private volatile List<ReviewScoreDistributionDTO> cachedReviewScoreDistribution;
    private volatile long cachedReviewScoreDistributionAtMillis;
    private static final long REVIEW_SCORE_DIST_CACHE_DURATION_MILLIS = 10 * 60 * 1000; // 10 分钟缓存
    private final Object reviewScoreDistCalculationLock = new Object();

    @Override
    public List<ReviewScoreDistributionDTO> getReviewScoreDistribution(LocalDate startDate, LocalDate endDate) {
        // 如果没有指定日期范围，使用缓存
        if (startDate == null && endDate == null) {
            List<ReviewScoreDistributionDTO> snapshot = cachedReviewScoreDistribution;
            if (snapshot != null && (System.currentTimeMillis() - cachedReviewScoreDistributionAtMillis) < REVIEW_SCORE_DIST_CACHE_DURATION_MILLIS) {
                return snapshot;
            }

            synchronized (reviewScoreDistCalculationLock) {
                snapshot = cachedReviewScoreDistribution;
                if (snapshot != null && (System.currentTimeMillis() - cachedReviewScoreDistributionAtMillis) < REVIEW_SCORE_DIST_CACHE_DURATION_MILLIS) {
                    return snapshot;
                }
                List<ReviewScoreDistributionDTO> result = calculateReviewScoreDistribution(startDate, endDate);
                cachedReviewScoreDistribution = result;
                cachedReviewScoreDistributionAtMillis = System.currentTimeMillis();
                return result;
            }
        }

        // 如果指定了日期范围，直接计算（不缓存）
        return calculateReviewScoreDistribution(startDate, endDate);
    }

    private List<ReviewScoreDistributionDTO> calculateReviewScoreDistribution(LocalDate startDate, LocalDate endDate) {
        // 如果指定了日期范围，使用旧的方式（暂时不支持日期范围过滤的 SQL 聚合）
        if (startDate != null || endDate != null) {
            return calculateReviewScoreDistributionLegacy(startDate, endDate);
        }
        
        // 使用 SQL 聚合优化
        List<Map<String, Object>> aggregatedData = orderReviewMapper.aggregateReviewScoreDistribution();
        if (CollectionUtils.isEmpty(aggregatedData)) {
            return Collections.emptyList();
        }
        
        Map<Integer, Long> scoreCountMap = new HashMap<>();
        for (Map<String, Object> row : aggregatedData) {
            Object scoreObj = row.get("score");
            Object countObj = row.get("count");
            if (scoreObj != null && countObj != null) {
                int score = ((Number) scoreObj).intValue();
                long count = ((Number) countObj).longValue();
                scoreCountMap.put(score, count);
            }
        }
        
        List<ReviewScoreDistributionDTO> result = new ArrayList<>();
        for (int score = 1; score <= 5; score++) {
            ReviewScoreDistributionDTO dto = new ReviewScoreDistributionDTO();
            dto.setScore(score);
            dto.setCount(scoreCountMap.getOrDefault(score, 0L));
            result.add(dto);
        }
        return result;
    }
    
    private List<ReviewScoreDistributionDTO> calculateReviewScoreDistributionLegacy(LocalDate startDate, LocalDate endDate) {
        QueryWrapper<OrderReview> wrapper = new QueryWrapper<>();
        if (startDate != null) {
            wrapper.ge("review_creation_date", startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le("review_creation_date", endDate.plusDays(1).atStartOfDay());
        }
        List<OrderReview> reviews = orderReviewMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(reviews)) {
            return Collections.emptyList();
        }
        Map<Integer, Long> map = reviews.stream()
                .filter(r -> r.getReviewScore() != null)
                .collect(Collectors.groupingBy(OrderReview::getReviewScore, Collectors.counting()));

        List<ReviewScoreDistributionDTO> result = new ArrayList<>();
        for (int score = 1; score <= 5; score++) {
            ReviewScoreDistributionDTO dto = new ReviewScoreDistributionDTO();
            dto.setScore(score);
            dto.setCount(map.getOrDefault(score, 0L));
            result.add(dto);
        }
        return result;
    }
}


