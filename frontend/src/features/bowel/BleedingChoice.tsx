import { useId } from 'react'
import { Icon } from '../../components/Icon'

interface BleedingChoiceProps {
  /** 아직 고르지 않았으면 null */
  value: boolean | null
  onChange: (value: boolean) => void
}

const OPTIONS = [
  { value: false, label: '없음', note: '보이지 않았어요', icon: 'do_not_disturb_on' },
  { value: true, label: '있음', note: '피가 섞여 있었어요', icon: 'water_drop' },
] as const

/**
 * 혈변 — "없음 / 있음" 2칸 큰 선택(min-h 56). 기본값 없음: 고르지 않으면 저장할 수 없다.
 * 없음 선택: sky-100 면 + sky-700 테두리 + check_circle.
 * 있음 선택: blood-bg 면 + blood 테두리 + 채운 물방울 + blood 글자. (빨강은 혈변에만 쓴다)
 * 응급 기호는 쓰지 않는다 — 눈에 띄되 불안을 키우지 않게.
 */
export function BleedingChoice({ value, onChange }: BleedingChoiceProps) {
  const titleId = useId()

  return (
    <div className="flex flex-col gap-2">
      <span id={titleId} className="text-body font-bold">
        혈변이 있었나요?
      </span>
      <div role="radiogroup" aria-labelledby={titleId} className="grid grid-cols-2 gap-2">
        {OPTIONS.map((option) => {
          const selected = option.value === value
          const tone = !selected
            ? 'border-gray-300 bg-white hover:border-gray-400'
            : option.value
              ? 'border-blood bg-blood-bg'
              : 'border-sky-700 bg-sky-100'
          const iconName = selected && !option.value ? 'check_circle' : option.icon
          const iconColor = !selected ? 'text-gray-400' : option.value ? 'text-blood' : 'text-sky-700'
          return (
            <button
              key={option.label}
              type="button"
              role="radio"
              aria-checked={selected}
              onClick={() => onChange(option.value)}
              className={`flex min-h-14 items-center gap-2.5 rounded-tile border-[1.5px] px-3 text-left transition-colors ${tone}`}
            >
              <Icon name={iconName} size={22} filled={selected} className={`flex-none ${iconColor}`} />
              <span className="flex min-w-0 flex-col">
                <span className={`text-body font-bold ${selected && option.value ? 'text-blood' : 'text-ink'}`}>{option.label}</span>
                <span className="text-caption text-gray-700">{option.note}</span>
              </span>
            </button>
          )
        })}
      </div>
    </div>
  )
}
