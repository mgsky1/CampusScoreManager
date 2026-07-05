import { getJson, postJson } from '@/utils/http'
import type {
  CreateScorePayload,
  EntryOptions,
  PageResult,
  TeacherScoreItem,
  UpdateScorePayload,
} from '@/types/api'

export interface TeacherScoreListQuery {
  page?: number
  size?: number
  sort?: string // 'grade,desc' | 'score,desc' | 'subject_name,asc' | 'updated_at,desc'
}

/** GET /api/v1/teacher/students/{sid}/scores */
export function listStudentScores(
  sid: number,
  query: TeacherScoreListQuery = {},
): Promise<PageResult<TeacherScoreItem>> {
  return getJson<PageResult<TeacherScoreItem>>(
    `/teacher/students/${sid}/scores`,
    query as Record<string, unknown>,
  )
}

/** GET /api/v1/teacher/students/{sid}/entry-options */
export function getEntryOptions(sid: number): Promise<EntryOptions> {
  return getJson<EntryOptions>(`/teacher/students/${sid}/entry-options`)
}

/** POST /api/v1/teacher/students/{sid}/scores */
export function createScore(
  sid: number,
  payload: CreateScorePayload,
): Promise<TeacherScoreItem> {
  return postJson<TeacherScoreItem>(`/teacher/students/${sid}/scores`, payload)
}

/** POST /api/v1/teacher/scores/{scoreId}/update */
export function updateScore(
  scoreId: number,
  payload: UpdateScorePayload,
): Promise<TeacherScoreItem> {
  return postJson<TeacherScoreItem>(`/teacher/scores/${scoreId}/update`, payload)
}

/** POST /api/v1/teacher/scores/{scoreId}/delete */
export function deleteScore(scoreId: number): Promise<void> {
  return postJson<void>(`/teacher/scores/${scoreId}/delete`)
}
