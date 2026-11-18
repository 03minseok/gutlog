import { useId } from 'react'
import { Icon } from '../../components/Icon'
import { SLEEP_MAX, SLEEP_MIN, SLEEP_OPTIONS, SLEEP_START, SLEEP_STEP, formatSleep } from './sleep'

interface SleepDurationPickerProps {
  label: string
  /** 분. 아직 고르지 않았으면 null */
  value: number | null
  onChange: (minutes: number) => void
  disabled?: boolean
}

const STEP_BUTTON =
  'flex h-12 w-12 flex-none items-center justify-center rounded-input border-[1.5px] border-gray-400 bg-white text-ink ' +
  'hover:border-ink focus-visible:ring-2 focus-visible:ring-sky-700 focus-visible:outline-none ' +
  'disabled:border-gray-200 disabled:text-gray-300 disabled:hover:border-gray-200'

/**
 * 수면 시간 고르기: [−] [7시간 30분 ⌄] [＋]. 30분 단위, 30분 ~ 24시간.
 * 가운데 칸을 누르면 목록(휴대폰에서는 돌림판)에서 바로 고르고, −/＋ 로 30분씩 바꾼다.
 * 처음엔 아무 값도 골라두지 않는다. 비어 있을 때 −/＋ 를 누르면 7시간부터 시작한다.
 */
export function SleepDurationPicker({ label, value, onChange, disabled = false }: SleepDurationPickerProps) {
  const labelId = useId()
  const step = (delta: number) => {
    if (value === null) return onChange(SLEEP_START)
    onChange(Math.min(SLEEP_MAX, Math.max(SLEEP_MIN, value + delta)))
  }

  return (
    <div className="flex flex-col gap-1.5">
      <span id={labelId} className="text-label font-semibold text-gray-700">
        {label}
      </span>
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={() => step(-SLEEP_STEP)}
          disabled={disabled || value === SLEEP_MIN}
          aria-label="30분 줄이기"
          className={STEP_BUTTON}
        >
          <Icon name="remove" size={20} />
        </button>
        <label className="relative flex h-12 min-w-0 flex-1 cursor-pointer items-center justify-center gap-1 rounded-input border-[1.5px] border-gray-400 bg-white px-3 transition-[border-color,box-shadow] focus-within:border-sky-700 focus-within:ring-[3px] focus-within:ring-sky-100">
          <span aria-hidden="true" className={`truncate text-body font-semibold tabular-nums ${value === null ? 'text-gray-450' : 'text-ink'}`}>
            {value === null ? '수면 시간 선택' : formatSleep(value)}
          </span>
          <Icon name="expand_more" size={18} className="flex-none text-gray-500" />
          <select
            aria-labelledby={labelId}
            value={value ?? ''}
            disabled={disabled}
            onChange={(e) => onChange(Number(e.target.value))}
            className="absolute inset-0 h-full w-full cursor-pointer opacity-0"
          >
            {value === null && (
              <option value="" disabled>
                수면 시간 선택
              </option>
            )}
            {SLEEP_OPTIONS.map((minutes) => (
              <option key={minutes} value={minutes}>
                {formatSleep(minutes)}
              </option>
            ))}
          </select>
        </label>
        <button
          type="button"
          onClick={() => step(SLEEP_STEP)}
          disabled={disabled || value === SLEEP_MAX}
          aria-label="30분 늘리기"
          className={STEP_BUTTON}
        >
          <Icon name="add" size={20} />
        </button>
      </div>
    </div>
  )
}
