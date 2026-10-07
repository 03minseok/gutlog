import axios, { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { tokenStorage } from '../auth/tokenStorage'
import { ApiError, type ApiResponse } from './types'

const LOGIN_URL = '/users/login'

const MESSAGE = {
  network: '서버에 연결할 수 없어요. 잠시 후 다시 시도해 주세요.',
  unauthorized: '로그인이 필요해요. 다시 로그인해 주세요.',
  server: '일시적인 오류가 발생했어요. 잠시 후 다시 시도해 주세요.',
  unknown: '요청을 처리하지 못했어요. 다시 시도해 주세요.',
}

export const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

/**
 * 401(로그인 요청 제외)을 받았을 때 실행할 동작.
 * AuthProvider가 등록해서 로그인 상태를 비우고, 라우트 가드가 로그인 화면으로 보낸다.
 */
let unauthorizedHandler: (() => void) | null = null

export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler
}

function isLoginRequest(config?: InternalAxiosRequestConfig) {
  return config?.url === LOGIN_URL
}

// 요청 인터셉터: 저장된 토큰을 Authorization 헤더에 붙인다.
api.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 응답 인터셉터: 401이면 토큰을 지우고 로그인 화면으로, 모든 오류는 ApiError로 바꾼다.
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status ?? null

    if (status === 401 && !isLoginRequest(error.config)) {
      tokenStorage.clear()
      if (unauthorizedHandler) {
        unauthorizedHandler()
      } else {
        window.location.assign('/login')
      }
    }

    return Promise.reject(toApiError(status, error.response?.data))
  },
)

function toApiError(status: number | null, body: ApiResponse<unknown> | undefined): ApiError {
  if (status === null) return new ApiError(null, MESSAGE.network)
  if (status >= 500) return new ApiError(status, MESSAGE.server)
  // 400·401(로그인 실패)·404·409는 서버 message를 그대로 보여준다. 401·403은 본문이 없을 수 있다.
  const serverMessage = body && typeof body === 'object' ? body.message : null
  if (serverMessage) return new ApiError(status, serverMessage)
  if (status === 401) return new ApiError(status, MESSAGE.unauthorized)
  return new ApiError(status, MESSAGE.unknown)
}

/** 공통 응답에서 data만 꺼낸다. */
export async function unwrap<T>(request: Promise<AxiosResponse<ApiResponse<T>>>): Promise<T> {
  const response = await request
  return response.data.data
}
