import { useRef } from 'react'
import { Icon } from './Icon'
import { addDays, formatDateLabel, relativeDayLabel } from '../lib/date'

interface DateNavigatorProps {
  /** 'yyyy-MM-dd' */
  date: string
  /** 이 날짜 이후로는 이동할 수 없다. (오늘) */
  max: string
  onChange: (date: string) => void
}

/** ‹ 10월 8일 목요일 · 오늘 › — 가운데를 누르면 날짜 선택기가 열린다. */
export function DateNavigator({ date, max, onChange }: DateNavigatorProps) {
  const pickerRef = useRef<HTMLInputElement>(null)
  const relative = relativeDayLabel(date, max)
  const canGoNext = date < max

  function openPicker() {
    const input = pickerRef.current
    if (!input) return
    try {
      input.showPicker()
    } catch {
      input.focus() // showPicker를 지원하지 않는 브라우저
    }
  }

  return (
    <div className="flex items-center justify-between rounded-tile border border-gray-200 bg-white p-1">
      <button
        type="button"
        onClick={() => onChange(addDays(date, -1))}
        aria-label="이전 날"
        className="flex h-11 w-11 items-center justify-center rounded-input text-gray-700 hover:bg-gray-100"
      >
        <Icon name="chevron_left" />
      </button>

      <div className="relative">
        <button
          type="button"
          onClick={openPicker}
          className="flex h-11 items-center gap-2 rounded-input px-3 hover:bg-gray-100"
          aria-label={`${formatDateLabel(date)}, 날짜 선택`}
        >
          <span className="text-section font-bold tabular-nums">{formatDateLabel(date)}</span>
          {relative && (
            <span className="rounded-full bg-sky-100 px-2 py-0.5 text-caption font-semibold text-sky-800">{relative}</span>
          )}
        </button>
        <input
          ref={pickerRef}
          type="date"
          value={date}
          max={max}
          onChange={(e) => e.target.value && onChange(e.target.value)}
          tabIndex={-1}
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 opacity-0"
        />
      </div>

      <button
        type="button"
        onClick={() => onChange(addDays(date, 1))}
        disabled={!canGoNext}
        aria-label="다음 날"
        className="flex h-11 w-11 items-center justify-center rounded-input text-gray-700 hover:bg-gray-100 disabled:text-gray-400 disabled:hover:bg-transparent"
      >
        <Icon name="chevron_right" />
      </button>
    </div>
  )
}
