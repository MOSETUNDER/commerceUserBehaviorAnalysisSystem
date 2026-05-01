import { defineStore } from 'pinia'
import {
  fetchUserActiveTimeHeatmap,
  fetchOrderFunnel,
  fetchUserBehaviorSankey
} from '../api/analysis'

// 用户行为路径分析相关数据的 store
export const useBehaviorStore = defineStore('behavior', {
  state: () => ({
    userActiveTimeHeatmap: [],
    orderFunnel: [],
    userBehaviorSankey: null,
    loading: {
      activeTimeHeatmap: false,
      orderFunnel: false,
      sankey: false
    }
  }),
  actions: {
    async loadUserActiveTimeHeatmap() {
      if (this.userActiveTimeHeatmap.length) return // 已缓存
      this.loading.activeTimeHeatmap = true
      try {
        const res = await fetchUserActiveTimeHeatmap()
        this.userActiveTimeHeatmap = Array.isArray(res) ? res : []
      } finally {
        this.loading.activeTimeHeatmap = false
      }
    },
    async loadOrderFunnel() {
      if (this.orderFunnel.length) return // 已缓存
      this.loading.orderFunnel = true
      try {
        const res = await fetchOrderFunnel()
        this.orderFunnel = Array.isArray(res) ? res : []
      } finally {
        this.loading.orderFunnel = false
      }
    },
    async loadUserBehaviorSankey() {
      if (this.userBehaviorSankey) return // 已缓存
      this.loading.sankey = true
      try {
        const res = await fetchUserBehaviorSankey()
        this.userBehaviorSankey = res || null
      } finally {
        this.loading.sankey = false
      }
    }
  }
})

