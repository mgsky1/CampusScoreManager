import { getJson, postJson } from '@/utils/http'
import type { UserRole } from '@/types/api'

/**
 * 当前账号信息（学生/教师合并视图）。
 * - `tel` 与 `role` 必有；学生额外含 `address` 与 `grade`。
 * - 教师视角 `address` / `grade` 为 `undefined`。
 */
export interface ProfileResponse {
  id: number
  loginName: string
  realName: string
  role: UserRole
  tel: string | null
  address?: string
  grade?: number
}

/**
 * 更新个人资料请求体。
 * - 学生：`tel` + `address` 必填。
 * - 教师：仅 `tel`，`address` 传了也会被后端忽略。
 */
export interface UpdateProfilePayload {
  tel: string
  address?: string
}

/** 修改密码请求体。 */
export interface ChangePasswordPayload {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

/** GET /api/v1/account/profile */
export function getProfile(): Promise<ProfileResponse> {
  return getJson<ProfileResponse>('/account/profile')
}

/** POST /api/v1/account/profile/update */
export function updateProfile(payload: UpdateProfilePayload): Promise<ProfileResponse> {
  return postJson<ProfileResponse>('/account/profile/update', payload)
}

/** POST /api/v1/account/password/change */
export function changePassword(payload: ChangePasswordPayload): Promise<void> {
  return postJson<void>('/account/password/change', payload)
}
