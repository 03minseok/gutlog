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

/**
 * 기록 입력 화면(식사·배변). 화면 높이에 맞춰 본문은 안에서 스크롤하고, 저장 버튼과 하단 탭은 아래에 고정된다.
 */
export function RecordFormLayout() {
  return (
    <div className="mx-auto flex h-dvh w-full max-w-[430px] flex-col bg-white">
      <main className="flex min-h-0 flex-1 flex-col">
        <Outlet />
      </main>
      <BottomTabBar />
    </div>
  )
}

/** 관리자 화면처럼 하단 탭 없이 쓰는 전체 화면. 화면 높이에 맞춰 본문은 안에서 스크롤한다. */
export function FullScreenLayout() {
  return (
    <div className="mx-auto flex h-dvh w-full max-w-[430px] flex-col bg-white">
      <main className="flex min-h-0 flex-1 flex-col">
        <Outlet />
      </main>
    </div>
  )
}

/** 탭 화면 상단 제목 (통계 시안 기준: 24/700, padding 12px 20px). */
export function TabHeader({ title }: { title: string }) {
  return (
    <header className="px-4 py-3">
      <h1 className="text-title font-bold tracking-[-0.015em]">{title}</h1>
    </header>
  )
}
