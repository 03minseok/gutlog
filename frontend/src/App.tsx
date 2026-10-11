import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthProvider'
import { GuestOnly, RequireAuth } from './auth/RequireAuth'
import { ToastProvider } from './components/toast/ToastProvider'
import { BowelFormPage } from './features/bowel/BowelFormPage'
import { HomePage } from './features/home/HomePage'
import { MealFormPage } from './features/meal/MealFormPage'
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
        <ToastProvider>
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
              {/* 기록 입력 화면에는 하단 탭이 없다. */}
              <Route path="/record/meal/new" element={<MealFormPage />} />
              <Route path="/record/meal/:id/edit" element={<MealFormPage />} />
              <Route path="/record/bowel/new" element={<BowelFormPage />} />
              <Route path="/record/bowel/:id/edit" element={<BowelFormPage />} />
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
