import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { ApiResponse } from '@/types/api'
import { ApiErrorCode } from '@/types/api'

/**
 * http.ts 是一个 module-level 单例。为了让每个 case 都能重置 handlers 与拦截器，
 * 我们在测试里用 `import.meta.vitest` 的 `resetModules` 语义：动态 import。
 */
async function loadHttp() {
  vi.resetModules()
  return await import('@/utils/http')
}

describe('http.ts response envelope', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    window.localStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('code=0 unwraps data field', async () => {
    const { http } = await loadHttp()
    const envelope: ApiResponse<{ hello: string }> = {
      code: 0,
      message: 'ok',
      data: { hello: 'world' },
    }
    const adapter = vi.fn().mockResolvedValue({
      status: 200,
      data: envelope,
      headers: {},
      config: {},
      statusText: 'OK',
    })
    http.defaults.adapter = adapter as never

    const res = await http.get('/anything')
    expect(res.data).toEqual({ hello: 'world' })
  })

  it('code!=0 with http 200 throws ApiError with same code', async () => {
    const { http, ApiError } = await loadHttp()
    const envelope: ApiResponse<null> = {
      code: ApiErrorCode.LOGIN_FAILED,
      message: '登录名或密码错误',
      data: null,
    }
    http.defaults.adapter = vi.fn().mockResolvedValue({
      status: 200,
      data: envelope,
      headers: {},
      config: {},
      statusText: 'OK',
    }) as never

    await expect(http.get('/api/v1/auth/login')).rejects.toBeInstanceOf(ApiError)
    try {
      await http.get('/api/v1/auth/login')
    } catch (e) {
      expect((e as InstanceType<typeof ApiError>).code).toBe(ApiErrorCode.LOGIN_FAILED)
    }
  })

  it('http 401 triggers onUnauthorized handler', async () => {
    const { http, setHttpHandlers } = await loadHttp()
    const onUnauthorized = vi.fn()
    setHttpHandlers({
      onUnauthorized,
      onForbidden: vi.fn(),
      onServerError: vi.fn(),
    })
    http.defaults.adapter = vi.fn().mockRejectedValue({
      response: {
        status: 401,
        data: { code: ApiErrorCode.UNAUTHORIZED, message: '未认证' },
      },
      isAxiosError: true,
    }) as never

    await expect(http.get('/api/v1/student/scores')).rejects.toThrow()
    expect(onUnauthorized).toHaveBeenCalledTimes(1)
  })

  it('http 403 triggers onForbidden handler', async () => {
    const { http, setHttpHandlers } = await loadHttp()
    const onForbidden = vi.fn()
    setHttpHandlers({
      onUnauthorized: vi.fn(),
      onForbidden,
      onServerError: vi.fn(),
    })
    http.defaults.adapter = vi.fn().mockRejectedValue({
      response: {
        status: 403,
        data: { code: ApiErrorCode.FORBIDDEN, message: '越权' },
      },
      isAxiosError: true,
    }) as never

    await expect(http.get('/api/v1/teacher/subjects')).rejects.toThrow()
    expect(onForbidden).toHaveBeenCalledTimes(1)
  })

  it('http 500 triggers onServerError handler', async () => {
    const { http, setHttpHandlers } = await loadHttp()
    const onServerError = vi.fn()
    setHttpHandlers({
      onUnauthorized: vi.fn(),
      onForbidden: vi.fn(),
      onServerError,
    })
    http.defaults.adapter = vi.fn().mockRejectedValue({
      response: {
        status: 500,
        data: { code: ApiErrorCode.INTERNAL_ERROR, message: '内部错误' },
      },
      isAxiosError: true,
    }) as never

    await expect(http.get('/api/v1/any')).rejects.toThrow()
    expect(onServerError).toHaveBeenCalledTimes(1)
  })

  it('request injects Authorization header when access token is stored', async () => {
    window.sessionStorage.setItem('csm.auth.access', 'abc.def.ghi')
    const { http } = await loadHttp()
    let capturedHeaders: Record<string, string> | undefined
    http.defaults.adapter = vi.fn().mockImplementation((config: unknown) => {
      capturedHeaders = (config as { headers?: Record<string, string> }).headers
      return Promise.resolve({
        status: 200,
        data: { code: 0, message: 'ok', data: null },
        headers: {},
        config,
        statusText: 'OK',
      })
    }) as never

    await http.get('/api/v1/auth/me')
    expect(capturedHeaders?.Authorization).toBe('Bearer abc.def.ghi')
  })
})
