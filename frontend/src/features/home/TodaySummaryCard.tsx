import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router'
import { bowelApi } from '../../api/bowel'
import { mealApi } from '../../api/meal'
import { medicationApi } from '../../api/medication'
import { Icon } from '../../components/Icon'
import { useRecordFormLinkState } from '../../components/recordForm'
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
 * - 불러오는 중: 회색 막대, 실패: "불러오지 못함", 매일 먹는 약 없음(m = 0): "약 없음" — 숫자와 같은 높이 한 줄로 써서 칸 높이를 맞춘다.
 * - 식사·복약·배변은 각자 카테고리 배경색 칸으로 나눠 구분하고, 칸을 누르면 바로 기록하러 간다.
 *   식사·배변 → 입력 화면, 복약 → 기록 탭(복약은 따로 입력 화면 없이 섹션에서 체크한다).
 * - 오른쪽 위 "바로가기"는 기록 탭(오늘)으로 이동한다.
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

  return (
    <section className="flex flex-col gap-3.5 rounded-card border border-gray-200 bg-white p-4" aria-labelledby="today-summary">
      <div className="flex items-center justify-between">
        <h2 id="today-summary" className="text-section font-bold">
          오늘의 기록
        </h2>
        <Link
          to="/record"
          className="-mr-2 flex h-11 items-center gap-0.5 rounded-input pr-1 pl-2.5 text-label font-semibold text-sky-700"
        >
          바로가기
          <Icon name="chevron_right" size={18} />
        </Link>
      </div>
      <div className="-mt-1 grid grid-cols-3 gap-2">
        <Column to="/record/meal/new" icon="restaurant" label="식사" action="기록하기" color="text-meal" className="bg-meal-bg">
          <Value load={meal} render={(count) => <Count value={count} unit="회" />} />
        </Column>
        <Column to="/record" icon="medication" label="복약" action="체크하기" color="text-med" className="bg-med-bg">
          <Value
            load={medication}
            render={({ taken, total }) =>
              total === 0 ? <Empty note="약 없음" /> : <Count value={taken} unit={`/ ${total}`} />
            }
          />
        </Column>
        <Column to="/record/bowel/new" icon="waves" label="배변" action="기록하기" color="text-bowel" className="bg-bowel-bg">
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
    </section>
  )
}

function Value<T>({ load, render }: { load: Load<T>; render: (value: T) => ReactNode }) {
  if (load.status === 'loading') {
    return <span className="block h-[25px] w-10 animate-pulse rounded-input bg-white/70" aria-label="불러오는 중" />
  }
  if (load.status === 'error') return <Empty note="불러오지 못함" />
  return render(load.value)
}

interface ColumnProps {
  to: string
  icon: string
  label: string
  /** 화면 낭독기용 동작 이름 (예: "기록하기") */
  action: string
  color: string
  className: string
  children: ReactNode
}

/** 카테고리 칸. 칸 전체가 링크다. 내용 높이만큼만 차지한다. */
function Column({ to, icon, label, action, color, className, children }: ColumnProps) {
  const formLinkState = useRecordFormLinkState()
  return (
    <Link
      to={to}
      state={to.startsWith('/record/') ? formLinkState : undefined}
      className={`flex flex-col gap-1.5 rounded-tile p-3 focus-visible:ring-2 focus-visible:ring-sky-700 focus-visible:outline-none ${className}`}
    >
      <span className={`flex items-center gap-1 text-label font-semibold ${color}`}>
        <Icon name={icon} size={16} />
        {label}
        <span className="sr-only">{action}</span>
        <Icon name="chevron_right" size={16} className="ml-auto" />
      </span>
      {children}
    </Link>
  )
}

/** 숫자와 단위를 같은 크기(18/700)로 쓴다. */
function Count({ value, unit }: { value: number; unit: string }) {
  return (
    <span className="text-heading font-bold tabular-nums">
      {value}
      {unit.startsWith('/') ? ` ${unit}` : unit}
    </span>
  )
}

/** 값이 없을 때: 숫자 자리(18px 줄 높이)에 한 줄 안내. 세 칸 높이가 같게 유지된다. */
function Empty({ note }: { note: string }) {
  return <span className="flex h-[25px] items-center text-label font-semibold whitespace-nowrap text-gray-500">{note}</span>
}
