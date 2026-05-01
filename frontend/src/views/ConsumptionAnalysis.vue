<script setup>
import { onMounted, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useAnalysisStore } from '../store/analysis'
import { Card, Row, Col, Spin } from 'ant-design-vue'

const store = useAnalysisStore()

const overviewCardRef = ref(null)
const orderTrendRef = ref(null)
const reviewBarRef = ref(null)
const orderAmountTrendRef = ref(null)
const averageOrderValueRef = ref(null)
const customerAmountDistRef = ref(null)

let orderTrendChart
let reviewBarChart
let orderAmountTrendChart
let averageOrderValueChart
let customerAmountDistChart

const loadAll = async () => {
  await Promise.all([
    store.loadOverview(),
    store.loadOrderTrend(),
    store.loadReviewScoreDistribution(),
    store.loadOrderAmountTrend('day'),
    store.loadAverageOrderValueTrend(),
    store.loadCustomerAmountDistribution()
  ])
  await nextTick()
  renderCharts()
}

const renderCharts = () => {
  // 订单趋势图（订单数 & GMV）
  if (orderTrendRef.value && store.orderTrend.length) {
    orderTrendChart = orderTrendChart || echarts.init(orderTrendRef.value)
    const xData = store.orderTrend.map((d) => d.date)
    const orderSeries = store.orderTrend.map((d) => d.orderCount)
    const gmvSeries = store.orderTrend.map((d) => d.gmv)
    orderTrendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['订单数', 'GMV'] },
      xAxis: { type: 'category', data: xData },
      yAxis: [
        { type: 'value', name: '订单数' },
        { type: 'value', name: 'GMV' }
      ],
      series: [
        { name: '订单数', type: 'line', data: orderSeries, smooth: true },
        { name: 'GMV', type: 'bar', yAxisIndex: 1, data: gmvSeries }
      ]
    })
    orderTrendChart.resize()
  }

  // 评价分数分布
  if (reviewBarRef.value && store.reviewScoreDistribution.length) {
    reviewBarChart = reviewBarChart || echarts.init(reviewBarRef.value)
    const xData = store.reviewScoreDistribution.map((d) => d.score)
    const counts = store.reviewScoreDistribution.map((d) => d.count)
    reviewBarChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params) => {
          const param = params[0]
          const total = counts.reduce((sum, val) => sum + val, 0)
          const percentage = total > 0 ? ((param.value / total) * 100).toFixed(2) : 0
          return `${param.name} 星<br/>数量: ${param.value}<br/>占比: ${percentage}%`
        }
      },
      grid: {
        left: '10%', // 增加左侧边距，确保 Y 轴标签完整显示
        right: '10%',
        top: '10%',
        bottom: '10%',
        containLabel: true // 确保坐标轴标签不被截断
      },
      title: {
        text: '订单评价分数分布',
        left: 'center',
        top: 10
      },
      xAxis: {
        type: 'category',
        data: xData,
        name: '评分',
        nameLocation: 'middle',
        nameGap: 30
      },
      yAxis: {
        type: 'value',
        name: '数量',
        nameLocation: 'middle',
        nameGap: 50,
        axisLabel: {
          formatter: (value) => {
            // 格式化大数值，避免显示过长
            if (value >= 1000) {
              return (value / 1000).toFixed(1) + 'K'
            }
            return value.toString()
          }
        }
      },
      series: [{
        name: '评价数量',
        type: 'bar',
        data: counts,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#83bff6' },
            { offset: 0.5, color: '#188df0' },
            { offset: 1, color: '#188df0' }
          ])
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#2378f7' },
              { offset: 0.7, color: '#2378f7' },
              { offset: 1, color: '#83bff6' }
            ])
          }
        }
      }]
    })
    reviewBarChart.resize()
  }

  // 订单金额趋势折线图（日/周/月）
  if (orderAmountTrendRef.value && store.orderAmountTrend.length) {
    orderAmountTrendChart = orderAmountTrendChart || echarts.init(orderAmountTrendRef.value)
    const xData = store.orderAmountTrend.map((d) => d.date)
    const gmvSeries = store.orderAmountTrend.map((d) => Number(d.gmv))
    orderAmountTrendChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params) => {
          const param = params[0]
          return `${param.name}<br/>订单金额: ${Number(param.value).toLocaleString()}`
        }
      },
      title: {
        text: '订单金额趋势',
        left: 'center',
        top: 10
      },
      xAxis: {
        type: 'category',
        data: xData,
        name: '日期'
      },
      yAxis: {
        type: 'value',
        name: '订单金额',
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
        name: '订单金额',
        type: 'line',
        data: gmvSeries,
        smooth: true,
        areaStyle: {
          opacity: 0.3
        },
        lineStyle: {
          width: 2
        }
      }]
    })
    orderAmountTrendChart.resize()
  }

  // 客单价趋势折线图（按月）
  if (averageOrderValueRef.value && store.averageOrderValueTrend.length) {
    averageOrderValueChart = averageOrderValueChart || echarts.init(averageOrderValueRef.value)
    const xData = store.averageOrderValueTrend.map((d) => d.month)
    const aovSeries = store.averageOrderValueTrend.map((d) => Number(d.averageOrderValue))
    averageOrderValueChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params) => {
          const param = params[0]
          const item = store.averageOrderValueTrend[param.dataIndex]
          return `${param.name}<br/>客单价: ${Number(param.value).toFixed(2)}<br/>订单数: ${item.orderCount}<br/>GMV: ${Number(item.totalGmv).toLocaleString()}`
        }
      },
      title: {
        text: '客单价趋势',
        left: 'center',
        top: 10
      },
      xAxis: {
        type: 'category',
        data: xData,
        name: '月份'
      },
      yAxis: {
        type: 'value',
        name: '客单价',
        axisLabel: {
          formatter: (value) => value.toFixed(0)
        }
      },
      series: [{
        name: '客单价',
        type: 'line',
        data: aovSeries,
        smooth: true,
        lineStyle: {
          width: 2,
          color: '#1890ff'
        },
        itemStyle: {
          color: '#1890ff'
        },
        markLine: {
          data: [
            { type: 'average', name: '平均值' }
          ]
        }
      }]
    })
    averageOrderValueChart.resize()
  }

  // 消费金额分布直方图
  if (customerAmountDistRef.value && store.customerAmountDistribution.length) {
    customerAmountDistChart = customerAmountDistChart || echarts.init(customerAmountDistRef.value)
    const xData = store.customerAmountDistribution.map((d) => d.amountRange)
    const counts = store.customerAmountDistribution.map((d) => d.customerCount)
    customerAmountDistChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params) => {
          const param = params[0]
          const total = counts.reduce((sum, val) => sum + val, 0)
          const percentage = total > 0 ? ((param.value / total) * 100).toFixed(2) : 0
          return `${param.name}<br/>客户数: ${param.value}<br/>占比: ${percentage}%`
        }
      },
      title: {
        text: '消费金额分布',
        left: 'center',
        top: 10
      },
      xAxis: {
        type: 'category',
        data: xData,
        name: '金额区间',
        axisLabel: {
          rotate: 45
        }
      },
      yAxis: {
        type: 'value',
        name: '客户数量'
      },
      series: [{
        name: '客户数',
        type: 'bar',
        data: counts,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#83bff6' },
            { offset: 0.5, color: '#188df0' },
            { offset: 1, color: '#188df0' }
          ])
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#2378f7' },
              { offset: 0.7, color: '#2378f7' },
              { offset: 1, color: '#83bff6' }
            ])
          }
        }
      }]
    })
    customerAmountDistChart.resize()
  }
}

// 窗口大小改变时重新调整图表
const handleResize = () => {
  orderTrendChart?.resize()
  reviewBarChart?.resize()
  orderAmountTrendChart?.resize()
  averageOrderValueChart?.resize()
  customerAmountDistChart?.resize()
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
        <Card title="运营指标概览">
          <Spin :spinning="store.loading.overview">
            <div class="kpi-grid" ref="overviewCardRef">
              <div class="kpi-item">
                <div class="kpi-label">客户总数</div>
                <div class="kpi-value">{{ store.overview?.customerCount ?? '-' }}</div>
              </div>
              <div class="kpi-item">
                <div class="kpi-label">订单总数</div>
                <div class="kpi-value">{{ store.overview?.orderCount ?? '-' }}</div>
              </div>
              <div class="kpi-item">
                <div class="kpi-label">有效订单数</div>
                <div class="kpi-value">{{ store.overview?.validOrderCount ?? '-' }}</div>
              </div>
              <div class="kpi-item">
                <div class="kpi-label">GMV 总额</div>
                <div class="kpi-value">{{ store.overview?.totalGmv ?? '-' }}</div>
              </div>
              <div class="kpi-item">
                <div class="kpi-label">客单价</div>
                <div class="kpi-value">{{ store.overview?.averageOrderValue ?? '-' }}</div>
              </div>
            </div>
          </Spin>
        </Card>
      </Col>

      <Col :span="16">
        <Card title="订单趋势（订单数 & GMV）">
          <Spin :spinning="store.loading.orderTrend">
            <div ref="orderTrendRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>
      <Col :span="8">
        <Card title="订单评价分数分布">
          <Spin :spinning="store.loading.reviewScore">
            <div ref="reviewBarRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>

      <Col :span="24">
        <Card title="订单金额趋势（销售高峰期分析）">
          <Spin :spinning="store.loading.orderAmountTrend">
            <div ref="orderAmountTrendRef" class="chart-large"></div>
          </Spin>
        </Card>
      </Col>

      <Col :span="12">
        <Card title="客单价趋势（平均消费变化）">
          <Spin :spinning="store.loading.averageOrderValueTrend">
            <div ref="averageOrderValueRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>
      <Col :span="12">
        <Card title="消费金额分布（高消费用户集中度）">
          <Spin :spinning="store.loading.customerAmountDistribution">
            <div ref="customerAmountDistRef" class="chart"></div>
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

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 16px;
}

.kpi-item {
  padding: 12px 16px;
  border-radius: 8px;
  background: #fafafa;
}

.kpi-label {
  font-size: 13px;
  color: #888;
  margin-bottom: 4px;
}

.kpi-value {
  font-size: 18px;
  font-weight: 600;
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
