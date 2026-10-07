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
 * 트랙 gray-100, p-1, rounded-btn, gap-1. 항목 h-12, rounded-[10px].
 * 선택: 흰 배경 + blue-700 700 + shadow-seg / 비선택: gray-700 500.
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
              'h-12 rounded-[10px] text-[16px] transition-colors ' +
              (selected ? 'bg-white font-bold text-blue-700 shadow-seg' : 'bg-transparent font-medium text-gray-700')
            }
          >
            {option.label}
          </button>
        )
      })}
    </div>
  )
}
