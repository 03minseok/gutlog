import { useEffect, useState } from 'react'
import { messageOf } from '../../api/types'

type Result<T> = { status: 'ready'; items: T[] } | { status: 'error'; message: string }
export type DayRecordsState<T> = { status: 'loading' } | Result<T>

/**
 * 하루치 기록 조회. 날짜가 바뀌면 다시 불러오고, reload()로 다시 시도한다.
 * fetchDay는 (from, to)를 받는 기간 조회 API다. 모듈 수준 함수(예: mealApi.listByPeriod)를 넘긴다.
 */
export function useDayRecords<T>(fetchDay: (from: string, to: string) => Promise<T[]>, date: string, errorMessage: string) {
  const [reloadKey, setReloadKey] = useState(0)
  const requestKey = `${date}#${reloadKey}`
  // 어떤 요청의 결과인지 함께 저장해서, 날짜가 바뀌면 이전 결과 대신 로딩을 보여준다.
  const [result, setResult] = useState<{ key: string; value: Result<T> } | null>(null)
  const state: DayRecordsState<T> = result?.key === requestKey ? result.value : { status: 'loading' }

  useEffect(() => {
    let cancelled = false
    fetchDay(date, date)
      .then((items) => !cancelled && setResult({ key: requestKey, value: { status: 'ready', items } }))
      .catch((err) => !cancelled && setResult({ key: requestKey, value: { status: 'error', message: messageOf(err, errorMessage) } }))
    // 날짜를 빠르게 바꿀 때 이전 날짜 응답이 늦게 와서 덮어쓰지 않게 한다.
    return () => {
      cancelled = true
    }
  }, [fetchDay, date, requestKey, errorMessage])

  return { state, reload: () => setReloadKey((k) => k + 1) }
}
