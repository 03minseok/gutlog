import { useState, type ReactNode } from 'react'
import { Link } from 'react-router'
import { Icon } from '../../components/Icon'
import { addDays, todayParam } from '../../lib/date'
import { WEIGHT_PATH } from './constants'
import { useWeightRecords } from './useWeightRecords'
import { WeightChart } from './WeightChart'
import { WeightEntryForm } from './WeightEntryForm'

/**
 * 마이페이지 체중 카드: 오늘 체중 입력(오늘 날짜로 저장) + 최근 30일 추이 그래프.
 * 과거 날짜 등록·수정·삭제와 기간 바꾸기는 "기록 관리" 화면에서 한다.
 */
export function WeightCard() {
  const [today] = useState(todayParam)
  const from = addDays(today, -29)
  const trend = useWeightRecords(from, today)

  return (
    <section aria-labelledby="weight-card-title" className="flex flex-col gap-4 rounded-card border border-gray-200 bg-white p-4">
      <div className="-my-2 flex items-center gap-2">
        <Icon name="monitor_weight" size={22} className="text-gray-700" />
        <h2 id="weight-card-title" className="flex-1 text-section font-bold">
          체중
        </h2>
        <Link to={WEIGHT_PATH} className="-mr-1 flex h-11 items-center px-1 text-label font-semibold text-sky-700 hover:text-sky-800">
          기록 관리
          <Icon name="chevron_right" size={18} />
        </Link>
      </div>

      <WeightEntryForm date={today} today={today} onSaved={trend.reload} />

      <div className="flex flex-col gap-2 border-t border-gray-200 pt-3">
        <p className="text-label font-semibold text-gray-700">최근 30일 추이</p>
        <TrendBody status={trend.status} count={trend.records.length} message={trend.message} onRetry={trend.reload}>
          <WeightChart records={trend.records} from={from} to={today} height={140} />
        </TrendBody>
      </div>
    </section>
  )
}

/** 그래프 자리의 상태: 불러오는 중 / 오류 / 기록 없음 / 그래프 */
export function TrendBody({
  status,
  count,
  message,
  onRetry,
  emptyText = '이 기간 체중 기록이 없어요',
  children,
}: {
  status: 'loading' | 'ready' | 'error'
  count: number
  message: string | null
  onRetry: () => void
  emptyText?: string
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
