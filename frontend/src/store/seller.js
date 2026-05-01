import { defineStore } from 'pinia'
import {
  fetchTopSellersBySales,
  fetchSellerSalesDistribution,
  fetchTopSellersRepurchaseRate,
  fetchTopSellersAverageOrderValue,
  fetchSellerOrderTrend,
  fetchSellerReviewDistribution
} from '../api/analysis'

// 卖家分析 store（带缓存）
export const useSellerStore = defineStore('seller', {
  state: () => ({
    topSellersBySales: [],
    salesDistribution: [],
    repurchaseRate: [],
    averageOrderValue: [],
    orderTrend: [],
    reviewDistribution: [],
    loading: {
      topSellers: false,
      salesDistribution: false,
      repurchaseRate: false,
      averageOrderValue: false,
      orderTrend: false,
      reviewDistribution: false
    }
  }),
  actions: {
    async loadTopSellersBySales(topN = 20) {
      if (this.topSellersBySales.length) return
      this.loading.topSellers = true
      try {
        this.topSellersBySales = await fetchTopSellersBySales(topN)
      } finally {
        this.loading.topSellers = false
      }
    },
    async loadSalesDistribution() {
      if (this.salesDistribution.length) return
      this.loading.salesDistribution = true
      try {
        this.salesDistribution = await fetchSellerSalesDistribution()
      } finally {
        this.loading.salesDistribution = false
      }
    },
    async loadRepurchaseRate(topN = 20) {
      if (this.repurchaseRate.length) return
      this.loading.repurchaseRate = true
      try {
        this.repurchaseRate = await fetchTopSellersRepurchaseRate(topN)
      } finally {
        this.loading.repurchaseRate = false
      }
    },
    async loadAverageOrderValue(topN = 20) {
      if (this.averageOrderValue.length) return
      this.loading.averageOrderValue = true
      try {
        this.averageOrderValue = await fetchTopSellersAverageOrderValue(topN)
      } finally {
        this.loading.averageOrderValue = false
      }
    },
    async loadOrderTrend(topN = 20) {
      if (this.orderTrend.length) return
      this.loading.orderTrend = true
      try {
        this.orderTrend = await fetchSellerOrderTrend(topN)
      } finally {
        this.loading.orderTrend = false
      }
    },
    async loadReviewDistribution(topN = 20) {
      if (this.reviewDistribution.length) return
      this.loading.reviewDistribution = true
      try {
        this.reviewDistribution = await fetchSellerReviewDistribution(topN)
      } finally {
        this.loading.reviewDistribution = false
      }
    }
  }
})


