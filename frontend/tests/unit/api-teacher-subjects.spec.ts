import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiErrorCode } from '@/types/api'

async function load() {
  vi.resetModules()
  const api = await import('@/api/teacherSubjects')
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

function errorAdapter(status: number, code: number, message = 'err') {
  return vi.fn().mockRejectedValue({
    response: { status, data: { code, message } },
    isAxiosError: true,
  })
}

describe('api/teacherSubjects', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    window.localStorage.clear()
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('listMySubjects unwraps PageResult on code=0', async () => {
    const { listMySubjects, http } = await load()
    http.defaults.adapter = okAdapter({
      items: [
        { id: 1, name: '计算机导论', grade: 1 },
        { id: 2, name: '汇编语言', grade: 2 },
      ],
      total: 2,
      page: 1,
      size: 20,
      hasNext: false,
    }) as never

    const r = await listMySubjects({ page: 1, size: 20 })
    expect(r.items).toHaveLength(2)
    expect(r.items[0].name).toBe('计算机导论')
  })

  it('createSubject returns SubjectMineItem on 201', async () => {
    const { createSubject, http } = await load()
    http.defaults.adapter = okAdapter(
      { id: 99, name: '云计算', grade: 3 },
      201,
      'Created',
    ) as never
    const r = await createSubject({ name: '云计算', grade: 3 })
    expect(r.id).toBe(99)
  })

  it('createSubject maps 2201 SUBJECT_DUPLICATE_FOR_TEACHER', async () => {
    const { createSubject, http } = await load()
    http.defaults.adapter = errorAdapter(
      409,
      ApiErrorCode.SUBJECT_DUPLICATE_FOR_TEACHER,
      '该教师已存在同名课程',
    ) as never
    await expect(createSubject({ name: 'Java EE', grade: 3 })).rejects.toMatchObject({
      code: ApiErrorCode.SUBJECT_DUPLICATE_FOR_TEACHER,
    })
  })

  it('createSubject maps 1000 field validation', async () => {
    const { createSubject, http } = await load()
    http.defaults.adapter = errorAdapter(400, ApiErrorCode.VALIDATION_FAILED) as never
    await expect(createSubject({ name: '', grade: 3 })).rejects.toMatchObject({
      code: ApiErrorCode.VALIDATION_FAILED,
    })
  })

  it('updateSubject returns updated item', async () => {
    const { updateSubject, http } = await load()
    http.defaults.adapter = okAdapter({ id: 2, name: '汇编语言进阶', grade: 3 }) as never
    const r = await updateSubject(2, { name: '汇编语言进阶', grade: 3 })
    expect(r.name).toBe('汇编语言进阶')
    expect(r.grade).toBe(3)
  })

  it('updateSubject maps 1404 NOT_FOUND', async () => {
    const { updateSubject, http } = await load()
    http.defaults.adapter = errorAdapter(404, ApiErrorCode.NOT_FOUND) as never
    await expect(updateSubject(999, { name: 'x', grade: 1 })).rejects.toMatchObject({
      code: ApiErrorCode.NOT_FOUND,
    })
  })

  it('updateSubject maps 1403 → FORBIDDEN via onForbidden handler', async () => {
    const { updateSubject, http, setHttpHandlers } = await load()
    const onForbidden = vi.fn()
    setHttpHandlers({ onUnauthorized: vi.fn(), onForbidden, onServerError: vi.fn() })
    http.defaults.adapter = errorAdapter(403, ApiErrorCode.FORBIDDEN) as never
    await expect(updateSubject(5, { name: 'x', grade: 3 })).rejects.toMatchObject({
      code: ApiErrorCode.FORBIDDEN,
    })
    expect(onForbidden).toHaveBeenCalled()
  })

  it('updateSubject maps 2201 SUBJECT_DUPLICATE_FOR_TEACHER', async () => {
    const { updateSubject, http } = await load()
    http.defaults.adapter = errorAdapter(409, ApiErrorCode.SUBJECT_DUPLICATE_FOR_TEACHER) as never
    await expect(updateSubject(2, { name: '计算机导论', grade: 1 })).rejects.toMatchObject({
      code: ApiErrorCode.SUBJECT_DUPLICATE_FOR_TEACHER,
    })
  })

  it('deleteSubject returns void on success', async () => {
    const { deleteSubject, http } = await load()
    http.defaults.adapter = okAdapter(null) as never
    await expect(deleteSubject(2)).resolves.toBeNull()
  })

  it('deleteSubject maps 2202 SUBJECT_HAS_SCORES', async () => {
    const { deleteSubject, http } = await load()
    http.defaults.adapter = errorAdapter(400, ApiErrorCode.SUBJECT_HAS_SCORES) as never
    await expect(deleteSubject(5)).rejects.toMatchObject({
      code: ApiErrorCode.SUBJECT_HAS_SCORES,
    })
  })

  it('deleteSubject maps 1404 NOT_FOUND', async () => {
    const { deleteSubject, http } = await load()
    http.defaults.adapter = errorAdapter(404, ApiErrorCode.NOT_FOUND) as never
    await expect(deleteSubject(999)).rejects.toMatchObject({
      code: ApiErrorCode.NOT_FOUND,
    })
  })
})
