import { defineStore } from 'pinia'
import {
  fetchStateSalesHeatmap,
  fetchStateOrderCount,
  fetchStateAverageOrderValue,
  fetchStateRepurchaseRate,
  fetchLogisticsEfficiency,
  fetchGeoUserGrowth
} from '../api/analysis'

// 地理分析相关数据的 store
// 注意：这里会对接口结果做缓存，避免同一页面频繁重复请求。
export const useGeoStore = defineStore('geo', {
  state: () => ({
    stateSalesHeatmap: [],
    stateOrderCount: [],
    stateAverageOrderValue: [],
    stateRepurchaseRate: [],
    logisticsEfficiency: [],
    geoUserGrowth: [],
    loading: {
      salesHeatmap: false,
      orderCount: false,
      averageOrderValue: false,
      repurchaseRate: false,
      logisticsEfficiency: false,
      userGrowth: false
    }
  }),
  actions: {
    async loadStateSalesHeatmap() {
      if (this.stateSalesHeatmap.length) return // 已缓存
      this.loading.salesHeatmap = true
      try {
        const res = await fetchStateSalesHeatmap()
        this.stateSalesHeatmap = Array.isArray(res) ? res : []
      } finally {
        this.loading.salesHeatmap = false
      }
    },
    async loadStateOrderCount(topN = 10) {
      if (this.stateOrderCount.length) return // 已缓存
      this.loading.orderCount = true
      try {
        const res = await fetchStateOrderCount(topN)
        this.stateOrderCount = Array.isArray(res) ? res : []
      } finally {
        this.loading.orderCount = false
      }
    },
    async loadStateAverageOrderValue(topN = 10) {
      if (this.stateAverageOrderValue.length) return // 已缓存
      this.loading.averageOrderValue = true
      try {
        const res = await fetchStateAverageOrderValue(topN)
        this.stateAverageOrderValue = Array.isArray(res) ? res : []
      } finally {
        this.loading.averageOrderValue = false
      }
    },
    async loadStateRepurchaseRate(topN = 10) {
      if (this.stateRepurchaseRate.length) return // 已缓存
      this.loading.repurchaseRate = true
      try {
        const res = await fetchStateRepurchaseRate(topN)
        this.stateRepurchaseRate = Array.isArray(res) ? res : []
      } finally {
        this.loading.repurchaseRate = false
      }
    },
    async loadLogisticsEfficiency() {
      if (this.logisticsEfficiency.length) return // 已缓存
      this.loading.logisticsEfficiency = true
      try {
        const res = await fetchLogisticsEfficiency()
        this.logisticsEfficiency = Array.isArray(res) ? res : []
      } finally {
        this.loading.logisticsEfficiency = false
      }
    },
    async loadGeoUserGrowth() {
      if (this.geoUserGrowth.length) return // 已缓存
      this.loading.userGrowth = true
      try {
        const res = await fetchGeoUserGrowth()
        this.geoUserGrowth = Array.isArray(res) ? res : []
      } finally {
        this.loading.userGrowth = false
      }
    }
  }
})
