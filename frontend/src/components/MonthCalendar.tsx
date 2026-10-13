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

/** 'yyyy-MM-dd' → 그 달 1일의 Date */
function monthStartOf(dateParam: string): Date {
  const date = parseDateParam(dateParam) ?? new Date()
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

/**
 * 한 달 달력. 날짜를 누르면 그날 기록으로 이동한다.
 * - 일요일 시작 7열. 날짜 칸 h-11(44px).
 * - 선택한 날: ink 원 + 흰 글자 700, 오늘: sky-700 글자 700 + 아래 점, 미래: 비활성(gray-300).
 * - ‹ › 로 달을 옮긴다. 다음 달은 오늘이 있는 달까지만.
 * - 바깥에서 날짜가 바뀌어 다른 달이 되면(이전·다음 날 버튼 등) 그 달로 따라간다.
 */
export function MonthCalendar({ date, max, onChange }: MonthCalendarProps) {
  // 보고 있는 달. 선택한 날짜가 바뀌면 그 날짜의 달로 맞춘다. (렌더 중 상태 조정)
  const [view, setView] = useState(() => ({ forDate: date, month: monthStartOf(date) }))
  let month = view.month
  if (view.forDate !== date) {
    month = monthStartOf(date)
    setView({ forDate: date, month })
  }

  const year = month.getFullYear()
  const monthIndex = month.getMonth()
  const daysInMonth = new Date(year, monthIndex + 1, 0).getDate()
  const leadingBlanks = month.getDay()
  const maxMonth = monthStartOf(max)
  const canGoNext = month < maxMonth

  const moveMonth = (offset: number) => setView({ forDate: date, month: new Date(year, monthIndex + offset, 1) })

  const cells: (string | null)[] = [
    ...Array.from({ length: leadingBlanks }, () => null),
    ...Array.from({ length: daysInMonth }, (_, i) => toDateParam(new Date(year, monthIndex, i + 1))),
  ]

  return (
    <div className="rounded-card border border-gray-200 bg-white px-2 pt-1 pb-2">
      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={() => moveMonth(-1)}
          aria-label="이전 달"
          className="flex h-11 w-11 items-center justify-center rounded-input text-gray-700 hover:bg-gray-100"
        >
          <Icon name="chevron_left" size={20} />
        </button>
        <h2 className="text-body font-bold tabular-nums" aria-live="polite">
          {year}년 {monthIndex + 1}월
        </h2>
        <button
          type="button"
          onClick={() => moveMonth(1)}
          disabled={!canGoNext}
          aria-label="다음 달"
          className="flex h-11 w-11 items-center justify-center rounded-input text-gray-700 hover:bg-gray-100 disabled:text-gray-400 disabled:hover:bg-transparent"
        >
          <Icon name="chevron_right" size={20} />
        </button>
      </div>

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
                  'relative flex h-9 w-9 items-center justify-center rounded-full text-body tabular-nums transition-colors ' +
                  (selected
                    ? 'bg-ink font-bold text-white'
                    : future
                      ? 'text-gray-300'
                      : isToday
                        ? 'font-bold text-sky-700 group-hover:bg-sky-50'
                        : 'text-ink group-hover:bg-gray-100')
                }
              >
                {day}
                {isToday && !selected && <span className="absolute bottom-1 h-1 w-1 rounded-full bg-sky-700" />}
              </span>
            </button>
          )
        })}
      </div>
    </div>
  )
}
