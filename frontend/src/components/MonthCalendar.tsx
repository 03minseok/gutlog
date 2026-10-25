import { useState } from 'react'
import { Icon } from './Icon'
import { formatDateLabel, parseDateParam, toDateParam } from '../lib/date'

interface MonthCalendarProps {
  /** 선택된 날짜 'yyyy-MM-dd' */
  date: string
  /** 이 날짜 이후는 고를 수 없다. (오늘) */
  max: string
  onChange: (date: string) => void
}

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']
/** 연도 선택 한 화면에 보이는 해 수 (3열 × 4줄) */
const YEARS_PER_PAGE = 12

type Mode = 'day' | 'month' | 'year'

/** 'yyyy-MM-dd' → 그 달 1일의 Date */
function monthStartOf(dateParam: string): Date {
  const date = parseDateParam(dateParam) ?? new Date()
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

/**
 * 한 달 달력. 날짜를 누르면 그날 기록으로 이동한다.
 * - 날짜 보기: 일요일 시작 7열, 날짜 칸 h-11(44px). 선택한 날 ink 원, 오늘 sky-100 원, 미래 비활성.
 * - 제목("2026년 10월")을 누르면 **월 선택**(그 해 12달, 3열), 월 선택에서 제목("2026년")을 한 번 더 누르면 **연도 선택**(12년, 3열).
 *   연도를 고르면 그 해의 월 선택으로, 월을 고르면 그 달의 날짜 보기로 돌아온다. (보는 달만 바뀌고 선택한 날짜는 그대로)
 * - ‹ › 는 보기에 따라 한 달 / 한 해 / 12년씩 옮긴다. 오늘 이후로는 갈 수 없다.
 * - 바깥에서 날짜가 바뀌어 다른 달이 되면(이전·다음 날 버튼 등) 그 달의 날짜 보기로 따라간다.
 */
export function MonthCalendar({ date, max, onChange }: MonthCalendarProps) {
  // 보고 있는 달과 보기 방식. 선택한 날짜가 바뀌면 그 날짜의 달·날짜 보기로 맞춘다. (렌더 중 상태 조정)
  const [view, setView] = useState(() => ({ forDate: date, month: monthStartOf(date), mode: 'day' as Mode }))
  let { month, mode } = view
  if (view.forDate !== date) {
    month = monthStartOf(date)
    mode = 'day'
    setView({ forDate: date, month, mode })
  }

  const year = month.getFullYear()
  const monthIndex = month.getMonth()
  const maxMonth = monthStartOf(max)
  const maxYear = maxMonth.getFullYear()
  const selectedMonth = monthStartOf(date)
  // 연도 보기: 보고 있는 해가 들어 있는 12년 묶음 (가장 최근 묶음이 올해로 끝나게)
  const yearPageEnd = maxYear - Math.floor((maxYear - year) / YEARS_PER_PAGE) * YEARS_PER_PAGE
  const yearPageStart = yearPageEnd - YEARS_PER_PAGE + 1

  const show = (nextMonth: Date, nextMode: Mode = mode) => setView({ forDate: date, month: nextMonth, mode: nextMode })

  const nav = {
    day: {
      title: `${year}년 ${monthIndex + 1}월`,
      titleLabel: `${year}년 ${monthIndex + 1}월, 월 선택`,
      prev: () => show(new Date(year, monthIndex - 1, 1)),
      next: () => show(new Date(year, monthIndex + 1, 1)),
      canNext: month < maxMonth,
      unit: '달',
      onTitle: () => show(month, 'month'),
    },
    month: {
      title: `${year}년`,
      titleLabel: `${year}년, 연도 선택`,
      prev: () => show(new Date(year - 1, monthIndex, 1)),
      next: () => show(new Date(Math.min(year + 1, maxYear), year + 1 === maxYear ? Math.min(monthIndex, maxMonth.getMonth()) : monthIndex, 1)),
      canNext: year < maxYear,
      unit: '해',
      onTitle: () => show(month, 'year'),
    },
    year: {
      title: `${yearPageStart} – ${yearPageEnd}`,
      titleLabel: `${yearPageStart}년부터 ${yearPageEnd}년, 날짜 보기로`,
      prev: () => show(new Date(year - YEARS_PER_PAGE, monthIndex, 1)),
      next: () => show(new Date(Math.min(year + YEARS_PER_PAGE, maxYear), monthIndex, 1)),
      canNext: yearPageEnd < maxYear,
      unit: '12년',
      onTitle: () => show(month, 'day'),
    },
  }[mode]

  return (
    <div className="rounded-card border border-gray-200 bg-white px-2 pt-1 pb-2">
      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={nav.prev}
          aria-label={`이전 ${nav.unit}`}
          className="flex h-11 w-11 items-center justify-center rounded-input text-gray-700 hover:bg-gray-100"
        >
          <Icon name="chevron_left" size={20} />
        </button>
        {/* 제목을 누르면 날짜 → 월 → 연도 → 날짜 순으로 보기가 바뀐다. */}
        <button
          type="button"
          onClick={nav.onTitle}
          aria-label={nav.titleLabel}
          className="flex h-11 items-center gap-0.5 rounded-input px-3 text-body font-bold tabular-nums"
        >
          <span aria-live="polite">{nav.title}</span>
          <Icon name={mode === 'year' ? 'expand_less' : 'expand_more'} size={18} className="text-gray-500" />
        </button>
        <button
          type="button"
          onClick={nav.next}
          disabled={!nav.canNext}
          aria-label={`다음 ${nav.unit}`}
          className="flex h-11 w-11 items-center justify-center rounded-input text-gray-700 hover:bg-gray-100 disabled:text-gray-400 disabled:hover:bg-transparent"
        >
          <Icon name="chevron_right" size={20} />
        </button>
      </div>

      {mode === 'day' && <DayGrid year={year} monthIndex={monthIndex} date={date} max={max} onChange={onChange} />}

      {mode === 'month' && (
        <PickGrid
          label={`${year}년 월 선택`}
          items={Array.from({ length: 12 }, (_, i) => {
            const value = new Date(year, i, 1)
            return {
              key: i,
              label: `${i + 1}월`,
              selected: value.getTime() === selectedMonth.getTime(),
              current: value.getTime() === maxMonth.getTime(),
              disabled: value > maxMonth,
              onPick: () => show(value, 'day'),
            }
          })}
        />
      )}

      {mode === 'year' && (
        <PickGrid
          label="연도 선택"
          items={Array.from({ length: YEARS_PER_PAGE }, (_, i) => {
            const y = yearPageStart + i
            return {
              key: y,
              label: `${y}`,
              selected: y === selectedMonth.getFullYear(),
              current: y === maxYear,
              disabled: y > maxYear,
              // 그 해의 월 선택으로. 올해면 이번 달을 넘지 않게.
              onPick: () => show(new Date(y, y === maxYear ? Math.min(monthIndex, maxMonth.getMonth()) : monthIndex, 1), 'month'),
            }
          })}
        />
      )}
    </div>
  )
}

interface DayGridProps {
  year: number
  monthIndex: number
  date: string
  max: string
  onChange: (date: string) => void
}

/** 날짜 보기: 요일 줄 + 날짜 칸 */
function DayGrid({ year, monthIndex, date, max, onChange }: DayGridProps) {
  const first = new Date(year, monthIndex, 1)
  const daysInMonth = new Date(year, monthIndex + 1, 0).getDate()
  const cells: (string | null)[] = [
    ...Array.from({ length: first.getDay() }, () => null),
    ...Array.from({ length: daysInMonth }, (_, i) => toDateParam(new Date(year, monthIndex, i + 1))),
  ]
  return (
    <>
      <div className="grid grid-cols-7" aria-hidden="true">
        {WEEKDAYS.map((day) => (
          <span key={day} className="flex h-7 items-center justify-center text-caption font-medium text-gray-500">
            {day}
          </span>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-y-0.5">
        {cells.map((cell, index) => {
          if (cell === null) return <span key={`blank-${index}`} />
          const day = Number(cell.slice(8))
          const selected = cell === date
          const isToday = cell === max
          const future = cell > max
          return (
            <button
              key={cell}
              type="button"
              onClick={() => onChange(cell)}
              disabled={future}
              aria-label={`${formatDateLabel(cell)}${isToday ? ', 오늘' : ''}`}
              aria-pressed={selected}
              aria-current={isToday ? 'date' : undefined}
              className="group flex h-11 items-center justify-center disabled:cursor-default"
            >
              <span
                className={
                  'flex h-9 w-9 items-center justify-center rounded-full pt-0.5 text-body tabular-nums transition-colors ' +
                  (selected
                    ? 'bg-ink font-bold text-white'
                    : future
                      ? 'text-gray-300'
                      : isToday
                        ? 'bg-sky-100 font-bold text-sky-800'
                        : 'text-ink group-hover:bg-gray-100')
                }
              >
                {day}
              </span>
            </button>
          )
        })}
      </div>
    </>
  )
}

interface PickItem {
  key: number
  label: string
  /** 선택한 날짜가 들어 있는 달·해 */
  selected: boolean
  /** 오늘이 들어 있는 달·해 */
  current: boolean
  disabled: boolean
  onPick: () => void
}

/**
 * 월·연도 선택: 3열 × 4줄, 칸 h-13, rounded-btn.
 * 선택(지금 보는 날짜의 달·해) ink 면 + 흰 글자, 이번 달·올해 sky-100 면 + sky-800, 미래 gray-300 비활성. — 날짜 칸과 같은 규칙.
 */
function PickGrid({ label, items }: { label: string; items: PickItem[] }) {
  return (
    <div role="group" aria-label={label} className="grid grid-cols-3 gap-1.5 px-1 pt-2">
      {items.map((item) => (
        <button
          key={item.key}
          type="button"
          onClick={item.onPick}
          disabled={item.disabled}
          aria-pressed={item.selected}
          className={
            'h-13 rounded-btn text-body tabular-nums transition-colors disabled:cursor-default ' +
            (item.selected
              ? 'bg-ink font-bold text-white'
              : item.disabled
                ? 'text-gray-300'
                : item.current
                  ? 'bg-sky-100 font-bold text-sky-800'
                  : 'text-ink hover:bg-gray-100')
          }
        >
          {item.label}
        </button>
      ))}
    </div>
  )
}
