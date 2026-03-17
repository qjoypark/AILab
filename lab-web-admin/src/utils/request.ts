import axios, { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 30000
})

// 是否正在刷新token
let isRefreshing = false
// 待重试的请求队列
let requestQueue: Array<(token: string) => void> = []

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data
    if (code === 200 || code === 0) {
      return data
    } else {
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message))
    }
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
    
    // 401错误且未重试过，尝试刷新token
    if (error.response?.status === 401 && !originalRequest._retry) {
      const userStore = useUserStore()
      
      // 如果没有refreshToken，直接登出
      if (!userStore.refreshToken) {
        userStore.logout()
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(error)
      }
      
      // 如果正在刷新token，将请求加入队列
      if (isRefreshing) {
        return new Promise((resolve) => {
          requestQueue.push((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(request(originalRequest))
          })
        })
      }
      
      originalRequest._retry = true
      isRefreshing = true
      
      try {
        // 刷新token
        const response = await axios.post('/api/v1/auth/refresh', {
          refreshToken: userStore.refreshToken
        })
        
        const { accessToken, refreshToken } = response.data.data
        userStore.setToken(accessToken, refreshToken)
        
        // 重试队列中的请求
        requestQueue.forEach(callback => callback(accessToken))
        requestQueue = []
        
        // 重试当前请求
        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        return request(originalRequest)
      } catch (refreshError) {
        // 刷新token失败，清空队列并登出
        requestQueue = []
        userStore.logout()
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    } else if (error.response?.status === 403) {
      ElMessage.error('没有权限访问该资源')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
