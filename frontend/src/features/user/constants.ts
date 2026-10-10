import type { IbdType, Role } from '../../api/user'

/** 회원 유형 — 백엔드 Role enum 기준 */
export const ROLE_OPTIONS: readonly { value: Role; label: string }[] = [
  { value: 'PATIENT', label: '환자' },
  { value: 'GUARDIAN', label: '보호자' },
  { value: 'OTHER', label: '기타' },
]

/** 질환 종류 — 백엔드 IbdType enum 기준 (시안은 2개, 백엔드는 4개) */
export const IBD_TYPE_OPTIONS: readonly { value: IbdType; label: string; description: string }[] = [
  { value: 'ULCERATIVE_COLITIS', label: '궤양성대장염', description: 'Ulcerative colitis' },
  { value: 'CROHNS_DISEASE', label: '크론병', description: "Crohn's disease" },
  { value: 'INTESTINAL_BEHCETS', label: '장 베체트병', description: "Intestinal Behçet's" },
  { value: 'INTESTINAL_TUBERCULOSIS', label: '결핵성 장염', description: 'Intestinal tuberculosis' },
]

/** 입력 규칙 — 백엔드 SignupRequest 검증과 같다. */
export const RULES = {
  username: {
    pattern: /^[a-z0-9]{4,16}$/,
    message: '영문 소문자와 숫자로 4~16자 입력해 주세요',
  },
  password: {
    min: 8,
    max: 20,
    message: '8~20자, 영문과 숫자를 포함해 주세요',
  },
  nickname: {
    max: 8,
    message: '닉네임은 8자 이하로 입력해 주세요',
  },
}

export function isValidUsername(username: string) {
  return RULES.username.pattern.test(username)
}

export function isValidPassword(password: string) {
  const { min, max } = RULES.password
  return password.length >= min && password.length <= max && /[A-Za-z]/.test(password) && /\d/.test(password)
}

export function isValidNickname(nickname: string) {
  const trimmed = nickname.trim()
  return trimmed.length > 0 && trimmed.length <= RULES.nickname.max
}
