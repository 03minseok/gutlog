import { Link } from 'react-router'
import { mealApi, type Meal } from '../../api/meal'
import { Icon } from '../../components/Icon'
import { splitDateTime } from '../../lib/date'
import { RecordSection } from '../record/RecordSection'
import { useDayRecords } from '../record/useDayRecords'

/** 하루치 식사 목록. 날짜가 바뀌면 다시 불러온다. */
export function MealList({ date }: { date: string }) {
  const { state, reload } = useDayRecords(mealApi.listByPeriod, date, '식사 기록을 불러오지 못했어요')

  return (
    <RecordSection
      title="식사"
      icon="restaurant"
      iconBox="bg-meal-bg text-meal"
      emptyMessage="이 날의 식사 기록이 없어요"
      state={state}
      onRetry={reload}
      getKey={(meal) => meal.id}
      renderItem={(meal) => <MealCard meal={meal} />}
    />
  )
}

function MealCard({ meal }: { meal: Meal }) {
  const { time } = splitDateTime(meal.eatenAt)
  return (
    <Link
      to={`/record/meal/${meal.id}/edit`}
      aria-label={`${time} 식사: ${meal.foods.join(', ')}. 수정하기`}
      className="flex items-start gap-3 rounded-card border border-gray-200 bg-white py-4 pr-2 pl-5 hover:border-gray-300"
    >
      <span className="w-[52px] flex-none pt-1.5 text-[17px] font-bold tabular-nums">{time}</span>
      <ul className="flex min-w-0 flex-1 flex-wrap gap-1.5">
        {meal.foods.map((food) => (
          <li key={food} className="max-w-full truncate rounded-full bg-meal-bg px-3 py-1.5 text-[14px] font-semibold text-meal">
            {food}
          </li>
        ))}
      </ul>
      <Icon name="chevron_right" className="flex-none self-center text-gray-400" />
    </Link>
  )
}
