import { Icon } from './Icon'

interface SelectTileProps {
  title: string
  description?: string
  selected: boolean
  onSelect: () => void
}

/**
 * 선택 타일 (min-h 64, rounded-tile). 라디오 그룹 안에서 쓴다.
 * 비선택: 1.5px gray-300 / 흰 배경 / 빈 원 gray-400.
 * 선택: 1.5px sky-700 / sky-100 배경 / check_circle sky-700 + 굵은 글자 — 색만으로 구분하지 않도록 아이콘을 바꾼다.
 */
export function SelectTile({ title, description, selected, onSelect }: SelectTileProps) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      onClick={onSelect}
      className={
        'flex min-h-16 flex-col justify-center gap-0.5 rounded-tile border-[1.5px] px-3.5 py-3 text-left transition-colors ' +
        (selected ? 'border-sky-700 bg-sky-100' : 'border-gray-300 bg-white hover:border-gray-400')
      }
    >
      <span className="flex w-full items-center justify-between gap-2">
        <span className={`text-body ${selected ? 'font-bold' : 'font-semibold'} text-ink`}>{title}</span>
        <Icon
          name={selected ? 'check_circle' : 'radio_button_unchecked'}
          size={20}
          filled={selected}
          className={selected ? 'text-sky-700' : 'text-gray-400'}
        />
      </span>
      {description && <span className="text-caption text-gray-500">{description}</span>}
    </button>
  )
}
