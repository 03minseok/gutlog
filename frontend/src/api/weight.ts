import { api, unwrap } from './client'
import type { ApiResponse } from './types'

/** 체중 기록. 날짜당 한 줄뿐이라 id 없이 날짜가 키다. */
export interface WeightRecord {
  /** 'yyyy-MM-dd' */
  date: string
  /** kg, 1.00 ~ 300.00, 소수점 둘째 자리까지 */
  weight: number
}

export const weightApi = {
  /** from·to('yyyy-MM-dd') 둘 다 포함. 날짜순으로 온다. */
  listByPeriod: (from: string, to: string) =>
    unwrap(api.get<ApiResponse<WeightRecord[]>>('/record/weight', { params: { from, to } })),

  /** 그 날짜 기록이 없으면 만들고, 있으면 값을 바꾼다. 미래 날짜는 400. */
  save: (date: string, weight: number) =>
    unwrap(api.put<ApiResponse<WeightRecord>>(`/record/weight/${date}`, { weight })),

  /** 그 날짜 기록이 없으면 404 */
  remove: (date: string) => unwrap(api.delete<ApiResponse<null>>(`/record/weight/${date}`)),
}
