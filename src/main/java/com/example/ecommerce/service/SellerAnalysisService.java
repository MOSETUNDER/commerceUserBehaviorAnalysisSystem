package com.example.ecommerce.service;

import com.example.ecommerce.dto.analysis.SellerStatsDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 卖家维度分析服务
 */
public interface SellerAnalysisService {

    /**
     * 卖家维度 TOPN（按 GMV / 好评率排序）
     */
    List<SellerStatsDTO> getTopSellers(LocalDate startDate, LocalDate endDate, int topN);

    /**
     * Top 卖家销售额（Top 20）
     */
    List<SellerStatsDTO> getTopSellersBySales(int topN);

    /**
     * 卖家销售额分布直方图（销售额分桶）
     */
    List<com.example.ecommerce.dto.analysis.SellerSalesDistributionDTO> getSellerSalesDistribution();

    /**
     * Top 卖家复购率（Top 20）
     */
    List<com.example.ecommerce.dto.analysis.SellerRepurchaseRateDTO> getTopSellersRepurchaseRate(int topN);

    /**
     * Top 卖家平均客单价（Top 20）
     */
    List<com.example.ecommerce.dto.analysis.SellerAverageOrderValueDTO> getTopSellersAverageOrderValue(int topN);

    /**
     * 卖家订单量趋势（按月，Top 20 卖家）
     */
    List<com.example.ecommerce.dto.analysis.SellerOrderTrendDTO> getSellerOrderTrend(int topN);

    /**
     * 卖家评分分布箱线图（各卖家评分分布）
     */
    List<com.example.ecommerce.dto.analysis.SellerReviewDistributionDTO> getSellerReviewDistribution(int topN);
}


