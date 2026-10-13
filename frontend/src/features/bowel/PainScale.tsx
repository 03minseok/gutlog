import { useId } from 'react'
import { PAIN_MAX, PAIN_MIN, painLevelInfo } from './constants'

interface PainScaleProps {
  /** 아직 고르지 않았으면 null */
  value: number | null
  onChange: (value: number) => void
}

const LEVELS = Array.from({ length: PAIN_MAX - PAIN_MIN + 1 }, (_, i) => PAIN_MIN + i)

/**
 * 통증 정도 — 숫자 버튼 11개를 6열 두 줄(0–5, 6–10), h-11 rounded-input. 처음엔 선택 없음.
 * 비선택 gray-100 / gray-700, 선택 ink / 흰색 700. 빨강은 혈변 전용이라 통증 정도는 숫자 + 문구로만 알린다.
 */
export function PainScale({ value, onChange }: PainScaleProps) {
  const titleId = useId()
  const info = value === null ? null : painLevelInfo(value)

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-baseline justify-between">
        <span id={titleId} className="text-body font-bold">
          통증 정도
        </span>
        <span className="text-label font-semibold text-gray-700 tabular-nums" aria-live="polite">
          {info ? (
            <>
              <span className="text-section font-bold text-ink">{value}</span> / 10 · {info.label}
            </>
          ) : (
            <span className="font-medium text-gray-500">아직 고르지 않았어요</span>
          )}
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
                'h-11 rounded-input text-body tabular-nums transition-colors ' +
                (selected ? 'bg-ink font-bold text-white' : 'bg-gray-100 font-medium text-gray-700 hover:bg-gray-150')
              }
            >
              {level}
            </button>
          )
        })}
      </div>
      <div className="flex justify-between text-caption text-gray-500">
        <span>0 통증 없음</span>
        <span>10 참을 수 없음</span>
      </div>
    </div>
  )
}
