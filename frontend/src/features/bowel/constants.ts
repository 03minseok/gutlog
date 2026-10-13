import type { BristolType } from '../../api/bowel'

/** CLAUDE.md "화면 표시 이름"의 bowelCondition 표 */
export const BRISTOL_OPTIONS: readonly { value: BristolType; type: string; description: string }[] = [
  { value: 'TYPE_1', type: '1형', description: '딱딱한 덩어리' },
  { value: 'TYPE_2', type: '2형', description: '울퉁불퉁한 덩어리' },
  { value: 'TYPE_3', type: '3형', description: '금이 간 소시지 모양' },
  { value: 'TYPE_4', type: '4형', description: '매끈한 소시지 모양' },
  { value: 'TYPE_5', type: '5형', description: '부드러운 덩어리' },
  { value: 'TYPE_6', type: '6형', description: '무른 변' },
  { value: 'TYPE_7', type: '7형', description: '물 같은 변' },
]

/** 'TYPE_4' → '4형 · 매끈한 소시지 모양' */
export function bristolLabel(value: BristolType): string {
  const option = BRISTOL_OPTIONS.find((o) => o.value === value)
  return option ? `${option.type} · ${option.description}` : value
}

export const PAIN_MIN = 0
export const PAIN_MAX = 10

/** 백엔드 검증 @Size(max = 500)과 같다. */
export const MEMO_MAX_LENGTH = 500

/**
 * 통증 정도 문구. 빨강은 혈변 전용이라 통증은 색으로 구분하지 않고 문구로만 전달한다.
 * 0 없음, 1–3 약함, 4–6 중간, 7–10 심함
 */
export function painLevelInfo(level: number): { label: string; color: string } {
  const color = 'text-gray-700'
  if (level === 0) return { label: '없음', color }
  if (level <= 3) return { label: '약함', color }
  if (level <= 6) return { label: '중간', color }
  return { label: '심함', color }
}
