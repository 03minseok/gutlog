import type { ReactNode } from 'react'
import { Button } from './Button'
import { FieldHint } from './FieldHint'
import { Icon } from './Icon'
import { formatDateWithDay, isFutureDate } from '../lib/date'

/** 기록 종류별 헤더 아이콘 */
export type RecordCategory = 'meal' | 'bowel' | 'medication'

const CATEGORY: Record<RecordCategory, { icon: string; color: string }> = {
  meal: { icon: 'restaurant', color: 'text-meal' },
  bowel: { icon: 'waves', color: 'text-bowel' },
  medication: { icon: 'medication', color: 'text-med' },
}

interface RecordFormScreenProps {
  category: RecordCategory
  title: string
  onClose: () => void
  /** 헤더 오른쪽 (예: 수정 화면의 삭제 버튼) */
  action?: ReactNode
  children: ReactNode
}

/**
 * 기록 입력 화면 틀 (전체 화면). 아래에 하단 탭이 함께 보인다. (layouts/TabLayout.tsx RecordFormLayout)
 * 헤더 52px(border-b gray-200): 닫기 + 카테고리 아이콘 + 제목, 수정 화면이면 오른쪽에 삭제.
 * 닫기는 보던 화면으로 돌아간다. (components/recordForm.ts)
 */
export function RecordFormScreen({ category, title, onClose, action, children }: RecordFormScreenProps) {
  const { icon, color } = CATEGORY[category]
  return (
    <div className="flex min-h-0 flex-1 flex-col bg-white">
      <header className="flex h-13 flex-none items-center gap-1 border-b border-gray-200 px-1.5">
        <button
          type="button"
          onClick={onClose}
          aria-label="닫기"
          className="flex h-11 w-11 items-center justify-center rounded-input text-ink hover:bg-gray-100"
        >
          <Icon name="close" size={22} />
        </button>
        <Icon name={icon} size={20} className={color} />
        <h1 className="ml-1 text-section font-bold">{title}</h1>
        {action}
      </header>
      {children}
    </div>
  )
}

/** 수정 화면 헤더 오른쪽의 삭제 버튼. 빨강 대신 휴지통 아이콘 + 문구로 구분하고, 누르면 확인창을 띄운다. */
export function DeleteAction({ onClick, disabled }: { onClick: () => void; disabled: boolean }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className="ml-auto flex h-11 items-center gap-1 rounded-input px-3 text-body font-semibold text-gray-700 hover:bg-gray-100 disabled:text-gray-400"
    >
      <Icon name="delete" size={20} />
      삭제
    </button>
  )
}

/** 수정 화면에서 기존 기록을 불러오는 중이거나 실패했을 때 */
export function FormLoadStatus({ error }: { error: string | null }) {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-4 px-4">
      {error === null ? (
        <p className="text-body text-gray-500" role="status">
          불러오는 중…
        </p>
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

/**
 * "언제였나요?" — 2열(1.25fr / 1fr) 날짜·시간 입력. 미래 날짜면 오류 문구를 보여준다.
 * 화면에는 "10월 13일 (화)", "13:18"처럼 앱의 다른 화면과 같은 형식으로 보여주고,
 * 실제 입력은 투명하게 겹친 네이티브 선택기가 받는다.
 */
export function DateTimeFields({ date, time, maxDate, onDateChange, onTimeChange }: DateTimeFieldsProps) {
  const dateError = isFutureDate(date, maxDate) ? FUTURE_DATE_MESSAGE : null
  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="mb-2 text-body font-bold">언제였나요?</legend>
      <div className="grid grid-cols-[minmax(0,1.25fr)_minmax(0,1fr)] gap-2">
        <PickerField icon="calendar_today" display={date ? formatDateWithDay(date) : '날짜 선택'} invalid={dateError !== null}>
          <input
            type="date"
            aria-label="날짜"
            value={date}
            max={maxDate}
            required
            onChange={(e) => onDateChange(e.target.value)}
            className="picker-input absolute inset-0 h-full w-full opacity-0"
          />
        </PickerField>
        <PickerField icon="schedule" display={time || '시간 선택'}>
          <input
            type="time"
            aria-label="시간"
            value={time}
            required
            onChange={(e) => onTimeChange(e.target.value)}
            className="picker-input absolute inset-0 h-full w-full opacity-0"
          />
        </PickerField>
      </div>
      {dateError && <FieldHint tone="error">{dateError}</FieldHint>}
    </fieldset>
  )
}

const FUTURE_DATE_MESSAGE = '미래 날짜는 기록할 수 없어요'

/** 날짜·시간 입력 칸: h-12, 1.5px gray-400, rounded-input, 왼쪽 아이콘 18 gray-500 + 표시 글자 15/600 */
function PickerField({ icon, display, invalid = false, children }: { icon: string; display: string; invalid?: boolean; children: ReactNode }) {
  return (
    <label
      className={
        'relative flex h-12 cursor-pointer items-center gap-2 rounded-input border-[1.5px] bg-white px-3 transition-[border-color,box-shadow] focus-within:ring-[3px] ' +
        (invalid ? 'border-error focus-within:ring-error-bg' : 'border-gray-400 focus-within:border-sky-700 focus-within:ring-sky-100')
      }
    >
      <Icon name={icon} size={18} className="flex-none text-gray-500" />
      <span aria-hidden="true" className="truncate text-body font-semibold text-ink tabular-nums">
        {display}
      </span>
      {children}
    </label>
  )
}
