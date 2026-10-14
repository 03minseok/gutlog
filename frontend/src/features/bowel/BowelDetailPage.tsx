import { bowelApi } from '../../api/bowel'
import { splitDateTime } from '../../lib/date'
import { DayGroup, DetailHeader, DetailHeaderLink, NoMatch, PeriodControl, RangeStatus, SearchBox, StatTiles } from '../record/CategoryDetail'
import { dateSearchTexts, groupByDate, matchesQuery, useDetailParams, useRangeRecords } from '../record/detail'
import { EmptyBox, EmptyWithLink } from '../record/RecordSection'
import { BleedingBadge } from './BleedingBadge'
import { BowelCard } from './BowelList'
import { bristolLabel, painLevelInfo } from './constants'

/**
 * 배변 상세 — 최근 배변 기록 전체.
 * 헤더(＋ 추가) → 검색창(혈변·형태·통증·메모·날짜) → 기간(최근 7일 / 30일 / 90일) → 기록 횟수 → 날짜별 배변 카드.
 * 기록 자체에 대한 정보만 둔다. 해석한 값(평균, 혈변 일수 등)은 분석 탭에서 보여준다.
 * 혈변 있었던 날은 날짜 줄에 혈변 배지를 붙인다. (빨강 + 물방울 + 문구)
 */
export function BowelDetailPage() {
  const { today, period, from, to, setPeriod, query, setQuery } = useDetailParams()
  const { state, reload } = useRangeRecords(bowelApi.listByPeriod, from, to, '배변 기록을 불러오지 못했어요')
  const all = state.status === 'ready' ? state.items : []
  const bowels = all.filter((bowel) => {
    const { date, time } = splitDateTime(bowel.bowelAt)
    return matchesQuery(
      [
        ...dateSearchTexts(date, today),
        time,
        bristolLabel(bowel.bowelCondition),
        `통증 ${bowel.painLevel}`,
        painLevelInfo(bowel.painLevel).label,
        // "혈변"은 혈변이 있는 기록에만 넣는다. (혈변 없음 기록이 "혈변" 검색에 걸리지 않게)
        bowel.bleeding ? '혈변 있음' : null,
        bowel.memo,
      ],
      query,
    )
  })
  const groups = groupByDate(bowels, (bowel) => splitDateTime(bowel.bowelAt).date)
  const searching = query.trim() !== ''

  return (
    <div className="flex flex-col">
      <DetailHeader category="bowel" action={<DetailHeaderLink to="/record/bowel/new" label="추가" icon="add" ariaLabel="배변 추가" />} />
      <div className="flex flex-col gap-4 px-4 pt-1 pb-6">
        <SearchBox value={query} onChange={setQuery} placeholder="혈변, 형태, 날짜 (예: 혈변, 6형)" />
        <PeriodControl value={period} onChange={setPeriod} />

        {state.status === 'ready' && all.length > 0 && (
          <StatTiles
            stats={[
              { label: searching ? '찾은 배변' : '총 배변', value: `${bowels.length}회` },
              { label: '기록한 날', value: `${groups.length}일` },
            ]}
          />
        )}

        <RangeStatus
          state={state}
          onRetry={reload}
          emptyMessage={
            <EmptyBox>
              <EmptyWithLink message="이 기간에 배변 기록이 없어요" to="/record/bowel/new" label="배변 기록하기" />
            </EmptyBox>
          }
        />
        {state.status === 'ready' && all.length > 0 && bowels.length === 0 && <NoMatch query={query.trim()} />}

        {groups.map((group) => {
          const bleeding = group.items.filter((bowel) => bowel.bleeding).length
          return (
            <DayGroup
              key={group.date}
              date={group.date}
              countLabel={`${group.items.length}회`}
              badge={bleeding > 0 && <BleedingBadge label={`혈변 ${bleeding}회`} />}
            >
              <ul className="flex flex-col gap-2">
                {group.items.map((bowel) => (
                  <li key={bowel.id}>
                    <BowelCard bowel={bowel} />
                  </li>
                ))}
              </ul>
            </DayGroup>
          )
        })}
      </div>
    </div>
  )
}
