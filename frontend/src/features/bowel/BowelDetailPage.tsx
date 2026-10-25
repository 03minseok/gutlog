import { useSearchParams } from 'react-router'
import { bowelApi, type Bowel, type BristolType } from '../../api/bowel'
import { Icon } from '../../components/Icon'
import { splitDateTime } from '../../lib/date'
import { DayGroup, DetailHeaderLink, DetailTop, NoMatch, PeriodCalendar, PeriodHeader, RangeStatus, RecordDot } from '../record/CategoryDetail'
import { groupByDate, useRangeParams, useRangeRecords, useSetParams, type RangeState } from '../record/detail'
import { EmptyBox, EmptyWithLink } from '../record/RecordSection'
import { BleedingBadge } from './BleedingBadge'
import { BowelFilters } from './BowelFilters'
import { BowelCard } from './BowelList'
import { BRISTOL_OPTIONS } from './constants'
import { isFilterActive, matchesFilter, PAIN_FILTER_LEVELS, type BowelFilterValue } from './filter'

const dateOf = (bowel: Bowel) => splitDateTime(bowel.bowelAt).date

/** 주소의 필터: ?blood=1 &pain=4 &types=5,6 (수정 화면에 다녀와도 유지) */
function useBowelFilter(): [BowelFilterValue, (next: BowelFilterValue) => void] {
  const [searchParams] = useSearchParams()
  const setParams = useSetParams()
  const pain = Number(searchParams.get('pain'))
  const types = (searchParams.get('types') ?? '')
    .split(',')
    .map((n) => `TYPE_${n}` as BristolType)
    .filter((type) => BRISTOL_OPTIONS.some((o) => o.value === type))
  const value: BowelFilterValue = {
    bleeding: searchParams.get('blood') === '1',
    pain: (PAIN_FILTER_LEVELS as readonly number[]).includes(pain) ? pain : null,
    types,
  }
  const setValue = (next: BowelFilterValue) =>
    setParams({
      blood: next.bleeding ? '1' : null,
      pain: next.pain === null ? null : String(next.pain),
      types: next.types.map((type) => type.slice(5)).join(','),
    })
  return [value, setValue]
}

/** 달력 표시: 혈변 있던 날은 물방울(blood), 아니면 점(bowel색). 색과 모양이 함께 다르다. */
function BowelMark({ bleeding }: { bleeding: boolean }) {
  return bleeding ? <Icon name="water_drop" size={12} filled className="text-blood" /> : <RecordDot color="bg-bowel" />
}

/**
 * 배변 상세.
 * 헤더(＋ 추가) + 전환 버튼 → 달력(주·월·년, 기록 있는 날 점 · 혈변 있던 날 물방울) → 필터 칸 → 기간 줄 → 범위 전체 또는 고른 날의 배변 카드.
 * - 들어오면 이번 주 전체(아무 날도 눌려 있지 않음). 날짜를 누르면 그날만, 다시 누르거나 "전체 보기"로 범위 전체.
 * - 달력 제목을 누르면 주 → 월 → 년으로 범위를 넓힌다.
 * - 필터(혈변 · 통증 n 이상 · 형태)는 달력에 잡힌 범위(고른 날이 있으면 그날) 안에서 거른다. 필터 중에는 달력 표시도 맞는 기록만.
 * 기록 자체에 대한 정보만 둔다. 해석한 값(평균, 혈변 일수 등)은 분석 탭에서 보여준다.
 * 혈변은 빨강 + 물방울 + 문구로 알린다. (달력은 물방울, 기간 줄·날짜 묶음은 혈변 배지)
 */
export function BowelDetailPage() {
  const { today, mode, start, end, selected, toggleDate, clearDate, move, cycleMode, openMonth } = useRangeParams()
  const [filter, setFilter] = useBowelFilter()
  const filtering = isFilterActive(filter)

  const { state, loadingMore, reload } = useRangeRecords(bowelApi.listByPeriod, start, end < today ? end : today, '배변 기록을 불러오지 못했어요')
  // 범위를 옮기는 동안 남아 있는 이전 범위 목록은 보여주지 않는다.
  const rangeState: RangeState<Bowel> = loadingMore ? { status: 'loading' } : state
  const inRange = rangeState.status === 'ready' ? rangeState.items : []
  const matched = inRange.filter((bowel) => matchesFilter(bowel, filter))

  // 날짜·달마다 맞는 기록이 있는지, 그중 혈변이 있었는지
  const days = new Map<string, boolean>()
  const months = new Map<string, boolean>()
  for (const bowel of matched) {
    const date = dateOf(bowel)
    days.set(date, (days.get(date) ?? false) || bowel.bleeding)
    months.set(date.slice(0, 7), (months.get(date.slice(0, 7)) ?? false) || bowel.bleeding)
  }
  const label = (bleeding: boolean | undefined) =>
    bleeding === undefined ? (filtering ? '맞는 기록 없음' : '기록 없음') : `${filtering ? '맞는 기록 있음' : '기록 있음'}${bleeding ? ', 혈변 있음' : ''}`

  const scope = selected ? inRange.filter((bowel) => dateOf(bowel) === selected) : inRange
  const shown = selected ? matched.filter((bowel) => dateOf(bowel) === selected) : matched
  const scopeState: RangeState<Bowel> = rangeState.status === 'ready' ? { status: 'ready', items: scope } : rangeState
  const shownBleeding = shown.filter((bowel) => bowel.bleeding).length

  return (
    <div className="flex flex-col">
      <DetailTop category="bowel" action={<DetailHeaderLink to="/record/bowel/new" label="추가" icon="add" ariaLabel="배변 추가" />} />
      <div className="flex flex-col gap-4 px-4 pt-3 pb-6">
        <PeriodCalendar
          mode={mode}
          start={start}
          today={today}
          selected={selected}
          onSelect={toggleDate}
          onMove={move}
          onCycleMode={cycleMode}
          onOpenMonth={openMonth}
          mark={(date) => days.has(date) && <BowelMark bleeding={days.get(date)!} />}
          markLabel={(date) => label(days.get(date))}
          monthMark={(month) => months.has(month.slice(0, 7)) && <BowelMark bleeding={months.get(month.slice(0, 7))!} />}
          monthMarkLabel={(month) => label(months.get(month.slice(0, 7)))}
          legend={
            <>
              <span className="flex items-center gap-1.5">
                <RecordDot color="bg-bowel" />
                {filtering ? '맞는 기록 있는 날' : '기록 있는 날'}
              </span>
              <span className="flex items-center gap-1">
                <Icon name="water_drop" size={12} filled className="text-blood" />
                혈변 있던 날
              </span>
            </>
          }
        />
        <BowelFilters value={filter} onChange={setFilter} />

        <PeriodHeader
          mode={mode}
          start={start}
          selected={selected}
          today={today}
          count={rangeState.status === 'ready' ? `${shown.length}회` : null}
          badge={shownBleeding > 0 && <BleedingBadge label={`혈변 ${shownBleeding}회`} />}
          onShowAll={clearDate}
        />
        <RangeStatus
          state={scopeState}
          onRetry={reload}
          emptyMessage={
            <EmptyBox>
              <EmptyWithLink
                message={selected ? '이 날 배변 기록이 없어요' : '이 기간 배변 기록이 없어요'}
                to={selected ? `/record/bowel/new?date=${selected}` : '/record/bowel/new'}
                label="배변 기록하기"
              />
            </EmptyBox>
          }
        />
        {scope.length > 0 && shown.length === 0 && <NoMatch message="조건에 맞는 배변 기록이 없어요" />}
        {selected ? <BowelCards bowels={shown} /> : <BowelGroups bowels={shown} />}
      </div>
    </div>
  )
}

function BowelCards({ bowels }: { bowels: Bowel[] }) {
  return (
    <ul className="flex flex-col gap-2">
      {bowels.map((bowel) => (
        <li key={bowel.id}>
          <BowelCard bowel={bowel} />
        </li>
      ))}
    </ul>
  )
}

/** 날짜별 묶음 (최근 날짜가 먼저). 혈변 있던 날은 날짜 줄에 혈변 배지. */
function BowelGroups({ bowels }: { bowels: Bowel[] }) {
  return groupByDate(bowels, dateOf).map((group) => {
    const bleeding = group.items.filter((bowel) => bowel.bleeding).length
    return (
      <DayGroup
        key={group.date}
        date={group.date}
        countLabel={`${group.items.length}회`}
        badge={bleeding > 0 && <BleedingBadge label={`혈변 ${bleeding}회`} />}
      >
        <BowelCards bowels={group.items} />
      </DayGroup>
    )
  })
}
