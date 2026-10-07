/** 음식 입력 규칙 — 백엔드 MealCreateRequest 검증과 같다. */
export const FOOD_RULES = {
  maxCount: 20,
  maxLength: 50,
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
