import type { SleepRecord } from '../../api/sleep'
import { LineChart, type YDomain } from '../../components/LineChart'
import { SLEEP_MAX, formatSleep } from './sleep'

interface SleepChartProps {
  /** 날짜순 기록 */
  records: SleepRecord[]
  /** 가로축 범위 (둘 다 포함) */
  from: string
  to: string
  height?: number
}

/** 날짜(일어난 날)별 수면 시간 선 그래프. 모양·동작은 공통 LineChart */
export function SleepChart({ records, from, to, height }: SleepChartProps) {
  return (
    <LineChart
      label="수면 시간 추이"
      points={records.map((r) => ({ date: r.date, value: r.sleepMinutes }))}
      from={from}
      to={to}
      height={height}
      padLeft={48}
      formatValue={formatSleep}
      formatTick={(minutes) => `${minutes / 60}시간`}
      yDomain={sleepDomain}
    />
  )
}

const HOUR = 60

/** 세로축 범위: 최저~최고에 1시간 여백을 두고 1시간 단위로 맞춘 눈금 3개(가운데도 정시). 0 ~ 24시간 안. */
function sleepDomain(values: number[]): YDomain {
  if (values.length === 0) return { lo: 0, hi: 1, ticks: [] }
  let lo = Math.floor((Math.min(...values) - HOUR) / HOUR) * HOUR
  let hi = Math.ceil((Math.max(...values) + HOUR) / HOUR) * HOUR
  // 가운데 눈금도 정시가 되도록 범위를 2시간 단위로 맞춘다.
  if (((hi - lo) / HOUR) % 2 !== 0) hi += HOUR
  if (hi > SLEEP_MAX) {
    lo -= hi - SLEEP_MAX
    hi = SLEEP_MAX
  }
  lo = Math.max(0, lo)
  return { lo, hi, ticks: [lo, (lo + hi) / 2, hi] }
}
