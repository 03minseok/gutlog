import { api, unwrap } from './client'
import type { ApiResponse } from './types'

/** 수면 기록 (낮잠 제외 총 수면 시간). 날짜당 한 줄뿐이라 id 없이 날짜가 키다. 날짜는 일어난 날이다. */
export interface SleepRecord {
  /** 'yyyy-MM-dd' — 일어난 날 */
  date: string
  /** 분, 30 ~ 1440, 30분 단위 */
  sleepMinutes: number
}

export const sleepApi = {
  /** from·to('yyyy-MM-dd') 둘 다 포함. 날짜순으로 온다. */
  listByPeriod: (from: string, to: string) =>
    unwrap(api.get<ApiResponse<SleepRecord[]>>('/record/sleep', { params: { from, to } })),

  /** 그 날짜 기록이 없으면 만들고, 있으면 값을 바꾼다. 미래 날짜는 400. */
  save: (date: string, sleepMinutes: number) =>
    unwrap(api.put<ApiResponse<SleepRecord>>(`/record/sleep/${date}`, { sleepMinutes })),

  /** 그 날짜 기록이 없으면 404 */
  remove: (date: string) => unwrap(api.delete<ApiResponse<null>>(`/record/sleep/${date}`)),
}
