/** 음식 입력 규칙 — 백엔드 MealCreateRequest 검증과 같다. */
export const FOOD_RULES = {
  maxCount: 20,
  maxLength: 50,
}

/** 자동완성 후보 개수: 입력 중이면 8개, 입력칸이 비었으면 자주 먹은 음식 5개 */
export const SUGGESTION_LIMIT = {
  typing: 8,
  empty: 5,
}

/**
 * 받아둔 내 음식 목록에서 자동완성 후보를 고른다.
 * - 입력값은 앞뒤 공백을 무시하고, 그 글자가 포함된 음식을 찾는다. (영문 대소문자 무시)
 * - 이미 칩으로 추가된 음식은 뺀다.
 * - 서버가 준 순서(많이 먹은 순)를 그대로 유지한다.
 */
export function filterFoodSuggestions(allFoods: string[], query: string, exclude: string[]): string[] {
  const q = query.trim().toLowerCase()
  const limit = q ? SUGGESTION_LIMIT.typing : SUGGESTION_LIMIT.empty
  const excluded = new Set(exclude.map((name) => name.trim()))
  const result: string[] = []
  for (const name of allFoods) {
    if (excluded.has(name)) continue
    if (q && !name.toLowerCase().includes(q)) continue
    result.push(name)
    if (result.length === limit) break
  }
  return result
}

export interface AddFoodsResult {
  foods: string[]
  error: string | null
}

/**
 * 입력 문자열(콤마로 여러 개 가능)을 기존 음식 목록에 더한다.
 * 앞뒤 공백 제거, 빈 값·중복은 건너뛴다. 규칙을 넘으면 아무것도 더하지 않고 error를 돌려준다.
 */
export function addFoods(foods: string[], raw: string): AddFoodsResult {
  const names = raw
    .split(',')
    .map((name) => name.trim())
    .filter((name) => name !== '')

  if (names.some((name) => name.length > FOOD_RULES.maxLength)) {
    return { foods, error: `음식 이름은 ${FOOD_RULES.maxLength}자까지 입력할 수 있어요` }
  }

  const next = [...foods]
  for (const name of names) {
    if (!next.includes(name)) next.push(name)
  }

  if (next.length > FOOD_RULES.maxCount) {
    return { foods, error: `음식은 ${FOOD_RULES.maxCount}개까지 입력할 수 있어요` }
  }
  return { foods: next, error: null }
}
