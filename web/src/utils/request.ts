import axios, {
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import {
  getAccessToken,
  getRefreshToken,
  setAccessToken,
  clearAuth,
} from './auth'
import { ElMessage } from 'element-plus'

const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 刷新 token 用的独立 axios 实例（不加拦截器，避免循环）
const refreshInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 是否正在刷新 token
let isRefreshing = false
// 等待刷新期间挂起的请求队列
let pendingRequests: Array<(token: string) => void> = []

// 请求拦截器
instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截��
instance.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response
    // Jiangke 框架：errorCode 为 "0" 或 "SUCCESS" 表示成���
    if (data.errorCode && data.errorCode !== '0' && data.errorCode !== 'SUCCESS') {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  async (error) => {
    const originalRequest = error.config

    // 401 自动刷新 token
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 已有刷新在进行中，把当前请求加入等待队列
        return new Promise((resolve) => {
          pendingRequests.push((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(instance(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const refreshToken = getRefreshToken()
        if (!refreshToken) {
          throw new Error('无刷新令牌')
        }

        const res = await refreshInstance.post('/auth/refresh', {
          refreshToken,
        })

        const newAccessToken = res.data?.data?.accessToken
        if (!newAccessToken) {
          throw new Error('刷新令牌失败：未获取到新的 access token')
        }

        // 保存新 token
        setAccessToken(newAccessToken)

        // 重试所有挂起的请求
        pendingRequests.forEach((callback) => callback(newAccessToken))
        pendingRequests = []

        // 重试当前请求
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        return instance(originalRequest)
      } catch (refreshError) {
        // 刷新失败，清除所有认证信息
        pendingRequests = []
        clearAuth()
        ElMessage.error('登录已过期，请重新登录')
        window.location.href = '/auth/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // 其他 HTTP 错误处理
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 403:
          ElMessage.error(data?.message || '没有权限访问')
          break
        case 404:
          ElMessage.error(data?.message || '请求的资源不存在')
          break
        case 409:
          ElMessage.error(data?.message || '资源冲突')
          break
        case 422:
          ElMessage.error(data?.message || '请求参数有误')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        case 502:
          ElMessage.error('网关错误')
          break
        default:
          ElMessage.error(data?.message || `请求错误 ${status}`)
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }

    return Promise.reject(error)
  }
)

export default instance
