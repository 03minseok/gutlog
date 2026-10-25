import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { MonthCalendar } from '../../components/MonthCalendar'
import { TabHeader } from '../../layouts/TabLayout'
import { isValidDateParam, todayParam } from '../../lib/date'
import { BowelList } from '../bowel/BowelList'
import { MealList } from '../meal/MealList'
import { MedicationSection } from '../medication/MedicationSection'

/**
 * 기록 탭. 날짜를 골라 그날의 기록을 본다.
 * 보던 날짜는 URL(?date=)에 둬서 수정 화면에 다녀와도 유지된다.
 * 맨 위 한 달 달력에서 날짜를 누르면 그날 기록으로 이동한다.
 * 순서는 식사 · 배변 · 복약. 복약은 따로 입력 화면 없이 섹션에서 바로 체크한다.
 */
export function RecordPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [today] = useState(todayParam)
  const param = searchParams.get('date')
  const date = isValidDateParam(param) && param <= today ? param : today
  const changeDate = (next: string) => setSearchParams({ date: next }, { replace: true })

  return (
    <div className="flex flex-1 flex-col">
      <TabHeader title="기록" />
      <div className="flex flex-col gap-4 px-4 pt-1 pb-6">
        <MonthCalendar date={date} max={today} onChange={changeDate} />
        <MealList date={date} />
        <BowelList date={date} />
        <MedicationSection date={date} />
      </div>
    </div>
  )
}
