import { useId, useState, type ReactNode } from 'react'
import { Link } from 'react-router'
import { FieldHint } from '../../components/FieldHint'
import { Icon } from '../../components/Icon'
import { useRecordFormLinkState } from '../../components/recordForm'
import type { DayRecordsState } from './useDayRecords'

interface RecordSectionProps<T> {
  title: string
  icon: string
  /** 섹션 배경색 (예: 'bg-meal-bg'). 식사·복약·배변을 서로 다른 면으로 묶어 구분한다. */
  panel: string
  /** 아이콘 색 (예: 'text-meal'). 아이콘 박스는 배경 위에서 보이도록 흰 면이다. */
  iconColor: string
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
  /** 아이콘·이름을 누르면 갈 상세 화면 */
  detailTo?: string
  /** 이 개수까지만 보여주고 나머지는 "더보기"로 펼친다. (없으면 모두) */
  collapseAfter?: number
  getKey: (item: T) => number
  renderItem: (item: T) => ReactNode
}

/** 기록 탭의 하루치 섹션: 헤더(아이콘·제목·N회) + 로딩·오류(다시 시도)·빈 상태·카드 목록 */
export function RecordSection<T>({
  title,
  icon,
  panel,
  iconColor,
  emptyMessage,
  state,
  onRetry,
  badge,
  countLabel,
  action,
  detailTo,
  collapseAfter,
  getKey,
  renderItem,
}: RecordSectionProps<T>) {
  const titleId = useId()
  const listId = useId()
  const [expanded, setExpanded] = useState(false)
  return (
    <section className={`flex flex-col gap-3 rounded-card px-3 pt-2 pb-3 ${panel}`} aria-labelledby={titleId}>
      <div className="flex items-center gap-2 px-1">
        {/* 아이콘·이름을 누르면 이 기록의 상세 화면으로 간다. (상세 화면 안에서는 링크 없이) */}
        {detailTo ? (
          <Link
            to={detailTo}
            aria-label={`${title} 자세히 보기`}
            className="-my-2 -ml-1 flex h-11 items-center gap-2 rounded-input pr-0.5 pl-1"
          >
            <SectionTitle id={titleId} title={title} icon={icon} iconColor={iconColor} />
            <Icon name="chevron_right" size={18} className="-ml-1 text-gray-400" />
          </Link>
        ) : (
          <span className="-ml-1 flex items-center gap-2 pl-1">
            <SectionTitle id={titleId} title={title} icon={icon} iconColor={iconColor} />
          </span>
        )}
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
        <EmptyBox>{emptyMessage}</EmptyBox>
      )}

      {state.status === 'ready' && state.items.length > 0 && (
        <CollapsibleList
          id={listId}
          items={state.items}
          limit={collapseAfter}
          expanded={expanded}
          onToggle={() => setExpanded((v) => !v)}
          getKey={getKey}
          renderItem={renderItem}
        />
      )}
    </section>
  )
}

/** 섹션 제목: 28px 흰 아이콘 박스(카테고리색 아이콘) + 이름 16/700 */
function SectionTitle({ id, title, icon, iconColor }: { id: string; title: string; icon: string; iconColor: string }) {
  return (
    <>
      <span className={`flex h-7 w-7 items-center justify-center rounded-[8px] bg-white ${iconColor}`}>
        <Icon name={icon} size={16} />
      </span>
      <h2 id={id} className="text-section font-bold">
        {title}
      </h2>
    </>
  )
}

interface CollapsibleListProps<T> {
  id: string
  items: T[]
  limit?: number
  expanded: boolean
  onToggle: () => void
  getKey: (item: T) => number
  renderItem: (item: T) => ReactNode
}

/**
 * 카드 목록. limit보다 많으면 limit개만 보여주고 아래 "더보기 (N개)" 버튼으로 펼친다. 펼치면 "접기"로 다시 접는다.
 * 버튼: h-11, 흰 반투명 면, 13/600 gray-700 + expand_more/less 아이콘, aria-expanded.
 */
function CollapsibleList<T>({ id, items, limit, expanded, onToggle, getKey, renderItem }: CollapsibleListProps<T>) {
  const collapsible = limit !== undefined && items.length > limit
  const visible = collapsible && !expanded ? items.slice(0, limit) : items
  return (
    <>
      <ul id={id} className="flex flex-col gap-2">
        {visible.map((item) => (
          <li key={getKey(item)}>{renderItem(item)}</li>
        ))}
      </ul>
      {collapsible && (
        <button
          type="button"
          onClick={onToggle}
          aria-expanded={expanded}
          aria-controls={id}
          className="flex h-11 items-center justify-center gap-0.5 rounded-btn bg-white/70 text-label font-semibold text-gray-700"
        >
          {expanded ? '접기' : `더보기 (${items.length - limit}개)`}
          <Icon name={expanded ? 'expand_less' : 'expand_more'} size={18} />
        </button>
      )}
    </>
  )
}

/** 빈 상태 상자: 점선 테두리, 반투명 흰 면(섹션 배경 위), 가운데 정렬. 위아래 여백은 안의 내용(EmptyWithLink)이 정한다. */
export function EmptyBox({ children }: { children: ReactNode }) {
  return (
    <div className="rounded-card border border-dashed border-gray-300 bg-white/60 px-4 text-center text-body text-gray-500">{children}</div>
  )
}

/**
 * 빈 상태 두 줄: 안내 문구 + 아래 "＋ ○○ 기록하기" 링크 (섹션마다 같은 모양).
 * 링크는 누르는 영역이 44px이라 글자 아래위로 약 11px이 비어 있다.
 * 그래서 위 여백은 12px, 아래 여백은 2px만 둬서 보이는 여백(위·문구와 링크 사이·아래)이 모두 약 12px로 같게 한다.
 */
export function EmptyWithLink({ message, to, label }: { message: string; to: string; label: string }) {
  const formLinkState = useRecordFormLinkState()
  return (
    <div className="flex flex-col items-center pt-3 pb-0.5">
      <p>{message}</p>
      <Link to={to} state={formLinkState} className="flex h-11 items-center gap-1 rounded-input px-2 text-body font-semibold text-sky-700">
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
