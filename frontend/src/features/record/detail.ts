import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { messageOf } from '../../api/types'
import { addDays, isValidDateParam, parseDateParam, toDateParam, todayParam } from '../../lib/date'

/** 기록 종류별 상세 화면(식사·배변·복약)이 함께 쓰는 값과 훅. 부품은 CategoryDetail.tsx */

export type Category = 'meal' | 'bowel' | 'medication'

export const CATEGORY_META: Record<Category, { label: string; icon: string; color: string; bg: string; border: string; box: string }> = {
  meal: { label: '식사', icon: 'restaurant', color: 'text-meal', bg: 'bg-meal-bg', border: 'border-meal', box: 'bg-meal-bg text-meal' },
  bowel: { label: '배변', icon: 'waves', color: 'text-bowel', bg: 'bg-bowel-bg', border: 'border-bowel', box: 'bg-bowel-bg text-bowel' },
  medication: { label: '복약', icon: 'medication', color: 'text-med', bg: 'bg-med-bg', border: 'border-med', box: 'bg-med-bg text-med' },
}

/** 상세 화면 경로 */
export const DETAIL_PATH: Record<Category, string> = {
  meal: '/record/meal',
  bowel: '/record/bowel',
  medication: '/record/medication',
}

/** 그 날짜가 들어 있는 주의 일요일 (기록 탭 달력과 같이 일요일 시작) */
export function weekStartOf(date: string): string {
  const day = parseDateParam(date)?.getDay() ?? 0
  return addDays(date, -day)
}

/** 상세 화면 달력이 보여주는 범위: 주 · 월 · 년 */
export type RangeMode = 'week' | 'month' | 'year'
export const RANGE_MODES: readonly RangeMode[] = ['week', 'month', 'year']

/** 그 날짜가 들어 있는 범위의 첫날·마지막 날 */
export function periodOf(mode: RangeMode, date: string): { start: string; end: string } {
  if (mode === 'week') {
    const start = weekStartOf(date)
    return { start, end: addDays(start, 6) }
  }
  const d = parseDateParam(date) ?? new Date()
  if (mode === 'month') {
    return { start: toDateParam(new Date(d.getFullYear(), d.getMonth(), 1)), end: toDateParam(new Date(d.getFullYear(), d.getMonth() + 1, 0)) }
  }
  return { start: `${d.getFullYear()}-01-01`, end: `${d.getFullYear()}-12-31` }
}

/** 범위를 n칸 옮긴 첫날 (주 ±7일, 월 ±1달, 년 ±1년) */
export function shiftPeriod(mode: RangeMode, start: string, n: number): string {
  if (mode === 'week') return addDays(start, n * 7)
  const d = parseDateParam(start) ?? new Date()
  if (mode === 'month') return toDateParam(new Date(d.getFullYear(), d.getMonth() + n, 1))
  return `${d.getFullYear() + n}-01-01`
}

/** '10월 11일 – 17일'(달이 바뀌면 '9월 27일 – 10월 3일') · '2026년 10월' · '2026년' */
export function periodLabel(mode: RangeMode, start: string): string {
  const [y, m, d] = [Number(start.slice(0, 4)), Number(start.slice(5, 7)), Number(start.slice(8))]
  if (mode === 'year') return `${y}년`
  if (mode === 'month') return `${y}년 ${m}월`
  const end = addDays(start, 6)
  const [em, ed] = [Number(end.slice(5, 7)), Number(end.slice(8))]
  return m === em ? `${m}월 ${d}일 – ${ed}일` : `${m}월 ${d}일 – ${em}월 ${ed}일`
}

/** 기간 줄 제목 (고른 날이 없을 때): '주 전체' · '10월 전체' · '2026년 전체' */
export function periodTitle(mode: RangeMode, start: string): string {
  if (mode === 'year') return `${start.slice(0, 4)}년 전체`
  if (mode === 'month') return `${Number(start.slice(5, 7))}월 전체`
  return '주 전체'
}

/** 주소 값 여러 개를 한 번에 고친다. 값이 null·빈 글자면 지운다. (replace — 뒤로 가기 기록을 쌓지 않는다) */
export function useSetParams() {
  const [, setSearchParams] = useSearchParams()
  return (patch: Record<string, string | null>) =>
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        for (const [key, value] of Object.entries(patch)) {
          if (value === null || value === '') next.delete(key)
          else next.set(key, value)
        }
        return next
      },
      { replace: true },
    )
}

/**
 * 식사·배변 상세의 달력 범위와 고른 날. 주소에 둬서 수정 화면에 다녀와도 유지된다.
 * - ?range= week(기본) · month · year, ?from= 범위의 첫날(없으면 오늘이 든 범위). 오늘보다 뒤 범위로는 갈 수 없다.
 * - ?date= 고른 날(범위 안). 없으면 범위 전체 — 들어오면 이번 주 전체이고 아무 날도 눌려 있지 않다.
 *   같은 날을 다시 누르면 범위 전체로 돌아간다. 범위를 옮기거나 바꾸면 고른 날은 풀린다(주 → 월로 넓힐 때만 유지).
 * - 달력 제목을 누르면 주 → 월 → 년 → 주 순으로 바뀐다. 년 보기에서 달을 누르면 그 달 보기로.
 */
export function useRangeParams() {
  const [searchParams] = useSearchParams()
  const setParams = useSetParams()
  const [today] = useState(todayParam)
  const rangeParam = searchParams.get('range') as RangeMode | null
  const mode: RangeMode = rangeParam && RANGE_MODES.includes(rangeParam) ? rangeParam : 'week'
  const current = periodOf(mode, today).start
  const fromParam = searchParams.get('from')
  const start = isValidDateParam(fromParam) && periodOf(mode, fromParam).start <= current ? periodOf(mode, fromParam).start : current
  const { end } = periodOf(mode, start)
  const dateParam = searchParams.get('date')
  const selected = isValidDateParam(dateParam) && dateParam >= start && dateParam <= end && dateParam <= today ? dateParam : null

  const go = (nextMode: RangeMode, nextStart: string, nextDate: string | null) =>
    setParams({
      range: nextMode === 'week' ? null : nextMode,
      from: nextStart === periodOf(nextMode, today).start ? null : nextStart,
      date: nextDate,
    })
  // 범위를 바꿀 때 기준이 되는 날: 고른 날 → 범위 안의 오늘 → 범위 첫날
  const anchor = selected ?? (today >= start && today <= end ? today : start)

  return {
    today,
    mode,
    start,
    end,
    selected,
    toggleDate: (date: string) => setParams({ date: date === selected ? null : date }),
    clearDate: () => setParams({ date: null }),
    move: (n: number) => go(mode, shiftPeriod(mode, start, n), null),
    cycleMode: () => {
      const next = RANGE_MODES[(RANGE_MODES.indexOf(mode) + 1) % RANGE_MODES.length]
      go(next, periodOf(next, anchor).start, next === 'month' ? selected : null)
    },
    openMonth: (monthStart: string) => go('month', monthStart, null),
  }
}

/** 식사 상세 검색어(?q=). 화면 안 상태로 바로 들고, 주소에는 따라 적기만 한다. (입력칸 값을 주소에서 다시 읽으면 한글 조합이 깨진다) */
export function useQueryParam() {
  const [searchParams] = useSearchParams()
  const setParams = useSetParams()
  const [query, setQueryState] = useState(() => searchParams.get('q') ?? '')
  return {
    query,
    setQuery: (next: string) => {
      setQueryState(next)
      setParams({ q: next })
    },
  }
}

export type RangeState<T> = { status: 'loading' } | { status: 'ready'; items: T[] } | { status: 'error'; message: string }

/**
 * 기간 조회. from·to가 바뀌면 다시 불러오고, reload()로 다시 시도한다.
 * 범위를 바꿔 다시 불러오는 동안에는 이전 목록을 그대로 보여주고 loadingMore를 true로 둔다. (화면이 비지 않게)
 */
export function useRangeRecords<T>(fetchRange: (from: string, to: string) => Promise<T[]>, from: string, to: string, errorMessage: string) {
  const [reloadKey, setReloadKey] = useState(0)
  const requestKey = `${from}~${to}#${reloadKey}`
  const [result, setResult] = useState<{ key: string; value: RangeState<T> } | null>(null)
  const fresh = result?.key === requestKey
  const keepPrevious = !fresh && result?.value.status === 'ready'
  const state: RangeState<T> = fresh || keepPrevious ? result!.value : { status: 'loading' }

  useEffect(() => {
    let cancelled = false
    fetchRange(from, to)
      .then((items) => !cancelled && setResult({ key: requestKey, value: { status: 'ready', items } }))
      .catch((err) => !cancelled && setResult({ key: requestKey, value: { status: 'error', message: messageOf(err, errorMessage) } }))
    return () => {
      cancelled = true
    }
  }, [fetchRange, from, to, requestKey, errorMessage])

  return { state, loadingMore: keepPrevious, reload: () => setReloadKey((k) => k + 1) }
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

/** 띄어쓰기·대소문자를 무시하고 비교하려고 정리한다. ("10월 13일" = "10월13일") */
function normalize(text: string): string {
  return text.toLowerCase().replace(/\s+/g, '')
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
