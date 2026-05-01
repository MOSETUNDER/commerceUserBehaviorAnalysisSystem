<template>
  <div class="page-wrapper">
    <Row :gutter="[16, 16]">
      <!-- Top 品类销售柱状图 -->
      <Col :span="12">
        <Card title="Top 10 品类销售额">
          <Spin :spinning="store.loading.topCategories">
            <div ref="categorySalesBarRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>

      <!-- 商品销售 Top N 条形图 -->
      <Col :span="12">
        <Card title="Top 20 商品销售">
          <Spin :spinning="store.loading.topProducts">
            <div ref="productSalesBarRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>

      <!-- 品类复购率热力图 -->
      <Col :span="24">
        <Card title="品类复购率热力图（品类 × 用户分层）">
          <Spin :spinning="store.loading.repurchaseHeatmap">
            <div ref="repurchaseHeatmapRef" class="chart-large"></div>
          </Spin>
        </Card>
      </Col>

      <!-- 品类各州销售地图 -->
      <Col :span="24">
        <Card title="品类各州销售地图">
          <Spin :spinning="store.loading.categoryStateSales">
            <div ref="categoryStateSalesRef" class="chart-large"></div>
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
import { useProductStore } from '../store/product'

const store = useProductStore()

// Chart refs
const categorySalesBarRef = ref(null)
const productSalesBarRef = ref(null)
const repurchaseHeatmapRef = ref(null)
const categoryStateSalesRef = ref(null)

// Chart instances
let categorySalesBarChart
let productSalesBarChart
let repurchaseHeatmapChart
let categoryStateSalesChart

const loadAll = async () => {
  await Promise.all([
    store.loadTopCategorySales(10),
    store.loadTopProductSales(20),
    store.loadCategoryRepurchaseHeatmap(),
    store.loadCategoryStateSales()
  ])
  await nextTick()
  setTimeout(() => {
    renderCharts()
  }, 100)
}

const renderCharts = () => {
  console.log('渲染商品分析图表，数据状态:', {
    topCategories: store.topCategorySales.length,
    topProducts: store.topProductSales.length,
    repurchaseHeatmap: store.categoryRepurchaseHeatmap.length,
    categoryStateSales: store.categoryStateSales.length
  })

  // Top 品类销售柱状图
  if (categorySalesBarRef.value && store.topCategorySales.length) {
    if (!categorySalesBarChart) {
      categorySalesBarChart = echarts.init(categorySalesBarRef.value)
    }
    const categories = store.topCategorySales.map((d) => d.categoryName)
    const sales = store.topCategorySales.map((d) => Number(d.totalAmount))
    categorySalesBarChart.setOption({
      title: { text: 'Top 10 品类销售额', left: 'center' },
      tooltip: {
        trigger: 'axis',
        formatter: '{b}<br/>销售额: R$ {c}'
      },
      xAxis: {
        type: 'category',
        data: categories,
        axisLabel: { rotate: 45 }
      },
      yAxis: {
        type: 'value',
        name: '销售额 (R$)'
      },
      series: [
        {
          type: 'bar',
          data: sales
        }
      ]
    })
  }

  // Top N 商品销售条形图（横向）
  if (productSalesBarRef.value && store.topProductSales.length) {
    if (!productSalesBarChart) {
      productSalesBarChart = echarts.init(productSalesBarRef.value)
    }
    const products = store.topProductSales.map((d) => d.productId.substring(0, 20) + '...')
    const sales = store.topProductSales.map((d) => Number(d.salesAmount))
    productSalesBarChart.setOption({
      title: { text: 'Top 20 商品销售额', left: 'center' },
      tooltip: {
        trigger: 'axis',
        formatter: function (params) {
          const data = store.topProductSales[params[0].dataIndex]
          return `商品ID: ${data.productId}<br/>品类: ${data.categoryName}<br/>销售额: R$ ${params[0].value}<br/>销售量: ${data.salesCount}`
        }
      },
      xAxis: {
        type: 'value',
        name: '销售额 (R$)'
      },
      yAxis: {
        type: 'category',
        data: products,
        axisLabel: { fontSize: 10 }
      },
      series: [
        {
          type: 'bar',
          data: sales
        }
      ]
    })
  }

  // 品类复购率热力图
  if (repurchaseHeatmapRef.value && store.categoryRepurchaseHeatmap.length) {
    if (!repurchaseHeatmapChart) {
      repurchaseHeatmapChart = echarts.init(repurchaseHeatmapRef.value)
    }

    // 获取所有品类和用户分层
    const categories = [
      ...new Set(store.categoryRepurchaseHeatmap.map((d) => d.categoryName))
    ].sort()
    const segments = ['高价值客户', '一般客户', '流失客户']

    // 构建热力图数据
    const heatmapData = []
    categories.forEach((category, catIdx) => {
      segments.forEach((segment, segIdx) => {
        const item = store.categoryRepurchaseHeatmap.find(
          (d) => d.categoryName === category && d.segmentLabel === segment
        )
        if (item) {
          heatmapData.push([
            segIdx,
            catIdx,
            (item.repurchaseRate * 100).toFixed(2),
            item.customerCount
          ])
        } else {
          heatmapData.push([segIdx, catIdx, 0, 0])
        }
      })
    })

    repurchaseHeatmapChart.setOption({
      title: { text: '品类复购率热力图', left: 'center' },
      tooltip: {
        position: 'top',
        formatter: function (params) {
          const category = categories[params.data[1]]
          const segment = segments[params.data[0]]
          const rate = params.data[2]
          const count = params.data[3]
          return `${category}<br/>${segment}<br/>复购率: ${rate}%<br/>用户数: ${count}`
        }
      },
      grid: {
        height: '50%',
        top: '10%'
      },
      xAxis: {
        type: 'category',
        data: categories,
        splitArea: {
          show: true
        },
        axisLabel: { rotate: 45 }
      },
      yAxis: {
        type: 'category',
        data: segments,
        splitArea: {
          show: true
        }
      },
      visualMap: {
        min: 0,
        max: 100,
        calculable: true,
        orient: 'horizontal',
        left: 'center',
        bottom: '5%',
        inRange: {
          color: ['#313695', '#4575b4', '#74add1', '#abd9e9', '#e0f3f8', '#ffffcc', '#fee090', '#fdae61', '#f46d43', '#d73027', '#a50026']
        }
      },
      series: [
        {
          name: '复购率',
          type: 'heatmap',
          data: heatmapData,
          label: {
            show: true
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }
      ]
    })
  }

  // 品类各州销售地图（使用柱状图展示）
  if (categoryStateSalesRef.value && store.categoryStateSales.length) {
    if (!categoryStateSalesChart) {
      categoryStateSalesChart = echarts.init(categoryStateSalesRef.value)
    }

    // 获取 Top 10 品类
    const categorySales = {}
    store.categoryStateSales.forEach((d) => {
      if (!categorySales[d.categoryName]) {
        categorySales[d.categoryName] = 0
      }
      categorySales[d.categoryName] += Number(d.salesAmount)
    })
    const topCategories = Object.keys(categorySales)
      .map((cat) => ({ cat, total: categorySales[cat] }))
      .sort((a, b) => b.total - a.total)
      .slice(0, 10)
      .map((item) => item.cat)

    // 获取所有州
    const states = [
      ...new Set(store.categoryStateSales.map((d) => d.state))
    ].sort()

    // 构建系列数据
    const seriesData = topCategories.map((category) => {
      const dataForCategory = states.map((state) => {
        const record = store.categoryStateSales.find(
          (d) => d.categoryName === category && d.state === state
        )
        return record ? Number(record.salesAmount) : 0
      })
      return {
        name: category,
        type: 'bar',
        stack: 'sales',
        data: dataForCategory
      }
    })

    categoryStateSalesChart.setOption({
      title: { text: '品类各州销售地图（Top 10 品类）', left: 'center' },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      legend: {
        data: topCategories,
        bottom: 0,
        type: 'scroll'
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
      series: seriesData
    })
  }

  // Resize all charts
  handleResize()
}

const handleResize = () => {
  categorySalesBarChart?.resize()
  productSalesBarChart?.resize()
  repurchaseHeatmapChart?.resize()
  categoryStateSalesChart?.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  categorySalesBarChart?.dispose()
  productSalesBarChart?.dispose()
  repurchaseHeatmapChart?.dispose()
  categoryStateSalesChart?.dispose()
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

.chart-large {
  width: 100%;
  height: 500px;
}
</style>

