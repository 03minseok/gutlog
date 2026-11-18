import { useRef, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router'
import { sleepApi, type SleepRecord } from '../../api/sleep'
import { messageOf } from '../../api/types'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { Icon } from '../../components/Icon'
import { SegmentedControl } from '../../components/SegmentedControl'
import { TrendBody } from '../../components/TrendBody'
import { useToast } from '../../components/toast/ToastContext'
import { addDays, formatDateWithDay, relativeDayLabel, todayParam } from '../../lib/date'
import { useRangeRecords } from '../record/detail'
import { SleepChart } from './SleepChart'
import { SleepEntryForm } from './SleepEntryForm'
import { DEFAULT_SLEEP_PERIOD, SLEEP_EMPTY_TEXT, SLEEP_LOAD_ERROR, SLEEP_PERIODS, formatSleep, type SleepPeriod } from './sleep'

const isPeriod = (value: string | null): value is SleepPeriod => SLEEP_PERIODS.some((p) => p.value === value)

/**
 * 수면 상세 (/record/sleep). 하단 탭 있음, gray-50 바탕. 체중 기록 관리 화면과 같은 구성이다.
 * 날짜(일어난 날)를 지정해 등록·수정(날짜당 한 줄, 있으면 바뀜) → 기간별(7·30·90일) 추이 그래프 → 날짜별 목록(수정·삭제).
 * 기간은 주소 ?period= 에 둔다.
 */
export function SleepDetailPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { showToast } = useToast()
  const [searchParams, setSearchParams] = useSearchParams()
  const [today] = useState(todayParam)
  const [date, setDate] = useState(today)
  const [formRefresh, setFormRefresh] = useState(0)
  const [pendingDelete, setPendingDelete] = useState<SleepRecord | null>(null)
  const [deleting, setDeleting] = useState(false)
  const formRef = useRef<HTMLElement>(null)

  const requested = searchParams.get('period')
  const period: SleepPeriod = isPeriod(requested) ? requested : DEFAULT_SLEEP_PERIOD
  const from = addDays(today, -(Number(period) - 1))
  const { state, reload } = useRangeRecords(sleepApi.listByPeriod, from, today, SLEEP_LOAD_ERROR)
  const records = state.status === 'ready' ? state.items : []
  const newestFirst = [...records].reverse()

  // 앱 안에서 들어왔으면 history가 있다. (첫 화면의 key는 'default')
  const goBack = () => (location.key !== 'default' ? navigate(-1) : navigate('/record'))

  function changePeriod(next: SleepPeriod) {
    setSearchParams(next === DEFAULT_SLEEP_PERIOD ? {} : { period: next }, { replace: true })
  }

  function editRecord(record: SleepRecord) {
    setDate(record.date)
    formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  async function confirmDelete() {
    if (!pendingDelete || deleting) return
    setDeleting(true)
    try {
      await sleepApi.remove(pendingDelete.date)
      showToast('수면 기록을 삭제했어요')
    } catch (err) {
      showToast(messageOf(err, '삭제하지 못했어요. 다시 시도해 주세요.'))
    } finally {
      setDeleting(false)
      setPendingDelete(null)
      reload()
      setFormRefresh((n) => n + 1)
    }
  }

  return (
    <div className="flex flex-col">
      <header className="flex h-13 items-center gap-1 px-1.5">
        <button
          type="button"
          onClick={goBack}
          aria-label="뒤로 가기"
          className="flex h-11 w-11 items-center justify-center rounded-input text-ink hover:bg-gray-100"
        >
          <Icon name="arrow_back_ios_new" size={20} />
        </button>
        <h1 className="text-section font-bold">수면 기록</h1>
      </header>

      <div className="flex flex-col gap-4 px-4 pt-1 pb-6">
        <section ref={formRef} aria-labelledby="sleep-form-title" className="flex scroll-mt-3 flex-col gap-3 rounded-card border border-gray-200 bg-white p-4">
          <h2 id="sleep-form-title" className="text-section font-bold">
            날짜를 지정해 기록
          </h2>
          <SleepEntryForm date={date} today={today} onDateChange={setDate} onChanged={reload} refreshKey={formRefresh} />
        </section>

        <section aria-labelledby="sleep-trend-title" className="flex flex-col gap-3 rounded-card border border-gray-200 bg-white p-4">
          <h2 id="sleep-trend-title" className="text-section font-bold">
            추이
          </h2>
          <SegmentedControl label="기간" options={SLEEP_PERIODS} value={period} onChange={changePeriod} />
          <TrendBody
            status={state.status}
            count={records.length}
            message={state.status === 'error' ? state.message : null}
            onRetry={reload}
            emptyText={SLEEP_EMPTY_TEXT}
          >
            <SleepChart records={records} from={from} to={today} />
            <TrendSummary records={records} />
          </TrendBody>
        </section>

        {state.status === 'ready' && newestFirst.length > 0 && (
          <section aria-labelledby="sleep-list-title" className="flex flex-col gap-2">
            <h2 id="sleep-list-title" className="px-1 text-label font-semibold text-gray-700">
              날짜별 기록 · {newestFirst.length}일
            </h2>
            <ul className="flex flex-col overflow-hidden rounded-card border border-gray-200 bg-white">
              {newestFirst.map((record) => {
                const dayName = relativeDayLabel(record.date, today)
                return (
                  <li key={record.date} className="flex min-h-14 items-center gap-1 border-b border-gray-200 pr-1 pl-4 last:border-b-0">
                    <span className="flex-1 text-body text-gray-700">
                      {formatDateWithDay(record.date)}
                      {dayName && <span className="ml-1.5 text-label text-gray-500">{dayName}</span>}
                    </span>
                    <span className="mr-1 text-body font-semibold text-ink tabular-nums">{formatSleep(record.sleepMinutes)}</span>
                    <button
                      type="button"
                      onClick={() => editRecord(record)}
                      aria-label={`${formatDateWithDay(record.date)} 수면 수정`}
                      className="flex h-11 w-11 items-center justify-center rounded-input text-gray-700 hover:bg-gray-100"
                    >
                      <Icon name="edit" size={20} />
                    </button>
                    <button
                      type="button"
                      onClick={() => setPendingDelete(record)}
                      aria-label={`${formatDateWithDay(record.date)} 수면 삭제`}
                      className="flex h-11 w-11 items-center justify-center rounded-input text-gray-700 hover:bg-gray-100"
                    >
                      <Icon name="delete" size={20} />
                    </button>
                  </li>
                )
              })}
            </ul>
          </section>
        )}
      </div>

      <ConfirmDialog
        open={pendingDelete !== null}
        title="수면 기록을 삭제할까요?"
        description={
          pendingDelete
            ? `${formatDateWithDay(pendingDelete.date)} ${formatSleep(pendingDelete.sleepMinutes)} 기록을 삭제해요. 되돌릴 수 없어요.`
            : undefined
        }
        confirmLabel="삭제"
        pending={deleting}
        onConfirm={confirmDelete}
        onCancel={() => setPendingDelete(null)}
      />
    </div>
  )
}

/** 그래프 아래 한 줄: 기록 n일 · 평균 · 최저 · 최고 */
function TrendSummary({ records }: { records: SleepRecord[] }) {
  const values = records.map((r) => r.sleepMinutes)
  // 평균은 10분 단위로 반올림해 보여준다.
  const average = Math.round(values.reduce((sum, v) => sum + v, 0) / values.length / 10) * 10
  return (
    <p className="text-caption text-gray-500 tabular-nums">
      기록 {records.length}일 · 평균 {formatSleep(average)} · 최저 {formatSleep(Math.min(...values))} · 최고 {formatSleep(Math.max(...values))}
    </p>
  )
}
