import { useEffect, useId, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router'
import { adminMedicineApi, type DosageForm, type MedicineCategory } from '../../api/medication'
import { messageOf } from '../../api/types'
import { Button } from '../../components/Button'
import { FieldHint } from '../../components/FieldHint'
import { NotFoundView } from '../../components/NotFoundView'
import { FormLoadStatus, RecordFormScreen } from '../../components/RecordFormScreen'
import { TextInput } from '../../components/TextInput'
import { useToast } from '../../components/toast/ToastContext'
import { CATEGORIES, CATEGORY_LABEL, DOSAGE_FORM_LABEL, DOSAGE_FORMS, MEDICINE_NAME_MAX } from '../medication/constants'
import { ADMIN_MEDICINES_PATH } from './constants'

interface FormValues {
  genericName: string
  brandName: string
  dosageForm: DosageForm | null
  category: MedicineCategory | null
}

type LoadState = { status: 'loading' } | { status: 'ready' } | { status: 'notFound' } | { status: 'error'; message: string }

/** 관리자 약 추가(/admin/medicines/new)·수정(/admin/medicines/:id/edit). 같은 성분명+제품명+제형은 하나만 있을 수 있다. */
export function AdminMedicineFormPage() {
  const params = useParams()
  const medicineId = params.id ? Number(params.id) : null
  const isEdit = medicineId !== null
  const invalidId = medicineId !== null && (!Number.isInteger(medicineId) || medicineId <= 0)
  const navigate = useNavigate()
  const { showToast } = useToast()
  const formLabelId = useId()
  const categoryLabelId = useId()

  const [values, setValues] = useState<FormValues>({ genericName: '', brandName: '', dosageForm: null, category: null })
  const [load, setLoad] = useState<LoadState>(invalidId ? { status: 'notFound' } : isEdit ? { status: 'loading' } : { status: 'ready' })
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  useEffect(() => {
    if (medicineId === null || invalidId) return
    let cancelled = false
    // 단건 조회 API가 없어서 관리자 목록에서 찾는다.
    adminMedicineApi
      .list()
      .then((items) => {
        if (cancelled) return
        const medicine = items.find((m) => m.id === medicineId)
        if (!medicine) {
          setLoad({ status: 'notFound' })
          return
        }
        setValues({ genericName: medicine.genericName, brandName: medicine.brandName, dosageForm: medicine.dosageForm, category: medicine.category })
        setLoad({ status: 'ready' })
      })
      .catch((err) => !cancelled && setLoad({ status: 'error', message: messageOf(err, '약 정보를 불러오지 못했어요') }))
    return () => {
      cancelled = true
    }
  }, [medicineId, invalidId])

  const update = (patch: Partial<FormValues>) => setValues((v) => ({ ...v, ...patch }))
  const goToList = () => navigate(ADMIN_MEDICINES_PATH, { replace: true })

  const generic = values.genericName.trim()
  const brand = values.brandName.trim()
  const missing = !generic
    ? '성분명을 입력하세요'
    : !brand
      ? '제품명을 입력하세요'
      : !values.dosageForm
        ? '제형을 선택하세요'
        : !values.category
          ? '분류를 선택하세요'
          : null
  const canSubmit = missing === null && generic.length <= MEDICINE_NAME_MAX && brand.length <= MEDICINE_NAME_MAX && !submitting

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!canSubmit || !values.dosageForm || !values.category) return
    setSubmitting(true)
    setSubmitError(null)
    const body = { genericName: generic, brandName: brand, dosageForm: values.dosageForm, category: values.category }
    try {
      if (medicineId !== null) await adminMedicineApi.update(medicineId, body)
      else await adminMedicineApi.create(body)
      showToast('약을 저장했어요')
      goToList()
    } catch (err) {
      // 409: 같은 성분명 + 제품명 + 제형이 이미 있다.
      setSubmitError(messageOf(err, '저장하지 못했어요. 다시 시도해 주세요.'))
      setSubmitting(false)
    }
  }

  const title = isEdit ? '약 수정 (관리자)' : '약 추가 (관리자)'

  if (load.status === 'notFound') {
    return (
      <RecordFormScreen category="medication" title={title} onClose={goToList}>
        <NotFoundView title="약을 찾을 수 없어요" onBack={goToList} />
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

  return (
    <RecordFormScreen category="medication" title={title} onClose={goToList}>
      <form onSubmit={handleSubmit} noValidate className="flex min-h-0 flex-1 flex-col">
        <div className="flex flex-1 flex-col gap-6 overflow-y-auto px-4 pt-5 pb-6">
          <TextInput
            label="성분명 (일반명)"
            value={values.genericName}
            maxLength={MEDICINE_NAME_MAX}
            placeholder="예: 메살라진"
            onChange={(e) => update({ genericName: e.target.value })}
          />
          <TextInput
            label="제품명 (국내)"
            value={values.brandName}
            maxLength={MEDICINE_NAME_MAX}
            placeholder="예: 펜타사"
            onChange={(e) => update({ brandName: e.target.value })}
          />
          <OptionGrid
            labelId={formLabelId}
            label="제형"
            hint="제형에 따라 복용량 단위가 정해져요 (정제 → 정, 관장액 → 회)"
            options={DOSAGE_FORMS.map((form) => ({ value: form, label: DOSAGE_FORM_LABEL[form] }))}
            value={values.dosageForm}
            onChange={(dosageForm) => update({ dosageForm })}
          />
          <OptionGrid
            labelId={categoryLabelId}
            label="분류"
            options={CATEGORIES.map((category) => ({ value: category, label: CATEGORY_LABEL[category] }))}
            value={values.category}
            onChange={(category) => update({ category })}
          />
        </div>

        <div className="flex flex-none flex-col gap-2 border-t border-gray-100 px-4 pt-3 pb-8">
          {submitError && <FieldHint tone="error">{submitError}</FieldHint>}
          <Button type="submit" disabled={!canSubmit}>
            {missing ?? '저장'}
          </Button>
        </div>
      </form>
    </RecordFormScreen>
  )
}

interface OptionGridProps<T extends string> {
  labelId: string
  label: string
  hint?: string
  options: { value: T; label: string }[]
  value: T | null
  onChange: (value: T) => void
}

/** 2열 선택 칩 (라디오 그룹) */
function OptionGrid<T extends string>({ labelId, label, hint, options, value, onChange }: OptionGridProps<T>) {
  return (
    <div className="flex flex-col gap-2">
      <span id={labelId} className="text-label font-semibold text-gray-700">
        {label}
      </span>
      <div role="radiogroup" aria-labelledby={labelId} className="grid grid-cols-2 gap-2">
        {options.map((option) => {
          const selected = option.value === value
          return (
            <button
              key={option.value}
              type="button"
              role="radio"
              aria-checked={selected}
              onClick={() => onChange(option.value)}
              className={
                'h-11 rounded-btn text-body transition-colors ' +
                (selected ? 'border-2 border-ink bg-sky-50 font-bold text-sky-700' : 'border-[1.5px] border-gray-400 bg-white font-medium text-ink')
              }
            >
              {option.label}
            </button>
          )
        })}
      </div>
      {hint && <FieldHint>{hint}</FieldHint>}
    </div>
  )
}
