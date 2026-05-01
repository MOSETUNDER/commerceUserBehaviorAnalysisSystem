<script setup>
import { onMounted, onUnmounted, ref, h } from 'vue'
import * as echarts from 'echarts'
import { Card, Row, Col, Spin, Table, Tag, Descriptions, Button, message } from 'ant-design-vue'
import { useRfmStore } from '../store/rfm'
import { exportRfmCsv } from '../api/analysis'

const store = useRfmStore()

const selectedCustomer = ref(null)

const segmentPieRef = ref(null)
const churnPieRef = ref(null)
const radarRef = ref(null)
const averageRadarRef = ref(null)
const scoreBarRef = ref(null)
const repurchaseProbHistRef = ref(null)
const scatterRef = ref(null)

let segmentPieChart
let churnPieChart
let radarChart
let averageRadarChart
let scoreBarChart
let repurchaseProbHistChart
let scatterChart

const loadAll = async () => {
  await Promise.all([
    store.loadCustomers(0, 500),
    store.loadSegmentDistribution(),
    store.loadChurnStats(),
    store.loadTopCustomers(10),
    store.loadSummaryStats(),
    store.loadAverageRadar(),
    store.loadScoreDistribution(),
    store.loadRepurchaseProbabilityDistribution(),
    store.loadScatterData(5000)
  ])
  renderCharts()
}

const renderCharts = () => {
  // 分层饼图
  if (segmentPieRef.value) {
    segmentPieChart = segmentPieChart || echarts.init(segmentPieRef.value)
    const data = store.segmentDistribution.map((d) => ({
      name: d.segmentLabel,
      value: d.count
    }))
    segmentPieChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', left: 'left' },
      series: [
        {
          type: 'pie',
          radius: '60%',
          data
        }
      ]
    })
  }

  // 流失饼图
  if (churnPieRef.value && store.churnStats) {
    churnPieChart = churnPieChart || echarts.init(churnPieRef.value)
    const stats = store.churnStats
    churnPieChart.setOption({
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: '60%',
          data: [
            { name: '活跃', value: stats.activeCount },
            { name: '沉睡', value: stats.sleepingCount },
            { name: '流失', value: stats.churnedCount }
          ]
        }
      ]
    })
  }

  // 雷达图（单个客户）
  if (radarRef.value && store.radarData && store.radarData.indicators) {
    radarChart = radarChart || echarts.init(radarRef.value)
    radarChart.setOption({
      tooltip: {},
      radar: {
        indicator: store.radarData.indicators,
        radius: '55%', // 减小半径，确保数据不超出边界
        center: ['50%', '55%'], // 稍微下移，给标签留出空间
        nameGap: 8, // 标签与雷达图的间距
        splitNumber: 5, // 分割段数
        shape: 'polygon', // 多边形
        splitArea: {
          show: true,
          areaStyle: {
            color: ['rgba(250, 250, 250, 0.3)', 'rgba(200, 200, 200, 0.3)']
          }
        },
        splitLine: {
          show: true,
          lineStyle: {
            color: '#e0e0e0'
          }
        },
        axisLine: {
          show: true,
          lineStyle: {
            color: '#999'
          }
        },
        name: {
          textStyle: {
            color: '#666',
            fontSize: 12
          }
        }
      },
      series: [
        {
          type: 'radar',
          data: store.radarData.data,
          areaStyle: {
            opacity: 0.3
          },
          lineStyle: {
            width: 2
          },
          symbolSize: 6
        }
      ]
    })
    // 确保图表自适应容器大小
    radarChart.resize()
  }

  // 平均得分雷达图
  if (averageRadarRef.value && store.averageRadarData && store.averageRadarData.indicators) {
    averageRadarChart = averageRadarChart || echarts.init(averageRadarRef.value)
    averageRadarChart.setOption({
      tooltip: {},
      radar: {
        indicator: store.averageRadarData.indicators,
        radius: '60%',
        center: ['50%', '50%'],
        nameGap: 8,
        splitNumber: 5,
        shape: 'polygon',
        splitArea: {
          show: true,
          areaStyle: {
            color: ['rgba(250, 250, 250, 0.3)', 'rgba(200, 200, 200, 0.3)']
          }
        },
        splitLine: {
          show: true,
          lineStyle: {
            color: '#e0e0e0'
          }
        },
        axisLine: {
          show: true,
          lineStyle: {
            color: '#999'
          }
        },
        name: {
          textStyle: {
            color: '#666',
            fontSize: 12
          }
        }
      },
      series: [
        {
          type: 'radar',
          data: store.averageRadarData.data,
          areaStyle: {
            opacity: 0.3
          },
          lineStyle: {
            width: 2
          },
          symbolSize: 6
        }
      ]
    })
  }

  // RFM 总分分布柱状图
  if (scoreBarRef.value && store.scoreDistribution.length) {
    scoreBarChart = scoreBarChart || echarts.init(scoreBarRef.value)
    const xData = store.scoreDistribution.map((d) => d.scoreRange)
    const yData = store.scoreDistribution.map((d) => d.count)
    scoreBarChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: xData, name: 'RFM 总分区间' },
      yAxis: { type: 'value', name: '客户数量' },
      series: [{ type: 'bar', data: yData }]
    })
  }

  // 复购概率分布直方图
  if (repurchaseProbHistRef.value && store.repurchaseProbabilityDistribution.length) {
    repurchaseProbHistChart = repurchaseProbHistChart || echarts.init(repurchaseProbHistRef.value)
    const xData = store.repurchaseProbabilityDistribution.map((d) => d.probabilityRange)
    const yData = store.repurchaseProbabilityDistribution.map((d) => d.count)
    repurchaseProbHistChart.setOption({
      tooltip: { trigger: 'axis' },
      title: {
        text: '复购概率分布',
        left: 'center'
      },
      xAxis: { 
        type: 'category', 
        data: xData, 
        name: '复购概率区间',
        nameLocation: 'middle',
        nameGap: 30
      },
      yAxis: { type: 'value', name: '客户数量' },
      series: [{ 
        type: 'bar', 
        data: yData,
        itemStyle: {
          color: '#1890ff'
        }
      }]
    })
  }

  // RFM 散点图
  if (scatterRef.value && store.scatterData.length) {
    scatterChart = scatterChart || echarts.init(scatterRef.value)
    
    console.log('RFM 散点图数据量:', store.scatterData.length)
    console.log('RFM 散点图前5条数据:', store.scatterData.slice(0, 5))
    
    // 准备散点图数据：X=R分数, Y=F分数, 大小=M分数, 颜色=M分数
    // 兼容后端可能返回的两种字段名：rScore/rscore, fScore/fscore, mScore/mscore
    const scatterData = store.scatterData.map((d) => {
      const r = d.rScore ?? d.rscore ?? 0
      const f = d.fScore ?? d.fscore ?? 0
      const m = d.mScore ?? d.mscore ?? 0
      return [
        r, // X轴：Recency
        f, // Y轴：Frequency
        m, // 大小：Monetary
        d.totalAmount, // 用于 tooltip 显示
        d.segmentLabel // 用于 tooltip 显示
      ]
    })

    console.log('处理后的散点图数据量:', scatterData.length)
    console.log('处理后的前5条数据:', scatterData.slice(0, 5))

    // 计算 M 分数的最大值，用于归一化大小
    const maxMScore = Math.max(...store.scatterData.map(d => d.mScore ?? d.mscore ?? 1), 1)
    
    scatterChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: (params) => {
          const data = params.data
          const item = store.scatterData[params.dataIndex]
          return `
            <div>
              <strong>客户ID:</strong> ${item?.customerId || '-'}<br/>
              <strong>R 分数:</strong> ${data[0]}<br/>
              <strong>F 分数:</strong> ${data[1]}<br/>
              <strong>M 分数:</strong> ${data[2]}<br/>
              <strong>累计消费:</strong> ${
                typeof data[3] === 'number'
                  ? data[3].toFixed(2)
                  : (Number.isFinite(Number(data[3])) ? Number(data[3]).toFixed(2) : data[3] || '-')
              }<br/>
              <strong>分层:</strong> ${data[4] || '-'}
            </div>
          `
        }
      },
      title: {
        text: 'RFM 用户价值分布',
        left: 'center',
        top: 10
      },
      xAxis: {
        type: 'value',
        name: 'Recency (R)',
        nameLocation: 'middle',
        nameGap: 30,
        min: 0.5,
        max: 5.5,
        splitLine: {
          show: true,
          lineStyle: {
            type: 'dashed'
          }
        }
      },
      yAxis: {
        type: 'value',
        name: 'Frequency (F)',
        nameLocation: 'middle',
        nameGap: 50,
        min: 0.5,
        max: 5.5,
        splitLine: {
          show: true,
          lineStyle: {
            type: 'dashed'
          }
        }
      },
      visualMap: {
        min: 1,
        max: 5,
        dimension: 2, // 使用 M 分数作为颜色映射
        inRange: {
          color: ['#50a3ba', '#eac736', '#d94e5d'] // 从低到高：蓝-黄-红
        },
        calculable: true,
        right: 10,
        top: 'center',
        text: ['高 M', '低 M'],
        textStyle: {
          color: '#333'
        }
      },
      series: [{
        type: 'scatter',
        data: scatterData,
        symbolSize: (data) => {
          // 根据 M 分数设置点的大小：M=1 最小，M=5 最大
          const mScore = data[2]
          return 10 + (mScore / maxMScore) * 30 // 大小范围：10-40
        },
        itemStyle: {
          opacity: 0.6,
          borderWidth: 1,
          borderColor: '#fff'
        },
        emphasis: {
          itemStyle: {
            opacity: 1,
            borderWidth: 2
          }
        }
      }]
    })
  }
}

const handleCustomerSelect = async (record) => {
  selectedCustomer.value = record
  await Promise.all([
    store.loadRadar(record.customerId),
    store.loadRepurchaseProbability(record.customerId)
  ])
  // 使用 nextTick 确保 DOM 更新后再渲染图表
  setTimeout(() => {
    renderCharts()
  }, 100)
}

// 导出 RFM 分析结果为 CSV
const handleExportRfm = async () => {
  try {
    message.loading('正在导出 RFM 分析结果...', 0)
    const response = await exportRfmCsv()
    
    // 创建 Blob 对象
    const blob = new Blob([response], { type: 'text/csv;charset=UTF-8' })
    
    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `rfm_analysis_${new Date().toISOString().split('T')[0]}.csv`
    document.body.appendChild(link)
    link.click()
    
    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    message.destroy()
    message.success('RFM 分析结果导出成功')
  } catch (error) {
    message.destroy()
    message.error('导出失败：' + (error.message || '未知错误'))
    console.error('导出 RFM 失败:', error)
  }
}

// 窗口大小变化时重新调整图表大小
const handleResize = () => {
  if (radarChart) radarChart.resize()
  if (averageRadarChart) averageRadarChart.resize()
  if (segmentPieChart) segmentPieChart.resize()
  if (churnPieChart) churnPieChart.resize()
  if (scoreBarChart) scoreBarChart.resize()
  if (repurchaseProbHistChart) repurchaseProbHistChart.resize()
  if (scatterChart) scatterChart.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  // 销毁图表实例
  if (radarChart) {
    radarChart.dispose()
    radarChart = null
  }
  if (averageRadarChart) {
    averageRadarChart.dispose()
    averageRadarChart = null
  }
  if (segmentPieChart) {
    segmentPieChart.dispose()
    segmentPieChart = null
  }
  if (churnPieChart) {
    churnPieChart.dispose()
    churnPieChart = null
  }
  if (scoreBarChart) {
    scoreBarChart.dispose()
    scoreBarChart = null
  }
  if (repurchaseProbHistChart) {
    repurchaseProbHistChart.dispose()
    repurchaseProbHistChart = null
  }
  if (scatterChart) {
    scatterChart.dispose()
    scatterChart = null
  }
})

</script>

<template>
  <div class="page-wrapper">

    <!-- RFM 统计卡片 -->
    <Card :title="'RFM 客户分类统计' + (store.summaryStats ? '(共' + store.summaryStats.totalCustomers + '用户)' : '')">
      <template #extra>
        <Button type="primary" @click="handleExportRfm">导出 CSV</Button>
      </template>
      <Spin :spinning="store.loading.summaryStats">
        <div class="stats-grid" v-if="store.summaryStats">
          <div class="stat-item">
            <div class="stat-label">高价值客户</div>
            <div class="stat-value">{{ store.summaryStats.highValueCount }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">潜力客户</div>
            <div class="stat-value">{{ store.summaryStats.potentialCount }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">一般客户</div>
            <div class="stat-value">{{ store.summaryStats.normalCount }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">流失客户</div>
            <div class="stat-value">{{ store.summaryStats.churnedCount }}</div>
          </div>
        </div>
        <div v-else class="stats-grid">
          <div class="stat-item">
            <div class="stat-label">加载中...</div>
            <div class="stat-value">-</div>
          </div>
        </div>
      </Spin>
    </Card>

    <Row :gutter="[16, 16]">
      <Col :span="8">
        <Card title="RFM维度平均得分雷达图">
          <Spin :spinning="store.loading.averageRadar">
            <div ref="averageRadarRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>
      <Col :span="8">
        <Card title="客户类型分布饼状图">
          <Spin :spinning="store.loading.segment">
            <div ref="segmentPieRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>
      <Col :span="8">
        <Card title="活跃 / 沉睡 / 流失 用户占比">
          <Spin :spinning="store.loading.churn">
            <div ref="churnPieRef" class="chart"></div>
          </Spin>
        </Card>
      </Col>

      <Col :span="24">
        <Card title="RFM总分分布柱状图">
          <Spin :spinning="store.loading.scoreDistribution">
            <div ref="scoreBarRef" class="chart-large"></div>
          </Spin>
        </Card>
      </Col>

      <Col :span="24">
        <Card title="复购概率分布直方图">
          <Spin :spinning="store.loading.repurchaseProbabilityDistribution">
            <div ref="repurchaseProbHistRef" class="chart-large"></div>
          </Spin>
        </Card>
      </Col>

      <Col :span="24">
        <Card title="RFM 用户价值分布散点图（X轴=Recency, Y轴=Frequency, 颜色/大小=Monetary）">
          <Spin :spinning="store.loading.scatter">
            <div ref="scatterRef" class="chart-large"></div>
          </Spin>
        </Card>
      </Col>

      <!-- 客户 RFM 列表和雷达图放在一行 -->
      <Col :span="14">
        <Card title="客户 RFM 列表（可点击查看雷达图）">
          <Spin :spinning="store.loading.customers">
            <Table
              size="small"
              :data-source="store.customers"
              :pagination="{ pageSize: 10 }"
              row-key="customerId"
              :scroll="{ x:800 }"
              :customRow="(record) => ({
                onClick: () => handleCustomerSelect(record),
                style: { cursor: 'pointer' }
              })"
            >
              <Table.Column title="客户ID" data-index="customerId" key="customerId" />
              <Table.Column title="最近下单日期" data-index="lastOrderDate" key="lastOrderDate" />
              <Table.Column title="订单数" data-index="orderCount" key="orderCount" />
              <Table.Column title="累计消费" data-index="totalAmount" key="totalAmount" />
              <Table.Column
                title="分层"
                key="segmentLabel"
                :customRender="({ record }) => h(Tag, null, () => record.segmentLabel)"
              />
            </Table>
          </Spin>
        </Card>
      </Col>
      <Col :span="10">
        <Card title="RFM 雷达图 & 复购预测">
          <Spin :spinning="store.loading.radar || store.loading.repurchase">
            <div v-if="selectedCustomer" class="radar-wrapper">
              <div ref="radarRef" class="chart-radar"></div>
              <Descriptions size="small" column="1" class="mt-12">
                <Descriptions.Item label="客户ID">
                  {{ selectedCustomer.customerId }}
                </Descriptions.Item>
                <Descriptions.Item label="复购概率">
                  {{ store.repurchaseProbability ?? '-' }}
                </Descriptions.Item>
              </Descriptions>
            </div>
            <div v-else class="placeholder">请在左侧表格中选择一个客户查看详情</div>
          </Spin>
        </Card>
      </Col>

      <!-- 高价值客户 Top 10 单独放一行 -->
      <Col :span="24">
        <Card title="高价值客户 Top 10">
          <Spin :spinning="store.loading.top">
            <Table
              size="small"
              :data-source="store.topCustomers"
              :pagination="false"
              row-key="customerId"
            >
              <Table.Column title="排名" key="rank" :width="60">
                <template #default="{ index }">
                  {{ index + 1 }}
                </template>
              </Table.Column>
              <Table.Column title="客户ID" data-index="customerId" key="customerId" :ellipsis="true" />
              <Table.Column title="订单数" data-index="orderCount" key="orderCount" :width="80" />
              <Table.Column title="累计消费" data-index="totalAmount" key="totalAmount" :width="120">
                <template #default="{ record }">
                  {{ record.totalAmount ? record.totalAmount.toFixed(2) : '-' }}
                </template>
              </Table.Column>
              <Table.Column title="客单价" data-index="averageOrderValue" key="averageOrderValue" :width="120">
                <template #default="{ record }">
                  {{ record.averageOrderValue ? record.averageOrderValue.toFixed(2) : '-' }}
                </template>
              </Table.Column>
            </Table>
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

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.toolbar-label {
  font-size: 14px;
}

.chart {
  width: 100%;
  height: 260px;
}

.chart-large {
  width: 100%;
  height: 350px;
}

.chart-radar {
  width: 100%;
  height: 280px;
  min-height: 280px;
  overflow: hidden;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  padding: 16px 0;
}

.stat-item {
  padding: 12px 16px;
  border-radius: 8px;
  background: #fafafa;
  text-align: center;
}

.stat-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #1890ff;
}

.radar-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.placeholder {
  text-align: center;
  color: #999;
  padding: 40px 0;
}

.mt-12 {
  margin-top: 12px;
}
</style>


