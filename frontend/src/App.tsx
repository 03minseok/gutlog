import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthProvider'
import { GuestOnly, RequireAuth } from './auth/RequireAuth'
import { HomePage } from './features/home/HomePage'
import { MyPage } from './features/mypage/MyPage'
import { RecordPage } from './features/record/RecordPage'
import { StatsPage } from './features/stats/StatsPage'
import { LoginPage } from './features/user/LoginPage'
import { SignupPage } from './features/user/SignupPage'
import { TabLayout } from './layouts/TabLayout'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<GuestOnly />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/signup" element={<SignupPage />} />
          </Route>

          <Route element={<RequireAuth />}>
            <Route element={<TabLayout />}>
              <Route index element={<HomePage />} />
              <Route path="/record" element={<RecordPage />} />
              <Route path="/stats" element={<StatsPage />} />
              <Route path="/mypage" element={<MyPage />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
