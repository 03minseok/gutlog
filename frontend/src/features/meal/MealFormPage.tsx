import { useEffect, useState, type FormEvent } from 'react'
import { useParams, useSearchParams } from 'react-router'
import { mealApi } from '../../api/meal'
import { isNotFound, messageOf } from '../../api/types'
import { Button } from '../../components/Button'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { useCloseRecordForm } from '../../components/recordForm'
import { FieldHint } from '../../components/FieldHint'
import { NotFoundView } from '../../components/NotFoundView'
import { DateTimeFields, DeleteAction, FormLoadStatus, RecordFormScreen } from '../../components/RecordFormScreen'
import { useToast } from '../../components/toast/ToastContext'
import { isFutureDate, isValidDateParam, splitDateTime, toDateTimeParam, toTimeValue, todayParam } from '../../lib/date'
import { FoodChipInput } from './FoodChipInput'
import { addFoods, filterFoodSuggestions } from './foods'
import { useMyFoods } from './useMyFoods'

interface FormValues {
  date: string
  time: string
  foods: string[]
}

type LoadState =
  | { status: 'loading' }
  | { status: 'ready' }
  | { status: 'notFound' }
  | { status: 'error'; message: string }

/** 식사 등록(/record/meal/new)·수정(/record/meal/:id/edit) 공용 화면. 하단 탭 없음. */
export function MealFormPage() {
  const params = useParams()
  const mealId = params.id ? Number(params.id) : null
  const isEdit = mealId !== null
  // /record/meal/abc/edit 같은 주소는 요청 없이 바로 "찾을 수 없음"
  const invalidId = mealId !== null && (!Number.isInteger(mealId) || mealId <= 0)
  const [searchParams] = useSearchParams()
  const { showToast } = useToast()
  const [today] = useState(todayParam)

  const [values, setValues] = useState<FormValues>(() => {
    const requested = searchParams.get('date')
    const date = isValidDateParam(requested) && requested <= today ? requested : today
    return { date, time: toTimeValue(new Date()), foods: [] }
  })
  // 닫기·삭제 후 돌아갈 목록 날짜 (수정이면 원래 기록의 날짜)
  const [listDate, setListDate] = useState(values.date)
  const [draft, setDraft] = useState('')
  const [foodError, setFoodError] = useState<string | null>(null)
  const [load, setLoad] = useState<LoadState>(
    invalidId ? { status: 'notFound' } : isEdit ? { status: 'loading' } : { status: 'ready' },
  )
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [deleting, setDeleting] = useState(false)
  // 자동완성용 내 음식 목록 (화면 진입 시 한 번, 실패하면 빈 목록)
  const myFoods = useMyFoods()

  useEffect(() => {
    if (mealId === null || invalidId) return
    let cancelled = false
    mealApi
      .get(mealId)
      .then((meal) => {
        if (cancelled) return
        const { date, time } = splitDateTime(meal.eatenAt)
        setValues({ date, time, foods: meal.foods })
        setListDate(date)
        setLoad({ status: 'ready' })
      })
      .catch((err) => {
        if (cancelled) return
        setLoad(isNotFound(err) ? { status: 'notFound' } : { status: 'error', message: messageOf(err, '식사 기록을 불러오지 못했어요') })
      })
    return () => {
      cancelled = true
    }
  }, [mealId, invalidId])

  // 닫기: 보던 화면으로 돌아간다. 주소로 바로 들어왔으면 기록 탭(목록 날짜)으로. (components/recordForm.ts)
  const closeForm = useCloseRecordForm(listDate)

  // --- 음식 칩 ---
  function commitFoods(text: string, remainder = '') {
    const result = addFoods(values.foods, text)
    setFoodError(result.error)
    if (result.error) {
      setDraft(text + (remainder ? `,${remainder}` : ''))
      return
    }
    setValues((v) => ({ ...v, foods: result.foods }))
    setDraft(remainder)
  }

  function removeFood(index: number) {
    setValues((v) => ({ ...v, foods: v.foods.filter((_, i) => i !== index) }))
    setFoodError(null)
  }

  // 저장할 때 입력칸에 남은 글자도 음식으로 포함한다.
  const pendingFoods = addFoods(values.foods, draft)
  const hasFoods = pendingFoods.foods.length > 0 || draft.trim() !== ''
  const canSubmit =
    hasFoods && values.date !== '' && values.time !== '' && !isFutureDate(values.date, today) && !submitting && !deleting

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!canSubmit) return
    if (pendingFoods.error) {
      setFoodError(pendingFoods.error)
      return
    }
    setSubmitting(true)
    setSubmitError(null)
    const body = { eatenAt: toDateTimeParam(values.date, values.time), foods: pendingFoods.foods }
    try {
      if (mealId !== null) await mealApi.update(mealId, body)
      else await mealApi.create(body)
      showToast('식사를 저장했어요')
      closeForm(values.date)
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
    if (mealId === null) return
    setDeleting(true)
    try {
      await mealApi.remove(mealId)
      showToast('식사를 삭제했어요')
      closeForm(listDate)
    } catch (err) {
      setConfirmOpen(false)
      setDeleting(false)
      if (isNotFound(err)) setLoad({ status: 'notFound' })
      else setSubmitError(messageOf(err, '삭제하지 못했어요. 다시 시도해 주세요.'))
    }
  }

  const title = isEdit ? '식사 수정' : '식사 기록'
  const close = () => closeForm()

  if (load.status === 'notFound') {
    return (
      <RecordFormScreen category="meal" title={title} onClose={close}>
        <NotFoundView title="식사 기록을 찾을 수 없어요" onBack={close} />
      </RecordFormScreen>
    )
  }

  if (load.status === 'loading' || load.status === 'error') {
    return (
      <RecordFormScreen category="meal" title={title} onClose={close}>
        <FormLoadStatus error={load.status === 'error' ? load.message : null} />
      </RecordFormScreen>
    )
  }

  return (
    <RecordFormScreen
      category="meal"
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
            onDateChange={(date) => setValues((v) => ({ ...v, date }))}
            onTimeChange={(time) => setValues((v) => ({ ...v, time }))}
          />

          <FoodChipInput
            label="무엇을 먹었나요?"
            foods={values.foods}
            draft={draft}
            error={foodError}
            suggestions={filterFoodSuggestions(myFoods, draft, values.foods)}
            onDraftChange={(text) => {
              setDraft(text)
              setFoodError(null)
            }}
            onCommit={commitFoods}
            onRemove={removeFood}
          />
        </div>

        <div className="flex flex-none flex-col gap-2 border-t border-gray-200 bg-white px-4 pt-2.5 pb-3">
          {submitError && <FieldHint tone="error">{submitError}</FieldHint>}
          <Button type="submit" disabled={!canSubmit}>
            {hasFoods ? '저장' : '음식을 입력하세요'}
          </Button>
        </div>
      </form>

      <ConfirmDialog
        open={confirmOpen}
        title="이 식사 기록을 삭제할까요?"
        description="삭제한 기록은 복구할 수 없어요."
        confirmLabel="삭제"
        pending={deleting}
        onConfirm={handleDelete}
        onCancel={() => setConfirmOpen(false)}
      />
    </RecordFormScreen>
  )
}
