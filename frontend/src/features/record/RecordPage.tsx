import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { DateNavigator } from '../../components/DateNavigator'
import { Icon } from '../../components/Icon'
import { TabHeader } from '../../layouts/TabLayout'
import { isValidDateParam, todayParam } from '../../lib/date'
import { BowelList } from '../bowel/BowelList'
import { MealList } from '../meal/MealList'

/**
 * 기록 탭. 날짜를 골라 그날의 기록을 본다.
 * 보던 날짜는 URL(?date=)에 둬서 수정 화면에 다녀와도 유지된다.
 * 식사·배변 섹션이 있고, 복약 API가 생기면 섹션을 추가한다.
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
        <BowelList date={date} />
        <div className="grid grid-cols-2 gap-2">
          <AddLink to={`/record/meal/new?date=${date}`} label="식사 기록" />
          <AddLink to={`/record/bowel/new?date=${date}`} label="배변 기록" />
        </div>
      </div>
    </>
  )
}

function AddLink({ to, label }: { to: string; label: string }) {
  return (
    <Link
      to={to}
      className="flex h-14 items-center justify-center gap-1.5 rounded-btn bg-blue-600 text-[17px] font-semibold text-white hover:bg-blue-700 active:bg-blue-800"
    >
      <Icon name="add" size={22} />
      {label}
    </Link>
  )
}
