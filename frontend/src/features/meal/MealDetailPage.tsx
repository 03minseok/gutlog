import { mealApi, type Meal } from '../../api/meal'
import { splitDateTime } from '../../lib/date'
import { DayGroup, DetailHeaderLink, DetailTop, NoMatch, PeriodCalendar, PeriodHeader, RangeStatus, RecordDot, SearchBox } from '../record/CategoryDetail'
import { groupByDate, matchesQuery, useQueryParam, useRangeParams, useRangeRecords, type RangeState } from '../record/detail'
import { EmptyBox, EmptyWithLink } from '../record/RecordSection'
import { MealCard } from './MealList'

const dateOf = (meal: Meal) => splitDateTime(meal.eatenAt).date

/**
 * 식사 상세.
 * 헤더(＋ 추가) + 전환 버튼 → 달력(주·월·년, 기록 있는 날 점) → 음식 검색창 → 기간 줄 → 범위 전체 또는 고른 날의 식사 카드.
 * - 들어오면 이번 주 전체(아무 날도 눌려 있지 않음). 날짜를 누르면 그날만, 다시 누르거나 "전체 보기"로 범위 전체.
 * - 달력 제목을 누르면 주 → 월 → 년으로 범위를 넓힌다.
 * - 음식 검색은 달력에 잡힌 범위(고른 날이 있으면 그날) 안에서 찾는다. 검색 중에는 달력 점도 맞는 식사가 있는 날만.
 * 기록 자체에 대한 정보만 둔다. 해석한 값(자주 먹은 음식 등)은 분석 탭에서 보여준다.
 * 카드를 누르면 수정, 닫으면 이 화면으로 돌아온다.
 */
export function MealDetailPage() {
  const { today, mode, start, end, selected, toggleDate, clearDate, move, cycleMode, openMonth } = useRangeParams()
  const { query, setQuery } = useQueryParam()
  const searching = query.trim() !== ''

  const { state, loadingMore, reload } = useRangeRecords(mealApi.listByPeriod, start, end < today ? end : today, '식사 기록을 불러오지 못했어요')
  // 범위를 옮기는 동안 남아 있는 이전 범위 목록은 보여주지 않는다.
  const rangeState: RangeState<Meal> = loadingMore ? { status: 'loading' } : state
  const inRange = rangeState.status === 'ready' ? rangeState.items : []
  const matched = inRange.filter((meal) => matchesQuery(meal.foods, query))
  const days = new Set(matched.map(dateOf))
  const months = new Set(matched.map((meal) => dateOf(meal).slice(0, 7)))

  const scope = selected ? inRange.filter((meal) => dateOf(meal) === selected) : inRange
  const shown = selected ? matched.filter((meal) => dateOf(meal) === selected) : matched
  const scopeState: RangeState<Meal> = rangeState.status === 'ready' ? { status: 'ready', items: scope } : rangeState
  const markText = searching ? '맞는 식사 있음' : '기록 있음'
  const noMarkText = searching ? '맞는 식사 없음' : '기록 없음'

  return (
    <div className="flex flex-col">
      <DetailTop category="meal" action={<DetailHeaderLink to="/record/meal/new" label="추가" icon="add" ariaLabel="식사 추가" />} />
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
          mark={(date) => days.has(date) && <RecordDot color="bg-meal" />}
          markLabel={(date) => (days.has(date) ? markText : noMarkText)}
          monthMark={(month) => months.has(month.slice(0, 7)) && <RecordDot color="bg-meal" />}
          monthMarkLabel={(month) => (months.has(month.slice(0, 7)) ? markText : noMarkText)}
          legend={
            <span className="flex items-center gap-1.5">
              <RecordDot color="bg-meal" />
              {searching ? `‘${query.trim()}’ 먹은 날` : '기록 있는 날'}
            </span>
          }
        />
        <SearchBox value={query} onChange={setQuery} placeholder="음식 이름 (예: 김치, 우유)" />

        <PeriodHeader
          mode={mode}
          start={start}
          selected={selected}
          today={today}
          count={rangeState.status === 'ready' ? `${shown.length}회` : null}
          onShowAll={clearDate}
        />
        <RangeStatus
          state={scopeState}
          onRetry={reload}
          emptyMessage={
            <EmptyBox>
              <EmptyWithLink
                message={selected ? '이 날 식사 기록이 없어요' : '이 기간 식사 기록이 없어요'}
                to={selected ? `/record/meal/new?date=${selected}` : '/record/meal/new'}
                label="식사 기록하기"
              />
            </EmptyBox>
          }
        />
        {scope.length > 0 && shown.length === 0 && <NoMatch message={`‘${query.trim()}’에 맞는 식사가 없어요`} />}
        {selected ? <MealCards meals={shown} /> : <MealGroups meals={shown} />}
      </div>
    </div>
  )
}

function MealCards({ meals }: { meals: Meal[] }) {
  return (
    <ul className="flex flex-col gap-2">
      {meals.map((meal) => (
        <li key={meal.id}>
          <MealCard meal={meal} />
        </li>
      ))}
    </ul>
  )
}

/** 날짜별 묶음 (최근 날짜가 먼저) */
function MealGroups({ meals }: { meals: Meal[] }) {
  return groupByDate(meals, dateOf).map((group) => (
    <DayGroup key={group.date} date={group.date} countLabel={`${group.items.length}회`}>
      <MealCards meals={group.items} />
    </DayGroup>
  ))
}
