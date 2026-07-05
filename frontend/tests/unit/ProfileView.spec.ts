import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ProfileForm from '@/components/ProfileForm.vue'
import StudentProfileView from '@/views/student/ProfileView.vue'
import TeacherProfileView from '@/views/teacher/ProfileView.vue'
import * as api from '@/api/account'
import { ApiError } from '@/utils/http'
import { ApiErrorCode } from '@/types/api'

// ---------- Element Plus stubs ----------
const stubs = {
  'el-input': {
    template:
      '<input :value="modelValue" :placeholder="placeholder" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)" class="el-input" />',
    props: ['modelValue', 'placeholder', 'disabled', 'clearable'],
    emits: ['update:modelValue'],
  },
  'el-button': {
    template:
      '<button class="el-button" :data-type="type" @click="$emit(\'click\', $event)"><slot /></button>',
    props: ['type', 'loading', 'nativeType', 'link'],
    emits: ['click'],
  },
  'el-form': {
    template: '<form class="el-form" @submit.prevent="$emit(\'submit\')"><slot /></form>',
    props: ['model', 'labelWidth', 'inline'],
    emits: ['submit'],
  },
  'el-form-item': {
    template:
      '<div class="el-form-item" :data-label="label" :data-error="error"><slot />' +
      '<span v-if="error" class="el-form-item__error">{{ error }}</span></div>',
    props: ['label', 'prop', 'error'],
  },
}

// ---------- ElMessage mocks (hoisted) ----------
const { messageMocks } = vi.hoisted(() => ({
  messageMocks: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
}))
vi.mock('element-plus', async () => {
  const actual = await vi.importActual<Record<string, unknown>>('element-plus')
  return { ...actual, ElMessage: messageMocks }
})

function mockGetTeacher() {
  return vi.spyOn(api, 'getProfile').mockResolvedValue({
    id: 1,
    loginName: 'ttt',
    realName: '田老师',
    role: 'TEACHER',
    tel: '18065853353',
  })
}

function mockGetStudent() {
  return vi.spyOn(api, 'getProfile').mockResolvedValue({
    id: 3,
    loginName: 'hhh',
    realName: '黄同学',
    role: 'STUDENT',
    tel: '18075853353',
    address: 'SMU',
    grade: 3,
  })
}

async function mountForm(role: 'STUDENT' | 'TEACHER') {
  const w = mount(ProfileForm, { props: { role }, global: { stubs } })
  await flushPromises()
  await flushPromises()
  return w
}

describe('ProfileForm / ProfileView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    messageMocks.success.mockReset()
    messageMocks.error.mockReset()
    messageMocks.warning.mockReset()
  })

  it('student ProfileView renders ProfileForm with STUDENT role', async () => {
    mockGetStudent()
    const w = mount(StudentProfileView, { global: { stubs } })
    await flushPromises()
    expect(w.findComponent(ProfileForm).props('role')).toBe('STUDENT')
  })

  it('teacher ProfileView renders ProfileForm with TEACHER role', async () => {
    mockGetTeacher()
    const w = mount(TeacherProfileView, { global: { stubs } })
    await flushPromises()
    expect(w.findComponent(ProfileForm).props('role')).toBe('TEACHER')
  })

  it('student view shows both tel and address inputs, prefilled', async () => {
    mockGetStudent()
    const w = await mountForm('STUDENT')
    const labels = w.findAll('.el-form-item').map((n) => n.attributes('data-label'))
    expect(labels).toContain('电话')
    expect(labels).toContain('地址')
    const tel = w.find('input[placeholder="8-11 位数字"]')
    const addr = w.find('input[placeholder="最长 50 字"]')
    expect((tel.element as HTMLInputElement).value).toBe('18075853353')
    expect((addr.element as HTMLInputElement).value).toBe('SMU')
  })

  it('teacher view shows only tel input (no address)', async () => {
    mockGetTeacher()
    const w = await mountForm('TEACHER')
    const labels = w.findAll('.el-form-item').map((n) => n.attributes('data-label'))
    expect(labels).toContain('电话')
    expect(labels).not.toContain('地址')
  })

  it('blocks submission when tel is empty (client-side)', async () => {
    mockGetTeacher()
    const updateSpy = vi.spyOn(api, 'updateProfile')
    const w = await mountForm('TEACHER')
    const tel = w.find('input[placeholder="8-11 位数字"]')
    await tel.setValue('')
    await w.find('button.el-button[data-type="primary"]').trigger('click')
    await flushPromises()
    expect(updateSpy).not.toHaveBeenCalled()
    const item = w.findAll('.el-form-item').find((n) => n.attributes('data-label') === '电话')
    expect(item?.attributes('data-error')).toBe('tel 不能为空')
  })

  it('blocks submission when tel is malformed', async () => {
    mockGetTeacher()
    const updateSpy = vi.spyOn(api, 'updateProfile')
    const w = await mountForm('TEACHER')
    const tel = w.find('input[placeholder="8-11 位数字"]')
    await tel.setValue('abc')
    await w.find('button.el-button[data-type="primary"]').trigger('click')
    await flushPromises()
    expect(updateSpy).not.toHaveBeenCalled()
    const item = w.findAll('.el-form-item').find((n) => n.attributes('data-label') === '电话')
    expect(item?.attributes('data-error')).toBe('tel 必须为 8-11 位数字')
  })

  it('teacher submit only sends {tel}', async () => {
    mockGetTeacher()
    const updateSpy = vi.spyOn(api, 'updateProfile').mockResolvedValue({
      id: 1,
      loginName: 'ttt',
      realName: '田老师',
      role: 'TEACHER',
      tel: '13900139000',
    })
    const w = await mountForm('TEACHER')
    const tel = w.find('input[placeholder="8-11 位数字"]')
    await tel.setValue('13900139000')
    await w.find('button.el-button[data-type="primary"]').trigger('click')
    await flushPromises()
    expect(updateSpy).toHaveBeenCalledWith({ tel: '13900139000' })
    expect(messageMocks.success).toHaveBeenCalledWith('保存成功')
  })

  it('student submit sends {tel, address}', async () => {
    mockGetStudent()
    const updateSpy = vi.spyOn(api, 'updateProfile').mockResolvedValue({
      id: 3,
      loginName: 'hhh',
      realName: '黄同学',
      role: 'STUDENT',
      tel: '13800138888',
      address: '深圳大学',
      grade: 3,
    })
    const w = await mountForm('STUDENT')
    const tel = w.find('input[placeholder="8-11 位数字"]')
    const addr = w.find('input[placeholder="最长 50 字"]')
    await tel.setValue('13800138888')
    await addr.setValue('深圳大学')
    await w.find('button.el-button[data-type="primary"]').trigger('click')
    await flushPromises()
    expect(updateSpy).toHaveBeenCalledWith({ tel: '13800138888', address: '深圳大学' })
    expect(messageMocks.success).toHaveBeenCalledWith('保存成功')
  })

  it('server 1000 with field=tel maps to tel error slot', async () => {
    mockGetTeacher()
    vi.spyOn(api, 'updateProfile').mockRejectedValue(
      new ApiError(ApiErrorCode.VALIDATION_FAILED, 'invalid', 400, [
        { field: 'tel', code: 'VALIDATION_PATTERN', message: 'tel 必须为 8-11 位数字' },
      ]),
    )
    const w = await mountForm('TEACHER')
    const tel = w.find('input[placeholder="8-11 位数字"]')
    await tel.setValue('12345678')
    await w.find('button.el-button[data-type="primary"]').trigger('click')
    await flushPromises()
    const item = w.findAll('.el-form-item').find((n) => n.attributes('data-label') === '电话')
    expect(item?.attributes('data-error')).toBe('tel 必须为 8-11 位数字')
  })

  it('student empty address blocked client-side', async () => {
    mockGetStudent()
    const updateSpy = vi.spyOn(api, 'updateProfile')
    const w = await mountForm('STUDENT')
    const addr = w.find('input[placeholder="最长 50 字"]')
    await addr.setValue('   ')
    await w.find('button.el-button[data-type="primary"]').trigger('click')
    await flushPromises()
    expect(updateSpy).not.toHaveBeenCalled()
    const item = w
      .findAll('.el-form-item')
      .find((n) => n.attributes('data-label') === '地址')
    expect(item?.attributes('data-error')).toBe('address 不能为空')
  })
})
