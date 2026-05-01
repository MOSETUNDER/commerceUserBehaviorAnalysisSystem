import http from './http'

// 运营总览
export function fetchOverview(params) {
  return http.get('/analysis/overview', { params })
}

// 订单趋势
export function fetchOrderTrend(params) {
  return http.get('/analysis/order-trend', { params })
}

// 评价分数分布
export function fetchReviewScoreDistribution(params) {
  return http.get('/analysis/review/score-distribution', { params })
}

// 订单量预测
export function fetchOrderForecast(params) {
  return http.get('/analysis/forecast/order-volume', { params })
}

// RFM 分层分布
export function fetchRfmSegmentDistribution() {
  return http.get('/analysis/rfm/segment-distribution')
}

// RFM 客户列表
export function fetchRfmCustomerList(params) {
  return http.get('/analysis/rfm/list', { params })
}

// RFM 雷达图
export function fetchRfmRadar(customerId) {
  return http.get(`/analysis/rfm/radar/${customerId}`)
}

// 用户流失统计
export function fetchChurnStats() {
  return http.get('/analysis/churn/stats')
}

// 高价值客户 TOPN
export function fetchTopCustomers(params) {
  return http.get('/analysis/top/customers', { params })
}

// 卖家 TOPN
export function fetchTopSellers(params) {
  return http.get('/analysis/top/sellers', { params })
}

// Top 卖家销售额
export function fetchTopSellersBySales(topN = 20) {
  return http.get('/analysis/sellers/top-sales', { params: { topN } })
}

// 卖家销售额分布
export function fetchSellerSalesDistribution() {
  return http.get('/analysis/sellers/sales-distribution')
}

// Top 卖家复购率
export function fetchTopSellersRepurchaseRate(topN = 20) {
  return http.get('/analysis/sellers/repurchase-rate', { params: { topN } })
}

// Top 卖家平均客单价
export function fetchTopSellersAverageOrderValue(topN = 20) {
  return http.get('/analysis/sellers/average-order-value', { params: { topN } })
}

// 卖家订单量趋势
export function fetchSellerOrderTrend(topN = 20) {
  return http.get('/analysis/sellers/order-trend', { params: { topN } })
}

// 卖家评分分布
export function fetchSellerReviewDistribution(topN = 20) {
  return http.get('/analysis/sellers/review-distribution', { params: { topN } })
}

// 各州销售额热力图（用于销售地图）
export function fetchStateSalesHeatmap() {
  return http.get('/analysis/geo/sales-heatmap')
}

// 各州订单量柱状图（Top N）
export function fetchStateOrderCount(topN = 10) {
  return http.get('/analysis/geo/order-count', { params: { topN } })
}

// 各州平均客单价柱状图（Top N）
export function fetchStateAverageOrderValue(topN = 10) {
  return http.get('/analysis/geo/average-order-value', { params: { topN } })
}

// 各州复购率柱状图（Top N）
export function fetchStateRepurchaseRate(topN = 10) {
  return http.get('/analysis/geo/repurchase-rate', { params: { topN } })
}

// 物流时效分布直方图（各州平均物流天数）
export function fetchLogisticsEfficiency() {
  return http.get('/analysis/geo/logistics-efficiency')
}

// 地域用户增长折线图（按月统计）
export function fetchGeoUserGrowth() {
  return http.get('/analysis/geo/user-growth')
}

// 客户复购预测
export function fetchRepurchaseProbability(customerId) {
  return http.get(`/analysis/prediction/repurchase/${customerId}`)
}

// RFM 汇总统计
export function fetchRfmSummaryStats() {
  return http.get('/analysis/rfm/summary-stats')
}

// RFM 平均得分雷达图
export function fetchAverageRfmRadar() {
  return http.get('/analysis/rfm/average-radar')
}

// RFM 总分分布
export function fetchRfmScoreDistribution() {
  return http.get('/analysis/rfm/score-distribution')
}

// 复购概率分布
export function fetchRepurchaseProbabilityDistribution() {
  return http.get('/analysis/repurchase/probability-distribution')
}

// RFM 散点图数据
export function fetchRfmScatterData(sampleSize = 5000) {
  return http.get('/analysis/rfm/scatter', { params: { sampleSize } })
}

// 订单金额趋势（按日/周/月）
export function fetchOrderAmountTrend(groupBy = 'day') {
  return http.get('/analysis/order-amount-trend', { params: { groupBy } })
}

// 客单价趋势（按月）
export function fetchAverageOrderValueTrend() {
  return http.get('/analysis/average-order-value-trend')
}

// 消费金额分布直方图
export function fetchCustomerAmountDistribution() {
  return http.get('/analysis/customer-amount-distribution')
}

// Top N 品类销售额
export function fetchTopCategorySales(topN = 10) {
  return http.get('/analysis/products/top-categories', { params: { topN } })
}

// 品类复购率热力图
export function fetchCategoryRepurchaseHeatmap() {
  return http.get('/analysis/products/category-repurchase-heatmap')
}

// Top N 商品销售
export function fetchTopProductSales(topN = 20) {
  return http.get('/analysis/products/top-sales', { params: { topN } })
}

// 品类各州销售地图
export function fetchCategoryStateSales() {
  return http.get('/analysis/products/category-state-sales')
}

// 用户活跃时间热力图
export function fetchUserActiveTimeHeatmap() {
  return http.get('/analysis/behavior/active-time-heatmap')
}

// 订单状态漏斗图
export function fetchOrderFunnel() {
  return http.get('/analysis/behavior/order-funnel')
}

// 用户购买行为桑基图
export function fetchUserBehaviorSankey() {
  return http.get('/analysis/behavior/sankey')
}

// RFM 分析结果导出（CSV）
export function exportRfmCsv() {
  return http.get('/analysis/export/rfm', {
    responseType: 'blob' // 重要：指定响应类型为 blob，用于下载文件
  })
}

