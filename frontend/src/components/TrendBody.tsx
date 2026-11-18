import type { ReactNode } from 'react'
import { Icon } from './Icon'

/** 추이 그래프 자리의 상태: 불러오는 중 / 오류 / 기록 없음 / 그래프 (체중·수면) */
export function TrendBody({
  status,
  count,
  message,
  onRetry,
  emptyText,
  children,
}: {
  status: 'loading' | 'ready' | 'error'
  count: number
  message: string | null
  onRetry: () => void
  emptyText: string
  children: ReactNode
}) {
  if (status === 'loading' && count === 0) return <div aria-hidden="true" className="h-36 animate-pulse rounded-input bg-gray-100" />
  if (status === 'error') {
    return (
      <div className="flex items-center gap-2 rounded-input bg-error-bg py-1 pr-1 pl-3">
        <Icon name="error" size={16} filled className="flex-none text-error" />
        <span className="flex-1 text-label font-medium text-error">{message}</span>
        <button type="button" onClick={onRetry} className="flex h-11 items-center gap-1 px-2 text-label font-semibold text-gray-700">
          <Icon name="refresh" size={16} />
          다시 시도
        </button>
      </div>
    )
  }
  if (count === 0) {
    return <p className="rounded-input border border-dashed border-gray-300 px-3 py-4 text-center text-body text-gray-500">{emptyText}</p>
  }
  return <>{children}</>
}
