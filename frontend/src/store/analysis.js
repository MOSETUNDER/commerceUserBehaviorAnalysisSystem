import { defineStore } from 'pinia'
import {
  fetchOverview,
  fetchOrderTrend,
  fetchReviewScoreDistribution,
  fetchOrderAmountTrend,
  fetchAverageOrderValueTrend,
  fetchCustomerAmountDistribution
} from '../api/analysis'

// 消费行为分析相关数据的 store
// 注意：这里会对接口结果做缓存，避免同一页面频繁重复请求。
export const useAnalysisStore = defineStore('analysis', {
  state: () => ({
    overview: null,
    orderTrend: [],
    reviewScoreDistribution: [],
    orderAmountTrend: [],
    averageOrderValueTrend: [],
    customerAmountDistribution: [],
    loading: {
      overview: false,
      orderTrend: false,
      reviewScore: false,
      orderAmountTrend: false,
      averageOrderValueTrend: false,
      customerAmountDistribution: false
    }
  }),
  actions: {
    async loadOverview() {
      if (this.overview) return // 已缓存
      this.loading.overview = true
      try {
        this.overview = await fetchOverview()
      } finally {
        this.loading.overview = false
      }
    },
    async loadOrderTrend() {
      if (this.orderTrend.length) return
      this.loading.orderTrend = true
      try {
        this.orderTrend = await fetchOrderTrend()
      } finally {
        this.loading.orderTrend = false
      }
    },
    async loadReviewScoreDistribution() {
      if (this.reviewScoreDistribution.length) return
      this.loading.reviewScore = true
      try {
        this.reviewScoreDistribution = await fetchReviewScoreDistribution()
      } finally {
        this.loading.reviewScore = false
      }
    },
    async loadOrderAmountTrend(groupBy = 'day') {
      if (this.orderAmountTrend.length) return
      this.loading.orderAmountTrend = true
      try {
        this.orderAmountTrend = await fetchOrderAmountTrend(groupBy)
      } finally {
        this.loading.orderAmountTrend = false
      }
    },
    async loadAverageOrderValueTrend() {
      if (this.averageOrderValueTrend.length) return
      this.loading.averageOrderValueTrend = true
      try {
        this.averageOrderValueTrend = await fetchAverageOrderValueTrend()
      } finally {
        this.loading.averageOrderValueTrend = false
      }
    },
    async loadCustomerAmountDistribution() {
      if (this.customerAmountDistribution.length) return
      this.loading.customerAmountDistribution = true
      try {
        this.customerAmountDistribution = await fetchCustomerAmountDistribution()
      } finally {
        this.loading.customerAmountDistribution = false
      }
    }
  }
})


