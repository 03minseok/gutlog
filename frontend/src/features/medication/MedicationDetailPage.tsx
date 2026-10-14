import { Link } from 'react-router'
import { medicationApi, type DailyMedication, type DailyMedicationItem } from '../../api/medication'
import { Icon } from '../../components/Icon'
import { addDays, formatDateWithDay } from '../../lib/date'
import { DetailHeader, DetailHeaderLink, NoMatch, RangeStatus, SearchBox, StatTiles } from '../record/CategoryDetail'
import { dateSearchTexts, matchesQuery, useDetailParams, useRangeRecords } from '../record/detail'
import { medicineTitle, MY_MEDICINES_PATH } from './constants'

const DAYS = 7

/**
 * 복약은 기간 조회 API가 없어 날짜별 조회를 하루씩 불러온다. (최근 7일 = 요청 7번)
 * 모듈 수준 함수라 렌더마다 새로 만들어지지 않는다. 결과는 최근 날짜가 먼저.
 */
function fetchDays(from: string, to: string): Promise<DailyMedication[]> {
  const dates: string[] = []
  for (let date = to; date >= from; date = addDays(date, -1)) dates.push(date)
  return Promise.all(dates.map((date) => medicationApi.daily(date)))
}

/** 약 하나의 그날 체크 상태: 매일 "1 / 2회", 간격 투여 "투여함", 필요할 때 "n회" (체크 없으면 null) */
function doseStatus(item: DailyMedicationItem): string | null {
  const taken = item.takenDoses.length
  if (item.scheduleType === 'DAILY') return `${taken} / ${item.dailyDoseCount ?? 1}회`
  if (taken === 0) return null
  return item.scheduleType === 'INTERVAL' ? '투여함' : `${taken}회`
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

/**
 * 복약 상세 — 최근 7일 복약 기록. (기간 조회 API가 없어 7일 고정)
 * 헤더(내 약 관리) → 검색창(약 이름·성분명·날짜) → 체크 수 → 날짜별 카드(그날 약마다 체크 상태).
 * 카드를 누르면 그날 기록 탭으로 가서 체크를 바꿀 수 있다.
 * 기록 자체에 대한 정보만 둔다. 복용률 같은 해석한 값은 분석 탭에서 보여준다.
 */
export function MedicationDetailPage() {
  const { today, query, setQuery } = useDetailParams()
  const from = addDays(today, -(DAYS - 1))
  const { state, reload } = useRangeRecords(fetchDays, from, today, '복약 기록을 불러오지 못했어요')
  const all = state.status === 'ready' ? state.items : []
  const searching = query.trim() !== ''

  // 날짜가 맞으면 그날 약 전부, 아니면 이름이 맞는 약만 남긴다. 검색 중에는 남은 약이 없는 날을 뺀다.
  const days = all
    .map((day) => {
      if (matchesQuery(dateSearchTexts(day.date, today), query)) return day
      const medicines = day.medicines.filter((item) =>
        matchesQuery([medicineTitle(item.medicine), item.medicine.brandName, item.medicine.genericName, ...dateSearchTexts(day.date, today)], query),
      )
      return { ...day, medicines }
    })
    .filter((day) => !searching || day.medicines.length > 0)

  const { taken, total } = dailyCounts(days.flatMap((day) => day.medicines))
  const checkedDays = days.filter((day) => day.medicines.some((item) => item.takenDoses.length > 0)).length

  return (
    <div className="flex flex-col">
      <DetailHeader category="medication" action={<DetailHeaderLink to={MY_MEDICINES_PATH} label="내 약 관리" />} />
      <div className="flex flex-col gap-4 px-4 pt-1 pb-6">
        <SearchBox value={query} onChange={setQuery} placeholder="약 이름이나 날짜 (예: 펜타사, 어제)" />
        <p className="-mt-1 px-1 text-label text-gray-500">최근 7일 기록</p>

        {state.status === 'ready' && (
          <StatTiles
            stats={[
              { label: '체크(매일 먹는 약)', value: total > 0 ? `${taken} / ${total}` : '없음' },
              { label: '체크한 날', value: `${checkedDays}일` },
            ]}
          />
        )}

        <RangeStatus state={state} onRetry={reload} emptyMessage={null} />
        {state.status === 'ready' && searching && days.length === 0 && <NoMatch query={query.trim()} />}

        <ul className="flex flex-col gap-2">
          {days.map((day) => (
            <li key={day.date}>
              <DayCard day={day} />
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}

/**
 * 하루 카드: 날짜 15/600 + (매일 먹는 약) "2 / 3" + 칩(다 먹음 / n회 남음) + ›,
 * 아래에 약마다 "펜타사 정제 … 1 / 2회". 누르면 그날 기록 탭.
 */
function DayCard({ day }: { day: DailyMedication }) {
  const { taken, total } = dailyCounts(day.medicines)
  const remaining = total - taken
  return (
    <Link
      to={`/record?date=${day.date}`}
      className="flex items-center gap-2 rounded-card border border-gray-200 bg-white py-3 pr-2 pl-4 hover:border-gray-300"
    >
      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <div className="flex items-center gap-2">
          <span className="flex-1 text-body font-semibold tabular-nums">{formatDateWithDay(day.date)}</span>
          {total > 0 && (
            <>
              <span className="text-body font-semibold tabular-nums">
                {taken} / {total}
              </span>
              {remaining <= 0 ? (
                <span className="flex items-center gap-0.5 rounded-full bg-ok-bg px-2 pt-[3px] pb-px text-label font-semibold text-ok">
                  <Icon name="check" size={14} />다 먹음
                </span>
              ) : (
                <span className="rounded-full bg-gray-100 px-2 pt-[3px] pb-px text-label font-medium text-gray-700 tabular-nums">{remaining}회 남음</span>
              )}
            </>
          )}
        </div>
        {day.medicines.length === 0 ? (
          <p className="text-label text-gray-500">복용 중인 약이 없어요</p>
        ) : (
          <ul className="flex flex-col gap-1">
            {day.medicines.map((item) => {
              const status = doseStatus(item)
              return (
                <li key={item.userMedicineId} className="flex items-center gap-2 text-label">
                  <span className="min-w-0 flex-1 truncate text-gray-700">{medicineTitle(item.medicine)}</span>
                  <span className={`flex-none tabular-nums ${status ? 'font-semibold text-ink' : 'text-gray-500'}`}>{status ?? '안 먹음'}</span>
                </li>
              )
            })}
          </ul>
        )}
      </div>
      <Icon name="chevron_right" size={20} className="flex-none text-gray-400" />
    </Link>
  )
}
