import { Link } from 'react-router'
import { Icon } from '../../components/Icon'

export interface QuickRecordItem {
  to: string
  label: string
  icon: string
  /** 아이콘 색 (예: 'text-meal') */
  color: string
}

/**
 * 하단 탭 바로 위에 붙는 "바로 기록" 막대. 엄지가 닿는 자리에 늘 보이게 한다.
 * 버튼: h-12, sky-100 면 + ink 글자, 카테고리색 아이콘.
 * bottom 값은 BottomTabBar 높이(pt-1 + min-h-12 + pb max(20px, safe-area))와 맞춘다.
 */
export function QuickRecordBar({ label, items }: { label: string; items: QuickRecordItem[] }) {
  return (
    <nav
      aria-label={label}
      className="sticky bottom-[calc(52px+max(20px,env(safe-area-inset-bottom)))] z-10 mt-auto grid gap-2 border-t border-gray-200 bg-white px-4 py-2"
      style={{ gridTemplateColumns: `repeat(${items.length}, minmax(0, 1fr))` }}
    >
      {items.map((item) => (
        <Link
          key={item.label}
          to={item.to}
          className="flex h-12 items-center justify-center gap-1.5 rounded-btn bg-sky-100 text-body font-semibold text-ink hover:bg-sky-50 active:bg-sky-300/40"
        >
          <Icon name={item.icon} size={20} className={item.color} />
          {item.label}
        </Link>
      ))}
    </nav>
  )
}
