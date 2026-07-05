import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { UserBrief, UserRole } from '@/types/api'

const ACCESS_KEY = 'csm.auth.access'
const REFRESH_KEY = 'csm.auth.refresh'
const USER_KEY = 'csm.auth.user'
const REMEMBER_KEY = 'csm.auth.remember'

interface Persist {
  accessToken: string
  refreshToken: string
  user: UserBrief
}

/** 读写 sessionStorage / localStorage 的封装（用户勾选"记住我"时切换）。 */
function pickStorage(remember: boolean): Storage {
  return remember ? window.localStorage : window.sessionStorage
}

function readPersisted(): { data: Persist; remember: boolean } | null {
  const remember = window.localStorage.getItem(REMEMBER_KEY) === '1'
  const s = pickStorage(remember)
  const access = s.getItem(ACCESS_KEY)
  const refresh = s.getItem(REFRESH_KEY)
  const userRaw = s.getItem(USER_KEY)
  if (!access || !refresh || !userRaw) return null
  try {
    const user = JSON.parse(userRaw) as UserBrief
    return { data: { accessToken: access, refreshToken: refresh, user }, remember }
  } catch {
    return null
  }
}

function writePersisted(data: Persist, remember: boolean): void {
  const s = pickStorage(remember)
  s.setItem(ACCESS_KEY, data.accessToken)
  s.setItem(REFRESH_KEY, data.refreshToken)
  s.setItem(USER_KEY, JSON.stringify(data.user))
  window.localStorage.setItem(REMEMBER_KEY, remember ? '1' : '0')
}

function clearPersisted(): void {
  for (const s of [window.localStorage, window.sessionStorage]) {
    s.removeItem(ACCESS_KEY)
    s.removeItem(REFRESH_KEY)
    s.removeItem(USER_KEY)
  }
  window.localStorage.removeItem(REMEMBER_KEY)
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const user = ref<UserBrief | null>(null)
  const remember = ref<boolean>(false)

  const isAuthenticated = computed(() => !!accessToken.value && !!user.value)
  const role = computed<UserRole | null>(() => user.value?.role ?? null)

  function hydrateFromStorage(): void {
    const p = readPersisted()
    if (!p) return
    accessToken.value = p.data.accessToken
    refreshToken.value = p.data.refreshToken
    user.value = p.data.user
    remember.value = p.remember
  }

  function login(payload: Persist, rememberMe: boolean): void {
    accessToken.value = payload.accessToken
    refreshToken.value = payload.refreshToken
    user.value = payload.user
    remember.value = rememberMe
    writePersisted(payload, rememberMe)
  }

  /** 只更新 tokens（refresh 成功后调用）。 */
  function updateTokens(next: { accessToken: string; refreshToken: string }): void {
    accessToken.value = next.accessToken
    refreshToken.value = next.refreshToken
    if (user.value) {
      writePersisted(
        {
          accessToken: next.accessToken,
          refreshToken: next.refreshToken,
          user: user.value,
        },
        remember.value,
      )
    }
  }

  function logout(): void {
    accessToken.value = null
    refreshToken.value = null
    user.value = null
    remember.value = false
    clearPersisted()
  }

  return {
    accessToken,
    refreshToken,
    user,
    remember,
    isAuthenticated,
    role,
    hydrateFromStorage,
    login,
    updateTokens,
    logout,
  }
})
