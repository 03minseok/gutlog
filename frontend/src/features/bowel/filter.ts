import type { Bowel, BristolType } from '../../api/bowel'

/** 배변 상세 필터 값과 거르기 (화면은 BowelFilters.tsx) */

export interface BowelFilterValue {
  bleeding: boolean
  /** 통증 n 이상 (1~5). null이면 전체 */
  pain: number | null
  /** 고른 형태들. 비어 있으면 전체 */
  types: BristolType[]
}

export const PAIN_FILTER_LEVELS = [1, 2, 3, 4, 5] as const

export function isFilterActive(value: BowelFilterValue): boolean {
  return value.bleeding || value.pain !== null || value.types.length > 0
}

/** 조건을 모두 만족하는 기록만 (AND) */
export function matchesFilter(bowel: Bowel, value: BowelFilterValue): boolean {
  if (value.bleeding && !bowel.bleeding) return false
  if (value.pain !== null && bowel.painLevel < value.pain) return false
  if (value.types.length > 0 && !value.types.includes(bowel.bowelCondition)) return false
  return true
}

/** 필터 칸에 보이는 요약: "혈변 · 통증 4 이상 · 5·6형" */
export function filterSummary(value: BowelFilterValue): string {
  return [
    value.bleeding && '혈변',
    value.pain !== null && (value.pain === 5 ? '통증 5' : `통증 ${value.pain} 이상`),
    value.types.length > 0 && `${value.types.map((type) => type.slice(5)).sort().join('·')}형`,
  ]
    .filter(Boolean)
    .join(' · ')
}
