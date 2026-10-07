import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { mealApi, type Meal } from '../../api/meal'
import { ApiError } from '../../api/types'
import { Button } from '../../components/Button'
import { FieldHint } from '../../components/FieldHint'
import { Icon } from '../../components/Icon'
import { splitDateTime } from '../../lib/date'

type Result = { status: 'ready'; meals: Meal[] } | { status: 'error'; message: string }
type State = { status: 'loading' } | Result

/** 하루치 식사 목록. 날짜가 바뀌면 다시 불러온다. */
export function MealList({ date }: { date: string }) {
  const [reloadKey, setReloadKey] = useState(0)
  const requestKey = `${date}#${reloadKey}`
  // 어떤 요청의 결과인지 함께 저장해서, 날짜가 바뀌면 이전 결과 대신 로딩을 보여준다.
  const [result, setResult] = useState<{ key: string; value: Result } | null>(null)
  const state: State = result?.key === requestKey ? result.value : { status: 'loading' }

  useEffect(() => {
    let cancelled = false
    mealApi
      .listByPeriod(date, date)
      .then((meals) => !cancelled && setResult({ key: requestKey, value: { status: 'ready', meals } }))
      .catch((err) => {
        if (cancelled) return
        const message = err instanceof ApiError ? err.message : '식사 기록을 불러오지 못했어요'
        setResult({ key: requestKey, value: { status: 'error', message } })
      })
    // 날짜를 빠르게 바꿀 때 이전 날짜 응답이 늦게 와서 덮어쓰지 않게 한다.
    return () => {
      cancelled = true
    }
  }, [date, requestKey])

  return (
    <section className="flex flex-col gap-3" aria-labelledby="meal-section-title">
      <div className="flex items-center gap-2 px-1">
        <span className="flex h-[30px] w-[30px] items-center justify-center rounded-[9px] bg-meal-bg text-meal">
          <Icon name="restaurant" size={18} />
        </span>
        <h2 id="meal-section-title" className="text-[17px] font-bold">
          식사
        </h2>
        {state.status === 'ready' && (
          <span className="text-[15px] font-medium text-gray-500 tabular-nums">{state.meals.length}회</span>
        )}
      </div>

      {state.status === 'loading' && <p className="px-1 py-6 text-center text-[15px] text-gray-500">불러오는 중…</p>}

      {state.status === 'error' && (
        <div className="flex flex-col items-start gap-3 rounded-card border border-gray-200 bg-white p-5">
          <FieldHint tone="error">{state.message}</FieldHint>
          <Button variant="outline" onClick={() => setReloadKey((k) => k + 1)}>
            다시 시도
          </Button>
        </div>
      )}

      {state.status === 'ready' && state.meals.length === 0 && (
        <p className="rounded-card border border-gray-200 bg-white px-5 py-8 text-center text-[15px] text-gray-500">
          이 날의 식사 기록이 없어요
        </p>
      )}

      {state.status === 'ready' && state.meals.length > 0 && (
        <ul className="flex flex-col gap-2">
          {state.meals.map((meal) => (
            <li key={meal.id}>
              <MealCard meal={meal} />
            </li>
          ))}
        </ul>
      )}
    </section>
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
