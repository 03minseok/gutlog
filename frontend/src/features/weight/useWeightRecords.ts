import { useCallback, useEffect, useState } from 'react'
import { messageOf } from '../../api/types'
import { weightApi, type WeightRecord } from '../../api/weight'

type Result = { key: string; records: WeightRecord[]; error: string | null }

/**
 * 기간(from·to 포함)의 체중 기록. 날짜순. reload로 다시 받는다.
 * 다시 받는 동안에는 이전 기록을 그대로 두어(status 'loading') 그래프가 깜빡이지 않게 한다.
 */
export function useWeightRecords(from: string, to: string) {
  const [version, setVersion] = useState(0)
  const [result, setResult] = useState<Result | null>(null)
  const key = `${from}|${to}|${version}`

  useEffect(() => {
    let cancelled = false
    weightApi
      .listByPeriod(from, to)
      .then((records) => !cancelled && setResult({ key, records, error: null }))
      .catch(
        (err: unknown) =>
          !cancelled && setResult({ key, records: [], error: messageOf(err, '체중 기록을 불러오지 못했어요') }),
      )
    return () => {
      cancelled = true
    }
  }, [from, to, key])

  const reload = useCallback(() => setVersion((v) => v + 1), [])
  const records = result && result.error === null ? result.records : []

  if (result?.key !== key) return { status: 'loading' as const, records, message: null, reload }
  if (result.error !== null) return { status: 'error' as const, records, message: result.error, reload }
  return { status: 'ready' as const, records, message: null, reload }
}
