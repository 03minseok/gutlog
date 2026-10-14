import { mealApi } from '../../api/meal'
import { splitDateTime } from '../../lib/date'
import { DayGroup, DetailHeader, DetailHeaderLink, NoMatch, PeriodControl, RangeStatus, SearchBox, StatTiles } from '../record/CategoryDetail'
import { dateSearchTexts, groupByDate, matchesQuery, useDetailParams, useRangeRecords } from '../record/detail'
import { EmptyBox, EmptyWithLink } from '../record/RecordSection'
import { MealCard } from './MealList'

/**
 * 식사 상세 — 최근 식사 기록 전체.
 * 헤더(＋ 추가) → 검색창(음식·날짜) → 기간(최근 7일 / 30일 / 90일) → 기록 횟수 → 날짜별 식사 카드.
 * 기록 자체에 대한 정보만 둔다. 기록 내용을 해석한 값(자주 먹은 음식, 평균 등)은 분석 탭에서 보여준다.
 * 카드를 누르면 수정, 닫으면 이 화면으로 돌아온다.
 */
export function MealDetailPage() {
  const { today, period, from, to, setPeriod, query, setQuery } = useDetailParams()
  const { state, reload } = useRangeRecords(mealApi.listByPeriod, from, to, '식사 기록을 불러오지 못했어요')
  const all = state.status === 'ready' ? state.items : []
  const meals = all.filter((meal) => {
    const { date, time } = splitDateTime(meal.eatenAt)
    return matchesQuery([...dateSearchTexts(date, today), time, ...meal.foods], query)
  })
  const groups = groupByDate(meals, (meal) => splitDateTime(meal.eatenAt).date)
  const searching = query.trim() !== ''

  return (
    <div className="flex flex-col">
      <DetailHeader category="meal" action={<DetailHeaderLink to="/record/meal/new" label="추가" icon="add" ariaLabel="식사 추가" />} />
      <div className="flex flex-col gap-4 px-4 pt-1 pb-6">
        <SearchBox value={query} onChange={setQuery} placeholder="음식이나 날짜 (예: 김치, 10월 13일)" />
        <PeriodControl value={period} onChange={setPeriod} />

        {state.status === 'ready' && all.length > 0 && (
          <StatTiles
            stats={[
              { label: searching ? '찾은 식사' : '총 식사', value: `${meals.length}회` },
              { label: '기록한 날', value: `${groups.length}일` },
            ]}
          />
        )}

        <RangeStatus
          state={state}
          onRetry={reload}
          emptyMessage={
            <EmptyBox>
              <EmptyWithLink message="이 기간에 식사 기록이 없어요" to="/record/meal/new" label="식사 기록하기" />
            </EmptyBox>
          }
        />
        {state.status === 'ready' && all.length > 0 && meals.length === 0 && <NoMatch query={query.trim()} />}

        {groups.map((group) => (
          <DayGroup key={group.date} date={group.date} countLabel={`${group.items.length}회`}>
            <ul className="flex flex-col gap-2">
              {group.items.map((meal) => (
                <li key={meal.id}>
                  <MealCard meal={meal} />
                </li>
              ))}
            </ul>
          </DayGroup>
        ))}
      </div>
    </div>
  )
}
