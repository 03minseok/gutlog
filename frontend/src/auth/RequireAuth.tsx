import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from './AuthContext'

/** 로그인이 필요한 화면. 토큰이 없으면 로그인 화면으로 보내고, 로그인 후 원래 화면으로 돌아온다. */
export function RequireAuth() {
  const { isLoggedIn } = useAuth()
  const location = useLocation()

  if (!isLoggedIn) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}

/** 로그인·회원가입처럼 로그인하지 않은 사람만 볼 화면. 이미 로그인했으면 홈으로 보낸다. */
export function GuestOnly() {
  const { isLoggedIn } = useAuth()
  if (isLoggedIn) return <Navigate to="/" replace />
  return <Outlet />
}
