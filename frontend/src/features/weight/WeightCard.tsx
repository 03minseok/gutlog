import { useState } from 'react'
import { Link } from 'react-router'
import { Icon } from '../../components/Icon'
import { TrendBody } from '../../components/TrendBody'
import { addDays, todayParam } from '../../lib/date'
import { WEIGHT_EMPTY_TEXT, WEIGHT_PATH } from './constants'
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
        <TrendBody status={trend.status} count={trend.records.length} message={trend.message} onRetry={trend.reload} emptyText={WEIGHT_EMPTY_TEXT}>
          <WeightChart records={trend.records} from={from} to={today} height={140} />
        </TrendBody>
      </div>
    </section>
  )
}
