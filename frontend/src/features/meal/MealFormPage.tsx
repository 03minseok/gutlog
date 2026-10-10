import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router'
import { mealApi } from '../../api/meal'
import { ApiError } from '../../api/types'
import { Button } from '../../components/Button'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { FieldHint } from '../../components/FieldHint'
import { Icon } from '../../components/Icon'
import { NotFoundView } from '../../components/NotFoundView'
import { useToast } from '../../components/toast/ToastContext'
import { isValidDateParam, splitDateTime, toDateTimeParam, toTimeValue, todayParam } from '../../lib/date'
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

const isNotFound = (err: unknown) => err instanceof ApiError && err.status === 404
const messageOf = (err: unknown, fallback: string) => (err instanceof ApiError ? err.message : fallback)

/** 식사 등록(/record/meal/new)·수정(/record/meal/:id/edit) 공용 화면. 하단 탭 없음. */
export function MealFormPage() {
  const params = useParams()
  const mealId = params.id ? Number(params.id) : null
  const isEdit = mealId !== null
  // /record/meal/abc/edit 같은 주소는 요청 없이 바로 "찾을 수 없음"
  const invalidId = mealId !== null && (!Number.isInteger(mealId) || mealId <= 0)
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
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

  const goToList = (date: string) => navigate(`/record?date=${date}`, { replace: true })

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
  const dateError = values.date > today ? '미래 날짜는 기록할 수 없어요' : null
  const canSubmit = hasFoods && values.date !== '' && values.time !== '' && !dateError && !submitting && !deleting

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
    if (mealId === null) return
    setDeleting(true)
    try {
      await mealApi.remove(mealId)
      showToast('식사를 삭제했어요')
      goToList(listDate)
    } catch (err) {
      setConfirmOpen(false)
      setDeleting(false)
      if (isNotFound(err)) setLoad({ status: 'notFound' })
      else setSubmitError(messageOf(err, '삭제하지 못했어요. 다시 시도해 주세요.'))
    }
  }

  const title = isEdit ? '식사 수정' : '식사 기록'
  const close = () => goToList(listDate)

  if (load.status === 'notFound') {
    return (
      <Screen title={title} onClose={close}>
        <NotFoundView title="식사 기록을 찾을 수 없어요" onBack={close} />
      </Screen>
    )
  }

  if (load.status === 'loading' || load.status === 'error') {
    return (
      <Screen title={title} onClose={close}>
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-6">
          {load.status === 'loading' ? (
            <p className="text-[15px] text-gray-500">불러오는 중…</p>
          ) : (
            <>
              <FieldHint tone="error">{load.message}</FieldHint>
              <Button variant="outline" onClick={() => window.location.reload()}>
                다시 시도
              </Button>
            </>
          )}
        </div>
      </Screen>
    )
  }

  return (
    <Screen
      title={title}
      onClose={close}
      action={
        isEdit && (
          <button
            type="button"
            onClick={() => setConfirmOpen(true)}
            disabled={submitting || deleting}
            className="ml-auto h-12 rounded-input px-3 text-[15px] font-semibold text-danger hover:bg-danger-bg disabled:text-gray-400"
          >
            삭제
          </button>
        )
      }
    >
      <form onSubmit={handleSubmit} noValidate className="flex min-h-0 flex-1 flex-col">
        <div className="flex flex-1 flex-col gap-7 overflow-y-auto px-5 pt-5 pb-6">
          <fieldset className="flex flex-col gap-2">
            <legend className="mb-2 text-[15px] font-bold">언제였나요?</legend>
            <div className="grid grid-cols-[minmax(0,1.2fr)_minmax(0,1fr)] gap-2">
              <PickerField icon="calendar_today" invalid={dateError !== null}>
                <input
                  type="date"
                  aria-label="날짜"
                  value={values.date}
                  max={today}
                  required
                  onChange={(e) => setValues((v) => ({ ...v, date: e.target.value }))}
                  className="picker-input min-w-0 flex-1 bg-transparent text-[16px] font-semibold text-ink tabular-nums outline-none"
                />
              </PickerField>
              <PickerField icon="schedule">
                <input
                  type="time"
                  aria-label="시간"
                  value={values.time}
                  required
                  onChange={(e) => setValues((v) => ({ ...v, time: e.target.value }))}
                  className="picker-input min-w-0 flex-1 bg-transparent text-[16px] font-semibold text-ink tabular-nums outline-none"
                />
              </PickerField>
            </div>
            {dateError && <FieldHint tone="error">{dateError}</FieldHint>}
          </fieldset>

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

        <div className="flex flex-none flex-col gap-2 border-t border-gray-100 px-5 pt-3 pb-8">
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
    </Screen>
  )
}

/** 배변 기록 시안의 입력 화면 틀: 헤더 56px(닫기 + 카테고리 아이콘 + 제목), border-b gray-100 */
function Screen({ title, onClose, action, children }: { title: string; onClose: () => void; action?: ReactNode; children: ReactNode }) {
  return (
    <div className="mx-auto flex h-dvh w-full max-w-[430px] flex-col bg-white">
      <header className="flex h-14 flex-none items-center gap-1 border-b border-gray-100 px-2">
        <button
          type="button"
          onClick={onClose}
          aria-label="닫기"
          className="flex h-12 w-12 items-center justify-center rounded-input text-ink hover:bg-gray-50"
        >
          <Icon name="close" />
        </button>
        <span className="flex h-7 w-7 items-center justify-center rounded-[8px] bg-meal-bg text-meal">
          <Icon name="restaurant" size={18} />
        </span>
        <h1 className="ml-1 text-[17px] font-bold">{title}</h1>
        {action}
      </header>
      {children}
    </div>
  )
}

/** 날짜·시간 입력 칸: h-14, 1.5px gray-200, rounded-input, 왼쪽 아이콘 20 gray-500 */
function PickerField({ icon, invalid = false, children }: { icon: string; invalid?: boolean; children: ReactNode }) {
  return (
    <label
      className={
        'relative flex h-14 items-center gap-2 rounded-input border-[1.5px] px-3.5 transition-[border-color,box-shadow] focus-within:ring-[3px] ' +
        (invalid ? 'border-danger focus-within:ring-danger-bg' : 'border-gray-200 focus-within:border-blue-600 focus-within:ring-blue-100')
      }
    >
      <Icon name={icon} size={20} className="text-gray-500" />
      {children}
    </label>
  )
}
