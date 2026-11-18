import { useEffect, useRef, useState, type FormEvent } from 'react'
import { messageOf } from '../../api/types'
import { weightApi, type WeightRecord } from '../../api/weight'
import { Button } from '../../components/Button'
import { FieldHint } from '../../components/FieldHint'
import { DateField } from '../../components/RecordFormScreen'
import { TextInput } from '../../components/TextInput'
import { useToast } from '../../components/toast/ToastContext'
import { formatDateWithDay, isFutureDate, isValidDateParam, relativeDayLabel } from '../../lib/date'
import { checkWeight, formatWeight, sanitizeWeightInput } from './weight'

interface WeightEntryFormProps {
  date: string
  today: string
  /** 주면 날짜 칸이 생겨 날짜를 고를 수 있다. 없으면 date로 고정(마이페이지: 오늘). */
  onDateChange?: (date: string) => void
  /** 저장이 끝나면 (목록·그래프 다시 받기) */
  onSaved: (record: WeightRecord) => void
  /** 바깥에서 지운 뒤 이 값을 바꾸면 그 날짜 기록을 다시 확인한다. */
  refreshKey?: number
}

/**
 * 체중 입력 (날짜당 한 줄). 고른 날짜에 이미 기록이 있으면 그 값을 채워 두고, 저장하면 값이 바뀐다고 알려준다.
 * 입력 규칙은 백엔드와 같다: 1~300kg, 소수점 둘째 자리까지. 미래 날짜는 저장할 수 없다.
 */
export function WeightEntryForm({ date, today, onDateChange, onSaved, refreshKey = 0 }: WeightEntryFormProps) {
  return (
    <div className="flex flex-col gap-3">
      {onDateChange && <DateField label="날짜" date={date} maxDate={today} onChange={onDateChange} />}
      {/* 날짜가 바뀌거나 바깥에서 지우면 입력 상태를 처음부터 다시 시작한다. */}
      <WeightValueForm key={`${date}:${refreshKey}`} date={date} today={today} withDateField={!!onDateChange} onSaved={onSaved} />
    </div>
  )
}

interface WeightValueFormProps {
  date: string
  today: string
  withDateField: boolean
  onSaved: (record: WeightRecord) => void
}

function WeightValueForm({ date, today, withDateField, onSaved }: WeightValueFormProps) {
  const { showToast } = useToast()
  const [text, setText] = useState('')
  const [touched, setTouched] = useState(false)
  const [existing, setExisting] = useState<WeightRecord | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  // 사용자가 먼저 고쳤으면, 늦게 온 기존 값으로 덮어쓰지 않는다.
  const editedRef = useRef(false)

  useEffect(() => {
    if (!isValidDateParam(date) || isFutureDate(date, today)) return
    let cancelled = false
    weightApi
      .listByPeriod(date, date)
      .then((records) => {
        if (cancelled) return
        const found = records[0] ?? null
        setExisting(found)
        if (found && !editedRef.current) setText(formatWeight(found.weight))
      })
      // 기존 값을 못 받아도 입력·저장은 할 수 있다. (저장은 덮어쓰기라 결과가 같다)
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [date, today])

  const check = checkWeight(text)
  const dateInvalid = !isValidDateParam(date) || isFutureDate(date, today)
  const unchanged = existing !== null && check.ok && check.value === existing.weight
  const showError = !check.ok && !check.empty && touched

  const buttonLabel = (() => {
    if (submitting) return '저장 중…'
    if (dateInvalid) return '날짜를 확인하세요'
    if (!check.ok) return check.empty ? '체중을 입력하세요' : '체중을 확인하세요'
    if (unchanged) return '저장된 값과 같아요'
    return existing ? '바꾼 값 저장' : '저장'
  })()

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setTouched(true)
    if (!check.ok || dateInvalid || unchanged || submitting) return
    setSubmitting(true)
    setSubmitError(null)
    try {
      const saved = await weightApi.save(date, check.value)
      setExisting(saved)
      setText(formatWeight(saved.weight))
      editedRef.current = false
      showToast('체중을 저장했어요')
      onSaved(saved)
    } catch (err) {
      setSubmitError(messageOf(err, '저장하지 못했어요. 다시 시도해 주세요.'))
    } finally {
      setSubmitting(false)
    }
  }

  const dayName = relativeDayLabel(date, today)
  const existingHint = existing && (
    <FieldHint>
      {dayName ?? formatDateWithDay(date)} {formatWeight(existing.weight)}kg 기록이 있어요. 저장하면 이 값으로 바뀌어요.
    </FieldHint>
  )

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-3">
      <TextInput
        label={withDateField ? '체중 (kg)' : `${dayName ?? formatDateWithDay(date)} 체중 (kg)`}
        value={text}
        inputMode="decimal"
        autoComplete="off"
        placeholder="예: 62.35"
        invalid={showError}
        onChange={(e) => {
          editedRef.current = true
          setSubmitError(null)
          setText(sanitizeWeightInput(e.target.value))
        }}
        onBlur={() => setTouched(true)}
        className="tabular-nums"
        hint={showError ? <FieldHint tone="error">{check.message}</FieldHint> : existingHint || undefined}
      />
      {submitError && (
        <div className="rounded-input bg-error-bg px-3 py-2.5">
          <FieldHint tone="error">{submitError}</FieldHint>
        </div>
      )}
      <Button type="submit" disabled={submitting || dateInvalid || !check.ok || unchanged}>
        {buttonLabel}
      </Button>
    </form>
  )
}
