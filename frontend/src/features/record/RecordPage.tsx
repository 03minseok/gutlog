import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { Icon } from '../../components/Icon'
import { TabHeader } from '../../layouts/TabLayout'
import { isValidDateParam, todayParam } from '../../lib/date'
import { DateNavigator } from '../meal/DateNavigator'
import { MealList } from '../meal/MealList'

/**
 * 기록 탭. 날짜를 골라 그날의 기록을 본다.
 * 보던 날짜는 URL(?date=)에 둬서 수정 화면에 다녀와도 유지된다.
 * 지금은 식사만 있고, 배변·복약 API가 생기면 섹션을 추가한다.
 */
export function RecordPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [today] = useState(todayParam)
  const param = searchParams.get('date')
  const date = isValidDateParam(param) && param <= today ? param : today

  return (
    <>
      <TabHeader title="기록" />
      <div className="flex flex-col gap-5 px-5 pt-1 pb-6">
        <DateNavigator date={date} max={today} onChange={(next) => setSearchParams({ date: next }, { replace: true })} />
        <MealList date={date} />
        <Link
          to={`/record/meal/new?date=${date}`}
          className="flex h-14 items-center justify-center gap-1.5 rounded-btn bg-blue-600 text-[17px] font-semibold text-white hover:bg-blue-700 active:bg-blue-800"
        >
          <Icon name="add" size={22} />
          식사 기록하기
        </Link>
      </div>
    </>
  )
}
