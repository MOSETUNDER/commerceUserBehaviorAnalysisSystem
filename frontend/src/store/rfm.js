import { defineStore } from 'pinia'
import {
  fetchRfmCustomerList,
  fetchRfmSegmentDistribution,
  fetchChurnStats,
  fetchTopCustomers,
  fetchRfmRadar,
  fetchRepurchaseProbability,
  fetchRfmSummaryStats,
  fetchAverageRfmRadar,
  fetchRfmScoreDistribution,
  fetchRepurchaseProbabilityDistribution,
  fetchRfmScatterData
} from '../api/analysis'

// RFM / 用户分层分析 store
export const useRfmStore = defineStore('rfm', {
  state: () => ({
    customers: [],
    segmentDistribution: [],
    churnStats: null,
    topCustomers: [],
    radarData: null,
    repurchaseProbability: null,
    summaryStats: null,
    averageRadarData: null,
    scoreDistribution: [],
    repurchaseProbabilityDistribution: [],
    scatterData: [],
    loading: {
      customers: false,
      segment: false,
      churn: false,
      top: false,
      radar: false,
      repurchase: false,
      summaryStats: false,
      averageRadar: false,
      scoreDistribution: false,
      repurchaseProbabilityDistribution: false,
      scatter: false
    }
  }),
  actions: {
    async loadCustomers(page = 0, size = 500) {
      if (this.customers.length) return
      this.loading.customers = true
      try {
        // 默认只加载第一页，每页 500 条，避免数据过大导致超时
        this.customers = await fetchRfmCustomerList({ page, size })
      } finally {
        this.loading.customers = false
      }
    },
    async loadSegmentDistribution() {
      if (this.segmentDistribution.length) return
      this.loading.segment = true
      try {
        this.segmentDistribution = await fetchRfmSegmentDistribution()
      } finally {
        this.loading.segment = false
      }
    },
    async loadChurnStats() {
      if (this.churnStats) return
      this.loading.churn = true
      try {
        this.churnStats = await fetchChurnStats()
      } finally {
        this.loading.churn = false
      }
    },
    async loadTopCustomers(topN = 10) {
      if (this.topCustomers.length) return
      this.loading.top = true
      try {
        this.topCustomers = await fetchTopCustomers({ topN })
      } finally {
        this.loading.top = false
      }
    },
    async loadRadar(customerId) {
      this.loading.radar = true
      try {
        this.radarData = await fetchRfmRadar(customerId)
      } finally {
        this.loading.radar = false
      }
    },
    async loadRepurchaseProbability(customerId) {
      this.loading.repurchase = true
      try {
        this.repurchaseProbability = await fetchRepurchaseProbability(customerId)
      } finally {
        this.loading.repurchase = false
      }
    },
    async loadSummaryStats() {
      if (this.summaryStats) return
      this.loading.summaryStats = true
      try {
        this.summaryStats = await fetchRfmSummaryStats()
      } finally {
        this.loading.summaryStats = false
      }
    },
    async loadAverageRadar() {
      if (this.averageRadarData) return
      this.loading.averageRadar = true
      try {
        this.averageRadarData = await fetchAverageRfmRadar()
      } finally {
        this.loading.averageRadar = false
      }
    },
    async loadScoreDistribution() {
      if (this.scoreDistribution.length) return
      this.loading.scoreDistribution = true
      try {
        this.scoreDistribution = await fetchRfmScoreDistribution()
      } finally {
        this.loading.scoreDistribution = false
      }
    },
    async loadRepurchaseProbabilityDistribution() {
      if (this.repurchaseProbabilityDistribution.length) return
      this.loading.repurchaseProbabilityDistribution = true
      try {
        this.repurchaseProbabilityDistribution = await fetchRepurchaseProbabilityDistribution()
      } finally {
        this.loading.repurchaseProbabilityDistribution = false
      }
    },
    async loadScatterData(sampleSize = 5000) {
      // 移除缓存检查，每次都重新加载（或者改为强制刷新参数）
      this.loading.scatter = true
      try {
        const data = await fetchRfmScatterData(sampleSize)
        console.log('RFM 散点图 API 返回数据量:', data?.length || 0)
        console.log('RFM 散点图 API 返回前5条:', data?.slice(0, 5))
        this.scatterData = data || []
      } finally {
        this.loading.scatter = false
      }
    }
  }
})


