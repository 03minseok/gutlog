import { useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { messageOf } from '../../api/types'
import { weightApi, type WeightRecord } from '../../api/weight'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { Icon } from '../../components/Icon'
import { SegmentedControl } from '../../components/SegmentedControl'
import { useToast } from '../../components/toast/ToastContext'
import { addDays, formatDateWithDay, relativeDayLabel, todayParam } from '../../lib/date'
import { DEFAULT_WEIGHT_PERIOD, WEIGHT_PERIODS, type WeightPeriod } from './constants'
import { useWeightRecords } from './useWeightRecords'
import { formatWeight } from './weight'
import { TrendBody } from './WeightCard'
import { WeightChart } from './WeightChart'
import { WeightEntryForm } from './WeightEntryForm'

const isPeriod = (value: string | null): value is WeightPeriod => WEIGHT_PERIODS.some((p) => p.value === value)

/**
 * 체중 기록 관리 (/mypage/weight). 하단 탭 있음, gray-50 바탕.
 * 날짜를 지정해 등록·수정(날짜당 한 줄, 있으면 바뀜) → 기간별(7·30·90일) 추이 그래프 → 날짜별 목록(수정·삭제).
 * 기간은 주소 ?period= 에 둔다.
 */
export function WeightPage() {
  const navigate = useNavigate()
  const { showToast } = useToast()
  const [searchParams, setSearchParams] = useSearchParams()
  const [today] = useState(todayParam)
  const [date, setDate] = useState(today)
  const [formRefresh, setFormRefresh] = useState(0)
  const [pendingDelete, setPendingDelete] = useState<WeightRecord | null>(null)
  const [deleting, setDeleting] = useState(false)
  const formRef = useRef<HTMLElement>(null)

  const requested = searchParams.get('period')
  const period: WeightPeriod = isPeriod(requested) ? requested : DEFAULT_WEIGHT_PERIOD
  const from = addDays(today, -(Number(period) - 1))
  const trend = useWeightRecords(from, today)
  const newestFirst = [...trend.records].reverse()

  function changePeriod(next: WeightPeriod) {
    setSearchParams(next === DEFAULT_WEIGHT_PERIOD ? {} : { period: next }, { replace: true })
  }

  function editRecord(record: WeightRecord) {
    setDate(record.date)
    formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  async function confirmDelete() {
    if (!pendingDelete || deleting) return
    setDeleting(true)
    try {
      await weightApi.remove(pendingDelete.date)
      showToast('체중 기록을 삭제했어요')
    } catch (err) {
      showToast(messageOf(err, '삭제하지 못했어요. 다시 시도해 주세요.'))
    } finally {
      setDeleting(false)
      setPendingDelete(null)
      trend.reload()
      setFormRefresh((n) => n + 1)
    }
  }

  return (
    <div className="flex flex-col">
      <header className="flex h-13 items-center gap-1 px-1.5">
        <button
          type="button"
          onClick={() => navigate('/mypage')}
          aria-label="마이페이지로 돌아가기"
          className="flex h-11 w-11 items-center justify-center rounded-input text-ink hover:bg-gray-100"
        >
          <Icon name="arrow_back_ios_new" size={20} />
        </button>
        <h1 className="text-section font-bold">체중 기록</h1>
      </header>

      <div className="flex flex-col gap-4 px-4 pt-1 pb-6">
        <section ref={formRef} aria-labelledby="weight-form-title" className="flex scroll-mt-3 flex-col gap-3 rounded-card border border-gray-200 bg-white p-4">
          <h2 id="weight-form-title" className="text-section font-bold">
            날짜를 지정해 기록
          </h2>
          <WeightEntryForm
            date={date}
            today={today}
            onDateChange={setDate}
            onSaved={trend.reload}
            refreshKey={formRefresh}
          />
        </section>

        <section aria-labelledby="weight-trend-title" className="flex flex-col gap-3 rounded-card border border-gray-200 bg-white p-4">
          <h2 id="weight-trend-title" className="text-section font-bold">
            추이
          </h2>
          <SegmentedControl label="기간" options={WEIGHT_PERIODS} value={period} onChange={changePeriod} />
          <TrendBody
            status={trend.status}
            count={trend.records.length}
            message={trend.message}
            onRetry={trend.reload}
          >
            <WeightChart records={trend.records} from={from} to={today} />
            <TrendSummary records={trend.records} />
          </TrendBody>
        </section>

        {trend.status !== 'error' && newestFirst.length > 0 && (
          <section aria-labelledby="weight-list-title" className="flex flex-col gap-2">
            <h2 id="weight-list-title" className="px-1 text-label font-semibold text-gray-700">
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
                    <span className="mr-1 text-body font-semibold text-ink tabular-nums">{formatWeight(record.weight)}kg</span>
                    <button
                      type="button"
                      onClick={() => editRecord(record)}
                      aria-label={`${formatDateWithDay(record.date)} 체중 수정`}
                      className="flex h-11 w-11 items-center justify-center rounded-input text-gray-700 hover:bg-gray-100"
                    >
                      <Icon name="edit" size={20} />
                    </button>
                    <button
                      type="button"
                      onClick={() => setPendingDelete(record)}
                      aria-label={`${formatDateWithDay(record.date)} 체중 삭제`}
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
        title="체중 기록을 삭제할까요?"
        description={
          pendingDelete
            ? `${formatDateWithDay(pendingDelete.date)} ${formatWeight(pendingDelete.weight)}kg 기록을 삭제해요. 되돌릴 수 없어요.`
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

/** 그래프 아래 한 줄: 기록 n일 · 최저 · 최고 · 처음 기록 대비 변화 */
function TrendSummary({ records }: { records: WeightRecord[] }) {
  const values = records.map((r) => r.weight)
  const change = values.length > 1 ? Math.round((values[values.length - 1] - values[0]) * 100) / 100 : null
  return (
    <p className="text-caption text-gray-500 tabular-nums">
      기록 {records.length}일 · 최저 {formatWeight(Math.min(...values))}kg · 최고 {formatWeight(Math.max(...values))}kg
      {change !== null && ` · 처음 기록 대비 ${change > 0 ? '+' : change < 0 ? '−' : '±'}${formatWeight(Math.abs(change))}kg`}
    </p>
  )
}
