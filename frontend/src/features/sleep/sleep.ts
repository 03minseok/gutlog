/** 수면 입력 규칙. 백엔드 SleepRequest 검증(30분 ~ 24시간, 30분 단위)과 같다. */
export const SLEEP_MIN = 30
export const SLEEP_MAX = 1440
export const SLEEP_STEP = 30
/** 처음 −/＋ 를 눌렀을 때 시작하는 값 (7시간). 저장 전 값을 미리 골라두지는 않는다. */
export const SLEEP_START = 420

/** 고를 수 있는 값: 30분, 1시간, … 24시간 (48개) */
export const SLEEP_OPTIONS: readonly number[] = Array.from(
  { length: (SLEEP_MAX - SLEEP_MIN) / SLEEP_STEP + 1 },
  (_, i) => SLEEP_MIN + i * SLEEP_STEP,
)

export const SLEEP_PATH = '/record/sleep'

/** 기록 없음·오류 문구 */
export const SLEEP_LOAD_ERROR = '수면 기록을 불러오지 못했어요'
export const SLEEP_EMPTY_TEXT = '이 기간 수면 기록이 없어요'

/** 일어난 날 기준 안내 */
export const SLEEP_DATE_HINT = '일어난 날로 기록해요. 15일 밤에 자고 16일에 일어났다면 16일이에요.'

/** 450 → '7시간 30분', 420 → '7시간', 30 → '30분' */
export function formatSleep(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = Math.round(minutes % 60)
  if (hours === 0) return `${rest}분`
  return rest === 0 ? `${hours}시간` : `${hours}시간 ${rest}분`
}

/** 추이 기간 (오늘 포함 n일) */
export const SLEEP_PERIODS = [
  { value: '7', label: '7일' },
  { value: '30', label: '30일' },
  { value: '90', label: '90일' },
] as const

export type SleepPeriod = (typeof SLEEP_PERIODS)[number]['value']

export const DEFAULT_SLEEP_PERIOD: SleepPeriod = '30'
