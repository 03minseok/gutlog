import { createContext, useContext } from 'react'
import type { User } from '../api/user'

export interface AuthContextValue {
  /** 저장된 토큰이 있으면 로그인 상태로 본다. (실제 검증은 백엔드가 한다) */
  isLoggedIn: boolean
  /** 내 정보. 로그인 직후나 /me 응답 전에는 null일 수 있다. */
  user: User | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth는 AuthProvider 안에서만 사용할 수 있어요.')
  return value
}
