import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { setHttpHandlers } from '@/utils/http'
import { ElMessage } from 'element-plus'

/**
 * 注册全局路由守卫 + 桥接 http.ts 的 401/403/5xx side-effect。
 * 调用方（main.ts）在 pinia + router 就绪后执行一次。
 */
export function registerRouterGuards(router: Router): void {
  // ---------- 路由守卫 ----------
  router.beforeEach((to) => {
    const auth = useAuthStore()

    // 尚未 hydrate 时先 hydrate（首刷/深链场景）
    if (!auth.isAuthenticated) {
      auth.hydrateFromStorage()
    }

    // 公开页放行
    if (to.meta.public) {
      return true
    }

    // 未登录：跳 /login?redirect=to.fullPath
    if (!auth.isAuthenticated) {
      return {
        path: '/login',
        query: { redirect: to.fullPath },
      }
    }

    // 已登录但角色不匹配：403
    if (to.meta.role && auth.role !== to.meta.role) {
      return { path: '/forbidden' }
    }

    return true
  })

  // ---------- http.ts side-effect ----------
  setHttpHandlers({
    onUnauthorized: (redirect) => {
      const auth = useAuthStore()
      auth.logout()
      router.replace({ path: '/login', query: redirect ? { redirect } : undefined })
    },
    onForbidden: () => {
      router.replace({ path: '/forbidden' })
    },
    onServerError: (message) => {
      ElMessage.error(message)
    },
  })
}
