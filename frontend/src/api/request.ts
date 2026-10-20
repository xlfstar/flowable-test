import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function handleUnauthorized() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = '/login'
  }
}

service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResult
    if (res && typeof res.code === 'number' && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        handleUnauthorized()
      }
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res as unknown as never
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      ElMessage.error('登录已过期, 请重新登录')
      handleUnauthorized()
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

const http = {
  get<T>(url: string, params?: Record<string, unknown>): Promise<ApiResult<T>> {
    return service.get(url, { params }) as unknown as Promise<ApiResult<T>>
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResult<T>> {
    return service.post(url, data, config) as unknown as Promise<ApiResult<T>>
  },
  put<T>(url: string, data?: unknown): Promise<ApiResult<T>> {
    return service.put(url, data) as unknown as Promise<ApiResult<T>>
  },
  delete<T>(url: string, params?: Record<string, unknown>): Promise<ApiResult<T>> {
    return service.delete(url, { params }) as unknown as Promise<ApiResult<T>>
  }
}

export default http
