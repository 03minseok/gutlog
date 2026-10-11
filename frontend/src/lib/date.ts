/**
 * 날짜·시각 유틸.
 * 백엔드는 시간대 없는 한국 시간(yyyy-MM-dd, yyyy-MM-ddTHH:mm:ss)을 쓰므로
 * toISOString()(UTC 변환)은 쓰지 않고 로컬 시각 그대로 문자열을 만든다.
 */

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']
const DATE_PARAM = /^(\d{4})-(\d{2})-(\d{2})$/

const pad = (n: number) => String(n).padStart(2, '0')

/** Date → 'yyyy-MM-dd' */
export function toDateParam(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** Date → 'HH:mm' (input type="time" 값) */
export function toTimeValue(date: Date): string {
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function todayParam(): string {
  return toDateParam(new Date())
}

/** 'yyyy-MM-dd' → 로컬 자정 Date. 형식이 틀리거나 없는 날짜(2월 30일 등)면 null */
export function parseDateParam(value: string | null | undefined): Date | null {
  const match = value ? DATE_PARAM.exec(value) : null
  if (!match) return null
  const [, y, m, d] = match.map(Number)
  const date = new Date(y, m - 1, d)
  return toDateParam(date) === value ? date : null
}

export function isValidDateParam(value: string | null | undefined): value is string {
  return parseDateParam(value) !== null
}

/** 'yyyy-MM-dd'에 n일 더하기 */
export function addDays(dateParam: string, days: number): string {
  const date = parseDateParam(dateParam) ?? new Date()
  return toDateParam(new Date(date.getFullYear(), date.getMonth(), date.getDate() + days))
}

/** 'yyyy-MM-dd' + 'HH:mm' → 'yyyy-MM-ddTHH:mm:00' */
export function toDateTimeParam(dateParam: string, time: string): string {
  return `${dateParam}T${time}:00`
}

/** 'yyyy-MM-ddTHH:mm:ss' → { date: 'yyyy-MM-dd', time: 'HH:mm' } */
export function splitDateTime(dateTime: string): { date: string; time: string } {
  return { date: dateTime.slice(0, 10), time: dateTime.slice(11, 16) }
}

/** 'yyyy-MM-dd' → '10월 8일 목요일' */
export function formatDateLabel(dateParam: string): string {
  const date = parseDateParam(dateParam)
  if (!date) return dateParam
  return `${date.getMonth() + 1}월 ${date.getDate()}일 ${WEEKDAYS[date.getDay()]}요일`
}

/** 오늘·어제면 그 이름, 아니면 null */
export function relativeDayLabel(dateParam: string, today: string = todayParam()): string | null {
  if (dateParam === today) return '오늘'
  if (dateParam === addDays(today, -1)) return '어제'
  return null
}

/** 'yyyy-MM-dd'끼리 비교: date가 today보다 뒤면 true */
export function isFutureDate(date: string, today: string): boolean {
  return date > today
}
