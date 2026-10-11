import { Link } from 'react-router'
import { bowelApi, type Bowel } from '../../api/bowel'
import { Icon } from '../../components/Icon'
import { splitDateTime } from '../../lib/date'
import { RecordSection } from '../record/RecordSection'
import { useDayRecords } from '../record/useDayRecords'
import { BleedingBadge } from './BleedingBadge'
import { bristolLabel, painLevelInfo } from './constants'

/** 하루치 배변 목록. 혈변 기록은 danger 색으로 눈에 띄게 표시한다. */
export function BowelList({ date }: { date: string }) {
  const { state, reload } = useDayRecords(bowelApi.listByPeriod, date, '배변 기록을 불러오지 못했어요')
  const bleedingCount = state.status === 'ready' ? state.items.filter((b) => b.bleeding).length : 0

  return (
    <RecordSection
      title="배변"
      icon="water_drop"
      iconBox="bg-bowel-bg text-bowel"
      emptyMessage="이 날의 배변 기록이 없어요"
      state={state}
      onRetry={reload}
      badge={bleedingCount > 0 && <BleedingBadge label={`혈변 ${bleedingCount}회`} />}
      getKey={(bowel) => bowel.id}
      renderItem={(bowel) => <BowelCard bowel={bowel} />}
    />
  )
}

function BowelCard({ bowel }: { bowel: Bowel }) {
  const { time } = splitDateTime(bowel.bowelAt)
  const condition = bristolLabel(bowel.bowelCondition)
  const pain = painLevelInfo(bowel.painLevel)
  const summary = `${time} 배변: ${condition}, 통증 ${bowel.painLevel}${bowel.bleeding ? ', 혈변 있음' : ''}`

  return (
    <Link
      to={`/record/bowel/${bowel.id}/edit`}
      aria-label={`${summary}. 수정하기`}
      className={
        'flex items-start gap-3 rounded-card border py-4 pr-2 pl-5 ' +
        (bowel.bleeding ? 'border-danger-border bg-danger-bg hover:border-danger' : 'border-gray-200 bg-white hover:border-gray-300')
      }
    >
      <span className="w-[52px] flex-none text-[17px] font-bold tabular-nums">{time}</span>
      <div className="flex min-w-0 flex-1 flex-col gap-1.5">
        <span className="text-[16px] font-semibold">{condition}</span>
        <div className="flex flex-wrap items-center gap-1.5">
          {bowel.bleeding && <BleedingBadge label="혈변" solid />}
          <span className={`rounded-full px-2.5 py-1 text-[13px] font-semibold ${bowel.bleeding ? 'bg-white' : 'bg-gray-100'} ${pain.color}`}>
            통증 {bowel.painLevel} · {pain.label}
          </span>
        </div>
        {bowel.memo && <p className="truncate text-[14px] text-gray-500">{bowel.memo}</p>}
      </div>
      <Icon name="chevron_right" className="flex-none self-center text-gray-400" />
    </Link>
  )
}
