import { ApiErrorCode } from '@/types/api'

/**
 * 错误码 → 中文文案。来源 contracts/api.md §0.3。
 * 前端优先按 code 分派 UI；未映射的 code 回落到后端 message 或此文件的 UNKNOWN。
 */
export const errorMessages: Record<number, string> = {
  [ApiErrorCode.SUCCESS]: '成功',
  [ApiErrorCode.VALIDATION_FAILED]: '请求参数校验失败',
  [ApiErrorCode.UNAUTHORIZED]: '未登录或登录已过期，请重新登录',
  [ApiErrorCode.FORBIDDEN]: '没有权限执行此操作',
  [ApiErrorCode.NOT_FOUND]: '资源不存在',
  [ApiErrorCode.CONFLICT]: '资源冲突',
  [ApiErrorCode.INTERNAL_ERROR]: '服务器内部错误，请稍后重试',
  [ApiErrorCode.LOGIN_FAILED]: '登录名或密码错误',
  [ApiErrorCode.OLD_PASSWORD_MISMATCH]: '原密码错误',
  [ApiErrorCode.CONFIRM_PASSWORD_MISMATCH]: '新密码与确认密码不一致',
  [ApiErrorCode.LOGIN_NAME_TAKEN]: '登录名已被使用',
  [ApiErrorCode.SUBJECT_DUPLICATE_FOR_TEACHER]: '该教师已存在同名课程',
  [ApiErrorCode.SUBJECT_HAS_SCORES]: '课程存在关联成绩，禁止直接删除',
  [ApiErrorCode.SCORE_OUT_OF_RANGE]: '成绩必须在 0–100 之间',
  [ApiErrorCode.SCORE_ALREADY_EXISTS]: '该学生的该门课程成绩已存在',
  [ApiErrorCode.SCORE_ALL_ENROLLED]: '该课程已无待录入学生',
  [ApiErrorCode.SCORE_SUBJECT_NOT_OWNED]: '目标课程不在您的授课表中',
}

/** 字段错误 code → 中文文案。 */
export const fieldErrorMessages: Record<string, string> = {
  VALIDATION_REQUIRED: '此字段必填',
  VALIDATION_LENGTH: '长度不符合要求',
  VALIDATION_PATTERN: '格式不正确',
  VALIDATION_NUMERIC: '必须为数字',
  VALIDATION_RANGE: '数值超出允许范围',
  VALIDATION_MISMATCH: '与其他字段不一致',
}

export function translateErrorCode(code: number, fallback = '请求失败'): string {
  return errorMessages[code] ?? fallback
}

export function translateFieldError(code: string, fallback = '字段校验失败'): string {
  return fieldErrorMessages[code] ?? fallback
}
