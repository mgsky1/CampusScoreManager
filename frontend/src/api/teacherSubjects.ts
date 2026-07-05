import { getJson, postJson } from '@/utils/http'
import type {
  CreateSubjectPayload,
  PageResult,
  SubjectMineItem,
  UpdateSubjectPayload,
} from '@/types/api'

export interface SubjectSearchQuery {
  page?: number
  size?: number
  keyword?: string
  sort?: string // 'grade,asc' | 'name,asc' | 'id,asc'
}

/** GET /api/v1/teacher/subjects */
export function listMySubjects(
  query: SubjectSearchQuery = {},
): Promise<PageResult<SubjectMineItem>> {
  return getJson<PageResult<SubjectMineItem>>(
    '/teacher/subjects',
    query as Record<string, unknown>,
  )
}

/** POST /api/v1/teacher/subjects */
export function createSubject(payload: CreateSubjectPayload): Promise<SubjectMineItem> {
  return postJson<SubjectMineItem>('/teacher/subjects', payload)
}

/** POST /api/v1/teacher/subjects/{id}/update */
export function updateSubject(
  id: number,
  payload: UpdateSubjectPayload,
): Promise<SubjectMineItem> {
  return postJson<SubjectMineItem>(`/teacher/subjects/${id}/update`, payload)
}

/** POST /api/v1/teacher/subjects/{id}/delete */
export function deleteSubject(id: number): Promise<void> {
  return postJson<void>(`/teacher/subjects/${id}/delete`)
}
