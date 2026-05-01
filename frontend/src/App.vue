<script setup>
import { Layout, Menu } from 'ant-design-vue'
import { RouterView, useRouter, useRoute } from 'vue-router'
import { computed } from 'vue'

const router = useRouter()
const route = useRoute()

const selectedKeys = computed(() => [route.path])

const menuItems = [
  {
    path: '/analysis/consumption',
    label: '消费行为分析'
  },
  {
    path: '/analysis/rfm',
    label: 'RFM 用户分层分析'
  },
  {
    path: '/analysis/seller',
    label: '卖家分析'
  },
  {
    path: '/analysis/geo',
    label: '地理分析'
  },
  {
    path: '/analysis/product',
    label: '商品/品类分析'
  },
  {
    path: '/analysis/behavior',
    label: '用户行为路径分析'
  }
]

const handleMenuClick = ({ key }) => {
  if (key && key !== route.path) {
    router.push(key)
  }
}
</script>

<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider theme="dark" width="220">
      <div class="logo-area">
        <span class="logo-title">Olist跨境电商分析</span>
      </div>
      <a-menu
        theme="dark"
        mode="inline"
        :selected-keys="selectedKeys"
        @click="handleMenuClick"
      >
        <a-menu-item v-for="item in menuItems" :key="item.path">
          {{ item.label }}
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="header-bar">
        <h1 class="header-title">用户消费行为数据分析系统</h1>
      </a-layout-header>
      <a-layout-content class="main-content">
        <RouterView />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.logo-area {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
  font-size: 16px;
}

.header-bar {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
}

.header-title {
  margin: 0;
  font-size: 18px;
}

.main-content {
  margin: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
</style>
