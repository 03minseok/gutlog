import { useEffect, useId, useState, type FormEvent } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router'
import { medicationApi, type Medicine, type ScheduleType, type UserMedicine } from '../../api/medication'
import { isNotFound, messageOf } from '../../api/types'
import { Button } from '../../components/Button'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { FieldHint } from '../../components/FieldHint'
import { Icon } from '../../components/Icon'
import { NotFoundView } from '../../components/NotFoundView'
import { DeleteAction, FormLoadStatus, RecordFormScreen } from '../../components/RecordFormScreen'
import type { RecordFormLinkState } from '../../components/recordForm'
import { SegmentedControl } from '../../components/SegmentedControl'
import { useToast } from '../../components/toast/ToastContext'
import {
  DAILY_DOSE_MAX,
  DAILY_DOSE_MIN,
  DOSE_MEMO_MAX,
  doseRule,
  INTERVAL_MAX,
  INTERVAL_MIN,
  INTERVAL_PRESETS,
  isValidDoseAmount,
  medicineTitle,
  MY_MEDICINES_PATH,
  SCHEDULE_OPTIONS,
} from './constants'
import { MedicinePicker } from './MedicinePicker'

interface FormValues {
  medicine: Medicine | null
  /** 복용 방식·횟수는 사용자가 고르기 전까지 null */
  scheduleType: ScheduleType | null
  dailyDoseCount: number | null
  /** 입력칸 그대로 (숫자로 바꾸기 전) */
  intervalDays: string
  doseAmount: string
  doseMemo: string
}

type LoadState =
  | { status: 'loading' }
  | { status: 'ready'; medicines: Medicine[]; takenIds: Set<number> }
  | { status: 'notFound' }
  | { status: 'error'; message: string }

const EMPTY: FormValues = { medicine: null, scheduleType: null, dailyDoseCount: null, intervalDays: '', doseAmount: '', doseMemo: '' }
const DAILY_COUNTS = Array.from({ length: DAILY_DOSE_MAX - DAILY_DOSE_MIN + 1 }, (_, i) => DAILY_DOSE_MIN + i)

function toFormValues(item: UserMedicine): FormValues {
  return {
    medicine: item.medicine,
    scheduleType: item.scheduleType,
    dailyDoseCount: item.dailyDoseCount,
    intervalDays: item.intervalDays === null ? '' : String(item.intervalDays),
    doseAmount: item.doseAmount === null ? '' : String(item.doseAmount),
    doseMemo: item.doseMemo ?? '',
  }
}

function parseIntervalDays(text: string): number | null {
  if (!/^\d+$/.test(text.trim())) return null
  const days = Number(text)
  return days >= INTERVAL_MIN && days <= INTERVAL_MAX ? days : null
}

/** 복용량: 비우면 null(입력 안 함). 숫자가 아니거나 제형 규칙에 맞지 않으면 'invalid' */
function parseDoseAmount(text: string, medicine: Medicine | null): number | null | 'invalid' {
  const trimmed = text.trim()
  if (trimmed === '') return null
  if (!/^\d+(\.\d)?$/.test(trimmed) || !medicine) return 'invalid'
  const amount = Number(trimmed)
  return isValidDoseAmount(medicine.dosageForm, amount) ? amount : 'invalid'
}

/** 아직 고르지 않은 첫 항목 (화면 순서대로). 저장 버튼 문구로 안내한다. */
function missingLabel(values: FormValues): string | null {
  if (!values.medicine) return '약을 선택하세요'
  if (!values.scheduleType) return '복용 방식을 선택하세요'
  if (values.scheduleType === 'DAILY' && values.dailyDoseCount === null) return '하루 복용 횟수를 선택하세요'
  if (values.scheduleType === 'INTERVAL' && values.intervalDays.trim() === '') return '투여 간격을 입력하세요'
  return null
}

/** 복용 약 등록(/record/medication/medicines/new)·수정(/:id/edit). 수정할 때는 약을 바꿀 수 없다. */
export function UserMedicineFormPage() {
  const params = useParams()
  const userMedicineId = params.id ? Number(params.id) : null
  const isEdit = userMedicineId !== null
  const invalidId = userMedicineId !== null && (!Number.isInteger(userMedicineId) || userMedicineId <= 0)
  const navigate = useNavigate()
  const { showToast } = useToast()
  const countLabelId = useId()
  const intervalId = useId()
  const doseId = useId()
  const memoId = useId()

  const [values, setValues] = useState<FormValues>(EMPTY)
  const [load, setLoad] = useState<LoadState>(invalidId ? { status: 'notFound' } : { status: 'loading' })
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [deleting, setDeleting] = useState(false)

  useEffect(() => {
    if (invalidId) return
    let cancelled = false
    // 수정: 단건 조회 API가 없어서 내 약 목록에서 찾는다. (끊은 약·남의 약은 목록에 없으므로 "찾을 수 없음")
    // 등록: 활성 약 전체와, 이미 복용 중인 약(다시 고를 수 없게)을 함께 받는다.
    Promise.all([isEdit ? Promise.resolve([] as Medicine[]) : medicationApi.searchMedicines(), medicationApi.listMyMedicines()])
      .then(([medicines, mine]) => {
        if (cancelled) return
        if (isEdit) {
          const item = mine.find((m) => m.id === userMedicineId)
          if (!item) {
            setLoad({ status: 'notFound' })
            return
          }
          setValues(toFormValues(item))
        }
        setLoad({ status: 'ready', medicines, takenIds: new Set(mine.map((m) => m.medicine.id)) })
      })
      .catch((err) => !cancelled && setLoad({ status: 'error', message: messageOf(err, '약 정보를 불러오지 못했어요') }))
    return () => {
      cancelled = true
    }
  }, [isEdit, userMedicineId, invalidId])

  const update = (patch: Partial<FormValues>) => setValues((v) => ({ ...v, ...patch }))
  const location = useLocation()
  // 앱 안의 링크로 열었으면(state.from) 보던 화면(약 관리 시트·약 관리 화면·기록 탭)으로 돌아간다. 주소로 바로 들어왔으면 약 관리 화면으로.
  const openedFrom = (location.state as Partial<RecordFormLinkState> | null)?.from
  const goToList = () => (openedFrom ? navigate(-1) : navigate(MY_MEDICINES_PATH, { replace: true }))

  const missing = missingLabel(values)
  const intervalDays = parseIntervalDays(values.intervalDays)
  const intervalError =
    values.scheduleType === 'INTERVAL' && values.intervalDays.trim() !== '' && intervalDays === null
      ? `투여 간격은 ${INTERVAL_MIN}~${INTERVAL_MAX}일로 입력해 주세요`
      : null
  const doseAmount = parseDoseAmount(values.doseAmount, values.medicine)
  const rule = values.medicine ? doseRule(values.medicine.dosageForm) : null
  const unit = values.medicine?.doseUnit ?? ''
  const doseError =
    doseAmount === 'invalid' && rule ? `${rule.step}${unit} 단위로 ${rule.min}~${rule.max}${unit}까지 입력해 주세요` : null
  const memoTooLong = values.doseMemo.trim().length > DOSE_MEMO_MAX
  const canSubmit = missing === null && !intervalError && !doseError && !memoTooLong && !submitting && !deleting

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!canSubmit || !values.medicine || !values.scheduleType || doseAmount === 'invalid') return
    setSubmitting(true)
    setSubmitError(null)
    const memo = values.doseMemo.trim()
    // 복용 방식에 맞지 않는 값은 보내지 않는다. (서버는 400으로 거절한다)
    const body = {
      scheduleType: values.scheduleType,
      dailyDoseCount: values.scheduleType === 'DAILY' ? values.dailyDoseCount : null,
      intervalDays: values.scheduleType === 'INTERVAL' ? intervalDays : null,
      doseAmount,
      doseMemo: memo === '' ? null : memo,
    }
    try {
      if (userMedicineId !== null) await medicationApi.updateMyMedicine(userMedicineId, body)
      else await medicationApi.createMyMedicine({ ...body, medicineId: values.medicine.id })
      showToast('약을 저장했어요')
      goToList()
    } catch (err) {
      if (isNotFound(err) && isEdit) setLoad({ status: 'notFound' })
      else setSubmitError(messageOf(err, '저장하지 못했어요. 다시 시도해 주세요.'))
      setSubmitting(false)
    }
  }

  async function handleDelete() {
    if (userMedicineId === null) return
    setDeleting(true)
    try {
      await medicationApi.removeMyMedicine(userMedicineId)
      showToast('복용을 중단했어요')
      goToList()
    } catch (err) {
      setConfirmOpen(false)
      setDeleting(false)
      if (isNotFound(err)) setLoad({ status: 'notFound' })
      else setSubmitError(messageOf(err, '중단하지 못했어요. 다시 시도해 주세요.'))
    }
  }

  const title = isEdit ? '약 수정' : '약 등록'

  if (load.status === 'notFound') {
    return (
      <RecordFormScreen category="medication" title={title} onClose={goToList}>
        <NotFoundView title="복용 약을 찾을 수 없어요" onBack={goToList} />
      </RecordFormScreen>
    )
  }
  if (load.status !== 'ready') {
    return (
      <RecordFormScreen category="medication" title={title} onClose={goToList}>
        <FormLoadStatus error={load.status === 'error' ? load.message : null} />
      </RecordFormScreen>
    )
  }

  function changeDose(direction: 1 | -1) {
    if (!rule) return
    const current = typeof doseAmount === 'number' ? doseAmount : 0
    const next = Math.min(rule.max, Math.max(rule.min, current + direction * rule.step))
    update({ doseAmount: String(Number(next.toFixed(1))) })
  }

  return (
    <RecordFormScreen
      category="medication"
      title={title}
      onClose={goToList}
      action={isEdit && <DeleteAction onClick={() => setConfirmOpen(true)} disabled={submitting || deleting} />}
    >
      <form onSubmit={handleSubmit} noValidate className="flex min-h-0 flex-1 flex-col">
        <div className="flex flex-1 flex-col gap-6 overflow-y-auto px-4 pt-4 pb-6">
          {isEdit && values.medicine ? (
            <div className="flex flex-col gap-1 rounded-tile bg-gray-50 px-4 py-3">
              <span className="text-body font-bold">{medicineTitle(values.medicine)}</span>
              <span className="text-label text-gray-500">{values.medicine.genericName} · 약은 바꿀 수 없어요. 다른 약이면 새로 등록해 주세요.</span>
            </div>
          ) : (
            <MedicinePicker
              medicines={load.medicines}
              takenIds={load.takenIds}
              value={values.medicine}
              // 약이 바뀌면 단위 규칙도 바뀌므로 복용량을 비운다.
              onChange={(medicine) => update({ medicine, doseAmount: '' })}
            />
          )}

          <div className="flex flex-col gap-2">
            <span className="text-body font-bold">복용 방식</span>
            <SegmentedControl
              label="복용 방식"
              options={SCHEDULE_OPTIONS}
              value={values.scheduleType}
              onChange={(scheduleType) => update({ scheduleType })}
            />
            {values.scheduleType === 'INTERVAL' && <FieldHint>주사처럼 며칠에 한 번 맞는 약이에요</FieldHint>}
            {values.scheduleType === 'AS_NEEDED' && <FieldHint>지사제처럼 필요할 때만 먹는 약이에요. 먹은 날만 체크해요</FieldHint>}
          </div>

          {values.scheduleType === 'DAILY' && (
            <div className="flex flex-col gap-2">
              <span id={countLabelId} className="text-body font-bold">
                하루 몇 번 먹나요?
              </span>
              <div role="radiogroup" aria-labelledby={countLabelId} className="grid grid-cols-6 gap-1.5">
                {DAILY_COUNTS.map((count) => {
                  const selected = values.dailyDoseCount === count
                  return (
                    <button
                      key={count}
                      type="button"
                      role="radio"
                      aria-checked={selected}
                      aria-label={`하루 ${count}회`}
                      onClick={() => update({ dailyDoseCount: count })}
                      className={
                        'h-11 rounded-input text-body tabular-nums transition-colors ' +
                        (selected ? 'bg-ink font-bold text-white' : 'bg-gray-100 font-medium text-gray-700 hover:bg-gray-150')
                      }
                    >
                      {count}
                    </button>
                  )
                })}
              </div>
            </div>
          )}

          {values.scheduleType === 'INTERVAL' && (
            <div className="flex flex-col gap-2">
              <label htmlFor={intervalId} className="text-body font-bold">
                며칠마다 맞나요?
              </label>
              <div className="flex flex-wrap gap-1.5">
                {INTERVAL_PRESETS.map((days) => (
                  <button
                    key={days}
                    type="button"
                    aria-pressed={intervalDays === days}
                    onClick={() => update({ intervalDays: String(days) })}
                    className={
                      'h-11 rounded-full px-4 text-body font-semibold transition-colors ' +
                      (intervalDays === days ? 'bg-ink text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-150')
                    }
                  >
                    {days % 7 === 0 ? `${days / 7}주` : `${days}일`}
                  </button>
                ))}
              </div>
              <div
                className={
                  'flex h-12 items-center gap-2 rounded-input border-[1.5px] bg-white px-3.5 focus-within:ring-[3px] ' +
                  (intervalError ? 'border-error focus-within:ring-error-bg' : 'border-gray-400 focus-within:border-sky-700 focus-within:ring-sky-100')
                }
              >
                <input
                  id={intervalId}
                  inputMode="numeric"
                  value={values.intervalDays}
                  onChange={(e) => update({ intervalDays: e.target.value.replace(/\D/g, '').slice(0, 3) })}
                  aria-invalid={intervalError ? true : undefined}
                  placeholder="14"
                  className="w-16 min-w-0 flex-1 bg-transparent text-body font-semibold text-ink tabular-nums outline-none"
                />
                <span className="text-body text-gray-500">일마다</span>
              </div>
              {intervalError && <FieldHint tone="error">{intervalError}</FieldHint>}
            </div>
          )}

          {values.medicine && (
            <div className="flex flex-col gap-2">
              <label htmlFor={doseId} className="text-body font-bold">
                한 번에 얼마나? <span className="ml-1 text-caption font-medium text-gray-500">선택</span>
              </label>
              <div className="flex items-center gap-2">
                <StepButton icon="remove" label="줄이기" onClick={() => changeDose(-1)} />
                <div
                  className={
                    'flex h-12 min-w-0 flex-1 items-center gap-2 rounded-input border-[1.5px] bg-white px-3.5 focus-within:ring-[3px] ' +
                    (doseError ? 'border-error focus-within:ring-error-bg' : 'border-gray-400 focus-within:border-sky-700 focus-within:ring-sky-100')
                  }
                >
                  <input
                    id={doseId}
                    inputMode="decimal"
                    value={values.doseAmount}
                    onChange={(e) => update({ doseAmount: e.target.value.replace(/[^\d.]/g, '').slice(0, 5) })}
                    aria-invalid={doseError ? true : undefined}
                    placeholder="1"
                    className="min-w-0 flex-1 bg-transparent text-center text-body font-semibold text-ink tabular-nums outline-none"
                  />
                  <span className="text-body text-gray-500">{unit}</span>
                </div>
                <StepButton icon="add" label="늘리기" onClick={() => changeDose(1)} />
              </div>
              {doseError ? (
                <FieldHint tone="error">{doseError}</FieldHint>
              ) : (
                rule && <FieldHint>{`${rule.step}${unit} 단위로 입력할 수 있어요`}</FieldHint>
              )}
            </div>
          )}

          <div className="flex flex-col gap-2">
            <label htmlFor={memoId} className="text-body font-bold">
              메모 <span className="ml-1 text-caption font-medium text-gray-500">선택</span>
            </label>
            <input
              id={memoId}
              value={values.doseMemo}
              onChange={(e) => update({ doseMemo: e.target.value })}
              maxLength={DOSE_MEMO_MAX}
              placeholder="예: 식후 30분"
              className="h-12 rounded-input border-[1.5px] border-gray-400 bg-white px-3.5 text-body text-ink outline-none focus:border-sky-700 focus:ring-[3px] focus:ring-sky-100"
            />
            <span className="self-end text-caption font-medium text-gray-500 tabular-nums">
              {DOSE_MEMO_MAX - values.doseMemo.length}자 남음
            </span>
          </div>
        </div>

        <div className="flex flex-none flex-col gap-2 border-t border-gray-200 bg-white px-4 pt-2.5 pb-3">
          {submitError && <FieldHint tone="error">{submitError}</FieldHint>}
          <Button type="submit" disabled={!canSubmit}>
            {missing ?? '저장'}
          </Button>
        </div>
      </form>

      <ConfirmDialog
        open={confirmOpen}
        title="이 약 복용을 중단할까요?"
        description="내 약 목록에서 빠지지만, 지금까지의 복용 기록은 남아요. 다시 먹게 되면 새로 등록하면 돼요."
        confirmLabel="복용 중단"
        pending={deleting}
        onConfirm={handleDelete}
        onCancel={() => setConfirmOpen(false)}
      />
    </RecordFormScreen>
  )
}

function StepButton({ icon, label, onClick }: { icon: string; label: string; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      className="flex h-12 w-12 flex-none items-center justify-center rounded-input bg-gray-100 text-gray-700 hover:bg-gray-150"
    >
      <Icon name={icon} size={20} />
    </button>
  )
}
