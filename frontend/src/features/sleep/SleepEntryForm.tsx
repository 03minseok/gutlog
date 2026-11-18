import { useEffect, useRef, useState, type FormEvent } from 'react'
import { sleepApi, type SleepRecord } from '../../api/sleep'
import { messageOf } from '../../api/types'
import { Button } from '../../components/Button'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { FieldHint } from '../../components/FieldHint'
import { Icon } from '../../components/Icon'
import { DateField } from '../../components/RecordFormScreen'
import { useToast } from '../../components/toast/ToastContext'
import { formatDateWithDay, isFutureDate, isValidDateParam, relativeDayLabel } from '../../lib/date'
import { SleepDurationPicker } from './SleepDurationPicker'
import { SLEEP_DATE_HINT, formatSleep } from './sleep'

interface SleepEntryFormProps {
  /** 일어난 날 */
  date: string
  today: string
  /** 주면 날짜 칸이 생겨 날짜를 고를 수 있다. 없으면 date로 고정(기록 탭: 보고 있는 날). */
  onDateChange?: (date: string) => void
  /** 저장·삭제가 끝나면 (목록·그래프 다시 받기) */
  onChanged: () => void
  /** 그 날짜 기록이 있으면 아래에 "기록 삭제" 버튼을 둔다. (따로 삭제할 목록이 없는 기록 탭용) */
  allowDelete?: boolean
  /** 바깥에서 지운 뒤 이 값을 바꾸면 그 날짜 기록을 다시 확인한다. */
  refreshKey?: number
}

/**
 * 수면 입력 (날짜당 한 줄, 일어난 날 기준). 고른 날짜에 이미 기록이 있으면 그 값을 채워 두고, 저장하면 값이 바뀐다고 알려준다.
 * 입력 규칙은 백엔드와 같다: 30분 ~ 24시간, 30분 단위. 미래 날짜는 저장할 수 없다.
 */
export function SleepEntryForm({ date, today, onDateChange, onChanged, allowDelete = false, refreshKey = 0 }: SleepEntryFormProps) {
  return (
    <div className="flex flex-col gap-3">
      {onDateChange && <DateField label="일어난 날" date={date} maxDate={today} onChange={onDateChange} />}
      {/* 날짜가 바뀌거나 바깥에서 지우면 입력 상태를 처음부터 다시 시작한다. */}
      <SleepValueForm
        key={`${date}:${refreshKey}`}
        date={date}
        today={today}
        withDateField={!!onDateChange}
        allowDelete={allowDelete}
        onChanged={onChanged}
      />
    </div>
  )
}

interface SleepValueFormProps {
  date: string
  today: string
  withDateField: boolean
  allowDelete: boolean
  onChanged: () => void
}

function SleepValueForm({ date, today, withDateField, allowDelete, onChanged }: SleepValueFormProps) {
  const { showToast } = useToast()
  const [minutes, setMinutes] = useState<number | null>(null)
  const [existing, setExisting] = useState<SleepRecord | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [confirmingDelete, setConfirmingDelete] = useState(false)
  const [deleting, setDeleting] = useState(false)
  // 사용자가 먼저 골랐으면, 늦게 온 기존 값으로 덮어쓰지 않는다.
  const editedRef = useRef(false)

  useEffect(() => {
    if (!isValidDateParam(date) || isFutureDate(date, today)) return
    let cancelled = false
    sleepApi
      .listByPeriod(date, date)
      .then((records) => {
        if (cancelled) return
        const found = records[0] ?? null
        setExisting(found)
        if (found && !editedRef.current) setMinutes(found.sleepMinutes)
      })
      // 기존 값을 못 받아도 입력·저장은 할 수 있다. (저장은 덮어쓰기라 결과가 같다)
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [date, today])

  const dateInvalid = !isValidDateParam(date) || isFutureDate(date, today)
  const unchanged = existing !== null && minutes === existing.sleepMinutes
  const busy = submitting || deleting

  const buttonLabel = (() => {
    if (submitting) return '저장 중…'
    if (dateInvalid) return '날짜를 확인하세요'
    if (minutes === null) return '수면 시간을 고르세요'
    if (unchanged) return '저장된 값과 같아요'
    return existing ? '바꾼 값 저장' : '저장'
  })()

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (minutes === null || dateInvalid || unchanged || busy) return
    setSubmitting(true)
    setSubmitError(null)
    try {
      const saved = await sleepApi.save(date, minutes)
      setExisting(saved)
      setMinutes(saved.sleepMinutes)
      editedRef.current = false
      showToast('수면 시간을 저장했어요')
      onChanged()
    } catch (err) {
      setSubmitError(messageOf(err, '저장하지 못했어요. 다시 시도해 주세요.'))
    } finally {
      setSubmitting(false)
    }
  }

  async function confirmDelete() {
    if (!existing || busy) return
    setDeleting(true)
    try {
      await sleepApi.remove(existing.date)
      setExisting(null)
      setMinutes(null)
      editedRef.current = false
      showToast('수면 기록을 삭제했어요')
      onChanged()
    } catch (err) {
      showToast(messageOf(err, '삭제하지 못했어요. 다시 시도해 주세요.'))
    } finally {
      setDeleting(false)
      setConfirmingDelete(false)
    }
  }

  const dayName = relativeDayLabel(date, today)
  const dayText = dayName ?? formatDateWithDay(date)

  return (
    <>
      <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-3">
        <div className="flex flex-col gap-1.5">
          <SleepDurationPicker
            label={withDateField ? '총 수면 시간 (낮잠 제외)' : `${dayText} 일어나기까지 잔 시간 (낮잠 제외)`}
            value={minutes}
            disabled={busy}
            onChange={(next) => {
              editedRef.current = true
              setSubmitError(null)
              setMinutes(next)
            }}
          />
          {existing ? (
            <FieldHint>
              {dayText} {formatSleep(existing.sleepMinutes)} 기록이 있어요. 저장하면 이 값으로 바뀌어요.
            </FieldHint>
          ) : (
            <FieldHint>{SLEEP_DATE_HINT}</FieldHint>
          )}
        </div>
        {submitError && (
          <div className="rounded-input bg-error-bg px-3 py-2.5">
            <FieldHint tone="error">{submitError}</FieldHint>
          </div>
        )}
        <Button type="submit" disabled={busy || dateInvalid || minutes === null || unchanged}>
          {buttonLabel}
        </Button>
        {allowDelete && existing && (
          <button
            type="button"
            onClick={() => setConfirmingDelete(true)}
            disabled={busy}
            className="-mt-1 flex h-11 items-center justify-center gap-1 self-center rounded-input px-3 text-label font-semibold text-gray-700 hover:bg-gray-100 disabled:text-gray-400"
          >
            <Icon name="delete" size={18} />
            기록 삭제
          </button>
        )}
      </form>

      <ConfirmDialog
        open={confirmingDelete}
        title="수면 기록을 삭제할까요?"
        description={existing ? `${formatDateWithDay(existing.date)} ${formatSleep(existing.sleepMinutes)} 기록을 삭제해요. 되돌릴 수 없어요.` : undefined}
        confirmLabel="삭제"
        pending={deleting}
        onConfirm={confirmDelete}
        onCancel={() => setConfirmingDelete(false)}
      />
    </>
  )
}
