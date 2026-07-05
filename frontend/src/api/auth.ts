import { getJson, postJson } from '@/utils/http'
import type { AuthLoginResponse, UserBrief } from '@/types/api'

/** POST /api/v1/auth/login */
export function login(payload: {
  loginName: string
  password: string
}): Promise<AuthLoginResponse> {
  return postJson<AuthLoginResponse>('/auth/login', payload)
}

/** POST /api/v1/auth/refresh */
export function refresh(refreshToken: string): Promise<AuthLoginResponse> {
  return postJson<AuthLoginResponse>('/auth/refresh', { refreshToken })
}

/** POST /api/v1/auth/logout */
export function logout(): Promise<void> {
  return postJson<void>('/auth/logout')
}

/** GET /api/v1/auth/me */
export function me(): Promise<UserBrief> {
  return getJson<UserBrief>('/auth/me')
}
