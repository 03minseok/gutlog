import type { DosageForm, Medicine, MedicineCategory, ScheduleType } from '../../api/medication'

/** 내 복용 약 관리 화면 경로 */
export const MY_MEDICINES_PATH = '/record/medication/medicines'

export const DOSAGE_FORM_LABEL: Record<DosageForm, string> = {
  TABLET: '정제',
  CAPSULE: '캡슐',
  GRANULE: '과립',
  ORAL_LIQUID: '내복액',
  SUPPOSITORY: '좌약',
  ENEMA: '관장액',
  SC_INJECTION: '피하주사',
  IV_INJECTION: '정맥주사',
}

export const DOSAGE_FORMS = Object.keys(DOSAGE_FORM_LABEL) as DosageForm[]

export const CATEGORY_LABEL: Record<MedicineCategory, string> = {
  AMINOSALICYLATE: '5-ASA',
  CORTICOSTEROID: '스테로이드',
  IMMUNOMODULATOR: '면역조절제',
  BIOLOGIC: '생물학적제제',
  JAK_INHIBITOR: 'JAK 억제제',
  ANTI_TUBERCULOSIS: '항결핵제',
  OTHER: '기타',
}

export const CATEGORIES = Object.keys(CATEGORY_LABEL) as MedicineCategory[]

export const SCHEDULE_OPTIONS: readonly { value: ScheduleType; label: string }[] = [
  { value: 'DAILY', label: '매일' },
  { value: 'INTERVAL', label: '간격 투여' },
  { value: 'AS_NEEDED', label: '필요할 때' },
]

// 백엔드 검증과 같은 값
export const DAILY_DOSE_MIN = 1
export const DAILY_DOSE_MAX = 6
export const INTERVAL_MIN = 2
export const INTERVAL_MAX = 180
export const DOSE_MEMO_MAX = 20
export const MEDICINE_NAME_MAX = 50

/** 자주 쓰는 투여 간격 (주 1회, 2주, 4주, 8주, 12주) */
export const INTERVAL_PRESETS = [7, 14, 28, 56, 84] as const

/** '펜타사 서방정'처럼 보여줄 이름: 제품명 + 제형 */
export function medicineTitle(medicine: Medicine): string {
  return `${medicine.brandName} ${DOSAGE_FORM_LABEL[medicine.dosageForm]}`
}

/**
 * 복용량 규칙 (백엔드 DosageForm과 같다): 내복액만 0.1mL 단위로 0.1~100, 나머지는 0.5 단위로 0.5~20.
 * 소수 오차를 피하려고 단위의 정수배인지로 검사한다.
 */
export function doseRule(form: DosageForm): { step: number; min: number; max: number } {
  return form === 'ORAL_LIQUID' ? { step: 0.1, min: 0.1, max: 100 } : { step: 0.5, min: 0.5, max: 20 }
}

export function isValidDoseAmount(form: DosageForm, amount: number): boolean {
  const { step, min, max } = doseRule(form)
  const units = amount / step
  return amount >= min - 1e-9 && amount <= max + 1e-9 && Math.abs(units - Math.round(units)) < 1e-9
}

/** 1.5 + '정' → '1.5정' */
export function formatDose(amount: number | null, unit: string): string | null {
  if (amount === null) return null
  return `${Number(amount.toFixed(1))}${unit}`
}

/** 복용 방식 한 줄 설명: '하루 3회', '14일마다', '필요할 때' */
export function scheduleSummary(scheduleType: ScheduleType, dailyDoseCount: number | null, intervalDays: number | null): string {
  if (scheduleType === 'DAILY') return `하루 ${dailyDoseCount}회`
  if (scheduleType === 'INTERVAL') return `${intervalDays}일마다`
  return '필요할 때'
}
