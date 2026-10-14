import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { messageOf } from '../../api/types'
import { addDays, parseDateParam, relativeDayLabel, todayParam } from '../../lib/date'

/** 기록 종류별 상세 화면(식사·배변·복약)이 함께 쓰는 값과 훅. 부품은 CategoryDetail.tsx */

export type Category = 'meal' | 'bowel' | 'medication'

export const CATEGORY_META: Record<Category, { label: string; icon: string; color: string; bg: string; box: string }> = {
  meal: { label: '식사', icon: 'restaurant', color: 'text-meal', bg: 'bg-meal-bg', box: 'bg-meal-bg text-meal' },
  bowel: { label: '배변', icon: 'waves', color: 'text-bowel', bg: 'bg-bowel-bg', box: 'bg-bowel-bg text-bowel' },
  medication: { label: '복약', icon: 'medication', color: 'text-med', bg: 'bg-med-bg', box: 'bg-med-bg text-med' },
}

/** 상세 화면 경로 */
export const DETAIL_PATH: Record<Category, string> = {
  meal: '/record/meal',
  bowel: '/record/bowel',
  medication: '/record/medication',
}

export const PERIOD_OPTIONS = [
  { value: '7', label: '7일' },
  { value: '30', label: '30일' },
  { value: '90', label: '90일' },
] as const

export type PeriodValue = (typeof PERIOD_OPTIONS)[number]['value']

const PERIOD_VALUES: readonly string[] = PERIOD_OPTIONS.map((option) => option.value)

/**
 * 상세 화면의 기간(?period=, 최근 7일 / 30일 / 90일, 오늘 기준)과 검색어(?q=).
 * 주소에 둬서 수정 화면에 다녀와도 유지된다.
 * 검색어는 화면 안 상태로 바로 들고 있고, 주소에는 따라 적기만 한다.
 * (입력칸 값을 주소에서 다시 읽으면 한 박자 늦게 반영돼 한글 조합이 깨지고 커서가 끝으로 튄다)
 */
export function useDetailParams() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [today] = useState(todayParam)
  const periodParam = searchParams.get('period') ?? ''
  const period = (PERIOD_VALUES.includes(periodParam) ? periodParam : '7') as PeriodValue
  const days = Number(period)
  const [query, setQueryState] = useState(() => searchParams.get('q') ?? '')
  const setParam = (key: string, value: string) =>
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        if (value === '') next.delete(key)
        else next.set(key, value)
        return next
      },
      { replace: true },
    )
  return {
    today,
    period,
    days,
    from: addDays(today, -(days - 1)),
    to: today,
    setPeriod: (next: PeriodValue) => setParam('period', next),
    query,
    setQuery: (next: string) => {
      setQueryState(next)
      setParam('q', next)
    },
  }
}

export type RangeState<T> = { status: 'loading' } | { status: 'ready'; items: T[] } | { status: 'error'; message: string }

/** 기간 조회. from·to가 바뀌면 다시 불러오고, reload()로 다시 시도한다. */
export function useRangeRecords<T>(fetchRange: (from: string, to: string) => Promise<T[]>, from: string, to: string, errorMessage: string) {
  const [reloadKey, setReloadKey] = useState(0)
  const requestKey = `${from}~${to}#${reloadKey}`
  const [result, setResult] = useState<{ key: string; value: RangeState<T> } | null>(null)
  const state: RangeState<T> = result?.key === requestKey ? result.value : { status: 'loading' }

  useEffect(() => {
    let cancelled = false
    fetchRange(from, to)
      .then((items) => !cancelled && setResult({ key: requestKey, value: { status: 'ready', items } }))
      .catch((err) => !cancelled && setResult({ key: requestKey, value: { status: 'error', message: messageOf(err, errorMessage) } }))
    return () => {
      cancelled = true
    }
  }, [fetchRange, from, to, requestKey, errorMessage])

  return { state, reload: () => setReloadKey((k) => k + 1) }
}

/** 날짜별로 묶기. 최근 날짜가 먼저, 같은 날 안에서는 시각 순(서버 정렬 그대로). */
export function groupByDate<T>(items: T[], dateOf: (item: T) => string): { date: string; items: T[] }[] {
  const groups = new Map<string, T[]>()
  for (const item of items) {
    const date = dateOf(item)
    groups.set(date, [...(groups.get(date) ?? []), item])
  }
  return [...groups.entries()].sort(([a], [b]) => (a < b ? 1 : -1)).map(([date, list]) => ({ date, items: list }))
}

// ---------------- 검색 ----------------

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

/** 띄어쓰기·대소문자를 무시하고 비교하려고 정리한다. ("10월 13일" = "10월13일") */
function normalize(text: string): string {
  return text.toLowerCase().replace(/\s+/g, '')
}

/**
 * 날짜로 검색할 수 있는 글자들: "2026-10-13", "10월 13일", "10/13", "10.13", "화요일", "오늘"·"어제".
 * 요일 한 글자("화")는 다른 단어에 너무 많이 걸려서 넣지 않는다.
 */
export function dateSearchTexts(dateParam: string, today: string): string[] {
  const date = parseDateParam(dateParam)
  if (!date) return [dateParam]
  const m = date.getMonth() + 1
  const d = date.getDate()
  const texts = [dateParam, `${m}월 ${d}일`, `${m}/${d}`, `${m}.${d}`, `${WEEKDAYS[date.getDay()]}요일`]
  const relative = relativeDayLabel(dateParam, today)
  if (relative) texts.push(relative)
  return texts
}

/**
 * 검색어가 기록의 글자들과 맞는지. 띄어쓰기는 무시한다.
 * - 검색어 전체가 한 글자들 안에 들어 있으면 맞다. ("통증 1" → "통증 1", "10월13일" → "10월 13일")
 * - 아니면 띄어 쓴 낱말이 모두 맞아야 한다(AND). ("김치 10월" → 김치가 들어간 10월 식사)
 *   이때 숫자 한 글자 낱말("1")은 시각·날짜 숫자에 너무 쉽게 걸려서 낱말로는 쓰지 않는다.
 * 검색어가 비어 있으면 모두 맞는다.
 */
export function matchesQuery(texts: (string | null | undefined)[], query: string): boolean {
  const phrase = normalize(query)
  if (phrase === '') return true
  const haystack = texts.filter(Boolean).map((text) => normalize(text as string))
  if (haystack.some((text) => text.includes(phrase))) return true
  const terms = query.trim().split(/\s+/).map(normalize)
  if (terms.length < 2 || terms.some((term) => /^\d$/.test(term))) return false
  return terms.every((term) => haystack.some((text) => text.includes(term)))
}
