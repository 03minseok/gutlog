import { Icon } from './Icon'

interface SelectTileProps {
  title: string
  description?: string
  selected: boolean
  onSelect: () => void
}

/**
 * 큰 선택 타일 (min-h 88, rounded-tile).
 * 비선택: 1.5px gray-200 / 흰 배경, 선택: 2px blue-600 / blue-50 배경 / blue-700 글자 + check_circle.
 * 라디오 그룹 안에서 쓴다.
 */
export function SelectTile({ title, description, selected, onSelect }: SelectTileProps) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      onClick={onSelect}
      className={
        'flex min-h-[88px] flex-col justify-between gap-1.5 rounded-tile px-4 py-3.5 text-left transition-colors ' +
        (selected ? 'border-2 border-blue-600 bg-blue-50' : 'border-[1.5px] border-gray-200 bg-white')
      }
    >
      <span className="flex w-full items-center justify-between gap-2">
        <span className={`text-[18px] font-bold ${selected ? 'text-blue-700' : 'text-ink'}`}>{title}</span>
        <Icon
          name={selected ? 'check_circle' : 'radio_button_unchecked'}
          size={22}
          className={selected ? 'text-blue-700' : 'text-ink'}
        />
      </span>
      {description && <span className="text-[14px] text-gray-700">{description}</span>}
    </button>
  )
}
