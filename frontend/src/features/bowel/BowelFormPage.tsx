import { useEffect, useId, useState, type FormEvent } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router'
import { bowelApi, type BristolType } from '../../api/bowel'
import { isNotFound, messageOf } from '../../api/types'
import { Button } from '../../components/Button'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { FieldHint } from '../../components/FieldHint'
import { NotFoundView } from '../../components/NotFoundView'
import { DateTimeFields, DeleteAction, FormLoadStatus, RecordFormScreen } from '../../components/RecordFormScreen'
import { useToast } from '../../components/toast/ToastContext'
import { isFutureDate, isValidDateParam, splitDateTime, toDateTimeParam, toTimeValue, todayParam } from '../../lib/date'
import { BleedingChoice } from './BleedingChoice'
import { BristolSelect } from './BristolSelect'
import { MEMO_MAX_LENGTH } from './constants'
import { PainScale } from './PainScale'

interface FormValues {
  date: string
  time: string
  /** 혈변·형태·통증은 사용자가 고르기 전까지 null. 손대지 않은 기본값이 저장되면 안 된다. */
  bowelCondition: BristolType | null
  bleeding: boolean | null
  painLevel: number | null
  memo: string
}

type LoadState =
  | { status: 'loading' }
  | { status: 'ready' }
  | { status: 'notFound' }
  | { status: 'error'; message: string }

/** 아직 고르지 않은 첫 항목을 저장 버튼 문구로 안내한다. (화면 순서대로) */
function missingLabel(values: FormValues): string | null {
  if (values.bowelCondition === null) return '배변 형태를 선택하세요'
  if (values.bleeding === null) return '혈변 여부를 선택하세요'
  if (values.painLevel === null) return '통증 정도를 선택하세요'
  return null
}

/** 배변 등록(/record/bowel/new)·수정(/record/bowel/:id/edit) 공용 화면. 하단 탭 없음. */
export function BowelFormPage() {
  const params = useParams()
  const bowelId = params.id ? Number(params.id) : null
  const isEdit = bowelId !== null
  // /record/bowel/abc/edit 같은 주소는 요청 없이 바로 "찾을 수 없음"
  const invalidId = bowelId !== null && (!Number.isInteger(bowelId) || bowelId <= 0)
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { showToast } = useToast()
  const [today] = useState(todayParam)
  const memoId = useId()
  const memoCountId = useId()

  const [values, setValues] = useState<FormValues>(() => {
    const requested = searchParams.get('date')
    const date = isValidDateParam(requested) && requested <= today ? requested : today
    return { date, time: toTimeValue(new Date()), bowelCondition: null, bleeding: null, painLevel: null, memo: '' }
  })
  // 닫기·삭제 후 돌아갈 목록 날짜 (수정이면 원래 기록의 날짜)
  const [listDate, setListDate] = useState(values.date)
  const [load, setLoad] = useState<LoadState>(
    invalidId ? { status: 'notFound' } : isEdit ? { status: 'loading' } : { status: 'ready' },
  )
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [deleting, setDeleting] = useState(false)

  useEffect(() => {
    if (bowelId === null || invalidId) return
    let cancelled = false
    bowelApi
      .get(bowelId)
      .then((bowel) => {
        if (cancelled) return
        const { date, time } = splitDateTime(bowel.bowelAt)
        setValues({
          date,
          time,
          bowelCondition: bowel.bowelCondition,
          bleeding: bowel.bleeding,
          painLevel: bowel.painLevel,
          memo: bowel.memo ?? '',
        })
        setListDate(date)
        setLoad({ status: 'ready' })
      })
      .catch((err) => {
        if (cancelled) return
        setLoad(isNotFound(err) ? { status: 'notFound' } : { status: 'error', message: messageOf(err, '배변 기록을 불러오지 못했어요') })
      })
    return () => {
      cancelled = true
    }
  }, [bowelId, invalidId])

  const goToList = (date: string) => navigate(`/record?date=${date}`, { replace: true })
  const update = (patch: Partial<FormValues>) => setValues((v) => ({ ...v, ...patch }))

  const missing = missingLabel(values)
  const canSubmit =
    missing === null && values.date !== '' && values.time !== '' && !isFutureDate(values.date, today) && !submitting && !deleting

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const { bowelCondition, bleeding, painLevel } = values
    if (!canSubmit || bowelCondition === null || bleeding === null || painLevel === null) return
    setSubmitting(true)
    setSubmitError(null)
    const memo = values.memo.trim()
    // 수정은 전체 교체라 메모를 비우면 null을 보내 지운다.
    const body = { bowelAt: toDateTimeParam(values.date, values.time), bleeding, bowelCondition, painLevel, memo: memo === '' ? null : memo }
    try {
      if (bowelId !== null) await bowelApi.update(bowelId, body)
      else await bowelApi.create(body)
      showToast('배변 기록을 저장했어요')
      goToList(values.date)
    } catch (err) {
      if (isNotFound(err)) {
        setLoad({ status: 'notFound' })
      } else {
        setSubmitError(messageOf(err, '저장하지 못했어요. 다시 시도해 주세요.'))
      }
      setSubmitting(false)
    }
  }

  async function handleDelete() {
    if (bowelId === null) return
    setDeleting(true)
    try {
      await bowelApi.remove(bowelId)
      showToast('배변 기록을 삭제했어요')
      goToList(listDate)
    } catch (err) {
      setConfirmOpen(false)
      setDeleting(false)
      if (isNotFound(err)) setLoad({ status: 'notFound' })
      else setSubmitError(messageOf(err, '삭제하지 못했어요. 다시 시도해 주세요.'))
    }
  }

  const title = isEdit ? '배변 수정' : '배변 기록'
  const close = () => goToList(listDate)

  if (load.status === 'notFound') {
    return (
      <RecordFormScreen category="bowel" title={title} onClose={close}>
        <NotFoundView title="배변 기록을 찾을 수 없어요" onBack={close} />
      </RecordFormScreen>
    )
  }

  if (load.status === 'loading' || load.status === 'error') {
    return (
      <RecordFormScreen category="bowel" title={title} onClose={close}>
        <FormLoadStatus error={load.status === 'error' ? load.message : null} />
      </RecordFormScreen>
    )
  }

  return (
    <RecordFormScreen
      category="bowel"
      title={title}
      onClose={close}
      action={isEdit && <DeleteAction onClick={() => setConfirmOpen(true)} disabled={submitting || deleting} />}
    >
      <form onSubmit={handleSubmit} noValidate className="flex min-h-0 flex-1 flex-col">
        <div className="flex flex-1 flex-col gap-6 overflow-y-auto px-4 pt-4 pb-6">
          <DateTimeFields
            date={values.date}
            time={values.time}
            maxDate={today}
            onDateChange={(date) => update({ date })}
            onTimeChange={(time) => update({ time })}
          />

          <BristolSelect value={values.bowelCondition} onChange={(bowelCondition) => update({ bowelCondition })} />

          <BleedingChoice value={values.bleeding} onChange={(bleeding) => update({ bleeding })} />

          <PainScale value={values.painLevel} onChange={(painLevel) => update({ painLevel })} />

          <div className="flex flex-col gap-2">
            <label htmlFor={memoId} className="text-body font-bold">
              메모 <span className="ml-1 text-caption font-medium text-gray-500">선택</span>
            </label>
            <textarea
              id={memoId}
              value={values.memo}
              onChange={(e) => update({ memo: e.target.value })}
              rows={3}
              maxLength={MEMO_MAX_LENGTH}
              placeholder="급박감, 잔변감, 점액 등 특이사항"
              aria-describedby={memoCountId}
              className="resize-none rounded-input border-[1.5px] border-gray-400 px-3.5 py-3 text-body text-ink outline-none transition-[border-color,box-shadow] focus:border-sky-700 focus:ring-[3px] focus:ring-sky-100"
            />
            <span id={memoCountId} className="self-end text-caption font-medium text-gray-500 tabular-nums">
              {MEMO_MAX_LENGTH - values.memo.length}자 남음
            </span>
          </div>
        </div>

        <div className="flex flex-none flex-col gap-2 border-t border-gray-200 bg-white px-4 pt-2.5 pb-[max(16px,env(safe-area-inset-bottom))]">
          {submitError && <FieldHint tone="error">{submitError}</FieldHint>}
          <Button type="submit" disabled={!canSubmit}>
            {missing ?? '저장'}
          </Button>
        </div>
      </form>

      <ConfirmDialog
        open={confirmOpen}
        title="이 배변 기록을 삭제할까요?"
        description="삭제한 기록은 복구할 수 없어요."
        confirmLabel="삭제"
        pending={deleting}
        onConfirm={handleDelete}
        onCancel={() => setConfirmOpen(false)}
      />
    </RecordFormScreen>
  )
}
