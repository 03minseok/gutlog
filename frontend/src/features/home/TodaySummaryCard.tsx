import { useEffect, useState, type ReactNode } from 'react'
import { mealApi } from '../../api/meal'
import { Icon } from '../../components/Icon'

/**
 * 오늘의 기록 카드 (홈 시안). 요약 API가 아직 없어서
 * 식사는 오늘 하루 기간 조회 결과의 개수로 계산하고, 복약·배변은 API가 생길 때까지 '–'로 비워둔다.
 */
export function TodaySummaryCard({ today }: { today: string }) {
  const [mealCount, setMealCount] = useState<number | null>(null)

  useEffect(() => {
    let cancelled = false
    mealApi
      .listByPeriod(today, today)
      .then((meals) => !cancelled && setMealCount(meals.length))
      .catch(() => {
        // 실패하면 '–' 그대로 둔다. 401은 인터셉터가 처리한다.
      })
    return () => {
      cancelled = true
    }
  }, [today])

  return (
    <section className="flex flex-col gap-[18px] rounded-card border border-gray-200 bg-white p-5" aria-labelledby="today-summary">
      <h2 id="today-summary" className="text-[17px] font-bold">
        오늘의 기록
      </h2>
      <div className="grid grid-cols-3">
        <Column icon="restaurant" label="식사" color="text-meal" className="pr-3">
          {mealCount === null ? <Empty /> : <Count value={mealCount} unit="회" />}
        </Column>
        <Column icon="medication" label="복약" color="text-med" className="border-l border-gray-100 px-3">
          <Empty />
        </Column>
        <Column icon="water_drop" label="배변" color="text-bowel" className="border-l border-gray-100 pl-3">
          <Empty />
        </Column>
      </div>
    </section>
  )
}

function Column({ icon, label, color, className, children }: { icon: string; label: string; color: string; className: string; children: ReactNode }) {
  return (
    <div className={`flex flex-col gap-2 ${className}`}>
      <span className={`flex items-center gap-1.5 text-[14px] font-semibold ${color}`}>
        <Icon name={icon} size={18} />
        {label}
      </span>
      {children}
    </div>
  )
}

function Count({ value, unit }: { value: number; unit: string }) {
  return (
    <span className="text-[32px] leading-none font-bold tracking-[-0.02em] tabular-nums">
      {value}
      <span className="text-[16px] font-medium tracking-normal text-gray-500"> {unit}</span>
    </span>
  )
}

function Empty() {
  return (
    <span className="text-[32px] leading-none font-bold text-gray-300" aria-label="기록 없음">
      –
    </span>
  )
}
