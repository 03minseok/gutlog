interface Option<T extends string> {
  value: T
  label: string
}

interface SegmentedControlProps<T extends string> {
  label: string
  options: readonly Option<T>[]
  value: T | null
  onChange: (value: T) => void
}

/**
 * 트랙 gray-100, p-1, rounded-btn, gap-1. 항목 h-11, rounded-input.
 * 선택: 흰 배경 + ink 700 + shadow-seg / 비선택: gray-500 500.
 */
export function SegmentedControl<T extends string>({ label, options, value, onChange }: SegmentedControlProps<T>) {
  return (
    <div
      role="radiogroup"
      aria-label={label}
      className="grid gap-1 rounded-btn bg-gray-100 p-1"
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
