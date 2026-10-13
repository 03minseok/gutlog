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

/**
 * 관리자 화면. 일반 사용자는 홈으로 보낸다. (화면 흐름용이며 실제 권한 검사는 백엔드가 403으로 한다)
 * 내 정보(/me)를 받기 전에는 판단할 수 없어서 잠시 비워 둔다.
 */
export function RequireAdmin() {
  const { user } = useAuth()
  if (!user) {
    return <p className="px-4 py-10 text-center text-body text-gray-500">불러오는 중…</p>
  }
  if (user.authority !== 'ADMIN') return <Navigate to="/" replace />
  return <Outlet />
}
