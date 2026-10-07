import { api, unwrap } from './client'
import type { ApiResponse } from './types'

export interface Meal {
  id: number
  /** 'yyyy-MM-ddTHH:mm:ss' */
  eatenAt: string
  /** 서버가 공백·중복을 정리한 음식 이름 */
  foods: string[]
}

/** 등록·수정 요청. 수정은 전체 교체다. */
export interface MealRequest {
  eatenAt: string
  foods: string[]
}

export const mealApi = {
  /** from·to('yyyy-MM-dd') 둘 다 포함. 먹은 시각 순서로 온다. */
  listByPeriod: (from: string, to: string) =>
    unwrap(api.get<ApiResponse<Meal[]>>('/record/meal', { params: { from, to } })),

  get: (id: number) => unwrap(api.get<ApiResponse<Meal>>(`/record/meal/${id}`)),

  create: (body: MealRequest) => unwrap(api.post<ApiResponse<Meal>>('/record/meal', body)),

  update: (id: number, body: MealRequest) => unwrap(api.put<ApiResponse<Meal>>(`/record/meal/${id}`, body)),

  remove: (id: number) => unwrap(api.delete<ApiResponse<null>>(`/record/meal/${id}`)),
}
