import axios, { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const normalizeToken = (value: unknown): string => {
  if (typeof value === 'string') {
    const normalized = value.trim()
    if (normalized && normalized !== 'undefined' && normalized !== 'null') {
      return normalized
    }
  }

  if (
    value &&
    typeof value === 'object' &&
    'value' in value &&
    typeof (value as { value?: unknown }).value === 'string'
  ) {
    return normalizeToken((value as { value?: unknown }).value)
  }

  return ''
}

const isPlainObject = (value: unknown): value is Record<string, unknown> =>
  Object.prototype.toString.call(value) === '[object Object]'

const normalizeQueryParams = (params: unknown): unknown => {
  if (params === undefined) {
    return undefined
  }

  if (params instanceof URLSearchParams) {
    return params
  }

  if (Array.isArray(params)) {
    const normalizedArray = params
      .map(item => normalizeQueryParams(item))
      .filter(item => item !== undefined)
    return normalizedArray.length > 0 ? normalizedArray : undefined
  }

  if (isPlainObject(params)) {
    const normalized: Record<string, unknown> = {}

    Object.entries(params).forEach(([key, value]) => {
      const normalizedValue = value === -1 || value === '-1'
        ? undefined
        : normalizeQueryParams(value)
      if (normalizedValue !== undefined) {
        normalized[key] = normalizedValue
      }
    })

    return normalized
  }

  return params
}

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
    const accessToken = normalizeToken(userStore.token)

    if (config.params !== undefined) {
      config.params = normalizeQueryParams(config.params) as InternalAxiosRequestConfig['params']
    }

    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
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
    if (response.config.responseType === 'blob' || response.data instanceof Blob) {
      return response.data
    }
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
    const requestUrl = originalRequest?.url ?? ''
    const isLogoutRequest = requestUrl.includes('/auth/logout')
    
    // 401错误且未重试过，尝试刷新token
    if (error.response?.status === 401 && !originalRequest._retry) {
      const userStore = useUserStore()
      const currentRefreshToken = normalizeToken(userStore.refreshToken)
      
      // 如果没有refreshToken，直接登出
      if (!currentRefreshToken) {
        userStore.logout()
        if (router.currentRoute.value.path !== '/login') {
          router.push('/login')
        }
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(error)
      }
      
      // 如果正在刷新token，将请求加入队列
      if (isRefreshing) {
        return new Promise((resolve) => {
          requestQueue.push((token: string) => {
            originalRequest.headers = originalRequest.headers ?? {}
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(request(originalRequest))
          })
        })
      }
      
      originalRequest._retry = true
      isRefreshing = true
      
      try {
        // 刷新token
        const response = await axios.post(
          '/api/v1/auth/refresh',
          null,
          {
            headers: {
              Authorization: `Bearer ${currentRefreshToken}`
            }
          }
        )

        const { token, refreshToken } = response.data.data
        userStore.setToken(token, refreshToken)
        
        // 重试队列中的请求
        requestQueue.forEach(callback => callback(token))
        requestQueue = []
        
        // 重试当前请求
        originalRequest.headers = originalRequest.headers ?? {}
        originalRequest.headers.Authorization = `Bearer ${token}`
        return request(originalRequest)
      } catch (refreshError) {
        // 刷新token失败，清空队列并登出
        requestQueue = []
        userStore.logout()
        if (router.currentRoute.value.path !== '/login') {
          router.push('/login')
        }
        ElMessage.error('登录已过期，请重新登录')
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    } else if (error.response?.status === 403) {
      const userStore = useUserStore()
      const accessToken = normalizeToken(userStore.token)

      if (!accessToken) {
        userStore.logout()
        if (router.currentRoute.value.path !== '/login') {
          router.push('/login')
        }
        ElMessage.error('登录状态失效，请重新登录')
      } else if (!isLogoutRequest) {
        ElMessage.error('没有权限访问该资源')
      }
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
