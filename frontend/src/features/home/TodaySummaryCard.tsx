import { useEffect, useState, type ReactNode } from 'react'
import { bowelApi } from '../../api/bowel'
import { mealApi } from '../../api/meal'
import { medicationApi } from '../../api/medication'
import { Icon } from '../../components/Icon'
import { splitDateTime } from '../../lib/date'

/** 요청 하나의 상태. 로딩·실패·결과를 화면에서 구분해 보여준다. */
type Load<T> = { status: 'loading' } | { status: 'error' } | { status: 'ready'; value: T }

const LOADING = { status: 'loading' } as const
const FAILED = { status: 'error' } as const

interface BowelSummary {
  count: number
  bleeding: number
  /** 오늘 마지막 혈변 기록 시각 'HH:mm' */
  lastBleedingTime: string | null
}

/**
 * 오늘의 기록 카드. 요약 API가 아직 없어서
 * 식사·배변은 오늘 하루 기간 조회 결과의 개수, 복약은 날짜별 조회의 n / m(DAILY 약의 회차만)으로 채운다.
 * - 불러오는 중: 회색 막대, 실패: "–" + "불러오지 못했어요", 매일 먹는 약 없음(m = 0): "–" + "매일 먹는 약 없음"
 * - 오늘 혈변 기록이 있으면 카드 아래에 혈변 줄(물방울 아이콘 + 횟수 + 마지막 시각)을 보여준다.
 */
export function TodaySummaryCard({ today }: { today: string }) {
  const [meal, setMeal] = useState<Load<number>>(LOADING)
  const [medication, setMedication] = useState<Load<{ taken: number; total: number }>>(LOADING)
  const [bowel, setBowel] = useState<Load<BowelSummary>>(LOADING)

  useEffect(() => {
    let cancelled = false
    // 401은 인터셉터가 처리한다.
    mealApi
      .listByPeriod(today, today)
      .then((meals) => !cancelled && setMeal({ status: 'ready', value: meals.length }))
      .catch(() => !cancelled && setMeal(FAILED))
    medicationApi
      .daily(today)
      .then(
        (daily) =>
          !cancelled && setMedication({ status: 'ready', value: { taken: daily.dailyTakenCount, total: daily.dailyTotalCount } }),
      )
      .catch(() => !cancelled && setMedication(FAILED))
    bowelApi
      .listByPeriod(today, today)
      .then((bowels) => {
        if (cancelled) return
        const bleeding = bowels.filter((b) => b.bleeding)
        // 기간 조회 결과는 시각 순으로 정렬되어 온다.
        const last = bleeding.at(-1)
        setBowel({
          status: 'ready',
          value: { count: bowels.length, bleeding: bleeding.length, lastBleedingTime: last ? splitDateTime(last.bowelAt).time : null },
        })
      })
      .catch(() => !cancelled && setBowel(FAILED))
    return () => {
      cancelled = true
    }
  }, [today])

  const nothingYet = meal.status === 'ready' && meal.value === 0 && bowel.status === 'ready' && bowel.value.count === 0

  return (
    <section className="flex flex-col gap-3.5 rounded-card border border-gray-200 bg-white p-4" aria-labelledby="today-summary">
      <h2 id="today-summary" className="text-section font-bold">
        오늘의 기록
      </h2>
      <div className="grid grid-cols-3">
        <Column icon="restaurant" label="식사" color="text-meal" className="pr-2.5">
          <Value load={meal} render={(count) => <Count value={count} unit="회" />} />
        </Column>
        <Column icon="medication" label="복약" color="text-med" className="border-l border-gray-200 px-2.5">
          <Value
            load={medication}
            render={({ taken, total }) =>
              total === 0 ? <Empty note="매일 먹는 약 없음" /> : <Count value={taken} unit={`/ ${total}`} />
            }
          />
        </Column>
        <Column icon="waves" label="배변" color="text-bowel" className="border-l border-gray-200 pl-2.5">
          <Value load={bowel} render={({ count }) => <Count value={count} unit="회" />} />
        </Column>
      </div>

      {bowel.status === 'ready' && bowel.value.bleeding > 0 && (
        <p className="flex min-h-11 items-center gap-2 rounded-tile bg-blood-bg px-3 text-body">
          <Icon name="water_drop" size={18} filled className="flex-none text-blood" />
          <span>
            <b className="font-semibold text-blood">혈변 {bowel.value.bleeding}회</b> 기록됨
          </span>
          {bowel.value.lastBleedingTime && (
            <span className="ml-auto text-label text-gray-500 tabular-nums">마지막 {bowel.value.lastBleedingTime}</span>
          )}
        </p>
      )}

      {nothingYet && <p className="text-label text-gray-500">아직 오늘 기록이 없어요. 아래 버튼으로 바로 기록해 보세요.</p>}
    </section>
  )
}

function Value<T>({ load, render }: { load: Load<T>; render: (value: T) => ReactNode }) {
  if (load.status === 'loading') {
    return <span className="block h-[26px] w-12 animate-pulse rounded-input bg-gray-100" aria-label="불러오는 중" />
  }
  if (load.status === 'error') return <Empty note="불러오지 못했어요" />
  return render(load.value)
}

function Column({ icon, label, color, className, children }: { icon: string; label: string; color: string; className: string; children: ReactNode }) {
  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      <span className={`flex items-center gap-1 text-label font-semibold ${color}`}>
        <Icon name={icon} size={16} />
        {label}
      </span>
      {children}
    </div>
  )
}

function Count({ value, unit }: { value: number; unit: string }) {
  return (
    <span className="text-num font-semibold tracking-[-0.02em] tabular-nums">
      {value}
      <span className="text-label font-medium tracking-normal text-gray-500"> {unit}</span>
    </span>
  )
}

function Empty({ note }: { note: string }) {
  return (
    <span className="flex flex-col gap-1">
      <span className="text-num font-semibold text-gray-400" aria-hidden="true">
        –
      </span>
      <span className="text-caption text-gray-500">{note}</span>
    </span>
  )
}
