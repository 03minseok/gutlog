const KEY = 'gutlog.accessToken'

/**
 * accessToken 저장소. 새로고침해도 로그인이 유지되도록 localStorage에 둔다.
 * 사생활 보호 모드 등에서 저장소 접근이 막혀도 앱이 멈추지 않게 예외를 삼킨다.
 */
export const tokenStorage = {
  get(): string | null {
    try {
      return localStorage.getItem(KEY)
    } catch {
      return null
    }
  },
  set(token: string) {
    try {
      localStorage.setItem(KEY, token)
    } catch {
      // 저장 실패 시 이번 탭에서만 로그인 상태가 유지된다.
    }
  },
  clear() {
    try {
      localStorage.removeItem(KEY)
    } catch {
      // 무시
    }
  },
}
