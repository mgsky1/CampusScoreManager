import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiErrorCode } from '@/types/api'

async function load() {
  vi.resetModules()
  const api = await import('@/api/teacherStudents')
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

describe('api/teacherStudents CRUD', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    window.localStorage.clear()
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  const validCreate = {
    loginName: 'new_stu',
    realName: '新同学',
    password: '123456',
    tel: '13800138000',
    address: 'SMU',
    grade: 3,
  }

  const validUpdate = {
    loginName: 'updated_stu',
    realName: '改名后',
    tel: '13900139000',
    address: 'SMU B',
    grade: 4,
  }

  it('createStudent returns StudentDetail on 201', async () => {
    const { createStudent, http } = await load()
    http.defaults.adapter = okAdapter(
      {
        id: 10,
        loginName: 'new_stu',
        realName: '新同学',
        tel: '13800138000',
        address: 'SMU',
        grade: 3,
      },
      201,
      'Created',
    ) as never

    const r = await createStudent(validCreate)
    expect(r.id).toBe(10)
    expect(r.loginName).toBe('new_stu')
  })

  it('createStudent maps 1000 validation error with field errors', async () => {
    const { createStudent, http } = await load()
    http.defaults.adapter = errorAdapter(400, ApiErrorCode.VALIDATION_FAILED, 'invalid', [
      { field: 'tel', code: 'VALIDATION_PATTERN', message: 'tel 必须为 8-11 位数字' },
    ]) as never

    await expect(
      createStudent({ ...validCreate, tel: '1234567' }),
    ).rejects.toMatchObject({ code: ApiErrorCode.VALIDATION_FAILED })
  })

  it('createStudent maps 2101 LOGIN_NAME_TAKEN', async () => {
    const { createStudent, http } = await load()
    http.defaults.adapter = errorAdapter(409, ApiErrorCode.LOGIN_NAME_TAKEN, '登录名已被使用') as never

    await expect(createStudent(validCreate)).rejects.toMatchObject({
      code: ApiErrorCode.LOGIN_NAME_TAKEN,
    })
  })

  it('updateStudent returns updated detail', async () => {
    const { updateStudent, http } = await load()
    http.defaults.adapter = okAdapter({
      id: 3,
      loginName: 'updated_stu',
      realName: '改名后',
      tel: '13900139000',
      address: 'SMU B',
      grade: 4,
    }) as never

    const r = await updateStudent(3, validUpdate)
    expect(r.grade).toBe(4)
    expect(r.realName).toBe('改名后')
  })

  it('updateStudent maps 1404 NOT_FOUND', async () => {
    const { updateStudent, http } = await load()
    http.defaults.adapter = errorAdapter(404, ApiErrorCode.NOT_FOUND, '学生不存在') as never
    await expect(updateStudent(99, validUpdate)).rejects.toMatchObject({
      code: ApiErrorCode.NOT_FOUND,
    })
  })

  it('updateStudent maps 2101 LOGIN_NAME_TAKEN', async () => {
    const { updateStudent, http } = await load()
    http.defaults.adapter = errorAdapter(409, ApiErrorCode.LOGIN_NAME_TAKEN) as never
    await expect(updateStudent(3, validUpdate)).rejects.toMatchObject({
      code: ApiErrorCode.LOGIN_NAME_TAKEN,
    })
  })

  it('updateStudent allows optional password field', async () => {
    const { updateStudent, http } = await load()
    const adapter = okAdapter({
      id: 3,
      loginName: 'updated_stu',
      realName: '改名后',
      tel: '13900139000',
      address: 'SMU B',
      grade: 4,
    })
    http.defaults.adapter = adapter as never
    await updateStudent(3, { ...validUpdate, password: 'newpass1' })
    // 验证请求体中包含 password
    const call = adapter.mock.calls[0][0] as { data?: string }
    expect(call.data).toBeTruthy()
    expect(JSON.parse(call.data as string).password).toBe('newpass1')
  })

  it('deleteStudent returns void on success', async () => {
    const { deleteStudent, http } = await load()
    http.defaults.adapter = okAdapter(null) as never
    await expect(deleteStudent(3)).resolves.toBeNull()
  })

  it('deleteStudent maps 1404 NOT_FOUND', async () => {
    const { deleteStudent, http } = await load()
    http.defaults.adapter = errorAdapter(404, ApiErrorCode.NOT_FOUND) as never
    await expect(deleteStudent(99)).rejects.toMatchObject({
      code: ApiErrorCode.NOT_FOUND,
    })
  })
})
