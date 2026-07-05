import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiErrorCode } from '@/types/api'

async function load() {
  vi.resetModules()
  const api = await import('@/api/studentScores')
  const httpMod = await import('@/utils/http')
  return { ...api, ...httpMod }
}

describe('api/studentScores', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    window.localStorage.clear()
  })
  afterEach(() => { vi.restoreAllMocks() })

  it('listMyScores unwraps PageResult on code=0', async () => {
    const { listMyScores, http } = await load()
    http.defaults.adapter = vi.fn().mockResolvedValue({
      status: 200,
      data: {
        code: 0,
        message: 'ok',
        data: {
          items: [
            {
              id: 1,
              studentId: 3,
              subjectId: 5,
              subjectName: 'Java EE',
              teacherId: 2,
              teacherName: '伍老师',
              score: 99,
              grade: 3,
              isFailing: false,
              updatedAt: '2026-07-04T12:00:00',
            },
          ],
          total: 1,
          page: 1,
          size: 50,
          hasNext: false,
        },
      },
      headers: {},
      config: {},
      statusText: 'OK',
    }) as never

    const r = await listMyScores({ page: 1, size: 50 })
    expect(r.items).toHaveLength(1)
    expect(r.items[0].subjectName).toBe('Java EE')
    expect(r.total).toBe(1)
  })

  it('listMyScores maps 403 to onForbidden handler', async () => {
    const { listMyScores, http, setHttpHandlers } = await load()
    const onForbidden = vi.fn()
    setHttpHandlers({ onUnauthorized: vi.fn(), onForbidden, onServerError: vi.fn() })
    http.defaults.adapter = vi.fn().mockRejectedValue({
      response: { status: 403, data: { code: ApiErrorCode.FORBIDDEN, message: '越权' } },
      isAxiosError: true,
    }) as never

    await expect(listMyScores()).rejects.toThrow()
    expect(onForbidden).toHaveBeenCalled()
  })
})
