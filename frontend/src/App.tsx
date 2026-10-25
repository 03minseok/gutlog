import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthProvider'
import { GuestOnly, RequireAdmin, RequireAuth } from './auth/RequireAuth'
import { ToastProvider } from './components/toast/ToastProvider'
import { AdminMedicineFormPage } from './features/admin/AdminMedicineFormPage'
import { AdminMedicinesPage } from './features/admin/AdminMedicinesPage'
import { BowelDetailPage } from './features/bowel/BowelDetailPage'
import { BowelFormPage } from './features/bowel/BowelFormPage'
import { HomePage } from './features/home/HomePage'
import { MealDetailPage } from './features/meal/MealDetailPage'
import { MealFormPage } from './features/meal/MealFormPage'
import { MedicationDetailPage } from './features/medication/MedicationDetailPage'
import { MyMedicinesPage } from './features/medication/MyMedicinesPage'
import { UserMedicineFormPage } from './features/medication/UserMedicineFormPage'
import { MyPage } from './features/mypage/MyPage'
import { RecordPage } from './features/record/RecordPage'
import { StatsPage } from './features/stats/StatsPage'
import { LoginPage } from './features/user/LoginPage'
import { SignupPage } from './features/user/SignupPage'
import { FullScreenLayout, RecordFormLayout, TabLayout } from './layouts/TabLayout'

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
                {/* 기록 종류별 상세 */}
                <Route path="/record/meal" element={<MealDetailPage />} />
                <Route path="/record/bowel" element={<BowelDetailPage />} />
                <Route path="/record/medication" element={<MedicationDetailPage />} />
                <Route path="/record/medication/medicines" element={<MyMedicinesPage />} />
              </Route>
              {/* 기록·약 입력 화면: 전체 화면이지만 하단 탭은 보인다. */}
              <Route element={<RecordFormLayout />}>
                <Route path="/record/meal/new" element={<MealFormPage />} />
                <Route path="/record/meal/:id/edit" element={<MealFormPage />} />
                <Route path="/record/bowel/new" element={<BowelFormPage />} />
                <Route path="/record/bowel/:id/edit" element={<BowelFormPage />} />
                <Route path="/record/medication/medicines/new" element={<UserMedicineFormPage />} />
                <Route path="/record/medication/medicines/:id/edit" element={<UserMedicineFormPage />} />
              </Route>

              <Route element={<RequireAdmin />}>
                <Route element={<FullScreenLayout />}>
                  <Route path="/admin/medicines" element={<AdminMedicinesPage />} />
                  <Route path="/admin/medicines/new" element={<AdminMedicineFormPage />} />
                  <Route path="/admin/medicines/:id/edit" element={<AdminMedicineFormPage />} />
                </Route>
              </Route>
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
