import { Link } from 'react-router'
import { bowelApi, type Bowel } from '../../api/bowel'
import { Icon } from '../../components/Icon'
import { useRecordFormLinkState } from '../../components/recordForm'
import { splitDateTime } from '../../lib/date'
import { AddRecordLink, EmptyWithLink, RecordSection } from '../record/RecordSection'
import { useDayRecords } from '../record/useDayRecords'
import { BleedingBadge } from './BleedingBadge'
import { bristolLabel, painLevelInfo } from './constants'

/** 하루치 배변 목록. 혈변 기록은 옅은 blood 면 + 물방울 배지 + "혈변 있음" 문구로 눈에 띄게 표시한다. */
export function BowelList({ date }: { date: string }) {
  const { state, reload } = useDayRecords(bowelApi.listByPeriod, date, '배변 기록을 불러오지 못했어요')
  const bleedingCount = state.status === 'ready' ? state.items.filter((b) => b.bleeding).length : 0

  return (
    <RecordSection
      title="배변"
      icon="waves"
      iconBox="bg-bowel-bg text-bowel"
      emptyMessage={<EmptyWithLink message="이 날의 배변 기록이 없어요" to={`/record/bowel/new?date=${date}`} label="배변 기록하기" />}
      state={state}
      onRetry={reload}
      badge={bleedingCount > 0 && <BleedingBadge label={`혈변 ${bleedingCount}회`} />}
      action={<AddRecordLink to={`/record/bowel/new?date=${date}`} label="배변" />}
      getKey={(bowel) => bowel.id}
      renderItem={(bowel) => <BowelCard bowel={bowel} />}
    />
  )
}

function BowelCard({ bowel }: { bowel: Bowel }) {
  const formLinkState = useRecordFormLinkState()
  const { time } = splitDateTime(bowel.bowelAt)
  const condition = bristolLabel(bowel.bowelCondition)
  const pain = painLevelInfo(bowel.painLevel)
  const summary = `${time} 배변: ${condition}, 통증 ${bowel.painLevel}${bowel.bleeding ? ', 혈변 있음' : ''}`

  return (
    <Link
      state={formLinkState}
      to={`/record/bowel/${bowel.id}/edit`}
      aria-label={`${summary}. 수정하기`}
      className={
        'flex min-h-14 items-center gap-3 rounded-card border py-3 pr-2 pl-4 ' +
        (bowel.bleeding ? 'border-blood-border bg-blood-bg' : 'border-gray-200 bg-white hover:border-gray-300')
      }
    >
      <span className="w-12 flex-none self-start text-body font-semibold tabular-nums">{time}</span>
      <div className="flex min-w-0 flex-1 flex-col gap-1.5 self-start">
        <span className="text-body">{condition}</span>
        <div className="flex flex-wrap items-center gap-1.5">
          {bowel.bleeding && <BleedingBadge label="혈변 있음" onTint />}
          <span className={`rounded-full px-2 py-0.5 text-label font-medium ${bowel.bleeding ? 'bg-white' : 'bg-gray-100'} ${pain.color}`}>
            통증 {bowel.painLevel} · {pain.label}
          </span>
        </div>
        {bowel.memo && <p className="truncate text-label text-gray-700">{bowel.memo}</p>}
      </div>
      <Icon name="chevron_right" size={20} className="flex-none text-gray-400" />
    </Link>
  )
}
