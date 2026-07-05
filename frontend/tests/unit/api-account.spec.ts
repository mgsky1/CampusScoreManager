import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiErrorCode } from '@/types/api'

async function load() {
  vi.resetModules()
  const api = await import('@/api/account')
  const httpMod = await import('@/utils/http')
  return { ...api, ...httpMod }
}

function okAdapter(data: unknown, status = 200, statusText = 'OK') {
  return vi.fn().mockResolvedValue({
    status,
    data: { code: 0, message: 'ok', data },
    headers: {},
    config: {},
    statusText,
  })
}

function errorAdapter(status: number, code: number, message = 'err', errors?: unknown[]) {
  return vi.fn().mockRejectedValue({
    response: { status, data: { code, message, errors } },
    isAxiosError: true,
  })
}

describe('api/account', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    window.localStorage.clear()
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('getProfile returns TEACHER profile without address/grade', async () => {
    const { getProfile, http } = await load()
    http.defaults.adapter = okAdapter({
      id: 1,
      loginName: 'ttt',
      realName: '田老师',
      role: 'TEACHER',
      tel: '18065853353',
    }) as never

    const r = await getProfile()
    expect(r.role).toBe('TEACHER')
    expect(r.tel).toBe('18065853353')
    expect(r.address).toBeUndefined()
    expect(r.grade).toBeUndefined()
  })

  it('getProfile returns STUDENT profile with address/grade', async () => {
    const { getProfile, http } = await load()
    http.defaults.adapter = okAdapter({
      id: 3,
      loginName: 'hhh',
      realName: '黄同学',
      role: 'STUDENT',
      tel: '18075853353',
      address: 'SMU',
      grade: 3,
    }) as never

    const r = await getProfile()
    expect(r.role).toBe('STUDENT')
    expect(r.address).toBe('SMU')
    expect(r.grade).toBe(3)
  })

  it('updateProfile submits {tel} only for teacher and returns detail', async () => {
    const { updateProfile, http } = await load()
    const adapter = okAdapter({
      id: 1,
      loginName: 'ttt',
      realName: '田老师',
      role: 'TEACHER',
      tel: '13900139000',
    })
    http.defaults.adapter = adapter as never

    const r = await updateProfile({ tel: '13900139000' })
    expect(r.tel).toBe('13900139000')
    const call = adapter.mock.calls[0][0] as { data?: string }
    expect(JSON.parse(call.data as string)).toEqual({ tel: '13900139000' })
  })

  it('updateProfile submits {tel,address} for student', async () => {
    const { updateProfile, http } = await load()
    const adapter = okAdapter({
      id: 3,
      loginName: 'hhh',
      realName: '黄同学',
      role: 'STUDENT',
      tel: '13800138888',
      address: '深圳大学',
      grade: 3,
    })
    http.defaults.adapter = adapter as never

    await updateProfile({ tel: '13800138888', address: '深圳大学' })
    const call = adapter.mock.calls[0][0] as { data?: string }
    expect(JSON.parse(call.data as string)).toEqual({
      tel: '13800138888',
      address: '深圳大学',
    })
  })

  it('updateProfile maps 1000 validation error with field errors', async () => {
    const { updateProfile, http } = await load()
    http.defaults.adapter = errorAdapter(400, ApiErrorCode.VALIDATION_FAILED, 'invalid', [
      { field: 'tel', code: 'VALIDATION_PATTERN' },
    ]) as never

    await expect(updateProfile({ tel: '' })).rejects.toMatchObject({
      code: ApiErrorCode.VALIDATION_FAILED,
    })
  })

  it('changePassword resolves on success', async () => {
    const { changePassword, http } = await load()
    http.defaults.adapter = okAdapter(null) as never
    await expect(
      changePassword({
        oldPassword: '123456',
        newPassword: 'newpass1',
        confirmPassword: 'newpass1',
      }),
    ).resolves.toBeNull()
  })

  it('changePassword maps 2002 OLD_PASSWORD_MISMATCH', async () => {
    const { changePassword, http } = await load()
    http.defaults.adapter = errorAdapter(400, ApiErrorCode.OLD_PASSWORD_MISMATCH, '原密码错误') as never
    await expect(
      changePassword({
        oldPassword: 'wrong',
        newPassword: 'newpass1',
        confirmPassword: 'newpass1',
      }),
    ).rejects.toMatchObject({ code: ApiErrorCode.OLD_PASSWORD_MISMATCH })
  })

  it('changePassword maps 2003 CONFIRM_PASSWORD_MISMATCH', async () => {
    const { changePassword, http } = await load()
    http.defaults.adapter = errorAdapter(400, ApiErrorCode.CONFIRM_PASSWORD_MISMATCH) as never
    await expect(
      changePassword({
        oldPassword: '123456',
        newPassword: 'newpass1',
        confirmPassword: 'other',
      }),
    ).rejects.toMatchObject({ code: ApiErrorCode.CONFIRM_PASSWORD_MISMATCH })
  })

  it('changePassword maps 1000 field validation (Bean Validation confirmMatchesNew)', async () => {
    const { changePassword, http } = await load()
    http.defaults.adapter = errorAdapter(400, ApiErrorCode.VALIDATION_FAILED, 'invalid', [
      { field: 'confirmMatchesNew', code: 'VALIDATION_MISMATCH' },
    ]) as never
    await expect(
      changePassword({
        oldPassword: '123456',
        newPassword: 'newpass1',
        confirmPassword: 'other',
      }),
    ).rejects.toMatchObject({ code: ApiErrorCode.VALIDATION_FAILED })
  })
})
