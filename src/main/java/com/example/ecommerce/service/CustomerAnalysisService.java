package com.example.ecommerce.service;

import com.example.ecommerce.dto.analysis.ChurnStatsDTO;
import com.example.ecommerce.dto.analysis.RfmCustomerScoreDTO;
import com.example.ecommerce.dto.analysis.RfmRadarChartDTO;
import com.example.ecommerce.dto.analysis.SegmentDistributionDTO;
import com.example.ecommerce.dto.analysis.TopCustomerDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 客户 / 用户相关分析服务
 */
public interface CustomerAnalysisService {

    /**
     * 计算所有客户的 RFM 得分，用于列表和分布图
     */
    List<RfmCustomerScoreDTO> calculateAllCustomerRfm(LocalDate asOfDate);

    /**
     * 按用户分层统计 RFM 分布（用于饼图/柱状图）
     */
    List<SegmentDistributionDTO> getRfmSegmentDistribution(LocalDate asOfDate);

    /**
     * 获取单个客户的 RFM 雷达图数据
     */
    RfmRadarChartDTO getCustomerRfmRadar(String customerId, LocalDate asOfDate);

    /**
     * 用户活跃 / 沉睡 / 流失统计
     */
    ChurnStatsDTO getChurnStats(LocalDate asOfDate, int activeDays, int sleepingDays);

    /**
     * 高价值 / 高频客户 TOPN
     */
    List<TopCustomerDTO> getTopCustomersByAmount(int topN);

    /**
     * 使用随机森林模型预测用户是否可能复购
     *
     * @param customerId 客户ID
     * @return 复购概率（0-1）
     */
    double predictCustomerRepurchaseProbability(String customerId);

    /**
     * 获取 RFM 分析汇总统计（总客户数、各类型客户数、平均得分等）
     */
    com.example.ecommerce.dto.analysis.RfmSummaryStatsDTO getRfmSummaryStats(LocalDate asOfDate);

    /**
     * 获取 RFM 维度平均得分雷达图数据（所有用户的平均 R/F/M）
     */
    RfmRadarChartDTO getAverageRfmRadar(LocalDate asOfDate);

    /**
     * 获取 RFM 总分分布（用于柱状图）
     */
    List<com.example.ecommerce.dto.analysis.RfmScoreDistributionDTO> getRfmScoreDistribution(LocalDate asOfDate);

    /**
     * 获取复购概率分布（用于直方图）
     */
    List<com.example.ecommerce.dto.analysis.RepurchaseProbabilityDistributionDTO> getRepurchaseProbabilityDistribution();

    /**
     * 获取 RFM 散点图数据（用于展示用户价值分布）
     * X轴=Recency, Y轴=Frequency, 颜色/大小=Monetary
     * 
     * @param sampleSize 采样数量，如果为0或负数则返回全部数据
     */
    List<com.example.ecommerce.dto.analysis.RfmScatterDataDTO> getRfmScatterData(int sampleSize);
}


