import axios from 'axios'

// 开发环境可在 Vite 配置代理到 Spring Boot 服务。
const http = axios.create({
  baseURL: '/api',
  timeout: 120000
})

// 请求拦截器，可在这里统一带上 token 等
http.interceptors.request.use(
  (config) => {
    // 示例：从本地存储读取 token
    const token = localStorage.getItem('TOKEN')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器，统一处理后端 Result 包装
http.interceptors.response.use(
  (response) => {
    const data = response.data
    // 如果响应类型是 blob（文件下载），直接返回 response
    if (response.config.responseType === 'blob') {
      return response.data
    }
    // 后端 Result 结构：{ code, message, data }
    if (data && typeof data === 'object' && 'code' in data && 'data' in data) {
      if (data.code === 0 || data.code === 200) {
        return data.data
      }
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  (error) => Promise.reject(error)
)

export default http


