import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { ResultCode, type ApiResult } from '@/types/api'
import { useUserStore } from '@/stores/user'
import { router } from '@/router'

NProgress.configure({ showSpinner: false })

/** Token 存储 Key */
const ACCESS_TOKEN_KEY = 'grain_access_token'
const REFRESH_TOKEN_KEY = 'grain_refresh_token'

export function getAccessToken(): string {
  return localStorage.getItem(ACCESS_TOKEN_KEY) || ''
}
export function getRefreshToken(): string {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || ''
}
export function setTokens(access: string, refresh: string) {
  localStorage.setItem(ACCESS_TOKEN_KEY, access)
  localStorage.setItem(REFRESH_TOKEN_KEY, refresh)
}
export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

/** 是否正在刷新 Token（防止并发刷新） */
let isRefreshing = false
let refreshSubscribers: ((token: string) => void)[] = []

function subscribeTokenRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb)
}
function notifyTokenRefresh(token: string) {
  refreshSubscribers.forEach((cb) => cb(token))
  refreshSubscribers = []
}

const service: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

/** 请求拦截器 - 注入 Token */
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    NProgress.start()
    const token = getAccessToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    NProgress.done()
    return Promise.reject(error)
  }
)

/** 响应拦截器 - 统一解包 + 错误处理 */
service.interceptors.response.use(
  (response) => {
    NProgress.done()
    const data = response.data as ApiResult

    // 文件下载等非 JSON 响应
    if (response.config.responseType === 'blob') {
      return response
    }

    if (data.code === ResultCode.SUCCESS) {
      return data.data as any
    }

    // 业务错误
    ElMessage.error(data.message || '请求失败')
    return Promise.reject(new Error(data.message || 'Error'))
  },
  async (error: AxiosError<ApiResult>) => {
    NProgress.done()
    const status = error.response?.status
    const code = error.response?.data?.code

    // Token 过期：尝试刷新
    if (status === 401 || code === ResultCode.TOKEN_EXPIRED || code === ResultCode.UNAUTHORIZED) {
      const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
      if (originalRequest._retry) {
        // 已经重试过，跳登录
        redirectToLogin()
        return Promise.reject(error)
      }

      if (isRefreshing) {
        // 正在刷新，挂起请求
        return new Promise((resolve) => {
          subscribeTokenRefresh((token: string) => {
            if (originalRequest.headers) {
              originalRequest.headers.Authorization = `Bearer ${token}`
            }
            resolve(service(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true
      try {
        const refreshToken = getRefreshToken()
        if (!refreshToken) {
          redirectToLogin()
          return Promise.reject(error)
        }
        const res = await axios.post<ApiResult<any>>('/api/v1/auth/refresh', null, {
          params: { refreshToken }
        })
        const data = res.data.data
        setTokens(data.accessToken, data.refreshToken)
        notifyTokenRefresh(data.accessToken)
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
        }
        return service(originalRequest)
      } catch (e) {
        redirectToLogin()
        return Promise.reject(e)
      } finally {
        isRefreshing = false
      }
    }

    // 403 无权限
    if (status === 403 || code === ResultCode.FORBIDDEN) {
      ElMessage.error('无访问权限')
      return Promise.reject(error)
    }

    // 登录锁定
    if (code === ResultCode.LOGIN_LOCKED) {
      ElMessage.error(error.response?.data?.message || '账号已锁定')
      return Promise.reject(error)
    }

    // 其他错误
    const msg = error.response?.data?.message || error.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

function redirectToLogin() {
  clearTokens()
  const userStore = useUserStore()
  userStore.reset()
  const current = router.currentRoute.value
  if (current.name !== 'Login') {
    router.replace({
      name: 'Login',
      query: { redirect: current.fullPath }
    })
  }
}

/** 通用请求方法 */
export const request = {
  get<T = unknown>(url: string, params?: Record<string, any>, config?: AxiosRequestConfig): Promise<T> {
    return service.get(url, { params, ...config }) as unknown as Promise<T>
  },
  post<T = unknown>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.post(url, data, config) as unknown as Promise<T>
  },
  put<T = unknown>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return service.put(url, data, config) as unknown as Promise<T>
  },
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.delete(url, config) as unknown as Promise<T>
  }
}

export default service
