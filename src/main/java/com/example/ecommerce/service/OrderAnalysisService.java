package com.example.ecommerce.service;

import com.example.ecommerce.dto.analysis.ForecastPointDTO;
import com.example.ecommerce.dto.analysis.KpiOverviewDTO;
import com.example.ecommerce.dto.analysis.TimeSeriesPointDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 订单 / 运营指标分析服务
 */
public interface OrderAnalysisService {

    /**
     * 获取总体运营指标概览（如用户数、订单数、GMV 等）
     */
    KpiOverviewDTO getKpiOverview(LocalDate startDate, LocalDate endDate);

    /**
     * 获取订单按日期的趋势（订单数 / GMV）
     */
    List<TimeSeriesPointDTO> getOrderTrend(LocalDate startDate, LocalDate endDate);

    /**
     * 预测未来一段时间订单量（简单示例，可用于折线预测图）
     */
    List<ForecastPointDTO> forecastOrderVolume(int days);

    /**
     * 获取订单金额趋势（按日/周/月聚合）
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param groupBy 聚合方式：day/week/month
     * @return 时间序列数据
     */
    List<TimeSeriesPointDTO> getOrderAmountTrend(LocalDate startDate, LocalDate endDate, String groupBy);

    /**
     * 获取客单价趋势（按月统计）
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 月度客单价数据
     */
    List<com.example.ecommerce.dto.analysis.AverageOrderValueDTO> getAverageOrderValueTrend(LocalDate startDate, LocalDate endDate);

    /**
     * 获取消费金额分布直方图数据（用户金额分桶）
     * 
     * @return 金额区间分布数据
     */
    List<com.example.ecommerce.dto.analysis.CustomerAmountDistributionDTO> getCustomerAmountDistribution();
}


