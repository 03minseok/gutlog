import { useId } from 'react'
import type { BristolType } from '../../api/bowel'
import { Icon } from '../../components/Icon'
import { BRISTOL_OPTIONS } from './constants'

interface BristolSelectProps {
  value: BristolType | null
  onChange: (value: BristolType) => void
}

/**
 * 배변 형태 — 1형부터 7형까지 한 줄씩 쌓은 목록(한 카드 안, 줄 사이 구분선). 설명이 늘 보여 고르기 전에도 차이를 알 수 있다.
 * 줄: min-h 44, "n형" 600 + 설명 + 오른쪽 라디오 아이콘.
 * 선택: sky-100 면 + "n형" 700 + check_circle(sky-700) — 색만으로 구분하지 않도록 아이콘도 바꾼다.
 */
export function BristolSelect({ value, onChange }: BristolSelectProps) {
  const labelId = useId()
  return (
    <div className="flex flex-col gap-2">
      <span id={labelId} className="text-body font-bold">
        배변 형태 <span className="ml-1 text-caption font-medium text-gray-500">브리스톨 척도</span>
      </span>
      <div role="radiogroup" aria-labelledby={labelId} className="overflow-hidden rounded-card border border-gray-300 bg-white">
        {BRISTOL_OPTIONS.map((option, index) => {
          const selected = option.value === value
          return (
            <button
              key={option.value}
              type="button"
              role="radio"
              aria-checked={selected}
              onClick={() => onChange(option.value)}
              className={
                'flex min-h-11 w-full items-center gap-2.5 px-3.5 py-1.5 text-left transition-colors ' +
                (index > 0 ? 'border-t border-gray-200 ' : '') +
                (selected ? 'bg-sky-100' : 'hover:bg-gray-50')
              }
            >
              <span className="min-w-0 flex-1 text-body">
                <span className={selected ? 'font-bold' : 'font-semibold'}>{option.type}</span>
                <span className="text-gray-700"> · {option.description}</span>
              </span>
              <Icon
                name={selected ? 'check_circle' : 'radio_button_unchecked'}
                size={20}
                filled={selected}
                className={`flex-none ${selected ? 'text-sky-700' : 'text-gray-400'}`}
              />
            </button>
          )
        })}
      </div>
    </div>
  )
}
