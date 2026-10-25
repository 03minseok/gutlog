import { api, unwrap } from './client'
import type { ApiResponse } from './types'

// 백엔드 enum(Role, IbdType)과 같은 값
export type Role = 'PATIENT' | 'GUARDIAN' | 'OTHER'
export type IbdType =
  | 'ULCERATIVE_COLITIS'
  | 'CROHNS_DISEASE'
  | 'INTESTINAL_BEHCETS'
  | 'INTESTINAL_TUBERCULOSIS'
/** 권한. 회원 유형(Role)과 별개다. ADMIN은 DB에서 직접 지정하고, 바뀌면 다시 로그인해야 반영된다. */
export type Authority = 'USER' | 'ADMIN'

export interface User {
  id: number
  username: string
  nickname: string
  role: Role
  ibdType: IbdType | null
  authority: Authority
}

export interface SignupRequest {
  username: string
  password: string
  nickname: string
  role: Role
  /** 환자(PATIENT)일 때만 보낸다. */
  ibdType?: IbdType
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  user: User
}

export const userApi = {
  /** true면 사용 가능, false면 이미 사용 중 */
  checkUsername: (username: string) =>
    unwrap(api.get<ApiResponse<boolean>>('/users/check-username', { params: { username } })),

  signup: (body: SignupRequest) => unwrap(api.post<ApiResponse<User>>('/users/signup', body)),

  login: (body: LoginRequest) => unwrap(api.post<ApiResponse<LoginResponse>>('/users/login', body)),

  getMe: () => unwrap(api.get<ApiResponse<User>>('/users/me')),
}
