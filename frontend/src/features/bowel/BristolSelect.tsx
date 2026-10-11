import { useId } from 'react'
import type { BristolType } from '../../api/bowel'
import { Icon } from '../../components/Icon'
import { BRISTOL_OPTIONS } from './constants'

interface BristolSelectProps {
  value: BristolType | null
  onChange: (value: BristolType) => void
}

/**
 * 배변 형태 — 2열 타일(min-h 56, rounded-btn), 라벨 16 + 설명 12 gray-500, 선택 시 check.
 * 비선택: 1.5px gray-200 / 흰 배경, 선택: 2px blue-600 / blue-50 / blue-700 700.
 */
export function BristolSelect({ value, onChange }: BristolSelectProps) {
  const labelId = useId()
  return (
    <div className="flex flex-col gap-2">
      <span id={labelId} className="text-[15px] font-bold">
        배변 형태
      </span>
      <div role="radiogroup" aria-labelledby={labelId} className="grid grid-cols-2 gap-2">
        {BRISTOL_OPTIONS.map((option) => {
          const selected = option.value === value
          return (
            <button
              key={option.value}
              type="button"
              role="radio"
              aria-checked={selected}
              onClick={() => onChange(option.value)}
              className={
                'flex min-h-14 items-center justify-between gap-2 rounded-btn px-3.5 py-2.5 text-left transition-colors ' +
                (selected ? 'border-2 border-blue-600 bg-blue-50' : 'border-[1.5px] border-gray-200 bg-white')
              }
            >
              <span className="flex min-w-0 flex-col gap-0.5">
                <span className={`text-[16px] ${selected ? 'font-bold text-blue-700' : 'font-semibold text-ink'}`}>{option.type}</span>
                <span className="text-[12px] text-gray-500">{option.description}</span>
              </span>
              <Icon name="check" size={20} className={`flex-none text-blue-700 ${selected ? '' : 'opacity-0'}`} />
            </button>
          )
        })}
      </div>
    </div>
  )
}
