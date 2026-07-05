import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiErrorCode } from '@/types/api'

async function load() {
  vi.resetModules()
  const api = await import('@/api/teacherScores')
  const httpMod = await import('@/utils/http')
  return { ...api, ...httpMod }
}

function okAdapter(data: unknown) {
  return vi.fn().mockResolvedValue({
    status: 200,
    data: { code: 0, message: 'ok', data },
    headers: {},
    config: {},
    statusText: 'OK',
  })
}

function errorAdapter(status: number, code: number, message = 'err') {
  return vi.fn().mockRejectedValue({
    response: { status, data: { code, message } },
    isAxiosError: true,
  })
}

describe('api/teacherScores', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    window.localStorage.clear()
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('listStudentScores unwraps PageResult on code=0', async () => {
    const { listStudentScores, http } = await load()
    http.defaults.adapter = okAdapter({
      items: [
        {
          id: 11,
          subjectId: 5,
          subjectName: 'Java EE',
          grade: 3,
          score: 99,
          isFailing: false,
          editable: true,
          updatedAt: '2026-07-04T12:00:00',
        },
      ],
      total: 1,
      page: 1,
      size: 20,
      hasNext: false,
    }) as never

    const r = await listStudentScores(3, { page: 1, size: 20 })
    expect(r.items).toHaveLength(1)
    expect(r.items[0].editable).toBe(true)
  })

  it('listStudentScores maps 404 → ApiError code=1404', async () => {
    const { listStudentScores, http } = await load()
    http.defaults.adapter = errorAdapter(404, ApiErrorCode.NOT_FOUND, '学生不存在') as never
    await expect(listStudentScores(99)).rejects.toMatchObject({
      code: ApiErrorCode.NOT_FOUND,
    })
  })

  it('getEntryOptions returns options + allEntered', async () => {
    const { getEntryOptions, http } = await load()
    http.defaults.adapter = okAdapter({
      student: { id: 3, realName: '黄同学', grade: 3 },
      options: [{ subjectId: 5, subjectName: 'Java EE', grade: 3 }],
      allEntered: false,
    }) as never

    const r = await getEntryOptions(3)
    expect(r.options).toHaveLength(1)
    expect(r.allEntered).toBe(false)
  })

  it('createScore returns TeacherScoreItem on 201', async () => {
    const { createScore, http } = await load()
    http.defaults.adapter = vi.fn().mockResolvedValue({
      status: 201,
      data: {
        code: 0,
        message: 'ok',
        data: {
          id: 101,
          subjectId: 5,
          subjectName: 'Java EE',
          grade: 3,
          score: 88,
          isFailing: false,
          editable: true,
        },
      },
      headers: {},
      config: {},
      statusText: 'Created',
    }) as never
    const r = await createScore(3, { subjectId: 5, score: 88 })
    expect(r.id).toBe(101)
  })

  it('createScore maps 1000 field validation', async () => {
    const { createScore, http } = await load()
    http.defaults.adapter = vi.fn().mockRejectedValue({
      response: {
        status: 400,
        data: {
          code: ApiErrorCode.VALIDATION_FAILED,
          message: 'invalid',
          errors: [{ field: 'score', code: 'VALIDATION_RANGE', message: '成绩必须在 0-100 之间' }],
        },
      },
      isAxiosError: true,
    }) as never
    await expect(createScore(3, { subjectId: 5, score: 101 })).rejects.toMatchObject({
      code: ApiErrorCode.VALIDATION_FAILED,
    })
  })

  it('createScore maps 2301 SCORE_OUT_OF_RANGE', async () => {
    const { createScore, http } = await load()
    http.defaults.adapter = errorAdapter(400, ApiErrorCode.SCORE_OUT_OF_RANGE) as never
    await expect(createScore(3, { subjectId: 5, score: -1 })).rejects.toMatchObject({
      code: ApiErrorCode.SCORE_OUT_OF_RANGE,
    })
  })

  it('createScore maps 2302 SCORE_ALREADY_EXISTS', async () => {
    const { createScore, http } = await load()
    http.defaults.adapter = errorAdapter(409, ApiErrorCode.SCORE_ALREADY_EXISTS) as never
    await expect(createScore(3, { subjectId: 5, score: 88 })).rejects.toMatchObject({
      code: ApiErrorCode.SCORE_ALREADY_EXISTS,
    })
  })

  it('createScore maps 2303 SCORE_ALL_ENROLLED', async () => {
    const { createScore, http } = await load()
    http.defaults.adapter = errorAdapter(400, ApiErrorCode.SCORE_ALL_ENROLLED) as never
    await expect(createScore(3, { subjectId: 5, score: 88 })).rejects.toMatchObject({
      code: ApiErrorCode.SCORE_ALL_ENROLLED,
    })
  })

  it('createScore maps 2304 SCORE_SUBJECT_NOT_OWNED via onForbidden', async () => {
    const { createScore, http, setHttpHandlers } = await load()
    const onForbidden = vi.fn()
    setHttpHandlers({ onUnauthorized: vi.fn(), onForbidden, onServerError: vi.fn() })
    http.defaults.adapter = errorAdapter(403, ApiErrorCode.SCORE_SUBJECT_NOT_OWNED) as never
    await expect(createScore(3, { subjectId: 5, score: 88 })).rejects.toMatchObject({
      code: ApiErrorCode.FORBIDDEN,
    })
    expect(onForbidden).toHaveBeenCalled()
  })

  it('updateScore returns updated item', async () => {
    const { updateScore, http } = await load()
    http.defaults.adapter = okAdapter({
      id: 101,
      subjectId: 5,
      subjectName: 'Java EE',
      grade: 3,
      score: 85,
      isFailing: false,
      editable: true,
    }) as never
    const r = await updateScore(101, { score: 85 })
    expect(r.score).toBe(85)
  })

  it('deleteScore returns void on success', async () => {
    const { deleteScore, http } = await load()
    http.defaults.adapter = okAdapter(null) as never
    await expect(deleteScore(101)).resolves.toBeNull()
  })
})
