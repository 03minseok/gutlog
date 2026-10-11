import { useId } from 'react'
import { PAIN_MAX, PAIN_MIN, painLevelInfo } from './constants'

interface PainScaleProps {
  /** 아직 고르지 않았으면 null */
  value: number | null
  onChange: (value: number) => void
}

const LEVELS = Array.from({ length: PAIN_MAX - PAIN_MIN + 1 }, (_, i) => PAIN_MIN + i)

/**
 * 통증 정도 — 숫자 버튼 11개를 6열 두 줄(0–5, 6–10), h-12 rounded-input.
 * 비선택 gray-100 / gray-700, 선택 blue-600 / 흰색 700. 시안은 기본값 0이지만 처음엔 선택 없음.
 */
export function PainScale({ value, onChange }: PainScaleProps) {
  const titleId = useId()
  const info = value === null ? null : painLevelInfo(value)

  return (
    <div className="flex flex-col gap-2.5">
      <div className="flex items-baseline justify-between">
        <span id={titleId} className="text-[15px] font-bold">
          통증 정도
        </span>
        <span className={`text-[15px] font-semibold ${info ? info.color : 'text-gray-400'}`} aria-live="polite">
          <span className="text-[24px] font-bold tabular-nums">{value ?? '–'}</span> / 10{info && ` · ${info.label}`}
        </span>
      </div>
      <div role="radiogroup" aria-labelledby={titleId} className="grid grid-cols-6 gap-1.5">
        {LEVELS.map((level) => {
          const selected = level === value
          return (
            <button
              key={level}
              type="button"
              role="radio"
              aria-checked={selected}
              aria-label={`${level}점`}
              onClick={() => onChange(level)}
              className={
                'h-12 rounded-input text-[17px] tabular-nums transition-colors ' +
                (selected ? 'bg-blue-600 font-bold text-white' : 'bg-gray-100 font-medium text-gray-700')
              }
            >
              {level}
            </button>
          )
        })}
      </div>
      <div className="flex justify-between text-[13px] text-gray-500">
        <span>0 통증 없음</span>
        <span>10 참을 수 없음</span>
      </div>
    </div>
  )
}
