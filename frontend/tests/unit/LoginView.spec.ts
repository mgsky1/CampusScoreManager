import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { nextTick } from 'vue'
import ElementPlus from 'element-plus'
import LoginView from '@/views/auth/LoginView.vue'
import * as authApi from '@/api/auth'
import { ApiError } from '@/utils/http'
import { useAuthStore } from '@/stores/auth'

function makeRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/login', component: LoginView },
      { path: '/student/dashboard', name: 'sd', component: { template: '<div>student</div>' } },
      { path: '/teacher/dashboard', name: 'td', component: { template: '<div>teacher</div>' } },
    ],
  })
}

async function mountLogin() {
  const router = makeRouter()
  await router.push('/login')
  await router.isReady()
  const wrapper = mount(LoginView, {
    global: { plugins: [router, ElementPlus] },
    attachTo: document.body,
  })
  return { wrapper, router }
}

describe('LoginView.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.sessionStorage.clear()
    window.localStorage.clear()
  })
  afterEach(() => {
    vi.restoreAllMocks()
    document.body.innerHTML = ''
  })

  it('blocks submit when loginName is empty (client validation)', async () => {
    const loginSpy = vi.spyOn(authApi, 'login')
    const { wrapper } = await mountLogin()
    await wrapper.find('button').trigger('click')
    await flushPromises()
    expect(loginSpy).not.toHaveBeenCalled()
  })

  it('renders 中文 error when server returns code=2001', async () => {
    vi.spyOn(authApi, 'login').mockRejectedValue(
      new ApiError(2001, '登录名或密码错误', 400),
    )
    const { wrapper } = await mountLogin()
    await wrapper.find('input[autocomplete="username"]').setValue('ttt')
    await wrapper.find('input[type="password"]').setValue('bad')
    await wrapper.find('button').trigger('click')
    await flushPromises()
    await nextTick()
    expect(wrapper.text()).toContain('登录名或密码错误')
  })

  it('navigates to /student/dashboard on successful STUDENT login', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue({
      accessToken: 'a',
      refreshToken: 'r',
      user: { id: 3, loginName: 'hhh', realName: '黄同学', role: 'STUDENT' },
    })
    const { wrapper, router } = await mountLogin()
    await wrapper.find('input[autocomplete="username"]').setValue('hhh')
    await wrapper.find('input[type="password"]').setValue('123456')
    await wrapper.find('button').trigger('click')
    await flushPromises()
    await nextTick()

    const auth = useAuthStore()
    expect(auth.user?.loginName).toBe('hhh')
    expect(router.currentRoute.value.path).toBe('/student/dashboard')
  })

  it('navigates to /teacher/dashboard on successful TEACHER login', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue({
      accessToken: 'a',
      refreshToken: 'r',
      user: { id: 1, loginName: 'ttt', realName: '田老师', role: 'TEACHER' },
    })
    const { wrapper, router } = await mountLogin()
    await wrapper.find('input[autocomplete="username"]').setValue('ttt')
    await wrapper.find('input[type="password"]').setValue('123456')
    await wrapper.find('button').trigger('click')
    await flushPromises()
    await nextTick()
    expect(router.currentRoute.value.path).toBe('/teacher/dashboard')
  })
})
