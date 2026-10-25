import { api, unwrap } from './client'
import type { ApiResponse } from './types'

/** 백엔드 enum BristolType과 같은 값 (브리스톨 대변 척도 1~7형) */
export type BristolType = 'TYPE_1' | 'TYPE_2' | 'TYPE_3' | 'TYPE_4' | 'TYPE_5' | 'TYPE_6' | 'TYPE_7'

export interface Bowel {
  id: number
  /** 'yyyy-MM-ddTHH:mm:ss' */
  bowelAt: string
  /** 혈변 여부 */
  bleeding: boolean
  bowelCondition: BristolType
  /** 0~10 */
  painLevel: number
  memo: string | null
}

/** 등록·수정 요청. 수정은 전체 교체라 memo를 빼면(null) 지워진다. */
export interface BowelRequest {
  bowelAt: string
  bleeding: boolean
  bowelCondition: BristolType
  painLevel: number
  /** 최대 500자 */
  memo: string | null
}

export const bowelApi = {
  /** from·to('yyyy-MM-dd') 둘 다 포함. 배변 시각 순서로 온다. */
  listByPeriod: (from: string, to: string) =>
    unwrap(api.get<ApiResponse<Bowel[]>>('/record/bowel', { params: { from, to } })),

  get: (id: number) => unwrap(api.get<ApiResponse<Bowel>>(`/record/bowel/${id}`)),

  create: (body: BowelRequest) => unwrap(api.post<ApiResponse<Bowel>>('/record/bowel', body)),

  update: (id: number, body: BowelRequest) => unwrap(api.put<ApiResponse<Bowel>>(`/record/bowel/${id}`, body)),

  remove: (id: number) => unwrap(api.delete<ApiResponse<null>>(`/record/bowel/${id}`)),
}
