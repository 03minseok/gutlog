/**
 * 회차 체크 규칙 (백엔드 토글과 같다)
 * - 체크: n회차는 n-1회차가 체크돼 있어야 하고, 1 ~ 하루 복용 횟수까지만.
 * - 취소: n+1회차가 체크돼 있으면 취소할 수 없다.
 * 회차 상한은 그 날짜에 적용된 일정의 하루 복용 횟수다. (응답의 dailyDoseCount가 이미 그날 일정 값)
 * 같은 날 하루 복용 횟수를 줄여서 범위 밖에 남은 회차는 체크는 못 하고 취소만 할 수 있다.
 */
export function canToggleDose(dose: number, taken: ReadonlySet<number>, maxDose: number): boolean {
  if (taken.has(dose)) return !taken.has(dose + 1)
  return dose <= maxDose && (dose === 1 || taken.has(dose - 1))
}

/** 화면에 보여줄 회차: 1 ~ 하루 복용 횟수, 범위 밖에 남은 기록이 있으면 거기까지 */
export function visibleDoses(maxDose: number, takenDoses: readonly number[]): number[] {
  const last = Math.max(maxDose, ...takenDoses)
  return Array.from({ length: last }, (_, i) => i + 1)
}
