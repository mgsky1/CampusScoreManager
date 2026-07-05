import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import PasswordForm from '@/components/PasswordForm.vue'
import StudentPasswordView from '@/views/student/PasswordView.vue'
import TeacherPasswordView from '@/views/teacher/PasswordView.vue'
import * as api from '@/api/account'
import { ApiError } from '@/utils/http'
import { ApiErrorCode } from '@/types/api'
import { useAuthStore } from '@/stores/auth'

// ---------- stubs ----------
const stubs = {
  'el-input': {
    template:
      '<input :value="modelValue" :placeholder="placeholder" :type="type" @input="$emit(\'update:modelValue\', $event.target.value)" class="el-input" />',
    props: ['modelValue', 'placeholder', 'type', 'showPassword'],
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

const { messageMocks } = vi.hoisted(() => ({
  messageMocks: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
}))
vi.mock('element-plus', async () => {
  const actual = await vi.importActual<Record<string, unknown>>('element-plus')
  return { ...actual, ElMessage: messageMocks }
})

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', name: 'login', component: { template: '<div />' } },
      { path: '/x/password', name: 'x.password', component: PasswordForm },
    ],
  })
}

async function mountForm() {
  const router = makeRouter()
  await router.push('/x/password')
  await router.isReady()
  const w = mount(PasswordForm, { global: { stubs, plugins: [router] } })
  await flushPromises()
  return { w, router }
}

function findItem(w: ReturnType<typeof mount>, label: string) {
  return w.findAll('.el-form-item').find((n) => n.attributes('data-label') === label)
}

function inputByPlaceholder(w: ReturnType<typeof mount>, placeholder: string) {
  return w.find(`input[placeholder="${placeholder}"]`)
}

describe('PasswordForm / PasswordView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    messageMocks.success.mockReset()
    messageMocks.error.mockReset()
    messageMocks.warning.mockReset()
  })

  it('student PasswordView mounts PasswordForm', async () => {
    const router = makeRouter()
    await router.push('/x/password')
    await router.isReady()
    const w = mount(StudentPasswordView, { global: { stubs, plugins: [router] } })
    await flushPromises()
    expect(w.findComponent(PasswordForm).exists()).toBe(true)
  })

  it('teacher PasswordView mounts PasswordForm', async () => {
    const router = makeRouter()
    await router.push('/x/password')
    await router.isReady()
    const w = mount(TeacherPasswordView, { global: { stubs, plugins: [router] } })
    await flushPromises()
    expect(w.findComponent(PasswordForm).exists()).toBe(true)
  })

  it('blocks submission when fields are empty', async () => {
    const changeSpy = vi.spyOn(api, 'changePassword')
    const { w } = await mountForm()
    await w.find('button.el-button[data-type="primary"]').trigger('click')
    await flushPromises()
    expect(changeSpy).not.toHaveBeenCalled()
    expect(findItem(w, '原密码')?.attributes('data-error')).toBe('原密码不能为空')
    expect(findItem(w, '新密码')?.attributes('data-error')).toBe('新密码不能为空')
    expect(findItem(w, '确认密码')?.attributes('data-error')).toBe('确认密码不能为空')
  })

  it('blocks submission when new != confirm client-side', async () => {
    const changeSpy = vi.spyOn(api, 'changePassword')
    const { w } = await mountForm()
    await inputByPlaceholder(w, '请输入原密码').setValue('123456')
    await inputByPlaceholder(w, '6-32 位').setValue('newpass1')
    await inputByPlaceholder(w, '再次输入新密码').setValue('newpass2')
    await w.find('button.el-button[data-type="primary"]').trigger('click')
    await flushPromises()
    expect(changeSpy).not.toHaveBeenCalled()
    expect(findItem(w, '确认密码')?.attributes('data-error')).toBe('确认密码与新密码不一致')
  })

  it('maps 2002 to oldPassword error slot', async () => {
    vi.spyOn(api, 'changePassword').mockRejectedValue(
      new ApiError(ApiErrorCode.OLD_PASSWORD_MISMATCH, '原密码错误', 400),
    )
    const { w } = await mountForm()
    await inputByPlaceholder(w, '请输入原密码').setValue('wrongpwd')
    await inputByPlaceholder(w, '6-32 位').setValue('newpass1')
    await inputByPlaceholder(w, '再次输入新密码').setValue('newpass1')
    await w.find('button.el-button[data-type="primary"]').trigger('click')
    await flushPromises()
    expect(findItem(w, '原密码')?.attributes('data-error')).toBe('原密码错误')
  })

  it('maps 2003 to confirmPassword error slot', async () => {
    vi.spyOn(api, 'changePassword').mockRejectedValue(
      new ApiError(ApiErrorCode.CONFIRM_PASSWORD_MISMATCH, 'mismatch', 400),
    )
    const { w } = await mountForm()
    // Bypass client-side mismatch check by making them equal client-side; server still rejects
    await inputByPlaceholder(w, '请输入原密码').setValue('123456')
    await inputByPlaceholder(w, '6-32 位').setValue('newpass1')
    await inputByPlaceholder(w, '再次输入新密码').setValue('newpass1')
    await w.find('button.el-button[data-type="primary"]').trigger('click')
    await flushPromises()
    expect(findItem(w, '确认密码')?.attributes('data-error')).toBe('确认密码与新密码不一致')
  })

  it('maps 1000 confirmMatchesNew to confirmPassword slot', async () => {
    vi.spyOn(api, 'changePassword').mockRejectedValue(
      new ApiError(ApiErrorCode.VALIDATION_FAILED, 'invalid', 400, [
        { field: 'confirmMatchesNew', code: 'VALIDATION_MISMATCH' },
      ]),
    )
    const { w } = await mountForm()
    await inputByPlaceholder(w, '请输入原密码').setValue('123456')
    await inputByPlaceholder(w, '6-32 位').setValue('newpass1')
    await inputByPlaceholder(w, '再次输入新密码').setValue('newpass1')
    await w.find('button.el-button[data-type="primary"]').trigger('click')
    await flushPromises()
    expect(findItem(w, '确认密码')?.attributes('data-error')).toBe('确认密码与新密码不一致')
  })

  it('on success calls logout + redirects to /login?msg=password-changed', async () => {
    vi.spyOn(api, 'changePassword').mockResolvedValue(undefined as unknown as void)
    const { w, router } = await mountForm()
    const auth = useAuthStore()
    // Prepopulate store to observe logout side-effect
    auth.login(
      {
        accessToken: 't1',
        refreshToken: 't2',
        user: { id: 1, loginName: 'ttt', realName: '田', role: 'TEACHER' },
      },
      false,
    )
    expect(auth.isAuthenticated).toBe(true)

    await inputByPlaceholder(w, '请输入原密码').setValue('123456')
    await inputByPlaceholder(w, '6-32 位').setValue('newpass1')
    await inputByPlaceholder(w, '再次输入新密码').setValue('newpass1')
    await w.find('button.el-button[data-type="primary"]').trigger('click')
    await flushPromises()
    await flushPromises()

    expect(messageMocks.success).toHaveBeenCalledWith('密码修改成功，请重新登录')
    expect(auth.isAuthenticated).toBe(false)
    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.query.msg).toBe('password-changed')
  })
})
