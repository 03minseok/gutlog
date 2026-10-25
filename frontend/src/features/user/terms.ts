/** 회원가입 약관 항목. 동의 내역 저장 API가 생기면 key를 서버와 맞춘다. */
export const TERMS = [
  { key: 'age', required: true, label: '만 14세 이상입니다' },
  { key: 'tos', required: true, label: '서비스 이용약관' },
  { key: 'privacy', required: true, label: '개인정보 수집·이용 동의' },
  { key: 'health', required: true, label: '민감정보(건강정보) 처리 동의' },
  { key: 'marketing', required: false, label: '알림·혜택 정보 수신' },
] as const

export type TermKey = (typeof TERMS)[number]['key']
export type Agreements = Record<TermKey, boolean>

export const EMPTY_AGREEMENTS: Agreements = { age: false, tos: false, privacy: false, health: false, marketing: false }

export function hasRequiredAgreements(agreements: Agreements) {
  return TERMS.every((term) => !term.required || agreements[term.key])
}
