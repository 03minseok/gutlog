import { useId } from 'react'
import { Icon } from '../../components/Icon'

interface BleedingChoiceProps {
  /** 아직 고르지 않았으면 null */
  value: boolean | null
  onChange: (value: boolean) => void
}

const OPTIONS = [
  { value: false, label: '없음' },
  { value: true, label: '있음' },
] as const

/**
 * 혈변 — 행 전체 카드(min-h 64, rounded-tile, 1.5px). 시안은 토글(기본 꺼짐)이지만,
 * "고르지 않음"을 표현하려고 오른쪽에 없음/있음 2칸 선택을 둔다. 기본값 없음.
 * 있음: danger-bg / danger-border, 아이콘·제목 danger.
 */
export function BleedingChoice({ value, onChange }: BleedingChoiceProps) {
  const titleId = useId()
  const bleeding = value === true
  const description = value === null ? '피가 보였는지 골라 주세요' : bleeding ? '피가 섞여 있었어요' : '피가 보이지 않았어요'

  return (
    <div
      className={
        'flex min-h-16 items-center justify-between gap-3 rounded-tile border-[1.5px] py-2.5 pr-2.5 pl-4 transition-colors ' +
        (bleeding ? 'border-danger-border bg-danger-bg' : 'border-gray-200 bg-white')
      }
    >
      <span className="flex min-w-0 items-center gap-2.5">
        <Icon name="emergency" size={22} className={`flex-none ${bleeding ? 'text-danger' : 'text-gray-300'}`} />
        <span className="flex min-w-0 flex-col gap-0.5">
          <span id={titleId} className={`text-[16px] font-bold ${bleeding ? 'text-danger' : 'text-ink'}`}>
            혈변
          </span>
          <span className="text-[13px] text-gray-500">{description}</span>
        </span>
      </span>

      <div role="radiogroup" aria-labelledby={titleId} className="flex flex-none gap-1 rounded-btn bg-gray-100 p-1">
        {OPTIONS.map((option) => {
          const selected = option.value === value
          const tone = !selected
            ? 'bg-transparent font-medium text-gray-700'
            : option.value
              ? 'bg-danger font-bold text-white shadow-seg'
              : 'bg-white font-bold text-blue-700 shadow-seg'
          return (
            <button
              key={option.label}
              type="button"
              role="radio"
              aria-checked={selected}
              onClick={() => onChange(option.value)}
              className={`h-10 w-[60px] rounded-[10px] text-[15px] transition-colors ${tone}`}
            >
              {option.label}
            </button>
          )
        })}
      </div>
    </div>
  )
}
