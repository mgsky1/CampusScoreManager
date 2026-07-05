/**
 * 与后端 contracts/api.md 对齐的 TypeScript 类型。
 * 后端源真相：`backend/src/main/java/com/campusscore/common/*.java`。
 */

// ---------- 通用信封 ----------

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data?: T
  errors?: FieldError[]
}

export interface FieldError {
  field: string
  code: string
  message?: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
  hasNext: boolean
}

// ---------- 用户 ----------

export type UserRole = 'STUDENT' | 'TEACHER'

export interface UserBrief {
  id: number
  loginName: string
  realName: string
  role: UserRole
}

export interface StudentDetail {
  id: number
  loginName: string
  realName: string
  tel: string
  address: string
  grade: number
}

export interface TeacherDetail {
  id: number
  loginName: string
  realName: string
  tel: string | null
}

// ---------- 授课 & 成绩 ----------

export interface SubjectItem {
  id: number
  name: string
  teacherId: number
  teacherName?: string
  grade: number
  createdAt?: string
}

export interface ScoreItem {
  id: number
  studentId: number
  studentName?: string
  subjectId: number
  subjectName: string
  teacherId: number
  teacherName?: string
  score: number
  /** 学生学年快照（录入时） */
  grade: number
  isFailing: boolean
  createdAt?: string
  updatedAt?: string
}

/**
 * 教师视角成绩条目（contracts §6.1）。
 */
export interface TeacherScoreItem {
  id: number
  subjectId: number
  subjectName: string
  grade: number
  score: number
  isFailing: boolean
  editable: boolean
  updatedAt?: string
}

/** entry-options 学生极简概要（contracts §6.2）。 */
export interface StudentBrief {
  id: number
  realName: string
  grade: number
}

/** 可录入课程候选项（contracts §6.2）。 */
export interface EntryOption {
  subjectId: number
  subjectName: string
  grade: number
}

/** entry-options 响应（contracts §6.2）。 */
export interface EntryOptions {
  student: StudentBrief
  options: EntryOption[]
  allEntered: boolean
}

/** 录入成绩请求体（contracts §6.3）。 */
export interface CreateScorePayload {
  subjectId: number
  score: number
}

/** 修改成绩请求体（contracts §6.4）。 */
export interface UpdateScorePayload {
  score: number
}

// ---------- 认证 ----------

export interface AuthLoginResponse {
  accessToken: string
  refreshToken: string
  user: UserBrief
}

// ---------- 错误码 ----------

export enum ApiErrorCode {
  SUCCESS = 0,
  VALIDATION_FAILED = 1000,
  UNAUTHORIZED = 1401,
  FORBIDDEN = 1403,
  NOT_FOUND = 1404,
  CONFLICT = 1409,
  INTERNAL_ERROR = 1500,
  LOGIN_FAILED = 2001,
  OLD_PASSWORD_MISMATCH = 2002,
  CONFIRM_PASSWORD_MISMATCH = 2003,
  LOGIN_NAME_TAKEN = 2101,
  SUBJECT_DUPLICATE_FOR_TEACHER = 2201,
  SUBJECT_HAS_SCORES = 2202,
  SCORE_OUT_OF_RANGE = 2301,
  SCORE_ALREADY_EXISTS = 2302,
  SCORE_ALL_ENROLLED = 2303,
  SCORE_SUBJECT_NOT_OWNED = 2304,
}

/** 新增学生请求体（contracts §4.3）。 */
export interface CreateStudentPayload {
  loginName: string
  realName: string
  password: string
  tel: string
  address: string
  grade: number
}

/** 修改学生请求体（contracts §4.4）。password 可选：留空则不重置。 */
export interface UpdateStudentPayload {
  loginName: string
  realName: string
  password?: string
  tel: string
  address: string
  grade: number
}

/** 教师课程 item（contracts §5.1）。 */
export interface SubjectMineItem {
  id: number
  name: string
  grade: number
}

/** 新增课程请求体（contracts §5.2）。 */
export interface CreateSubjectPayload {
  name: string
  grade: number
}

/** 修改课程请求体（contracts §5.3）。 */
export interface UpdateSubjectPayload {
  name: string
  grade: number
}
