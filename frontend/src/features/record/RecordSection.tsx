import { useId, type ReactNode } from 'react'
import { Link } from 'react-router'
import { FieldHint } from '../../components/FieldHint'
import { Icon } from '../../components/Icon'
import { useRecordFormLinkState } from '../../components/recordForm'
import type { DayRecordsState } from './useDayRecords'

interface RecordSectionProps<T> {
  title: string
  icon: string
  /** 아이콘 박스 색 (예: 'bg-meal-bg text-meal') */
  iconBox: string
  /** 빈 상태 안내 (문구 또는 안내 + 링크) */
  emptyMessage: ReactNode
  state: DayRecordsState<T>
  onRetry: () => void
  /** 헤더의 "N회" 오른쪽에 붙는 요소 (예: 혈변 배지) */
  badge?: ReactNode
  /** 헤더 숫자를 바꾼다. 기본은 "N회" (예: 복약은 "2 / 3") */
  countLabel?: string
  /** 헤더 오른쪽 끝 (예: "내 약 관리" 링크) */
  action?: ReactNode
  getKey: (item: T) => number
  renderItem: (item: T) => ReactNode
}

/** 기록 탭의 하루치 섹션: 헤더(아이콘·제목·N회) + 로딩·오류(다시 시도)·빈 상태·카드 목록 */
export function RecordSection<T>({
  title,
  icon,
  iconBox,
  emptyMessage,
  state,
  onRetry,
  badge,
  countLabel,
  action,
  getKey,
  renderItem,
}: RecordSectionProps<T>) {
  const titleId = useId()
  return (
    <section className="flex flex-col gap-3" aria-labelledby={titleId}>
      <div className="flex items-center gap-2 px-1">
        <span className={`flex h-7 w-7 items-center justify-center rounded-[8px] ${iconBox}`}>
          <Icon name={icon} size={16} />
        </span>
        <h2 id={titleId} className="text-section font-bold">
          {title}
        </h2>
        {state.status === 'ready' && (
          <span className="text-label font-medium text-gray-500 tabular-nums">{countLabel ?? `${state.items.length}회`}</span>
        )}
        {state.status === 'ready' && badge}
        {action && <span className="ml-auto">{action}</span>}
      </div>

      {/* 불러오는 중: 카드 모양 자리만 잡아 두어 데이터가 와도 화면이 덜 출렁이게 한다. */}
      {state.status === 'loading' && (
        <div role="status" aria-label="불러오는 중" className="flex h-16 items-center gap-3 rounded-card border border-gray-200 bg-white px-4">
          <span className="h-4 w-12 animate-pulse rounded-input bg-gray-100" />
          <span className="h-4 flex-1 animate-pulse rounded-input bg-gray-100" />
        </div>
      )}

      {state.status === 'error' && (
        <div className="flex min-h-14 items-center justify-between gap-2 rounded-card border border-error-border bg-error-bg py-1.5 pr-1.5 pl-4">
          <FieldHint tone="error">{state.message}</FieldHint>
          <button
            type="button"
            onClick={onRetry}
            className="flex h-11 flex-none items-center gap-1 rounded-input px-3 text-label font-semibold text-ink hover:bg-white/70"
          >
            <Icon name="refresh" size={18} />
            다시 시도
          </button>
        </div>
      )}

      {state.status === 'ready' && state.items.length === 0 && (
        <div className="rounded-card border border-dashed border-gray-300 px-4 py-5 text-center text-body text-gray-500">{emptyMessage}</div>
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

/** 빈 상태 안내 + 바로 기록 링크 (섹션마다 같은 모양) */
export function EmptyWithLink({ message, to, label }: { message: string; to: string; label: string }) {
  const formLinkState = useRecordFormLinkState()
  return (
    <div className="flex flex-col items-center gap-1">
      <p>{message}</p>
      <Link to={to} state={formLinkState} className="inline-flex min-h-11 items-center gap-1 px-2 text-body font-semibold text-sky-700 hover:text-sky-800">
        <Icon name="add" size={18} />
        {label}
      </Link>
    </div>
  )
}

/** 섹션 헤더 오른쪽 "＋ 추가" 링크. 기록이 이미 있는 날에도 새 기록을 바로 쓸 수 있게 한다. */
export function AddRecordLink({ to, label }: { to: string; label: string }) {
  const formLinkState = useRecordFormLinkState()
  return (
    <Link
      to={to}
      state={formLinkState}
      aria-label={`${label} 추가`}
      className="-mr-1 flex h-11 items-center gap-0.5 rounded-input px-2 text-label font-semibold text-sky-700"
    >
      <Icon name="add" size={18} />
      추가
    </Link>
  )
}
