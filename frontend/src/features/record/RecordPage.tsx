import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { DateNavigator } from '../../components/DateNavigator'
import { TabHeader } from '../../layouts/TabLayout'
import { isValidDateParam, todayParam } from '../../lib/date'
import { BowelList } from '../bowel/BowelList'
import { MealList } from '../meal/MealList'
import { MedicationSection } from '../medication/MedicationSection'
import { QuickRecordBar } from './QuickRecordBar'

/**
 * 기록 탭. 날짜를 골라 그날의 기록을 본다.
 * 보던 날짜는 URL(?date=)에 둬서 수정 화면에 다녀와도 유지된다.
 * 순서는 식사 · 복약 · 배변. 복약은 따로 입력 화면 없이 섹션에서 바로 체크한다.
 * 식사·배변 추가 버튼은 목록 길이와 상관없이 엄지 자리에 있도록 하단 고정 막대에 둔다.
 */
export function RecordPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [today] = useState(todayParam)
  const param = searchParams.get('date')
  const date = isValidDateParam(param) && param <= today ? param : today

  return (
    <div className="flex flex-1 flex-col">
      <TabHeader title="기록" />
      <div className="flex flex-col gap-6 px-4 pt-1 pb-6">
        <DateNavigator date={date} max={today} onChange={(next) => setSearchParams({ date: next }, { replace: true })} />
        <MealList date={date} />
        <MedicationSection date={date} />
        <BowelList date={date} />
      </div>
      <QuickRecordBar
        label="이 날 기록 추가"
        items={[
          { to: `/record/meal/new?date=${date}`, label: '식사 기록', icon: 'restaurant', color: 'text-meal' },
          { to: `/record/bowel/new?date=${date}`, label: '배변 기록', icon: 'waves', color: 'text-bowel' },
        ]}
      />
    </div>
  )
}
