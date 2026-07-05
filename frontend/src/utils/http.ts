import axios, { AxiosError, type AxiosInstance, type AxiosResponse } from 'axios'
import type { ApiResponse, FieldError } from '@/types/api'
import { ApiErrorCode } from '@/types/api'
import { translateErrorCode } from '@/utils/errorMap'

/**
 * 业务错误（后端已回 200 或 4xx，但 code != 0）。
 * 401 / 403 / 5xx 有专门的分派（跳登录 / 跳 403 / 弹全局 message）；
 * 其它 code 由调用方 try/catch 处理。
 */
export class ApiError extends Error {
  readonly code: number
  readonly httpStatus: number | undefined
  readonly errors?: FieldError[]

  constructor(code: number, message: string, httpStatus?: number, errors?: FieldError[]) {
    super(message)
    this.code = code
    this.httpStatus = httpStatus
    this.errors = errors
  }
}

// ---------- 全局 side-effect handler（在 main.ts 注册） ----------
// 保持 http.ts 与 Element Plus / router 解耦，便于单测。

export interface HttpHandlers {
  onUnauthorized: (redirect?: string) => void
  onForbidden: () => void
  onServerError: (message: string) => void
}

let handlers: HttpHandlers = {
  onUnauthorized: () => {
    /* placeholder — main.ts 会覆盖 */
  },
  onForbidden: () => {
    /* placeholder */
  },
  onServerError: () => {
    /* placeholder */
  },
}

export function setHttpHandlers(h: HttpHandlers): void {
  handlers = h
}

// ---------- axios 单例 ----------

const baseURL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '/api'

export const http: AxiosInstance = axios.create({
  baseURL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器：注入 Bearer token
http.interceptors.request.use((config) => {
  // 动态引入，避免在 store 初始化前访问 pinia
  const token = window.sessionStorage.getItem('csm.auth.access') ??
    window.localStorage.getItem('csm.auth.access')
  if (token) {
    config.headers = config.headers ?? {}
    ;(config.headers as Record<string, string>)['Authorization'] = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：解构信封 + 分派 side-effect
http.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    const body = response.data
    if (!body || typeof body.code !== 'number') {
      // 非标准响应，直接抛给调用方
      throw new ApiError(ApiErrorCode.INTERNAL_ERROR, '非标准响应体', response.status)
    }
    if (body.code === ApiErrorCode.SUCCESS) {
      // 把 data 提升为 axios response.data，便于 `const {data} = await http.get(...)`
      // 保留原信封在 response.headers['x-envelope'] 以便调试（非必需）
      return { ...response, data: body.data as unknown } as AxiosResponse<unknown>
    }
    // 200 但 code != 0：抛业务错误
    throw new ApiError(body.code, body.message ?? translateErrorCode(body.code), response.status, body.errors)
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status
    const body = error.response?.data
    const code = body?.code ?? ApiErrorCode.INTERNAL_ERROR
    const message = body?.message ?? translateErrorCode(code, error.message)

    // 401：清 token + 跳登录
    if (status === 401 || code === ApiErrorCode.UNAUTHORIZED) {
      const redirect = window.location.pathname + window.location.search
      handlers.onUnauthorized(redirect)
      return Promise.reject(new ApiError(ApiErrorCode.UNAUTHORIZED, message, status))
    }

    // 403：跳 forbidden
    if (status === 403 || code === ApiErrorCode.FORBIDDEN) {
      handlers.onForbidden()
      return Promise.reject(new ApiError(ApiErrorCode.FORBIDDEN, message, status))
    }

    // 5xx：全局 toast
    if (status && status >= 500) {
      handlers.onServerError(translateErrorCode(ApiErrorCode.INTERNAL_ERROR, '服务暂时不可用'))
      return Promise.reject(new ApiError(code, message, status))
    }

    // 其它（含网络错误 / 4xx）交给调用方
    return Promise.reject(new ApiError(code, message, status, body?.errors))
  },
)

// ---------- 便捷读方法 ----------

export async function getJson<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const res = await http.get<T>(url, { params })
  return res.data
}

export async function postJson<T>(url: string, body?: unknown): Promise<T> {
  const res = await http.post<T>(url, body ?? {})
  return res.data
}
