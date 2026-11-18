/** 체중 입력 규칙. 백엔드 WeightRequest 검증(1.00 ~ 300.00, 소수점 둘째 자리까지)과 같다. */
export const WEIGHT_MIN = 1
export const WEIGHT_MAX = 300

const WEIGHT_PATTERN = /^\d{1,3}(\.\d{1,2})?$/

export type WeightCheck = { ok: true; value: number } | { ok: false; empty: boolean; message: string }

export function checkWeight(text: string): WeightCheck {
  const trimmed = text.trim()
  if (trimmed === '') return { ok: false, empty: true, message: '체중을 입력하세요' }
  if (!/^\d+(\.\d+)?$/.test(trimmed)) return { ok: false, empty: false, message: '숫자로 입력해 주세요 (예: 62.35)' }
  if (!WEIGHT_PATTERN.test(trimmed) && /\.\d{3,}$/.test(trimmed)) {
    return { ok: false, empty: false, message: '소수점 둘째 자리까지 입력할 수 있어요' }
  }
  const value = Number(trimmed)
  if (!WEIGHT_PATTERN.test(trimmed) || value < WEIGHT_MIN || value > WEIGHT_MAX) {
    return { ok: false, empty: false, message: `${WEIGHT_MIN}~${WEIGHT_MAX}kg 사이로 입력해 주세요` }
  }
  return { ok: true, value }
}

/** 입력 중에는 숫자와 점 하나만 남긴다. */
export function sanitizeWeightInput(text: string): string {
  const cleaned = text.replace(/[^\d.]/g, '')
  const dot = cleaned.indexOf('.')
  return dot === -1 ? cleaned : cleaned.slice(0, dot + 1) + cleaned.slice(dot + 1).replace(/\./g, '')
}

/** 62.3 → '62.3', 62.35 → '62.35', 62 → '62' */
export function formatWeight(weight: number): string {
  return weight.toLocaleString('ko-KR', { maximumFractionDigits: 2, useGrouping: false })
}
