<template>
  <div class="page-wrapper">
    <Row :gutter="[16, 16]">
      <!-- 各州销售额热力图 -->
      <Col :span="24">
        <Card title="各州销售额热力图（销售地图）">
          <Spin :spinning="store.loading.salesHeatmap">
            <div ref="salesHeatmapRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>

      <!-- 各州订单量柱状图 -->
      <Col :span="12">
        <Card title="Top 10 各州订单量">
          <Spin :spinning="store.loading.orderCount">
            <div ref="orderCountBarRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>

      <!-- 各州平均客单价柱状图 -->
      <Col :span="12">
        <Card title="Top 10 各州平均客单价">
          <Spin :spinning="store.loading.averageOrderValue">
            <div ref="avgOrderValueBarRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>

      <!-- 各州复购率柱状图 -->
      <Col :span="12">
        <Card title="Top 10 各州复购率">
          <Spin :spinning="store.loading.repurchaseRate">
            <div ref="repurchaseRateBarRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>

      <!-- 物流时效分布直方图 -->
      <Col :span="12">
        <Card title="各州物流时效分布（平均物流天数）">
          <Spin :spinning="store.loading.logisticsEfficiency">
            <div ref="logisticsHistRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>

      <!-- 地域用户增长折线图 -->
      <Col :span="24">
        <Card title="地域用户增长趋势（按月统计）">
          <Spin :spinning="store.loading.userGrowth">
            <div ref="userGrowthLineRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>
    </Row>
  </div>
</template>

<script setup>
import { onMounted, ref, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Card, Row, Col, Spin } from 'ant-design-vue'
import { useGeoStore } from '../store/geo'

const store = useGeoStore()

// Chart refs
const salesHeatmapRef = ref(null)
const orderCountBarRef = ref(null)
const avgOrderValueBarRef = ref(null)
const repurchaseRateBarRef = ref(null)
const logisticsHistRef = ref(null)
const userGrowthLineRef = ref(null)

// Chart instances
let salesHeatmapChart
let orderCountBarChart
let avgOrderValueBarChart
let repurchaseRateBarChart
let logisticsHistChart
let userGrowthLineChart

const loadAll = async () => {
  await Promise.all([
    store.loadStateSalesHeatmap(),
    store.loadStateOrderCount(10),
    store.loadStateAverageOrderValue(10),
    store.loadStateRepurchaseRate(10),
    store.loadLogisticsEfficiency(),
    store.loadGeoUserGrowth()
  ])
  // 使用 nextTick 确保 DOM 更新后再渲染图表
  await nextTick()
  setTimeout(() => {
    renderCharts()
  }, 100)
}

const renderCharts = () => {
  console.log('渲染图表，数据状态:', {
    salesHeatmap: store.stateSalesHeatmap.length,
    orderCount: store.stateOrderCount.length,
    avgOrderValue: store.stateAverageOrderValue.length,
    repurchaseRate: store.stateRepurchaseRate.length,
    logistics: store.logisticsEfficiency.length,
    userGrowth: store.geoUserGrowth.length
  })

  // 各州销售额热力图（使用柱状图展示，实际项目中可以用地图组件）
  if (salesHeatmapRef.value && store.stateSalesHeatmap.length) {
    if (!salesHeatmapChart) {
      salesHeatmapChart = echarts.init(salesHeatmapRef.value)
    }
    const states = store.stateSalesHeatmap.map((d) => d.state)
    const sales = store.stateSalesHeatmap.map((d) => Number(d.sales))
    const minSales = Math.min(...sales)
    const maxSales = Math.max(...sales)
    const colors = ['#50a3ba', '#eac736', '#d94e5d']
    
    salesHeatmapChart.setOption({
      title: { text: '各州销售额热力图', left: 'center' },
      tooltip: {
        trigger: 'axis',
        formatter: '{b}<br/>销售额: R$ {c}'
      },
      xAxis: {
        type: 'category',
        data: states,
        axisLabel: { rotate: 45 }
      },
      yAxis: {
        type: 'value',
        name: '销售额 (R$)'
      },
      visualMap: {
        min: minSales,
        max: maxSales,
        calculable: true,
        orient: 'horizontal',
        left: 'center',
        bottom: '5%',
        inRange: {
          color: colors
        }
      },
      series: [
        {
          type: 'bar',
          data: sales.map((value) => ({
            value,
            itemStyle: {
              color: (() => {
                if (maxSales === minSales) return colors[0]
                const ratio = (value - minSales) / (maxSales - minSales)
                const colorIndex = Math.min(Math.floor(ratio * colors.length), colors.length - 1)
                return colors[colorIndex]
              })()
            }
          }))
        }
      ]
    })
  }

  // 各州订单量柱状图
  if (orderCountBarRef.value && store.stateOrderCount.length) {
    if (!orderCountBarChart) {
      orderCountBarChart = echarts.init(orderCountBarRef.value)
    }
    const states = store.stateOrderCount.map((d) => d.state)
    const orderCounts = store.stateOrderCount.map((d) => d.orderCount)
    orderCountBarChart.setOption({
      title: { text: 'Top 10 各州订单量', left: 'center' },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: states,
        axisLabel: { rotate: 45 }
      },
      yAxis: {
        type: 'value',
        name: '订单量'
      },
      series: [
        {
          type: 'bar',
          data: orderCounts
        }
      ]
    })
  }

  // 各州平均客单价柱状图
  if (avgOrderValueBarRef.value && store.stateAverageOrderValue.length) {
    if (!avgOrderValueBarChart) {
      avgOrderValueBarChart = echarts.init(avgOrderValueBarRef.value)
    }
    const states = store.stateAverageOrderValue.map((d) => d.state)
    const avgValues = store.stateAverageOrderValue.map((d) =>
      Number(d.averageOrderValue).toFixed(2)
    )
    avgOrderValueBarChart.setOption({
      title: { text: 'Top 10 各州平均客单价', left: 'center' },
      tooltip: {
        trigger: 'axis',
        formatter: '{b}<br/>平均客单价: R$ {c}'
      },
      xAxis: {
        type: 'category',
        data: states,
        axisLabel: { rotate: 45 }
      },
      yAxis: {
        type: 'value',
        name: '平均客单价 (R$)'
      },
      series: [
        {
          type: 'bar',
          data: avgValues
        }
      ]
    })
  }

  // 各州复购率柱状图
  if (repurchaseRateBarRef.value && store.stateRepurchaseRate.length) {
    if (!repurchaseRateBarChart) {
      repurchaseRateBarChart = echarts.init(repurchaseRateBarRef.value)
    }
    const states = store.stateRepurchaseRate.map((d) => d.state)
    const rates = store.stateRepurchaseRate.map((d) =>
      (d.repurchaseRate * 100).toFixed(2)
    )
    repurchaseRateBarChart.setOption({
      title: { text: 'Top 10 各州复购率', left: 'center' },
      tooltip: {
        trigger: 'axis',
        formatter: function (params) {
          const data = store.stateRepurchaseRate[params[0].dataIndex]
          return `${params[0].name}<br/>复购率: ${params[0].value}%<br/>总用户数: ${data.totalCustomers}<br/>复购用户数: ${data.repurchaseCustomers}`
        }
      },
      xAxis: {
        type: 'category',
        data: states,
        axisLabel: { rotate: 45 }
      },
      yAxis: {
        type: 'value',
        name: '复购率 (%)',
        max: 100
      },
      series: [
        {
          type: 'bar',
          data: rates
        }
      ]
    })
  }

  // 物流时效分布直方图
  if (logisticsHistRef.value && store.logisticsEfficiency.length) {
    if (!logisticsHistChart) {
      logisticsHistChart = echarts.init(logisticsHistRef.value)
    }
    const states = store.logisticsEfficiency.map((d) => d.state)
    const avgDays = store.logisticsEfficiency.map((d) =>
      Number(d.averageDeliveryDays).toFixed(1)
    )
    logisticsHistChart.setOption({
      title: { text: '各州物流时效分布', left: 'center' },
      tooltip: {
        trigger: 'axis',
        formatter: function (params) {
          const data = store.logisticsEfficiency[params[0].dataIndex]
          return `${params[0].name}<br/>平均物流天数: ${params[0].value} 天<br/>订单数: ${data.orderCount}`
        }
      },
      xAxis: {
        type: 'category',
        data: states,
        axisLabel: { rotate: 45 }
      },
      yAxis: {
        type: 'value',
        name: '平均物流天数'
      },
      series: [
        {
          type: 'bar',
          data: avgDays
        }
      ]
    })
  }

  // 地域用户增长折线图
  if (userGrowthLineRef.value && store.geoUserGrowth.length) {
    if (!userGrowthLineChart) {
      userGrowthLineChart = echarts.init(userGrowthLineRef.value)
    }

    // 按州分组数据
    const stateDataMap = {}
    store.geoUserGrowth.forEach((d) => {
      if (!stateDataMap[d.state]) {
        stateDataMap[d.state] = []
      }
      stateDataMap[d.state].push({
        month: d.month,
        newCustomers: d.newCustomers
      })
    })

    // 获取所有月份
    const allMonths = [
      ...new Set(store.geoUserGrowth.map((d) => d.month))
    ].sort()

    // 构建系列数据（只显示 Top 10 州）
    const topStates = Object.keys(stateDataMap)
      .map((state) => ({
        state,
        total: stateDataMap[state].reduce(
          (sum, d) => sum + d.newCustomers,
          0
        )
      }))
      .sort((a, b) => b.total - a.total)
      .slice(0, 10)
      .map((item) => item.state)

    const seriesData = topStates.map((state) => {
      const dataForState = allMonths.map((month) => {
        const record = stateDataMap[state].find((d) => d.month === month)
        return record ? record.newCustomers : 0
      })
      return {
        name: state,
        type: 'line',
        smooth: true,
        data: dataForState
      }
    })

    userGrowthLineChart.setOption({
      title: { text: '地域用户增长趋势（Top 10 州）', left: 'center' },
      tooltip: { trigger: 'axis' },
      legend: {
        data: topStates,
        bottom: 0,
        type: 'scroll'
      },
      xAxis: {
        type: 'category',
        data: allMonths
      },
      yAxis: {
        type: 'value',
        name: '新增用户数'
      },
      series: seriesData
    })
  }

  // Resize all charts
  handleResize()
}

const handleResize = () => {
  salesHeatmapChart?.resize()
  orderCountBarChart?.resize()
  avgOrderValueBarChart?.resize()
  repurchaseRateBarChart?.resize()
  logisticsHistChart?.resize()
  userGrowthLineChart?.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  salesHeatmapChart?.dispose()
  orderCountBarChart?.dispose()
  avgOrderValueBarChart?.dispose()
  repurchaseRateBarChart?.dispose()
  logisticsHistChart?.dispose()
  userGrowthLineChart?.dispose()
})
</script>

<style scoped>
.page-wrapper {
  padding: 16px;
}

.chart {
  width: 100%;
  height: 400px;
}
</style>
