interface Option<T extends string> {
  value: T
  label: string
}

interface SegmentedControlProps<T extends string> {
  label: string
  options: readonly Option<T>[]
  value: T | null
  onChange: (value: T) => void
  /** 트랙 배경 (기본 gray-100). 카테고리 색 면 위에서는 'bg-white/70' */
  track?: string
}

/**
 * 트랙 gray-100, p-1, rounded-btn, gap-1. 항목 h-11, rounded-input.
 * 선택: 흰 배경 + ink 700 + shadow-seg / 비선택: gray-500 500.
 */
export function SegmentedControl<T extends string>({ label, options, value, onChange, track = 'bg-gray-100' }: SegmentedControlProps<T>) {
  return (
    <div
      role="radiogroup"
      aria-label={label}
      className={`grid gap-1 rounded-btn p-1 ${track}`}
      style={{ gridTemplateColumns: `repeat(${options.length}, minmax(0, 1fr))` }}
    >
      {options.map((option) => {
        const selected = option.value === value
        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={selected}
            onClick={() => onChange(option.value)}
            className={
              'h-11 rounded-input text-body transition-colors ' +
              (selected ? 'bg-white font-bold text-ink shadow-seg' : 'bg-transparent font-medium text-gray-500 hover:text-ink')
            }
          >
            {option.label}
          </button>
        )
      })}
    </div>
  )
}
