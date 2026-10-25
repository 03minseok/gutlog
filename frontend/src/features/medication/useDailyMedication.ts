import { useEffect, useState } from 'react'
import { medicationApi, type DailyMedication } from '../../api/medication'
import { messageOf } from '../../api/types'

type Result = { status: 'ready'; data: DailyMedication } | { status: 'error'; message: string }
export type DailyMedicationState = { status: 'loading' } | Result

/**
 * 날짜별 복용 현황.
 * 날짜가 바뀌면 로딩부터 보여주고, refresh()는 지금 화면을 그대로 둔 채 다시 받아 바꾼다. (체크한 뒤 n/m 갱신용)
 */
export function useDailyMedication(date: string) {
  const [reloadKey, setReloadKey] = useState(0)
  // 어떤 날짜의 결과인지 함께 저장해서, 날짜가 바뀌면 이전 날짜 결과 대신 로딩을 보여준다.
  const [result, setResult] = useState<{ date: string; value: Result } | null>(null)
  const state: DailyMedicationState = result?.date === date ? result.value : { status: 'loading' }

  useEffect(() => {
    let cancelled = false
    medicationApi
      .daily(date)
      .then((data) => !cancelled && setResult({ date, value: { status: 'ready', data } }))
      .catch((err) => !cancelled && setResult({ date, value: { status: 'error', message: messageOf(err, '복약 기록을 불러오지 못했어요') } }))
    // 날짜를 빠르게 바꿀 때 이전 날짜 응답이 늦게 와서 덮어쓰지 않게 한다.
    return () => {
      cancelled = true
    }
  }, [date, reloadKey])

  return { state, refresh: () => setReloadKey((k) => k + 1) }
}
