<script setup>
import { onMounted, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Card, Row, Col, Spin } from 'ant-design-vue'
import { useSellerStore } from '../store/seller'

const store = useSellerStore()

const topSalesBarRef = ref(null)
const salesDistRef = ref(null)
const repurchaseRateRef = ref(null)
const avgOrderValueRef = ref(null)
const orderTrendRef = ref(null)
const reviewDistRef = ref(null)

let topSalesBarChart
let salesDistChart
let repurchaseRateChart
let avgOrderValueChart
let orderTrendChart
let reviewDistChart

const loadAll = async () => {
  await Promise.all([
    store.loadTopSellersBySales(20),
    store.loadSalesDistribution(),
    store.loadRepurchaseRate(20),
    store.loadAverageOrderValue(20),
    store.loadOrderTrend(20),
    store.loadReviewDistribution(20)
  ])
  await nextTick()
  renderCharts()
}

const renderCharts = () => {
  // Top 卖家销售额柱状图
  if (topSalesBarRef.value && store.topSellersBySales.length) {
    topSalesBarChart = topSalesBarChart || echarts.init(topSalesBarRef.value)
    const names = store.topSellersBySales.map((s) => s.sellerId.substring(0, 8) + '...')
    const gmvs = store.topSellersBySales.map((s) => Number(s.totalGmv))
    topSalesBarChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params) => {
          const param = params[0]
          const seller = store.topSellersBySales[param.dataIndex]
          return `${seller.sellerId}<br/>销售额: ${Number(param.value).toLocaleString()}<br/>订单数: ${seller.orderCount}`
        }
      },
      title: {
        text: 'Top 20 卖家销售额',
        left: 'center',
        top: 10
      },
      grid: {
        left: '10%',
        right: '10%',
        top: '15%',
        bottom: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: names,
        axisLabel: {
          rotate: 45
        }
      },
      yAxis: {
        type: 'value',
        name: '销售额',
        axisLabel: {
          formatter: (value) => {
            if (value >= 1000) {
              return (value / 1000).toFixed(1) + 'K'
            }
            return value.toString()
          }
        }
      },
      series: [{
        name: '销售额',
        type: 'bar',
        data: gmvs,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#83bff6' },
            { offset: 0.5, color: '#188df0' },
            { offset: 1, color: '#188df0' }
          ])
        }
      }]
    })
    topSalesBarChart.resize()
  }

  // 卖家销售额分布直方图
  if (salesDistRef.value && store.salesDistribution.length) {
    salesDistChart = salesDistChart || echarts.init(salesDistRef.value)
    const xData = store.salesDistribution.map((d) => d.salesRange)
    const counts = store.salesDistribution.map((d) => d.sellerCount)
    salesDistChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params) => {
          const param = params[0]
          return `${param.name}<br/>卖家数: ${param.value}`
        }
      },
      title: {
        text: '卖家销售额分布',
        left: 'center',
        top: 10
      },
      grid: {
        left: '10%',
        right: '10%',
        top: '15%',
        bottom: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: xData,
        name: '销售额区间',
        axisLabel: {
          rotate: 45
        }
      },
      yAxis: {
        type: 'value',
        name: '卖家数量'
      },
      series: [{
        name: '卖家数',
        type: 'bar',
        data: counts,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#ffd93d' },
            { offset: 0.5, color: '#ff6b6b' },
            { offset: 1, color: '#ff6b6b' }
          ])
        }
      }]
    })
    salesDistChart.resize()
  }

  // 卖家复购率柱状图
  if (repurchaseRateRef.value && store.repurchaseRate.length) {
    repurchaseRateChart = repurchaseRateChart || echarts.init(repurchaseRateRef.value)
    const names = store.repurchaseRate.map((s) => s.sellerId.substring(0, 8) + '...')
    const rates = store.repurchaseRate.map((s) => Math.round(s.repurchaseRate * 100))
    repurchaseRateChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params) => {
          const param = params[0]
          const seller = store.repurchaseRate[param.dataIndex]
          return `${seller.sellerId}<br/>复购率: ${param.value}%<br/>总客户数: ${seller.totalCustomers}<br/>复购客户数: ${seller.repurchaseCustomers}`
        }
      },
      title: {
        text: 'Top 20 卖家复购率',
        left: 'center',
        top: 10
      },
      grid: {
        left: '10%',
        right: '10%',
        top: '15%',
        bottom: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: names,
        axisLabel: {
          rotate: 45
        }
      },
      yAxis: {
        type: 'value',
        name: '复购率(%)',
        max: 100
      },
      series: [{
        name: '复购率',
        type: 'bar',
        data: rates,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#95e1d3' },
            { offset: 0.5, color: '#f38181' },
            { offset: 1, color: '#f38181' }
          ])
        }
      }]
    })
    repurchaseRateChart.resize()
  }

  // 卖家平均客单价柱状图
  if (avgOrderValueRef.value && store.averageOrderValue.length) {
    avgOrderValueChart = avgOrderValueChart || echarts.init(avgOrderValueRef.value)
    const names = store.averageOrderValue.map((s) => s.sellerId.substring(0, 8) + '...')
    const aovs = store.averageOrderValue.map((s) => Number(s.averageOrderValue))
    avgOrderValueChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params) => {
          const param = params[0]
          const seller = store.averageOrderValue[param.dataIndex]
          return `${seller.sellerId}<br/>平均客单价: ${Number(param.value).toFixed(2)}<br/>订单数: ${seller.orderCount}<br/>总GMV: ${Number(seller.totalGmv).toLocaleString()}`
        }
      },
      title: {
        text: 'Top 20 卖家平均客单价',
        left: 'center',
        top: 10
      },
      grid: {
        left: '10%',
        right: '10%',
        top: '15%',
        bottom: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: names,
        axisLabel: {
          rotate: 45
        }
      },
      yAxis: {
        type: 'value',
        name: '平均客单价'
      },
      series: [{
        name: '平均客单价',
        type: 'bar',
        data: aovs,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#a8edea' },
            { offset: 0.5, color: '#fed6e3' },
            { offset: 1, color: '#fed6e3' }
          ])
        }
      }]
    })
    avgOrderValueChart.resize()
  }

  // 卖家订单量趋势折线图（按月）
  if (orderTrendRef.value && store.orderTrend.length) {
    orderTrendChart = orderTrendChart || echarts.init(orderTrendRef.value)
    
    // 按卖家分组数据
    const sellerDataMap = new Map()
    store.orderTrend.forEach(item => {
      if (!sellerDataMap.has(item.sellerId)) {
        sellerDataMap.set(item.sellerId, [])
      }
      sellerDataMap.get(item.sellerId).push(item)
    })

    // 获取所有月份
    const allMonths = new Set()
    store.orderTrend.forEach(item => allMonths.add(item.month))
    const sortedMonths = Array.from(allMonths).sort()

    // 构建 series 数据（最多显示前10个卖家）
    const topSellers = Array.from(sellerDataMap.keys()).slice(0, 10)
    const series = topSellers.map(sellerId => {
      const sellerItems = sellerDataMap.get(sellerId)
      const data = sortedMonths.map(month => {
        const item = sellerItems.find(i => i.month === month)
        return item ? item.orderCount : 0
      })
      return {
        name: sellerId.substring(0, 8) + '...',
        type: 'line',
        data: data,
        smooth: true
      }
    })

    orderTrendChart.setOption({
      tooltip: {
        trigger: 'axis'
      },
      title: {
        text: 'Top 20 卖家订单量趋势（按月）',
        left: 'center',
        top: 10
      },
      legend: {
        data: topSellers.map(id => id.substring(0, 8) + '...'),
        top: 35,
        type: 'scroll'
      },
      grid: {
        left: '10%',
        right: '10%',
        top: '20%',
        bottom: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: sortedMonths,
        boundaryGap: false
      },
      yAxis: {
        type: 'value',
        name: '订单数'
      },
      series: series
    })
    orderTrendChart.resize()
  }

  // 卖家评分分布箱线图
  if (reviewDistRef.value && store.reviewDistribution.length) {
    reviewDistChart = reviewDistChart || echarts.init(reviewDistRef.value)
    
    const categories = store.reviewDistribution.map(s => s.sellerId.substring(0, 8) + '...')
    const boxData = store.reviewDistribution.map(s => {
      const scores = s.reviewScores
      scores.sort((a, b) => a - b)
      const q1Index = Math.floor(scores.length * 0.25)
      const medianIndex = Math.floor(scores.length * 0.5)
      const q3Index = Math.floor(scores.length * 0.75)
      return [
        scores[0], // min
        scores[q1Index], // Q1
        scores[medianIndex], // median
        scores[q3Index], // Q3
        scores[scores.length - 1] // max
      ]
    })

    reviewDistChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: (params) => {
          const index = params.dataIndex
          const seller = store.reviewDistribution[index]
          return `${seller.sellerId}<br/>平均评分: ${seller.averageScore.toFixed(2)}<br/>最低: ${seller.minScore}<br/>最高: ${seller.maxScore}<br/>中位数: ${seller.medianScore}`
        }
      },
      title: {
        text: 'Top 20 卖家评分分布',
        left: 'center',
        top: 10
      },
      grid: {
        left: '10%',
        right: '10%',
        top: '15%',
        bottom: '20%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: categories,
        boundaryGap: true,
        nameGap: 30,
        splitArea: {
          show: false
        },
        splitLine: {
          show: false
        },
        axisLabel: {
          rotate: 45
        }
      },
      yAxis: {
        type: 'value',
        name: '评分',
        min: 1,
        max: 5,
        splitArea: {
          show: true
        }
      },
      series: [{
        name: '评分分布',
        type: 'boxplot',
        data: boxData,
        itemStyle: {
          color: '#b8e994',
          borderColor: '#6c5ce7'
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }]
    })
    reviewDistChart.resize()
  }
}

// 窗口大小改变时重新调整图表
const handleResize = () => {
  topSalesBarChart?.resize()
  salesDistChart?.resize()
  repurchaseRateChart?.resize()
  avgOrderValueChart?.resize()
  orderTrendChart?.resize()
  reviewDistChart?.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})
</script>

<template>
  <div class="page-wrapper">
    <Row :gutter="[16, 16]">
      <Col :span="24">
        <Card title="Top 20 卖家销售额">
          <Spin :spinning="store.loading.topSellers">
            <div ref="topSalesBarRef" class="chart-large"></div>
          </Spin>
        </Card>
      </Col>

      <Col :span="12">
        <Card title="卖家销售额分布">
          <Spin :spinning="store.loading.salesDistribution">
            <div ref="salesDistRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>
      <Col :span="12">
        <Card title="Top 20 卖家复购率">
          <Spin :spinning="store.loading.repurchaseRate">
            <div ref="repurchaseRateRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>

      <Col :span="24">
        <Card title="Top 20 卖家平均客单价">
          <Spin :spinning="store.loading.averageOrderValue">
            <div ref="avgOrderValueRef" class="chart-large"></div>
          </Spin>
        </Card>
      </Col>

      <Col :span="24">
        <Card title="Top 20 卖家订单量趋势（按月）">
          <Spin :spinning="store.loading.orderTrend">
            <div ref="orderTrendRef" class="chart-large"></div>
          </Spin>
        </Card>
      </Col>

      <Col :span="24">
        <Card title="Top 20 卖家评分分布">
          <Spin :spinning="store.loading.reviewDistribution">
            <div ref="reviewDistRef" class="chart-large"></div>
          </Spin>
        </Card>
      </Col>
    </Row>
  </div>
</template>

<style scoped>
.page-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chart {
  width: 100%;
  height: 280px;
}

.chart-large {
  width: 100%;
  height: 400px;
}
</style>
