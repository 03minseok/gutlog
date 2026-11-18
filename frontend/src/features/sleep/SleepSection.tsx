import { useId, useState } from 'react'
import { Link } from 'react-router'
import { Icon } from '../../components/Icon'
import { todayParam } from '../../lib/date'
import { SleepEntryForm } from './SleepEntryForm'
import { SLEEP_PATH } from './sleep'

/**
 * 기록 탭 맨 아래 수면 섹션. 보고 있는 날(= 일어난 날)의 수면 시간을 바로 등록·수정·삭제한다.
 * 수면은 식사·배변·복약처럼 카테고리 색이 없어 흰 카드(1px gray-200)로 묶고, 아이콘 박스는 gray-100 면에 gray-700 아이콘.
 * 아이콘·이름을 누르면 수면 상세(추이 그래프)로 간다.
 */
export function SleepSection({ date }: { date: string }) {
  const titleId = useId()
  const [today] = useState(todayParam)

  return (
    <section className="flex flex-col gap-3 rounded-card border border-gray-200 bg-white px-3 pt-2 pb-3" aria-labelledby={titleId}>
      <div className="flex items-center gap-2 px-1">
        <Link to={SLEEP_PATH} aria-label="수면 자세히 보기" className="-my-2 -ml-1 flex h-11 items-center gap-2 rounded-input pr-0.5 pl-1">
          <span className="flex h-7 w-7 items-center justify-center rounded-[8px] bg-gray-100 text-gray-700">
            <Icon name="bedtime" size={16} />
          </span>
          <h2 id={titleId} className="text-section font-bold">
            수면
          </h2>
          <Icon name="chevron_right" size={18} className="-ml-1 text-gray-400" />
        </Link>
      </div>
      <div className="px-1">
        <SleepEntryForm date={date} today={today} onChanged={() => {}} allowDelete />
      </div>
    </section>
  )
}
