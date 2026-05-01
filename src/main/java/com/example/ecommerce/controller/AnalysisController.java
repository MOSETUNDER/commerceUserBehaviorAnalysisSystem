package com.example.ecommerce.controller;

import com.example.ecommerce.common.Result;
import com.example.ecommerce.dto.analysis.KpiOverviewDTO;
import com.example.ecommerce.dto.analysis.RfmCustomerScoreDTO;
import com.example.ecommerce.dto.analysis.RfmRadarChartDTO;
import com.example.ecommerce.dto.analysis.TimeSeriesPointDTO;
import com.example.ecommerce.dto.analysis.SegmentDistributionDTO;
import com.example.ecommerce.dto.analysis.ReviewScoreDistributionDTO;
import com.example.ecommerce.dto.analysis.ChurnStatsDTO;
import com.example.ecommerce.dto.analysis.TopCustomerDTO;
import com.example.ecommerce.dto.analysis.*;
import com.example.ecommerce.dto.analysis.AverageOrderValueDTO;
import com.example.ecommerce.dto.analysis.CustomerAmountDistributionDTO;
import com.example.ecommerce.dto.analysis.ForecastPointDTO;
import com.example.ecommerce.dto.analysis.SellerStatsDTO;
import com.example.ecommerce.dto.analysis.SellerSalesDistributionDTO;
import com.example.ecommerce.dto.analysis.SellerRepurchaseRateDTO;
import com.example.ecommerce.dto.analysis.SellerAverageOrderValueDTO;
import com.example.ecommerce.dto.analysis.SellerOrderTrendDTO;
import com.example.ecommerce.dto.analysis.SellerReviewDistributionDTO;
import com.example.ecommerce.dto.analysis.*;
import com.example.ecommerce.service.CustomerAnalysisService;
import com.example.ecommerce.service.OrderAnalysisService;
import com.example.ecommerce.service.ProductAnalysisService;
import com.example.ecommerce.service.ReviewAnalysisService;
import com.example.ecommerce.service.SellerAnalysisService;
import com.example.ecommerce.service.GeoAnalysisService;
import com.example.ecommerce.service.UserBehaviorAnalysisService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户消费行为分析接口
 *
 * 说明：
 * - 前端应该在单个分析页面中拆分为多个并发请求，分别获取不同图表所需数据；
 * - 数据量较大时，前端可通过 store 缓存这些接口的响应结果，避免重复计算。
 */
@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
@Api(tags = "用户消费行为分析")
public class AnalysisController {

    private final CustomerAnalysisService customerAnalysisService;
    private final OrderAnalysisService orderAnalysisService;
    private final ProductAnalysisService productAnalysisService;
    private final ReviewAnalysisService reviewAnalysisService;
    private final SellerAnalysisService sellerAnalysisService;
    private final GeoAnalysisService geoAnalysisService;
    private final UserBehaviorAnalysisService userBehaviorAnalysisService;

    @GetMapping("/overview")
    @ApiOperation("运营指标概览（用户数/订单数/GMV/客单价）")
    public Result<KpiOverviewDTO> overview(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(orderAnalysisService.getKpiOverview(startDate, endDate));
    }

    @GetMapping("/order-trend")
    @ApiOperation("订单趋势（按日期统计订单数与GMV，用于折线图）")
    public Result<List<TimeSeriesPointDTO>> orderTrend(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(orderAnalysisService.getOrderTrend(startDate, endDate));
    }

    @GetMapping("/order-amount-trend")
    @ApiOperation("订单金额趋势（按日/周/月聚合，用于折线图）")
    public Result<List<TimeSeriesPointDTO>> orderAmountTrend(
            @RequestParam(required = false, defaultValue = "day") String groupBy) {
        return Result.success(orderAnalysisService.getOrderAmountTrend(null, null, groupBy));
    }

    @GetMapping("/average-order-value-trend")
    @ApiOperation("客单价趋势（按月统计，用于折线图）")
    public Result<List<AverageOrderValueDTO>> averageOrderValueTrend() {
        return Result.success(orderAnalysisService.getAverageOrderValueTrend(null, null));
    }

    @GetMapping("/customer-amount-distribution")
    @ApiOperation("消费金额分布直方图（用户金额分桶）")
    public Result<List<CustomerAmountDistributionDTO>> customerAmountDistribution() {
        return Result.success(orderAnalysisService.getCustomerAmountDistribution());
    }

    @GetMapping("/rfm/list")
    @ApiOperation("全量客户 RFM 列表与分层信息（支持分页，避免返回过多数据）")
    public Result<List<RfmCustomerScoreDTO>> rfmList(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "500") int size) {
        // 限制每页最大数量，避免单次返回过多数据
        size = Math.min(size, 1000);
        
        List<RfmCustomerScoreDTO> all = customerAnalysisService.calculateAllCustomerRfm(null);
        // 分页处理，避免返回过多数据导致超时
        int start = Math.max(0, page * size);
        int end = Math.min(all.size(), start + size);
        if (start >= all.size()) {
            return Result.success(Collections.emptyList());
        }
        return Result.success(all.subList(start, end));
    }

    @GetMapping("/rfm/segment-distribution")
    @ApiOperation("RFM 用户分层分布（用于饼图/柱状图）")
    public Result<List<SegmentDistributionDTO>> rfmSegmentDistribution() {
        return Result.success(customerAnalysisService.getRfmSegmentDistribution(null));
    }

    @GetMapping("/rfm/radar/{customerId}")
    @ApiOperation("单个客户 RFM 雷达图数据（customerId 实际是 customer_unique_id）")
    public Result<RfmRadarChartDTO> rfmRadar(@PathVariable String customerId) {
        return Result.success(customerAnalysisService.getCustomerRfmRadar(customerId, null));
    }

    @GetMapping("/rfm/summary-stats")
    @ApiOperation("RFM 分析汇总统计（总客户数、各类型客户数、平均得分）")
    public Result<com.example.ecommerce.dto.analysis.RfmSummaryStatsDTO> rfmSummaryStats() {
        return Result.success(customerAnalysisService.getRfmSummaryStats(null));
    }

    @GetMapping("/rfm/average-radar")
    @ApiOperation("RFM 维度平均得分雷达图（所有用户的平均 R/F/M）")
    public Result<RfmRadarChartDTO> averageRfmRadar() {
        return Result.success(customerAnalysisService.getAverageRfmRadar(null));
    }

    @GetMapping("/rfm/score-distribution")
    @ApiOperation("RFM 总分分布（用于柱状图）")
    public Result<List<com.example.ecommerce.dto.analysis.RfmScoreDistributionDTO>> rfmScoreDistribution() {
        return Result.success(customerAnalysisService.getRfmScoreDistribution(null));
    }

    @GetMapping("/review/score-distribution")
    @ApiOperation("订单评价得分分布（1-5 星）")
    public Result<List<ReviewScoreDistributionDTO>> reviewScoreDistribution(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(reviewAnalysisService.getReviewScoreDistribution(startDate, endDate));
    }

    @GetMapping("/prediction/repurchase/{customerId}")
    @ApiOperation("随机森林模型预测 - 客户复购概率（customerId 实际是 customer_unique_id）")
    public Result<Double> predictRepurchase(@PathVariable String customerId) {
        return Result.success(customerAnalysisService.predictCustomerRepurchaseProbability(customerId));
    }

    @GetMapping("/churn/stats")
    @ApiOperation("用户活跃 / 沉睡 / 流失统计")
    public Result<ChurnStatsDTO> churnStats(
            @RequestParam(required = false, defaultValue = "30") int activeDays,
            @RequestParam(required = false, defaultValue = "90") int sleepingDays) {
        return Result.success(customerAnalysisService.getChurnStats(null, activeDays, sleepingDays));
    }

    @GetMapping("/top/customers")
    @ApiOperation("高价值客户 TOPN")
    public Result<List<TopCustomerDTO>> topCustomers(
            @RequestParam(required = false, defaultValue = "10") int topN) {
        return Result.success(customerAnalysisService.getTopCustomersByAmount(topN));
    }

    @GetMapping("/repurchase/probability-distribution")
    @ApiOperation("复购概率分布（用于直方图）")
    public Result<List<com.example.ecommerce.dto.analysis.RepurchaseProbabilityDistributionDTO>> repurchaseProbabilityDistribution() {
        return Result.success(customerAnalysisService.getRepurchaseProbabilityDistribution());
    }

    @GetMapping("/rfm/scatter")
    @ApiOperation("RFM 散点图数据（X轴=Recency, Y轴=Frequency, 颜色/大小=Monetary）")
    public Result<List<com.example.ecommerce.dto.analysis.RfmScatterDataDTO>> rfmScatter(
            @RequestParam(required = false, defaultValue = "5000") int sampleSize) {
        // 限制最大采样数量，避免返回过多数据
        sampleSize = Math.min(sampleSize, 10000);
        return Result.success(customerAnalysisService.getRfmScatterData(sampleSize));
    }

    @GetMapping("/products/top-categories")
    @ApiOperation("Top N 品类销售额柱状图")
    public Result<List<CategorySalesDTO>> getTopCategorySales(
            @RequestParam(required = false, defaultValue = "10") int topN) {
        return Result.success(productAnalysisService.getTopCategorySales(topN));
    }

    @GetMapping("/products/category-repurchase-heatmap")
    @ApiOperation("品类复购率热力图（品类 × 用户分层）")
    public Result<List<CategoryRepurchaseHeatmapDTO>> getCategoryRepurchaseHeatmap() {
        return Result.success(productAnalysisService.getCategoryRepurchaseHeatmap());
    }

    @GetMapping("/products/top-sales")
    @ApiOperation("Top N 商品销售条形图")
    public Result<List<ProductSalesDTO>> getTopProductSales(
            @RequestParam(required = false, defaultValue = "20") int topN) {
        return Result.success(productAnalysisService.getTopProductSales(topN));
    }

    @GetMapping("/products/category-state-sales")
    @ApiOperation("品类各州销售地图")
    public Result<List<CategoryStateSalesDTO>> getCategoryStateSales() {
        return Result.success(productAnalysisService.getCategoryStateSales());
    }

    @GetMapping("/forecast/order-volume")
    @ApiOperation("订单量预测（简单移动平均示例）")
    public Result<List<ForecastPointDTO>> forecastOrderVolume(
            @RequestParam(required = false, defaultValue = "7") int days) {
        return Result.success(orderAnalysisService.forecastOrderVolume(days));
    }

    @GetMapping("/top/sellers")
    @ApiOperation("卖家维度 TOPN（订单数 / GMV / 好评率）")
    public Result<List<SellerStatsDTO>> topSellers(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false, defaultValue = "10") int topN) {
        return Result.success(sellerAnalysisService.getTopSellers(startDate, endDate, topN));
    }

    @GetMapping("/sellers/top-sales")
    @ApiOperation("Top 卖家销售额（Top 20）")
    public Result<List<SellerStatsDTO>> topSellersBySales(
            @RequestParam(required = false, defaultValue = "20") int topN) {
        return Result.success(sellerAnalysisService.getTopSellersBySales(topN));
    }

    @GetMapping("/sellers/sales-distribution")
    @ApiOperation("卖家销售额分布直方图（销售额分桶）")
    public Result<List<SellerSalesDistributionDTO>> sellerSalesDistribution() {
        return Result.success(sellerAnalysisService.getSellerSalesDistribution());
    }

    @GetMapping("/sellers/repurchase-rate")
    @ApiOperation("Top 卖家复购率（Top 20）")
    public Result<List<SellerRepurchaseRateDTO>> topSellersRepurchaseRate(
            @RequestParam(required = false, defaultValue = "20") int topN) {
        return Result.success(sellerAnalysisService.getTopSellersRepurchaseRate(topN));
    }

    @GetMapping("/sellers/average-order-value")
    @ApiOperation("Top 卖家平均客单价（Top 20）")
    public Result<List<SellerAverageOrderValueDTO>> topSellersAverageOrderValue(
            @RequestParam(required = false, defaultValue = "20") int topN) {
        return Result.success(sellerAnalysisService.getTopSellersAverageOrderValue(topN));
    }

    @GetMapping("/sellers/order-trend")
    @ApiOperation("卖家订单量趋势（按月，Top 20 卖家）")
    public Result<List<SellerOrderTrendDTO>> sellerOrderTrend(
            @RequestParam(required = false, defaultValue = "20") int topN) {
        return Result.success(sellerAnalysisService.getSellerOrderTrend(topN));
    }

    @GetMapping("/sellers/review-distribution")
    @ApiOperation("卖家评分分布箱线图（各卖家评分分布）")
    public Result<List<SellerReviewDistributionDTO>> sellerReviewDistribution(
            @RequestParam(required = false, defaultValue = "20") int topN) {
        return Result.success(sellerAnalysisService.getSellerReviewDistribution(topN));
    }

    @GetMapping("/geo/sales-heatmap")
    @ApiOperation("各州销售额热力图（用于销售地图）")
    public Result<List<StateSalesHeatmapDTO>> getStateSalesHeatmap() {
        return Result.success(geoAnalysisService.getStateSalesHeatmap());
    }

    @GetMapping("/geo/order-count")
    @ApiOperation("各州订单量柱状图（Top N）")
    public Result<List<StateOrderCountDTO>> getStateOrderCount(
            @RequestParam(required = false, defaultValue = "10") int topN) {
        return Result.success(geoAnalysisService.getStateOrderCount(topN));
    }

    @GetMapping("/geo/average-order-value")
    @ApiOperation("各州平均客单价柱状图（Top N）")
    public Result<List<StateAverageOrderValueDTO>> getStateAverageOrderValue(
            @RequestParam(required = false, defaultValue = "10") int topN) {
        return Result.success(geoAnalysisService.getStateAverageOrderValue(topN));
    }

    @GetMapping("/geo/repurchase-rate")
    @ApiOperation("各州复购率柱状图（Top N）")
    public Result<List<StateRepurchaseRateDTO>> getStateRepurchaseRate(
            @RequestParam(required = false, defaultValue = "10") int topN) {
        return Result.success(geoAnalysisService.getStateRepurchaseRate(topN));
    }

    @GetMapping("/geo/logistics-efficiency")
    @ApiOperation("物流时效分布直方图（各州平均物流天数）")
    public Result<List<LogisticsEfficiencyDTO>> getLogisticsEfficiency() {
        return Result.success(geoAnalysisService.getLogisticsEfficiency());
    }

    @GetMapping("/geo/user-growth")
    @ApiOperation("地域用户增长折线图（按月统计）")
    public Result<List<GeoUserGrowthDTO>> getGeoUserGrowth() {
        return Result.success(geoAnalysisService.getGeoUserGrowth());
    }

    @GetMapping("/behavior/active-time-heatmap")
    @ApiOperation("用户活跃时间热力图（周几 × 几点）")
    public Result<List<UserActiveTimeHeatmapDTO>> getUserActiveTimeHeatmap() {
        return Result.success(userBehaviorAnalysisService.getUserActiveTimeHeatmap());
    }

    @GetMapping("/behavior/order-funnel")
    @ApiOperation("订单状态漏斗图（下单 → 批准 → 发货 → 交付）")
    public Result<List<OrderFunnelDTO>> getOrderFunnel() {
        return Result.success(userBehaviorAnalysisService.getOrderFunnel());
    }

    @GetMapping("/behavior/sankey")
    @ApiOperation("用户购买行为桑基图（首次购买 → 复购流转）")
    public Result<UserBehaviorSankeyDTO> getUserBehaviorSankey() {
        return Result.success(userBehaviorAnalysisService.getUserBehaviorSankey());
    }

    @GetMapping(value = "/export/rfm", produces = "text/csv;charset=UTF-8")
    @ApiOperation("RFM 分析结果导出（CSV）")
    public void exportRfmCsv(HttpServletResponse response) throws IOException {
        List<RfmCustomerScoreDTO> list = customerAnalysisService.calculateAllCustomerRfm(null);

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"rfm_analysis.csv\"");

        StringBuilder sb = new StringBuilder();
        // 表头（注意：customerId 实际是 customer_unique_id，真实用户ID）
        sb.append("customerUniqueId,lastOrderDate,orderCount,totalAmount,rScore,fScore,mScore,rfmScore,segmentLabel\n");
        for (RfmCustomerScoreDTO dto : list) {
            sb.append(dto.getCustomerId()).append(","); // 实际是 customer_unique_id
            sb.append(dto.getLastOrderDate() == null ? "" : dto.getLastOrderDate()).append(",");
            sb.append(dto.getOrderCount()).append(",");
            sb.append(dto.getTotalAmount()).append(",");
            sb.append(dto.getRScore()).append(",");
            sb.append(dto.getFScore()).append(",");
            sb.append(dto.getMScore()).append(",");
            sb.append(dto.getRfmScore()).append(",");
            sb.append(dto.getSegmentLabel() == null ? "" : dto.getSegmentLabel().replace(",", " "));
            sb.append("\n");
        }

        response.getWriter().write(sb.toString());
        response.getWriter().flush();
    }
}


