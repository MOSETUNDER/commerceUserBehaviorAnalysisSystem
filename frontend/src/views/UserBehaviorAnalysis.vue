<template>
  <div class="page-wrapper">
    <Row :gutter="[16, 16]">
      <!-- 用户活跃时间热力图 -->
      <Col :span="24">
        <Card title="用户活跃时间热力图（周几 × 几点）">
          <Spin :spinning="store.loading.activeTimeHeatmap">
            <div ref="activeTimeHeatmapRef" class="chart-large"></div>
          </Spin>
        </Card>
      </Col>

      <!-- 订单状态漏斗图 -->
      <Col :span="12">
        <Card title="订单状态漏斗图（下单 → 批准 → 发货 → 交付）">
          <Spin :spinning="store.loading.orderFunnel">
            <div ref="orderFunnelRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>

      <!-- 用户购买行为桑基图 -->
      <Col :span="12">
        <Card title="用户购买行为桑基图（首次购买 → 复购流转）">
          <Spin :spinning="store.loading.sankey">
            <div ref="sankeyRef" class="chart"></div>
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
import { useBehaviorStore } from '../store/behavior'

const store = useBehaviorStore()

// Chart refs
const activeTimeHeatmapRef = ref(null)
const orderFunnelRef = ref(null)
const sankeyRef = ref(null)

// Chart instances
let activeTimeHeatmapChart
let orderFunnelChart
let sankeyChart

const loadAll = async () => {
  await Promise.all([
    store.loadUserActiveTimeHeatmap(),
    store.loadOrderFunnel(),
    store.loadUserBehaviorSankey()
  ])
  await nextTick()
  setTimeout(() => {
    renderCharts()
  }, 100)
}

const renderCharts = () => {
  console.log('渲染用户行为分析图表，数据状态:', {
    activeTimeHeatmap: store.userActiveTimeHeatmap.length,
    orderFunnel: store.orderFunnel.length,
    sankey: store.userBehaviorSankey !== null
  })

  // 用户活跃时间热力图
  if (activeTimeHeatmapRef.value && store.userActiveTimeHeatmap.length) {
    if (!activeTimeHeatmapChart) {
      activeTimeHeatmapChart = echarts.init(activeTimeHeatmapRef.value)
    }

    // 构建热力图数据：X轴=周几（1-7），Y轴=几点（0-23）
    const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    const hours = Array.from({ length: 24 }, (_, i) => i)
    
    const heatmapData = []
    const maxOrderCount = Math.max(...store.userActiveTimeHeatmap.map(d => d.orderCount))
    
    store.userActiveTimeHeatmap.forEach((d) => {
      heatmapData.push([
        d.dayOfWeek - 1, // 转换为索引（0-6）
        d.hour,
        d.orderCount
      ])
    })

    activeTimeHeatmapChart.setOption({
      title: { text: '用户活跃时间热力图', left: 'center' },
      tooltip: {
        position: 'top',
        formatter: function (params) {
          const day = days[params.data[0]]
          const hour = params.data[1]
          const count = params.data[2]
          return `${day} ${hour}:00<br/>订单数: ${count}`
        }
      },
      grid: {
        height: '50%',
        top: '10%'
      },
      xAxis: {
        type: 'category',
        data: days,
        splitArea: {
          show: true
        }
      },
      yAxis: {
        type: 'category',
        data: hours.map(h => `${h}:00`),
        splitArea: {
          show: true
        }
      },
      visualMap: {
        min: 0,
        max: maxOrderCount,
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
          name: '订单数',
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

  // 订单状态漏斗图
  if (orderFunnelRef.value && store.orderFunnel.length) {
    if (!orderFunnelChart) {
      orderFunnelChart = echarts.init(orderFunnelRef.value)
    }

    const stages = store.orderFunnel.map((d) => d.stageName)
    const values = store.orderFunnel.map((d) => d.orderCount)
    const rates = store.orderFunnel.map((d) => d.conversionRate)

    orderFunnelChart.setOption({
      title: { text: '订单状态漏斗图', left: 'center' },
      tooltip: {
        trigger: 'item',
        formatter: function (params) {
          const index = params.dataIndex
          const data = store.orderFunnel[index]
          return `${params.name}<br/>订单数: ${data.orderCount}<br/>转化率: ${data.conversionRate}%`
        }
      },
      series: [
        {
          name: '订单状态',
          type: 'funnel',
          left: '10%',
          top: 60,
          bottom: 60,
          width: '80%',
          min: 0,
          max: values[0], // 最大值是第一个阶段
          minSize: '0%',
          maxSize: '100%',
          sort: 'none',
          gap: 2,
          label: {
            show: true,
            position: 'inside',
            formatter: '{b}: {c} ({d}%)'
          },
          labelLine: {
            length: 10,
            lineStyle: {
              width: 1,
              type: 'solid'
            }
          },
          itemStyle: {
            borderColor: '#fff',
            borderWidth: 1
          },
          emphasis: {
            label: {
              fontSize: 20
            }
          },
          data: store.orderFunnel.map((d, index) => ({
            value: d.orderCount,
            name: d.stageName,
            itemStyle: {
              color: ['#5470c6', '#91cc75', '#fac858', '#ee6666'][index % 4]
            }
          }))
        }
      ]
    })
  }

  // 用户购买行为桑基图
  if (sankeyRef.value && store.userBehaviorSankey && store.userBehaviorSankey.nodes && store.userBehaviorSankey.nodes.length > 0) {
    if (!sankeyChart) {
      sankeyChart = echarts.init(sankeyRef.value)
    }

    // 转换节点数据格式：确保每个节点都有 name 属性
    const nodes = store.userBehaviorSankey.nodes.map(node => ({
      name: node.name || node.name
    }))

    // 转换连接数据格式：确保 source 和 target 是节点名称（字符串）
    const links = store.userBehaviorSankey.links.map(link => {
      const sourceName = nodes[link.source]?.name || nodes[link.source]?.name
      const targetName = nodes[link.target]?.name || nodes[link.target]?.name
      return {
        source: sourceName,
        target: targetName,
        value: link.value
      }
    }).filter(link => link.value > 0) // 过滤掉值为0的连接

    console.log('桑基图数据:', { nodes, links })

    // 如果没有连接数据，显示提示信息
    if (links.length === 0) {
      console.warn('桑基图没有有效连接数据，显示提示信息')
      sankeyChart.setOption({
        title: { 
          text: '用户购买行为流转', 
          left: 'center',
          subtext: '暂无复购数据（所有用户均为首次购买）',
          subtextStyle: {
            color: '#999',
            fontSize: 14
          }
        },
        graphic: [
          {
            type: 'text',
            left: 'center',
            top: 'middle',
            style: {
              text: '当前数据集中所有用户均为首次购买，暂无复购流转数据',
              fontSize: 16,
              fill: '#999',
              textAlign: 'center'
            }
          }
        ]
      })
      return
    }

    sankeyChart.setOption({
      title: { text: '用户购买行为流转', left: 'center' },
      tooltip: {
        trigger: 'item',
        triggerOn: 'mousemove',
        formatter: function (params) {
          if (params.dataType === 'edge') {
            return `${params.data.source} → ${params.data.target}<br/>用户数: ${params.data.value}`
          } else {
            return `${params.name}<br/>用户数: ${params.value || 0}`
          }
        }
      },
      series: [
        {
          type: 'sankey',
          layout: 'none',
          focus: 'adjacency',
          data: nodes,
          links: links,
          emphasis: {
            focus: 'adjacency'
          },
          lineStyle: {
            color: 'gradient',
            curveness: 0.5
          },
          label: {
            fontSize: 14
          }
        }
      ]
    })
  }

  // Resize all charts
  handleResize()
}

const handleResize = () => {
  activeTimeHeatmapChart?.resize()
  orderFunnelChart?.resize()
  sankeyChart?.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  activeTimeHeatmapChart?.dispose()
  orderFunnelChart?.dispose()
  sankeyChart?.dispose()
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

