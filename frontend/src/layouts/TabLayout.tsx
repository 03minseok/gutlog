import { Outlet } from 'react-router'
import { BottomTabBar } from '../components/BottomTabBar'

/** 하단 탭이 있는 화면. 배경 gray-50, 데스크톱에서는 430px로 가운데 정렬. */
export function TabLayout() {
  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-[430px] flex-col bg-gray-50">
      <main className="flex flex-1 flex-col">
        <Outlet />
      </main>
      <BottomTabBar />
    </div>
  )
}

/** 탭 화면 상단 제목 (통계 시안 기준: 24/700, padding 12px 20px). */
export function TabHeader({ title }: { title: string }) {
  return (
    <header className="px-5 py-3">
      <h1 className="text-[24px] font-bold tracking-[-0.015em]">{title}</h1>
    </header>
  )
}
