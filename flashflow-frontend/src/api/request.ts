import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: '/api/flashflow',
  timeout: 15000,
})

// Token 刷新锁（防止并发刷新）
let isRefreshing = false
let refreshSubscribers: ((token: string) => void)[] = []

function onRefreshed(token: string) {
  refreshSubscribers.forEach(cb => cb(token))
  refreshSubscribers = []
}

function addRefreshSubscriber(cb: (token: string) => void) {
  refreshSubscribers.push(cb)
}

// 请求拦截器：自动带 Token
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一错误处理 + Token 自动刷新
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 0) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }
    return res
  },
  async (error) => {
    const originalRequest = error.config

    // 401 且未重试过 → 尝试刷新 Token
    if (error.response?.status === 401 && !originalRequest._retry) {
      const userStore = useUserStore()
      const refreshToken = localStorage.getItem('flashflow_refreshToken')
      if (refreshToken) {
        if (!isRefreshing) {
          isRefreshing = true
          originalRequest._retry = true
          try {
            const res = await axios.post('/api/flashflow/auth/refresh', null, {
              params: { refreshToken }
            })
            const newToken = res.data.data.accessToken
            userStore.setToken(newToken)
            localStorage.setItem('flashflow_token', newToken)
            isRefreshing = false
            onRefreshed(newToken)
            // 重试原请求
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            return request(originalRequest)
          } catch {
            isRefreshing = false
            // 拒绝所有等待刷新结果的排队请求，避免永久挂起
            refreshSubscribers.forEach(cb => cb(''))
            refreshSubscribers = []
            userStore.logout()
            window.location.href = '/login'
            return Promise.reject(error)
          }
        } else {
          // 等待其他请求的刷新结果
          return new Promise(resolve => {
            addRefreshSubscriber((token: string) => {
              originalRequest.headers.Authorization = `Bearer ${token}`
              resolve(request(originalRequest))
            })
          })
        }
      } else {
        userStore.logout()
        window.location.href = '/login'
      }
    }

    // 网络不可达（无响应）不弹 Toast，页面自行处理空状态
    if (!error.response) {
      return Promise.reject(error)
    }

    // 不显示401错误弹窗
    if (error.response.status !== 401) {
      ElMessage.error(error.response.data?.msg || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
