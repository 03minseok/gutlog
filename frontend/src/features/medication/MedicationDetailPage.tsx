import { useState } from 'react'
import { useSearchParams } from 'react-router'
import { medicationApi, type DailyMedication, type DailyMedicationItem } from '../../api/medication'
import { Icon } from '../../components/Icon'
import { addDays, formatDateWithDay, isValidDateParam, relativeDayLabel, todayParam } from '../../lib/date'
import { DetailTop, PeriodCalendar, ToolBarButton } from '../record/CategoryDetail'
import { useRangeRecords, weekStartOf } from '../record/detail'
import { EmptyWithLink, RecordSection } from '../record/RecordSection'
import type { DayRecordsState } from '../record/useDayRecords'
import { MY_MEDICINES_PATH } from './constants'
import { MedicationDayCard } from './MedicationDayCard'
import { useMyMedicinesSheet } from './useMyMedicinesSheet'

/**
 * 복약은 기간 조회 API가 없어 날짜별 조회를 하루씩 불러온다. (한 주 = 요청 최대 7번)
 * TODO: 날짜별 조회와 같은 모양의 기간 조회 API가 생기면 이 함수만 바꿔 요청 1번으로 줄인다.
 * 모듈 수준 함수라 렌더마다 새로 만들어지지 않는다.
 */
function fetchDays(from: string, to: string): Promise<DailyMedication[]> {
  const dates: string[] = []
  for (let date = from; date <= to; date = addDays(date, 1)) dates.push(date)
  return Promise.all(dates.map((date) => medicationApi.daily(date)))
}

/** 매일 먹는 약(DAILY)의 체크 회차 수 / 전체 회차 수. 줄인 횟수를 넘는 옛 체크는 세지 않는다. */
function dailyCounts(medicines: DailyMedicationItem[]): { taken: number; total: number } {
  let taken = 0
  let total = 0
  for (const item of medicines) {
    if (item.scheduleType !== 'DAILY' || item.stopped) continue
    const count = item.dailyDoseCount ?? 1
    total += count
    taken += item.takenDoses.filter((dose) => dose <= count).length
  }
  return { taken, total }
}

type Mark = 'full' | 'half' | 'none' | 'blank'

/** 날짜 칸 아래 표시: 다 먹음 ● / 일부 ◐ / 안 먹음 ○. 매일 먹는 약이 없는 날·미래·불러오는 중은 비움. */
function markOf(day: DailyMedication | undefined): Mark {
  if (!day) return 'blank'
  const { taken, total } = dailyCounts(day.medicines)
  if (total === 0) return 'blank'
  if (taken >= total) return 'full'
  return taken > 0 ? 'half' : 'none'
}

const MARK_LABEL: Record<Mark, string> = { full: '다 먹음', half: '일부 먹음', none: '안 먹음', blank: '' }

/**
 * 복약 상세 — 날짜를 고르고, 그날 복용 중인 약을 바로 체크한다. 모아 보는 건 통계 탭이 맡는다.
 * 헤더 + 전환 버튼 → 달력(주, 날마다 체크 표시) → "내 약 관리" 칸(식사 검색창과 같은 크기) → 고른 날 요약(날짜 · 2 / 3 · n회 남음) → 복용 중인 약 + 체크 카드.
 * 복약은 날짜별 조회를 하루씩 불러와야 해서 달력을 월·년으로 넓히지 않는다.
 * 고른 날은 주소(?date=)에 둔다: 홈에서는 오늘, 기록 탭에서는 보던 날짜로 들어온다. 미래는 고를 수 없다.
 * 체크하면 그날만 다시 불러와 주간 표시와 요약을 바로 바꾼다.
 * "내 약 관리"는 새 화면이 아니라 아래에서 올라오는 시트로 연다. (useMyMedicinesSheet)
 */
export function MedicationDetailPage() {
  const [today] = useState(todayParam)
  const [searchParams, setSearchParams] = useSearchParams()
  const param = searchParams.get('date')
  const selected = isValidDateParam(param) && param <= today ? param : today

  function select(date: string) {
    const next = date > today ? today : date
    setSearchParams(
      (prev) => {
        const params = new URLSearchParams(prev)
        if (next === today) params.delete('date')
        else params.set('date', next)
        return params
      },
      { replace: true },
    )
  }

  const { openSheet, sheet } = useMyMedicinesSheet()

  const weekStart = weekStartOf(selected)
  const weekEnd = addDays(weekStart, 6)
  const fetchTo = weekEnd < today ? weekEnd : today
  const { state, reload } = useRangeRecords(fetchDays, weekStart, fetchTo, '복약 기록을 불러오지 못했어요')

  // 체크를 바꾼 날만 다시 불러와 덮어쓴다. (한 주를 다시 부르지 않게)
  const [overrides, setOverrides] = useState<Record<string, DailyMedication>>({})
  function refreshDay(date: string) {
    medicationApi
      .daily(date)
      .then((day) => setOverrides((prev) => ({ ...prev, [date]: day })))
      .catch(() => reload())
  }

  // 주를 넘기는 동안은 이전 주 목록이 남아 있으므로, 이번 주 날짜만 찾는다.
  const byDate = new Map((state.status === 'ready' ? state.items : []).map((day) => [day.date, overrides[day.date] ?? day]))
  const day = byDate.get(selected)
  const sectionState: DayRecordsState<DailyMedicationItem> =
    state.status === 'error' ? state : day ? { status: 'ready', items: day.medicines } : { status: 'loading' }
  const { taken, total } = dailyCounts(day?.medicines ?? [])
  const relative = relativeDayLabel(selected, today)

  return (
    <div className="flex flex-col">
      <DetailTop category="medication" />
      <div className="flex flex-col gap-4 px-4 pt-3 pb-6">
        <PeriodCalendar
          mode="week"
          start={weekStart}
          selected={selected}
          today={today}
          onSelect={select}
          onMove={(weeks) => select(addDays(selected, weeks * 7))}
          mark={(date) => <MarkDot mark={markOf(byDate.get(date))} />}
          markLabel={(date) => MARK_LABEL[markOf(byDate.get(date))]}
          legend={
            <>
              <span className="flex items-center gap-1">
                <MarkDot mark="full" />다 먹음
              </span>
              <span className="flex items-center gap-1">
                <MarkDot mark="half" />일부
              </span>
              <span className="flex items-center gap-1">
                <MarkDot mark="none" />안 먹음
              </span>
            </>
          }
        />
        <ToolBarButton icon="medication" label="내 약 관리" onClick={openSheet} haspopup="dialog" />

        <div className="-mb-1 flex min-h-7 items-center gap-2 px-1">
          <h2 className="flex-1 text-body font-bold tabular-nums">
            {formatDateWithDay(selected)}
            {relative && <span className="ml-1.5 text-label font-medium text-gray-500">{relative}</span>}
          </h2>
          {day && total > 0 && (
            <>
              <span className="text-body font-semibold tabular-nums">
                {taken} / {total}
              </span>
              {taken >= total ? (
                <span className="flex items-center gap-0.5 rounded-full bg-ok-bg px-2 pt-[3px] pb-px text-label font-semibold text-ok">
                  <Icon name="check" size={14} />다 먹음
                </span>
              ) : (
                <span className="rounded-full bg-gray-100 px-2 pt-[3px] pb-px text-label font-medium text-gray-700 tabular-nums">
                  {total - taken}회 남음
                </span>
              )}
            </>
          )}
        </div>

        <RecordSection
          title="복용 중인 약"
          icon="medication"
          panel="bg-med-bg"
          iconColor="text-med"
          state={sectionState}
          onRetry={reload}
          countLabel={day ? `${day.medicines.length}개` : ''}
          emptyMessage={<EmptyWithLink message="이 날 복용 중이던 약이 없어요" to={`${MY_MEDICINES_PATH}/new`} label="먹는 약 등록하기" />}
          getKey={(item) => item.userMedicineId}
          renderItem={(item) => <MedicationDayCard item={item} date={selected} onChanged={() => refreshDay(selected)} />}
        />
      </div>
      {sheet}
    </div>
  )
}

/** 체크 표시 9px: 꽉 찬 원 / 반 찬 원 / 빈 원 (복약색, 빈 원은 gray-500) */
function MarkDot({ mark }: { mark: Mark }) {
  const style = {
    full: 'bg-med',
    half: 'border-[1.5px] border-med bg-[linear-gradient(90deg,var(--color-med)_50%,transparent_50%)]',
    none: 'border-[1.5px] border-gray-500',
    blank: 'invisible',
  }[mark]
  return <span aria-hidden="true" className={`block h-[9px] w-[9px] flex-none rounded-full ${style}`} />
}
