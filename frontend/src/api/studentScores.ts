import { getJson } from '@/utils/http'
import type { PageResult, ScoreItem } from '@/types/api'

export interface ScoreListQuery {
  page?: number
  size?: number
  sort?: string // 'grade,desc'
}

/** GET /api/v1/student/scores */
export function listMyScores(query: ScoreListQuery = {}): Promise<PageResult<ScoreItem>> {
  return getJson<PageResult<ScoreItem>>('/student/scores', query as Record<string, unknown>)
}
