import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiErrorCode } from '@/types/api'

async function load() {
  vi.resetModules()
  const api = await import('@/api/auth')
  const httpMod = await import('@/utils/http')
  return { ...api, ...httpMod }
}

describe('api/auth', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    window.localStorage.clear()
  })
  afterEach(() => { vi.restoreAllMocks() })

  it('login returns unwrapped data on code=0', async () => {
    const { login, http } = await load()
    http.defaults.adapter = vi.fn().mockResolvedValue({
      status: 200,
      data: {
        code: 0,
        message: 'ok',
        data: {
          accessToken: 'a',
          refreshToken: 'r',
          user: { id: 3, loginName: 'hhh', realName: '黄同学', role: 'STUDENT' },
        },
      },
      headers: {},
      config: {},
      statusText: 'OK',
    }) as never

    const r = await login({ loginName: 'hhh', password: '123456' })
    expect(r.accessToken).toBe('a')
    expect(r.user.role).toBe('STUDENT')
  })

  it('login throws ApiError with code 2001 when wrong password', async () => {
    const { login, http, ApiError } = await load()
    http.defaults.adapter = vi.fn().mockResolvedValue({
      status: 400,
      data: { code: ApiErrorCode.LOGIN_FAILED, message: '登录名或密码错误' },
      headers: {},
      config: {},
      statusText: 'Bad Request',
    }) as never

    await expect(login({ loginName: 'x', password: 'bad' })).rejects.toBeInstanceOf(ApiError)
  })

  it('refresh triggers onUnauthorized when 401', async () => {
    const { refresh, http, setHttpHandlers } = await load()
    const onUnauthorized = vi.fn()
    setHttpHandlers({ onUnauthorized, onForbidden: vi.fn(), onServerError: vi.fn() })
    http.defaults.adapter = vi.fn().mockRejectedValue({
      response: { status: 401, data: { code: ApiErrorCode.UNAUTHORIZED, message: '过期' } },
      isAxiosError: true,
    }) as never

    await expect(refresh('bad')).rejects.toThrow()
    expect(onUnauthorized).toHaveBeenCalled()
  })

  it('me returns UserBrief', async () => {
    const { me, http } = await load()
    http.defaults.adapter = vi.fn().mockResolvedValue({
      status: 200,
      data: {
        code: 0,
        message: 'ok',
        data: { id: 1, loginName: 'ttt', realName: '田老师', role: 'TEACHER' },
      },
      headers: {},
      config: {},
      statusText: 'OK',
    }) as never

    const u = await me()
    expect(u.role).toBe('TEACHER')
  })
})
