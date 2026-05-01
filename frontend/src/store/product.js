import { defineStore } from 'pinia'
import {
  fetchTopCategorySales,
  fetchCategoryRepurchaseHeatmap,
  fetchTopProductSales,
  fetchCategoryStateSales
} from '../api/analysis'

// 商品/品类分析相关数据的 store
export const useProductStore = defineStore('product', {
  state: () => ({
    topCategorySales: [],
    categoryRepurchaseHeatmap: [],
    topProductSales: [],
    categoryStateSales: [],
    loading: {
      topCategories: false,
      repurchaseHeatmap: false,
      topProducts: false,
      categoryStateSales: false
    }
  }),
  actions: {
    async loadTopCategorySales(topN = 10) {
      if (this.topCategorySales.length) return // 已缓存
      this.loading.topCategories = true
      try {
        const res = await fetchTopCategorySales(topN)
        this.topCategorySales = Array.isArray(res) ? res : []
      } finally {
        this.loading.topCategories = false
      }
    },
    async loadCategoryRepurchaseHeatmap() {
      if (this.categoryRepurchaseHeatmap.length) return // 已缓存
      this.loading.repurchaseHeatmap = true
      try {
        const res = await fetchCategoryRepurchaseHeatmap()
        this.categoryRepurchaseHeatmap = Array.isArray(res) ? res : []
      } finally {
        this.loading.repurchaseHeatmap = false
      }
    },
    async loadTopProductSales(topN = 20) {
      if (this.topProductSales.length) return // 已缓存
      this.loading.topProducts = true
      try {
        const res = await fetchTopProductSales(topN)
        this.topProductSales = Array.isArray(res) ? res : []
      } finally {
        this.loading.topProducts = false
      }
    },
    async loadCategoryStateSales() {
      if (this.categoryStateSales.length) return // 已缓存
      this.loading.categoryStateSales = true
      try {
        const res = await fetchCategoryStateSales()
        this.categoryStateSales = Array.isArray(res) ? res : []
      } finally {
        this.loading.categoryStateSales = false
      }
    }
  }
})

