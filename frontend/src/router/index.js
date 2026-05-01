import { createRouter, createWebHistory } from 'vue-router'
import ConsumptionAnalysis from '../views/ConsumptionAnalysis.vue'
import RfmAnalysis from '../views/RfmAnalysis.vue'
import SellerAnalysis from '../views/SellerAnalysis.vue'
import GeoAnalysis from '../views/GeoAnalysis.vue'
import ProductAnalysis from '../views/ProductAnalysis.vue'
import UserBehaviorAnalysis from '../views/UserBehaviorAnalysis.vue'

const routes = [
  {
    path: '/',
    redirect: '/analysis/consumption'
  },
  {
    path: '/analysis/consumption',
    name: 'ConsumptionAnalysis',
    component: ConsumptionAnalysis
  },
  {
    path: '/analysis/rfm',
    name: 'RfmAnalysis',
    component: RfmAnalysis
  },
  {
    path: '/analysis/seller',
    name: 'SellerAnalysis',
    component: SellerAnalysis
  },
  {
    path: '/analysis/geo',
    name: 'GeoAnalysis',
    component: GeoAnalysis
  },
  {
    path: '/analysis/product',
    name: 'ProductAnalysis',
    component: ProductAnalysis
  },
  {
    path: '/analysis/behavior',
    name: 'UserBehaviorAnalysis',
    component: UserBehaviorAnalysis
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

