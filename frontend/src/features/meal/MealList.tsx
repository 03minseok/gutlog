import { Link } from 'react-router'
import { mealApi, type Meal } from '../../api/meal'
import { Icon } from '../../components/Icon'
import { useRecordFormLinkState } from '../../components/recordForm'
import { splitDateTime } from '../../lib/date'
import { AddRecordLink, EmptyWithLink, RecordSection } from '../record/RecordSection'
import { useDayRecords } from '../record/useDayRecords'

/** 하루치 식사 목록. 날짜가 바뀌면 다시 불러온다. */
export function MealList({ date }: { date: string }) {
  const { state, reload } = useDayRecords(mealApi.listByPeriod, date, '식사 기록을 불러오지 못했어요')

  return (
    <RecordSection
      title="식사"
      icon="restaurant"
      iconBox="bg-meal-bg text-meal"
      emptyMessage={<EmptyWithLink message="이 날의 식사 기록이 없어요" to={`/record/meal/new?date=${date}`} label="식사 기록하기" />}
      state={state}
      onRetry={reload}
      action={<AddRecordLink to={`/record/meal/new?date=${date}`} label="식사" />}
      getKey={(meal) => meal.id}
      renderItem={(meal) => <MealCard meal={meal} />}
    />
  )
}

/** 식사 카드: 시간 + 음식 이름(가운뎃점으로 이어 씀). 음식을 칩으로 그리면 각각 누를 수 있어 보여서 글자로 쓴다. */
function MealCard({ meal }: { meal: Meal }) {
  const formLinkState = useRecordFormLinkState()
  const { time } = splitDateTime(meal.eatenAt)
  return (
    <Link
      state={formLinkState}
      to={`/record/meal/${meal.id}/edit`}
      aria-label={`${time} 식사: ${meal.foods.join(', ')}. 수정하기`}
      className="flex min-h-14 items-center gap-3 rounded-card border border-gray-200 bg-white py-3 pr-2 pl-4 hover:border-gray-300"
    >
      <span className="w-12 flex-none self-start text-body font-semibold tabular-nums">{time}</span>
      <p className="min-w-0 flex-1 self-start text-body break-keep text-ink">{meal.foods.join(' · ')}</p>
      <Icon name="chevron_right" size={20} className="flex-none text-gray-400" />
    </Link>
  )
}
