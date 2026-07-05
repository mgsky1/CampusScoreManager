import { describe, expect, it, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { registerRouterGuards } from '@/router/guards'
import type { UserBrief } from '@/types/api'

function buildRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', redirect: '/student/dashboard' },
      { path: '/login', name: 'login', component: { template: '<div/>' }, meta: { public: true } },
      {
        path: '/forbidden',
        name: 'forbidden',
        component: { template: '<div/>' },
        meta: { public: true },
      },
      {
        path: '/student/dashboard',
        name: 'student.dashboard',
        component: { template: '<div/>' },
        meta: { role: 'STUDENT' },
      },
      {
        path: '/teacher/dashboard',
        name: 'teacher.dashboard',
        component: { template: '<div/>' },
        meta: { role: 'TEACHER' },
      },
    ],
  })
  registerRouterGuards(router)
  return router
}

describe('router guards', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    window.sessionStorage.clear()
    window.localStorage.clear()
  })

  it('redirects unauthenticated user to /login with redirect query', async () => {
    const router = buildRouter()
    await router.push('/student/dashboard')
    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.query.redirect).toBe('/student/dashboard')
  })

  it('sends wrong-role user to /forbidden', async () => {
    const router = buildRouter()
    const auth = useAuthStore()
    const teacher: UserBrief = { id: 1, loginName: 't', realName: 'T', role: 'TEACHER' }
    auth.login(
      { accessToken: 'a', refreshToken: 'r', user: teacher },
      false,
    )

    await router.push('/student/dashboard')
    expect(router.currentRoute.value.path).toBe('/forbidden')
  })

  it('lets same-role user through', async () => {
    const router = buildRouter()
    const auth = useAuthStore()
    const student: UserBrief = { id: 3, loginName: 's', realName: 'S', role: 'STUDENT' }
    auth.login(
      { accessToken: 'a', refreshToken: 'r', user: student },
      false,
    )
    await router.push('/student/dashboard')
    expect(router.currentRoute.value.path).toBe('/student/dashboard')
  })

  it('public route bypasses auth check', async () => {
    const router = buildRouter()
    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/login')
    await router.push('/forbidden')
    expect(router.currentRoute.value.path).toBe('/forbidden')
  })
})
