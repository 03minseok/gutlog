import { useId, useRef, type ReactNode } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { FieldHint } from '../../components/FieldHint'
import { Icon } from '../../components/Icon'
import { useRecordFormLinkState } from '../../components/recordForm'
import { addDays, formatDateLabel, formatDateWithDay, parseDateParam, relativeDayLabel } from '../../lib/date'
import { CATEGORY_META, DETAIL_PATH, periodLabel, periodOf, periodTitle, type Category, type RangeMode, type RangeState } from './detail'

/**
 * 기록 종류별 상세 화면(식사·배변·복약)이 함께 쓰는 부품.
 * 화면 구성: 헤더 + 전환 버튼(식사·배변·복약) → 달력(주·월·년) → 도구 칸(식사 검색 · 배변 필터 · 복약 내 약 관리, 같은 크기) → 기간 줄 → 기록.
 * 복약은 달력이 주만이고, 아래는 고른 날의 체크.
 * 값과 훅은 detail.ts
 */

const SWITCH_ORDER: Category[] = ['meal', 'bowel', 'medication']

interface DetailTopProps {
  category: Category
  /** 헤더 오른쪽 동작 (예: "＋ 추가", "내 약 관리") */
  action?: ReactNode
}

/**
 * 상세 화면 위쪽: 헤더(뒤로 + "기록 모아보기" 16/700 + 오른쪽 동작) → 전환 버튼 한 줄(식사 · 배변 · 복약).
 * 전환 버튼: 3열, h-11, rounded-btn, 카테고리색 아이콘 18 + 이름 15.
 *   지금 화면 = 카테고리 배경색 면 + 1.5px 카테고리색 테두리 + 700(aria-current), 나머지 = 흰 면 + 1px gray-200 + 500.
 *   눌러서 다른 기록 상세로 바로 넘어간다. (replace — 뒤로 가면 상세에 들어오기 전 화면으로)
 * 뒤로는 보던 화면으로(주소로 바로 들어왔으면 홈으로).
 */
export function DetailTop({ category, action }: DetailTopProps) {
  const navigate = useNavigate()
  const location = useLocation()
  // 앱 안에서 들어왔으면 history가 있다. (첫 화면의 key는 'default')
  const goBack = () => (location.key !== 'default' ? navigate(-1) : navigate('/'))

  return (
    <div className="flex flex-col gap-1">
      <header className="flex h-13 items-center gap-1 px-1.5">
        <button
          type="button"
          onClick={goBack}
          aria-label="뒤로"
          className="flex h-11 w-11 items-center justify-center rounded-input text-ink hover:bg-gray-100"
        >
          <Icon name="arrow_back_ios_new" size={20} />
        </button>
        <h1 className="text-section font-bold">기록 모아보기</h1>
        {action && <span className="mr-2.5 ml-auto">{action}</span>}
      </header>
      <nav aria-label="기록 종류" className="grid grid-cols-3 gap-2 px-4">
        {SWITCH_ORDER.map((key) => {
          const meta = CATEGORY_META[key]
          const current = key === category
          return (
            <Link
              key={key}
              to={DETAIL_PATH[key]}
              replace
              aria-current={current ? 'page' : undefined}
              className={
                'flex h-11 items-center justify-center gap-1 rounded-btn text-body transition-colors ' +
                (current ? `${meta.bg} border-[1.5px] ${meta.border} font-bold text-ink` : 'border border-gray-200 bg-white font-medium text-gray-700')
              }
            >
              <Icon name={meta.icon} size={18} className={meta.color} />
              {meta.label}
            </Link>
          )
        })}
      </nav>
    </div>
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

interface PeriodHeaderProps {
  mode: RangeMode
  start: string
  selected: string | null
  today?: string
  /** "3회" — 불러오는 중이면 null */
  count: string | null
  badge?: ReactNode
  /** 고른 날이 있을 때 오른쪽 "전체 보기" (범위 전체로) */
  onShowAll: () => void
}

/**
 * 지금 보여주는 기간 한 줄: "주 전체 · 12회" · "10월 전체" · "2026년 전체" / "10월 14일 (수) 어제 · 3회" + 배지 · 오른쪽 "전체 보기"(고른 날이 있을 때).
 * 제목 15/700, 횟수 13 gray-500, 링크 13/600 sky-700(44px).
 */
export function PeriodHeader({ mode, start, selected, today, count, badge, onShowAll }: PeriodHeaderProps) {
  const relative = selected && today ? relativeDayLabel(selected, today) : null
  return (
    <div className="-my-1 flex min-h-11 items-center gap-1.5 px-1">
      <h2 className="text-body font-bold tabular-nums">
        {selected ? formatDateWithDay(selected) : periodTitle(mode, start)}
        {relative && <span className="ml-1.5 text-label font-medium text-gray-500">{relative}</span>}
      </h2>
      {count && <span className="text-label font-medium text-gray-500 tabular-nums">· {count}</span>}
      {badge}
      {selected && (
        <button
          type="button"
          onClick={onShowAll}
          className="-mr-1 ml-auto flex h-11 items-center rounded-input px-2 text-label font-semibold text-sky-700"
        >
          전체 보기
        </button>
      )}
    </div>
  )
}

/** 검색·필터 결과가 없을 때: 점선 상자 + 안내 (예: "'김치'가 들어간 식사가 없어요") */
export function NoMatch({ message }: { message: string }) {
  return <div className="rounded-card border border-dashed border-gray-300 px-4 py-5 text-center text-body text-gray-500">{message}</div>
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

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

interface PeriodCalendarProps {
  mode: RangeMode
  /** 범위의 첫날 */
  start: string
  today: string
  /** 고른 날. null이면 범위 전체 (아무 날도 눌려 있지 않음) */
  selected: string | null
  onSelect: (date: string) => void
  /** -1: 이전 범위, 1: 다음 범위 */
  onMove: (n: number) => void
  /** 제목을 누르면 주 → 월 → 년으로 바꾼다. 없으면 제목은 글자만 (복약은 주만) */
  onCycleMode?: () => void
  /** 년 보기에서 달을 눌렀을 때 */
  onOpenMonth?: (monthStart: string) => void
  /** 날짜 칸 아래 표시 (없으면 비움) */
  mark: (date: string) => ReactNode
  /** 화면 낭독기용 표시 문구 (예: "기록 있음") */
  markLabel: (date: string) => string
  /** 년 보기 달 칸 아래 표시 */
  monthMark?: (monthStart: string) => ReactNode
  monthMarkLabel?: (monthStart: string) => string
  /** 표시 설명 한 줄 (없으면 안 그림) */
  legend?: ReactNode
}

const MODE_UNIT: Record<RangeMode, string> = { week: '주', month: '달', year: '해' }
const NEXT_MODE_LABEL: Record<RangeMode, string> = { week: '월', month: '년', year: '주' }

/**
 * 상세 화면 달력 (흰 카드, 1px gray-200, rounded-card) — 식사·배변·복약 상세가 함께 쓴다.
 * - 위: ‹ 제목 › (44px 버튼, 오늘이 든 범위면 › 비활성). 좌우로 밀어도 넘어간다.
 *   제목("10월 11일 – 17일" · "2026년 10월" · "2026년" 15/700 + ⌄)을 누르면 주 → 월 → 년 → 주로 범위가 바뀐다.
 * - 주: 일~토 7칸 한 줄. 월: 그 달 날짜(일요일 시작, 기록 탭 달력과 같은 배치). 년: 1월~12월 4×3, 누르면 그 달 보기.
 * - 날짜 원은 달력과 같은 규칙: 고른 날 ink, 오늘 sky-100, 미래 gray-300 비활성. 아래에 표시(mark).
 * - 아래: 표시 설명 한 줄(12 gray-500, 위 gray-100 구분선).
 */
export function PeriodCalendar(props: PeriodCalendarProps) {
  const { mode, start, today, onMove, onCycleMode, legend } = props
  const { end } = periodOf(mode, start)
  const isCurrent = end >= today
  const touch = useRef<{ x: number; y: number } | null>(null)

  return (
    <div
      className="rounded-card border border-gray-200 bg-white px-2 pt-1 pb-2.5"
      onTouchStart={(e) => (touch.current = { x: e.touches[0].clientX, y: e.touches[0].clientY })}
      onTouchEnd={(e) => {
        const from = touch.current
        touch.current = null
        if (!from) return
        const dx = e.changedTouches[0].clientX - from.x
        const dy = e.changedTouches[0].clientY - from.y
        if (Math.abs(dx) < 50 || Math.abs(dx) < Math.abs(dy)) return
        if (dx > 0) onMove(-1)
        else if (!isCurrent) onMove(1)
      }}
    >
      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={() => onMove(-1)}
          aria-label={`이전 ${MODE_UNIT[mode]}`}
          className="flex h-11 w-11 items-center justify-center rounded-input text-gray-700 hover:bg-gray-100"
        >
          <Icon name="chevron_left" size={20} />
        </button>
        {onCycleMode ? (
          <button
            type="button"
            onClick={onCycleMode}
            aria-label={`${periodLabel(mode, start)}, 눌러서 ${NEXT_MODE_LABEL[mode]} 단위로 보기`}
            className="flex h-11 items-center gap-0.5 rounded-input px-3 text-body font-bold tabular-nums"
          >
            <span aria-live="polite">{periodLabel(mode, start)}</span>
            <Icon name="expand_more" size={18} className="text-gray-500" />
          </button>
        ) : (
          <span aria-live="polite" className="text-body font-bold tabular-nums">
            {periodLabel(mode, start)}
          </span>
        )}
        <button
          type="button"
          onClick={() => onMove(1)}
          disabled={isCurrent}
          aria-label={`다음 ${MODE_UNIT[mode]}`}
          className="flex h-11 w-11 items-center justify-center rounded-input text-gray-700 hover:bg-gray-100 disabled:text-gray-400 disabled:hover:bg-transparent"
        >
          <Icon name="chevron_right" size={20} />
        </button>
      </div>

      {mode === 'year' ? <YearGrid {...props} /> : <DayGrid {...props} />}

      {legend && (
        <div className="mt-1.5 flex justify-center gap-3 border-t border-gray-100 pt-2 text-caption text-gray-500" aria-hidden="true">
          {legend}
        </div>
      )}
    </div>
  )
}

/** 주(7칸 한 줄)·월(그 달 날짜) 칸. 요일 머리줄 + 날짜 원 + 아래 표시. 월은 원을 조금 줄여 높이를 아낀다. */
function DayGrid({ mode, start, today, selected, onSelect, mark, markLabel }: PeriodCalendarProps) {
  const { end } = periodOf(mode, start)
  const days: string[] = []
  for (let date = start; date <= end; date = addDays(date, 1)) days.push(date)
  const blanks = mode === 'month' ? (parseDateParam(start)?.getDay() ?? 0) : 0
  const compact = mode === 'month'

  return (
    <>
      <div className="grid grid-cols-7" aria-hidden="true">
        {WEEKDAYS.map((day) => (
          <span key={day} className="flex h-6 items-center justify-center text-caption font-medium text-gray-500">
            {day}
          </span>
        ))}
      </div>
      <div className="grid grid-cols-7">
        {Array.from({ length: blanks }, (_, i) => (
          <span key={`blank-${i}`} />
        ))}
        {days.map((date) => {
          const isSelected = date === selected
          const isToday = date === today
          const future = date > today
          return (
            <button
              key={date}
              type="button"
              onClick={() => onSelect(date)}
              disabled={future}
              aria-label={[formatDateLabel(date), isToday && '오늘', !future && markLabel(date)].filter(Boolean).join(', ')}
              aria-pressed={isSelected}
              aria-current={isToday ? 'date' : undefined}
              className="group flex flex-col items-center gap-0.5 py-0.5 disabled:cursor-default"
            >
              <span
                className={
                  `flex items-center justify-center rounded-full pt-0.5 text-body tabular-nums transition-colors ${compact ? 'h-8 w-8' : 'h-9 w-9'} ` +
                  (isSelected
                    ? 'bg-ink font-bold text-white'
                    : future
                      ? 'text-gray-300'
                      : isToday
                        ? 'bg-sky-100 font-bold text-sky-800'
                        : 'text-ink group-hover:bg-gray-100')
                }
              >
                {Number(date.slice(8))}
              </span>
              <span className="flex h-3 items-center justify-center" aria-hidden="true">
                {!future && mark(date)}
              </span>
            </button>
          )
        })}
      </div>
    </>
  )
}

/** 년 보기: 1월~12월 4×3. 칸 = "10월" 15 + 아래 표시. 이번 달 sky-100, 미래 달 비활성. 누르면 그 달 보기. */
function YearGrid({ start, today, onOpenMonth, monthMark, monthMarkLabel }: PeriodCalendarProps) {
  const year = Number(start.slice(0, 4))
  const thisMonth = today.slice(0, 7)
  return (
    <div className="grid grid-cols-4 gap-1 pt-1">
      {Array.from({ length: 12 }, (_, i) => {
        const monthStart = `${year}-${String(i + 1).padStart(2, '0')}-01`
        const future = monthStart.slice(0, 7) > thisMonth
        const current = monthStart.slice(0, 7) === thisMonth
        return (
          <button
            key={monthStart}
            type="button"
            onClick={() => onOpenMonth?.(monthStart)}
            disabled={future}
            aria-label={[`${year}년 ${i + 1}월`, current && '이번 달', !future && monthMarkLabel?.(monthStart)].filter(Boolean).join(', ')}
            className="group flex h-14 flex-col items-center justify-center gap-0.5 rounded-input disabled:cursor-default"
          >
            <span
              className={
                'flex h-8 items-center justify-center rounded-full px-3 pt-0.5 text-body tabular-nums ' +
                (future ? 'text-gray-300' : current ? 'bg-sky-100 font-bold text-sky-800' : 'text-ink group-hover:bg-gray-100')
              }
            >
              {i + 1}월
            </span>
            <span className="flex h-3 items-center justify-center" aria-hidden="true">
              {!future && monthMark?.(monthStart)}
            </span>
          </button>
        )
      })}
    </div>
  )
}

/** 기록 있음 표시: 카테고리색 점 6px (예: 'bg-meal') */
export function RecordDot({ color }: { color: string }) {
  return <span className={`block h-1.5 w-1.5 rounded-full ${color}`} />
}

interface ToolBarButtonProps {
  icon: string
  label: ReactNode
  onClick: () => void
  /** 아래 칸이 열려 있으면 true (테두리 ink) */
  expanded?: boolean
  /** 글자 오른쪽 아이콘 (기본 ›) */
  indicator?: string
  /** 칸 오른쪽 끝에 따로 누르는 버튼 (예: 필터 지우기). 있으면 indicator는 그리지 않는다. */
  trailing?: ReactNode
  haspopup?: 'dialog'
}

/**
 * 달력 아래 한 줄 도구 칸 — 식사 검색창(SearchBox)·배변 필터·복약 "내 약 관리"가 모두 같은 크기다.
 * h-12, 너비 꽉 참, rounded-input, 1.5px gray-400(열려 있으면 ink), 흰 면, 왼쪽 아이콘 20 gray-500 + 글자 15.
 */
export function ToolBarButton({ icon, label, onClick, expanded, indicator = 'chevron_right', trailing, haspopup }: ToolBarButtonProps) {
  return (
    <div className={`flex h-12 w-full items-center rounded-input border-[1.5px] bg-white pl-3 ${expanded ? 'border-ink' : 'border-gray-400'}`}>
      <button
        type="button"
        onClick={onClick}
        aria-expanded={expanded}
        aria-haspopup={haspopup}
        className="flex h-full min-w-0 flex-1 items-center gap-2 pr-2 text-left"
      >
        <Icon name={icon} size={20} className="flex-none text-gray-500" />
        <span className="min-w-0 flex-1 truncate text-body text-ink">{label}</span>
        {!trailing && <Icon name={indicator} size={20} className="flex-none text-gray-400" />}
      </button>
      {trailing}
    </div>
  )
}
