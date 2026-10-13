import { NavLink } from 'react-router'
import { Icon } from './Icon'

const TABS = [
  { to: '/', label: '홈', icon: 'home' },
  { to: '/record', label: '기록', icon: 'edit_note' },
  { to: '/stats', label: '통계', icon: 'bar_chart' },
  { to: '/mypage', label: '마이페이지', icon: 'person' },
] as const

/**
 * 흰 배경, border-t gray-200, 4열, padding 4px 4px 20px(또는 safe-area). 항목 min-h 48, 아이콘 22 + 라벨 11.
 * 활성: sky-700 + FILL 아이콘 + 700 / 비활성: gray-500 + 500.
 */
export function BottomTabBar() {
  return (
    <nav
      aria-label="주요 메뉴"
      className="sticky bottom-0 z-10 grid grid-cols-4 border-t border-gray-200 bg-white px-1 pt-1 pb-[max(20px,env(safe-area-inset-bottom))]"
    >
      {TABS.map((tab) => (
        <NavLink
          key={tab.to}
          to={tab.to}
          end={tab.to === '/'}
          className={({ isActive }) =>
            `flex min-h-12 flex-col items-center justify-center gap-0.5 ${isActive ? 'text-sky-700' : 'text-gray-500'}`
          }
        >
          {({ isActive }) => (
            <>
              <Icon name={tab.icon} size={22} filled={isActive} />
              <span className={`text-tab ${isActive ? 'font-bold' : 'font-medium'}`}>{tab.label}</span>
            </>
          )}
        </NavLink>
      ))}
    </nav>
  )
}
