import { Button } from './Button'
import { Icon } from './Icon'

interface NotFoundViewProps {
  title: string
  onBack: () => void
  backLabel?: string
}

/**
 * 404 안내. 남의 기록에 접근해도 백엔드는 404를 주므로(존재 여부 숨김)
 * "삭제됐거나 볼 수 없는 기록"으로 안내한다.
 */
export function NotFoundView({ title, onBack, backLabel = '목록으로' }: NotFoundViewProps) {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-4 px-6 py-10 text-center">
      <span className="flex h-16 w-16 items-center justify-center rounded-tile bg-gray-100 text-gray-500">
        <Icon name="search_off" size={32} />
      </span>
      <div className="flex flex-col gap-2">
        <h2 className="text-[20px] font-bold">{title}</h2>
        <p className="text-[15px] leading-[1.5] text-gray-700">삭제되었거나 볼 수 없는 기록이에요.</p>
      </div>
      <Button onClick={onBack} className="mt-2">
        {backLabel}
      </Button>
    </div>
  )
}
