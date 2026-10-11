import type { ReactNode } from 'react'
import { Button } from './Button'
import { FieldHint } from './FieldHint'
import { Icon } from './Icon'
import { isFutureDate } from '../lib/date'

/** 기록 종류별 헤더 아이콘 박스 색 */
export type RecordCategory = 'meal' | 'bowel'

const CATEGORY: Record<RecordCategory, { icon: string; box: string }> = {
  meal: { icon: 'restaurant', box: 'bg-meal-bg text-meal' },
  bowel: { icon: 'water_drop', box: 'bg-bowel-bg text-bowel' },
}

interface RecordFormScreenProps {
  category: RecordCategory
  title: string
  onClose: () => void
  /** 헤더 오른쪽 (예: 수정 화면의 삭제 버튼) */
  action?: ReactNode
  children: ReactNode
}

/** 기록 입력 화면 틀 (배변 기록 시안): 헤더 56px(닫기 + 카테고리 아이콘 + 제목), border-b gray-100. 하단 탭 없음. */
export function RecordFormScreen({ category, title, onClose, action, children }: RecordFormScreenProps) {
  const { icon, box } = CATEGORY[category]
  return (
    <div className="mx-auto flex h-dvh w-full max-w-[430px] flex-col bg-white">
      <header className="flex h-14 flex-none items-center gap-1 border-b border-gray-100 px-2">
        <button
          type="button"
          onClick={onClose}
          aria-label="닫기"
          className="flex h-12 w-12 items-center justify-center rounded-input text-ink hover:bg-gray-50"
        >
          <Icon name="close" />
        </button>
        <span className={`flex h-7 w-7 items-center justify-center rounded-[8px] ${box}`}>
          <Icon name={icon} size={18} />
        </span>
        <h1 className="ml-1 text-[17px] font-bold">{title}</h1>
        {action}
      </header>
      {children}
    </div>
  )
}

/** 수정 화면 헤더 오른쪽의 삭제 버튼 */
export function DeleteAction({ onClick, disabled }: { onClick: () => void; disabled: boolean }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className="ml-auto h-12 rounded-input px-3 text-[15px] font-semibold text-danger hover:bg-danger-bg disabled:text-gray-400"
    >
      삭제
    </button>
  )
}

/** 수정 화면에서 기존 기록을 불러오는 중이거나 실패했을 때 */
export function FormLoadStatus({ error }: { error: string | null }) {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-4 px-6">
      {error === null ? (
        <p className="text-[15px] text-gray-500">불러오는 중…</p>
      ) : (
        <>
          <FieldHint tone="error">{error}</FieldHint>
          <Button variant="outline" onClick={() => window.location.reload()}>
            다시 시도
          </Button>
        </>
      )}
    </div>
  )
}

interface DateTimeFieldsProps {
  date: string
  time: string
  /** 이 날짜 이후는 고를 수 없다. (오늘) */
  maxDate: string
  onDateChange: (date: string) => void
  onTimeChange: (time: string) => void
}

/** "언제였나요?" — 2열(1.2fr / 1fr) 날짜·시간 입력. 미래 날짜면 오류 문구를 보여준다. */
export function DateTimeFields({ date, time, maxDate, onDateChange, onTimeChange }: DateTimeFieldsProps) {
  const dateError = isFutureDate(date, maxDate) ? FUTURE_DATE_MESSAGE : null
  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="mb-2 text-[15px] font-bold">언제였나요?</legend>
      <div className="grid grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)] gap-2">
        <PickerField icon="calendar_today" invalid={dateError !== null}>
          <input
            type="date"
            aria-label="날짜"
            value={date}
            max={maxDate}
            required
            onChange={(e) => onDateChange(e.target.value)}
            className="picker-input min-w-0 flex-1 bg-transparent text-[16px] font-semibold text-ink tabular-nums outline-none"
          />
        </PickerField>
        <PickerField icon="schedule">
          <input
            type="time"
            aria-label="시간"
            value={time}
            required
            onChange={(e) => onTimeChange(e.target.value)}
            className="picker-input min-w-0 flex-1 bg-transparent text-[16px] font-semibold text-ink tabular-nums outline-none"
          />
        </PickerField>
      </div>
      {dateError && <FieldHint tone="error">{dateError}</FieldHint>}
    </fieldset>
  )
}

const FUTURE_DATE_MESSAGE = '미래 날짜는 기록할 수 없어요'

/** 날짜·시간 입력 칸: h-14, 1.5px gray-200, rounded-input, 왼쪽 아이콘 20 gray-500 */
function PickerField({ icon, invalid = false, children }: { icon: string; invalid?: boolean; children: ReactNode }) {
  return (
    <label
      className={
        'relative flex h-14 items-center gap-2 rounded-input border-[1.5px] px-3.5 transition-[border-color,box-shadow] focus-within:ring-[3px] ' +
        (invalid ? 'border-danger focus-within:ring-danger-bg' : 'border-gray-200 focus-within:border-blue-600 focus-within:ring-blue-100')
      }
    >
      <Icon name={icon} size={20} className="text-gray-500" />
      {children}
    </label>
  )
}
