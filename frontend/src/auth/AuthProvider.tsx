import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { setUnauthorizedHandler } from '../api/client'
import { userApi, type User } from '../api/user'
import { AuthContext, type AuthContextValue } from './AuthContext'
import { tokenStorage } from './tokenStorage'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => tokenStorage.get())
  const [user, setUser] = useState<User | null>(null)

  const clearSession = useCallback(() => {
    tokenStorage.clear()
    setToken(null)
    setUser(null)
  }, [])

  // 401을 받으면(로그인 요청 제외) 로그인 상태를 비운다. → RequireAuth가 /login으로 보낸다.
  useEffect(() => {
    setUnauthorizedHandler(clearSession)
    return () => setUnauthorizedHandler(null)
  }, [clearSession])

  // 새로고침 등으로 토큰만 있고 내 정보가 없으면 /me로 복원한다.
  // 토큰이 만료됐으면 /me가 401 → 위 핸들러가 로그아웃 처리한다.
  useEffect(() => {
    if (!token || user) return
    let cancelled = false
    userApi
      .getMe()
      .then((me) => {
        if (!cancelled) setUser(me)
      })
      .catch(() => {
        // 401은 인터셉터가 처리한다. 네트워크 오류 등은 토큰을 유지하고 다음 기회에 다시 시도한다.
      })
    return () => {
      cancelled = true
    }
  }, [token, user])

  const login = useCallback(async (username: string, password: string) => {
    const { accessToken, user: me } = await userApi.login({ username, password })
    tokenStorage.set(accessToken)
    setToken(accessToken)
    setUser(me)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ isLoggedIn: token !== null, user, login, logout: clearSession }),
    [token, user, login, clearSession],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
