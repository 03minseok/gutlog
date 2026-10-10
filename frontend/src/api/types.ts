/** 백엔드 공통 응답 형태: { success, data, message } */
export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

/**
 * 화면에서 다루는 오류 하나의 형태.
 * status가 null이면 서버에 닿지 못한 경우(네트워크 오류)다.
 */
export class ApiError extends Error {
  readonly status: number | null

  constructor(status: number | null, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}
