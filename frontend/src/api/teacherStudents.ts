import { getJson, postJson } from '@/utils/http'
import type {
  CreateStudentPayload,
  PageResult,
  StudentDetail,
  UpdateStudentPayload,
} from '@/types/api'

export interface StudentSearchQuery {
  page?: number
  size?: number
  keyword?: string
  sort?: string // e.g. 'real_name,asc' | 'grade,desc' | 'id,asc'
}

/** GET /api/v1/teacher/students */
export function searchStudents(
  query: StudentSearchQuery = {},
): Promise<PageResult<StudentDetail>> {
  return getJson<PageResult<StudentDetail>>(
    '/teacher/students',
    query as Record<string, unknown>,
  )
}

/** GET /api/v1/teacher/students/{id} */
export function getStudent(id: number): Promise<StudentDetail> {
  return getJson<StudentDetail>(`/teacher/students/${id}`)
}

/** POST /api/v1/teacher/students */
export function createStudent(payload: CreateStudentPayload): Promise<StudentDetail> {
  return postJson<StudentDetail>('/teacher/students', payload)
}

/** POST /api/v1/teacher/students/{id}/update */
export function updateStudent(
  id: number,
  payload: UpdateStudentPayload,
): Promise<StudentDetail> {
  return postJson<StudentDetail>(`/teacher/students/${id}/update`, payload)
}

/** POST /api/v1/teacher/students/{id}/delete */
export function deleteStudent(id: number): Promise<void> {
  return postJson<void>(`/teacher/students/${id}/delete`)
}
