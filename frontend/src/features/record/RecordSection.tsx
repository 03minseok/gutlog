import { useId, type ReactNode } from 'react'
import { Button } from '../../components/Button'
import { FieldHint } from '../../components/FieldHint'
import { Icon } from '../../components/Icon'
import type { DayRecordsState } from './useDayRecords'

interface RecordSectionProps<T> {
  title: string
  icon: string
  /** 아이콘 박스 색 (예: 'bg-meal-bg text-meal') */
  iconBox: string
  emptyMessage: string
  state: DayRecordsState<T>
  onRetry: () => void
  /** 헤더의 "N회" 오른쪽에 붙는 요소 (예: 혈변 배지) */
  badge?: ReactNode
  getKey: (item: T) => number
  renderItem: (item: T) => ReactNode
}

/** 기록 탭의 하루치 섹션: 헤더(아이콘·제목·N회) + 로딩·오류(다시 시도)·빈 상태·카드 목록 */
export function RecordSection<T>({ title, icon, iconBox, emptyMessage, state, onRetry, badge, getKey, renderItem }: RecordSectionProps<T>) {
  const titleId = useId()
  return (
    <section className="flex flex-col gap-3" aria-labelledby={titleId}>
      <div className="flex items-center gap-2 px-1">
        <span className={`flex h-[30px] w-[30px] items-center justify-center rounded-[9px] ${iconBox}`}>
          <Icon name={icon} size={18} />
        </span>
        <h2 id={titleId} className="text-[17px] font-bold">
          {title}
        </h2>
        {state.status === 'ready' && (
          <span className="text-[15px] font-medium text-gray-500 tabular-nums">{state.items.length}회</span>
        )}
        {state.status === 'ready' && badge}
      </div>

      {state.status === 'loading' && <p className="px-1 py-6 text-center text-[15px] text-gray-500">불러오는 중…</p>}

      {state.status === 'error' && (
        <div className="flex flex-col items-start gap-3 rounded-card border border-gray-200 bg-white p-5">
          <FieldHint tone="error">{state.message}</FieldHint>
          <Button variant="outline" onClick={onRetry}>
            다시 시도
          </Button>
        </div>
      )}

      {state.status === 'ready' && state.items.length === 0 && (
        <p className="rounded-card border border-gray-200 bg-white px-5 py-8 text-center text-[15px] text-gray-500">{emptyMessage}</p>
      )}

      {state.status === 'ready' && state.items.length > 0 && (
        <ul className="flex flex-col gap-2">
          {state.items.map((item) => (
            <li key={getKey(item)}>{renderItem(item)}</li>
          ))}
        </ul>
      )}
    </section>
  )
}
