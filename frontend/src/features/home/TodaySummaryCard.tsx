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
 * - 식사·복약·배변은 각자 카테고리 배경색 칸으로 나눠 구분한다.
 *   칸 윗줄("식사 ›")을 누르면 그 기록의 상세 화면, 숫자 옆 ＋를 누르면 바로 기록한다.
 *   ＋: 식사·배변 → 입력 화면, 복약 → 기록 탭(복약은 따로 입력 화면 없이 섹션에서 체크한다).
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
    <section className="flex flex-col gap-3.5 rounded-card border border-gray-200 bg-white px-4 pt-3 pb-4" aria-labelledby="today-summary">
      <div className="flex items-center justify-between">
        <h2 id="today-summary" className="text-section font-bold">
          오늘의 기록
        </h2>
        <Link
          to="/record"
          className="-my-2.5 -mr-2 flex h-11 items-center gap-0.5 rounded-input pr-1 pl-2.5 text-label font-semibold text-sky-700"
        >
          바로가기
          <Icon name="chevron_right" size={18} />
        </Link>
      </div>
      <div className="grid grid-cols-3 gap-2">
        <Column detailTo="/record/meal" addTo="/record/meal/new" icon="restaurant" label="식사" addLabel="식사 기록하기" color="text-meal" className="bg-meal-bg">
          <Value load={meal} render={(count) => <Count value={count} unit="회" />} />
        </Column>
        <Column detailTo="/record/medication" addTo="/record" icon="medication" label="복약" addLabel="오늘 복약 체크하기" color="text-med" className="bg-med-bg">
          <Value
            load={medication}
            render={({ taken, total }) =>
              total === 0 ? <Empty note="약 없음" /> : <Count value={taken} unit={`/ ${total}`} />
            }
          />
        </Column>
        <Column detailTo="/record/bowel" addTo="/record/bowel/new" icon="waves" label="배변" addLabel="배변 기록하기" color="text-bowel" className="bg-bowel-bg">
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
  /** 칸 윗줄을 누르면 갈 상세 화면 */
  detailTo: string
  /** ＋를 누르면 갈 곳 (입력 화면 또는 기록 탭) */
  addTo: string
  icon: string
  label: string
  /** ＋ 버튼의 화면 낭독기용 이름 (예: "식사 기록하기") */
  addLabel: string
  color: string
  className: string
  children: ReactNode
}

/**
 * 카테고리 칸 (rounded-tile, p-3, 높이는 내용만큼).
 * - 윗줄: 아이콘 + 이름 + › — 칸 위쪽 44px 전체가 상세 화면 링크.
 * - 아랫줄: 수치 + 오른쪽 ＋(흰 원 28px, 누르는 영역 44px) — 바로 기록.
 */
function Column({ detailTo, addTo, icon, label, addLabel, color, className, children }: ColumnProps) {
  const formLinkState = useRecordFormLinkState()
  return (
    <div className={`flex flex-col rounded-tile px-3 pt-3 pb-3.5 ${className}`}>
      <Link
        to={detailTo}
        aria-label={`${label} 자세히 보기`}
        className={`-mx-3 -mt-3 -mb-1.5 flex min-h-11 items-center gap-1 rounded-t-tile px-3 text-label font-semibold focus-visible:ring-2 focus-visible:ring-sky-700 focus-visible:outline-none ${color}`}
      >
        <Icon name={icon} size={16} />
        {label}
        <Icon name="chevron_right" size={16} className="ml-auto" />
      </Link>
      <div className="flex h-7 items-center justify-between gap-1">
        {children}
        <Link
          to={addTo}
          state={addTo.startsWith('/record/') ? formLinkState : undefined}
          aria-label={addLabel}
          className="-my-2 -mr-2 flex h-11 w-11 flex-none items-center justify-center rounded-full focus-visible:ring-2 focus-visible:ring-sky-700 focus-visible:outline-none"
        >
          <span className="flex h-7 w-7 items-center justify-center rounded-full bg-white text-ink ring-1 ring-black/5">
            <Icon name="add" size={18} />
          </span>
        </Link>
      </div>
    </div>
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
