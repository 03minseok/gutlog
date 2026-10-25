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

/** 통증은 0~5로 기록한다. (백엔드 검증은 0~10이라 그 안의 값만 보낸다) */
export const PAIN_MIN = 0
export const PAIN_MAX = 5
/** 새 기록의 통증 기본값 */
export const PAIN_DEFAULT = 0

/** 백엔드 검증 @Size(max = 500)과 같다. */
export const MEMO_MAX_LENGTH = 500

/** 통증 0~5 문구 */
const PAIN_LABELS = ['없음', '경미', '약함', '보통', '심함', '매우 심함'] as const

/**
 * 통증 정도 문구. 빨강은 혈변 전용이라 통증은 색으로 구분하지 않고 문구로만 전달한다.
 * 0 없음, 1 경미, 2 약함, 3 보통, 4 심함, 5 매우 심함. (0~5 척도 이전에 6 이상으로 저장된 기록은 매우 심함)
 */
export function painLevelInfo(level: number): { label: string; color: string } {
  const color = 'text-gray-700'
  return { label: PAIN_LABELS[Math.min(Math.max(level, 0), PAIN_LABELS.length - 1)], color }
}
