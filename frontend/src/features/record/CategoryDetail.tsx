import { useId, type ReactNode } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { FieldHint } from '../../components/FieldHint'
import { Icon } from '../../components/Icon'
import { useRecordFormLinkState } from '../../components/recordForm'
import { SegmentedControl } from '../../components/SegmentedControl'
import { formatDateWithDay } from '../../lib/date'
import { CATEGORY_META, PERIOD_OPTIONS, type Category, type PeriodValue, type RangeState } from './detail'

/**
 * 기록 종류별 상세 화면(식사·배변·복약)이 함께 쓰는 부품.
 * 화면 구성: 헤더(뒤로 + 아이콘 + 이름 + 추가) → 검색창 → 기간 선택 → 요약 칸 → 날짜별 기록.
 * 값과 훅은 detail.ts
 */

interface DetailHeaderProps {
  category: Category
  /** 오른쪽 위 동작 (예: "＋ 추가", "내 약 관리") */
  action?: ReactNode
}

/** 상세 화면 헤더: 뒤로 + 카테고리 아이콘 박스 + 이름 16/700 + 오른쪽 동작. 뒤로는 보던 화면으로(주소로 바로 들어왔으면 홈으로). */
export function DetailHeader({ category, action }: DetailHeaderProps) {
  const navigate = useNavigate()
  const location = useLocation()
  const { label, icon, box } = CATEGORY_META[category]
  // 앱 안에서 들어왔으면 history가 있다. (첫 화면의 key는 'default')
  const goBack = () => (location.key !== 'default' ? navigate(-1) : navigate('/'))

  return (
    <header className="flex h-13 items-center gap-1 px-1.5">
      <button
        type="button"
        onClick={goBack}
        aria-label="뒤로"
        className="flex h-11 w-11 items-center justify-center rounded-input text-ink hover:bg-gray-100"
      >
        <Icon name="arrow_back_ios_new" size={20} />
      </button>
      <span className={`flex h-7 w-7 items-center justify-center rounded-[8px] ${box}`}>
        <Icon name={icon} size={16} />
      </span>
      <h1 className="ml-1 text-section font-bold">{label}</h1>
      {action && <span className="mr-2.5 ml-auto">{action}</span>}
    </header>
  )
}

/** 헤더 오른쪽 링크 (13/600 sky-700, 44px, hover 배경 없음). 입력 화면으로 가면 닫았을 때 이 화면으로 돌아온다. */
export function DetailHeaderLink({ to, label, icon, ariaLabel }: { to: string; label: string; icon?: string; ariaLabel?: string }) {
  const formLinkState = useRecordFormLinkState()
  return (
    <Link
      to={to}
      state={formLinkState}
      aria-label={ariaLabel}
      className="-mr-1 flex h-11 items-center gap-0.5 rounded-input px-2 text-label font-semibold text-sky-700"
    >
      {icon && <Icon name={icon} size={18} />}
      {label}
    </Link>
  )
}

/**
 * 검색창: h-12, 1.5px gray-400, rounded-input, 흰 면, 왼쪽 search 아이콘, 글자가 있으면 오른쪽 지우기(44×44).
 * 입력할 때마다 바로 거른다(서버 요청 없이, 불러온 기간 안에서).
 */
export function SearchBox({ value, onChange, placeholder }: { value: string; onChange: (value: string) => void; placeholder: string }) {
  const id = useId()
  return (
    <div className="flex h-12 items-center rounded-input border-[1.5px] border-gray-400 bg-white pl-3 focus-within:border-sky-700 focus-within:ring-[3px] focus-within:ring-sky-100">
      <Icon name="search" size={20} className="flex-none text-gray-500" />
      <label htmlFor={id} className="sr-only">
        검색
      </label>
      <input
        id={id}
        type="search"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        enterKeyHint="search"
        autoComplete="off"
        className="h-full min-w-0 flex-1 bg-transparent px-2 text-body text-ink outline-none [&::-webkit-search-cancel-button]:hidden"
      />
      {value && (
        <button
          type="button"
          onClick={() => onChange('')}
          aria-label="검색어 지우기"
          className="flex h-11 w-11 flex-none items-center justify-center rounded-input text-gray-500"
        >
          <Icon name="close" size={18} />
        </button>
      )}
    </div>
  )
}

/** 기간 선택: 최근 7일 / 30일 / 90일 (gray-150 트랙 — gray-50 바탕 위에서 보이게) */
export function PeriodControl({ value, onChange }: { value: PeriodValue; onChange: (value: PeriodValue) => void }) {
  return <SegmentedControl label="기간" options={PERIOD_OPTIONS} value={value} onChange={onChange} track="bg-gray-150" />
}

export interface Stat {
  label: string
  value: ReactNode
}

/** 요약 칸 — 흰 면 + 1px gray-200, rounded-tile, px-3 pt-3 pb-2.5, 라벨 13/600 gray-700 + 값 18/700. */
export function StatTiles({ stats }: { stats: Stat[] }) {
  return (
    <dl className="grid gap-2" style={{ gridTemplateColumns: `repeat(${stats.length}, minmax(0, 1fr))` }}>
      {stats.map((stat) => (
        <div key={stat.label} className="flex flex-col gap-1.5 rounded-tile border border-gray-200 bg-white px-3 pt-3 pb-2.5">
          <dt className="text-label font-semibold text-gray-700">{stat.label}</dt>
          <dd className="text-heading font-bold tabular-nums">{stat.value}</dd>
        </div>
      ))}
    </dl>
  )
}

/** 날짜 묶음: "10월 13일 (화) · 2회" 13/600 gray-700 + 기록 카드들 */
export function DayGroup({ date, countLabel, badge, children }: { date: string; countLabel: string; badge?: ReactNode; children: ReactNode }) {
  return (
    <section className="flex flex-col gap-2" aria-label={formatDateWithDay(date)}>
      <h3 className="flex items-center gap-1.5 px-1 text-label font-semibold text-gray-700">
        <span className="tabular-nums">{formatDateWithDay(date)}</span>
        <span className="font-medium text-gray-500">· {countLabel}</span>
        {badge}
      </h3>
      {children}
    </section>
  )
}

/** 검색 결과가 없을 때: 점선 상자 + "'김치'에 맞는 기록이 없어요" */
export function NoMatch({ query }: { query: string }) {
  return (
    <div className="rounded-card border border-dashed border-gray-300 px-4 py-5 text-center text-body text-gray-500">
      ‘{query}’에 맞는 기록이 없어요
    </div>
  )
}

/** 상세 화면 공통 상태 표시: 불러오는 중 / 오류 / 기간 안에 기록 없음 */
export function RangeStatus({ state, onRetry, emptyMessage }: { state: RangeState<unknown>; onRetry: () => void; emptyMessage: ReactNode }) {
  if (state.status === 'loading') {
    return (
      <div role="status" aria-label="불러오는 중" className="flex flex-col gap-2">
        {[0, 1].map((i) => (
          <div key={i} className="flex h-14 items-center gap-3 rounded-card border border-gray-200 bg-white px-4">
            <span className="h-4 w-12 animate-pulse rounded-input bg-gray-100" />
            <span className="h-4 flex-1 animate-pulse rounded-input bg-gray-100" />
          </div>
        ))}
      </div>
    )
  }
  if (state.status === 'error') {
    return (
      <div className="flex min-h-14 items-center justify-between gap-2 rounded-card border border-error-border bg-error-bg py-1.5 pr-1.5 pl-4">
        <FieldHint tone="error">{state.message}</FieldHint>
        <button
          type="button"
          onClick={onRetry}
          className="flex h-11 flex-none items-center gap-1 rounded-input px-3 text-label font-semibold text-ink hover:bg-white/70"
        >
          <Icon name="refresh" size={18} />
          다시 시도
        </button>
      </div>
    )
  }
  if (state.items.length === 0) return <>{emptyMessage}</>
  return null
}
