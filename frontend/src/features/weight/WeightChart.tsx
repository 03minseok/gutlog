import type { WeightRecord } from '../../api/weight'
import { LineChart, type YDomain } from '../../components/LineChart'
import { formatWeight } from './weight'

interface WeightChartProps {
  /** 날짜순 기록 */
  records: WeightRecord[]
  /** 가로축 범위 (둘 다 포함) */
  from: string
  to: string
  height?: number
}

/** 날짜별 체중 선 그래프. 모양·동작은 공통 LineChart */
export function WeightChart({ records, from, to, height }: WeightChartProps) {
  return (
    <LineChart
      label="체중 추이"
      points={records.map((r) => ({ date: r.date, value: r.weight }))}
      from={from}
      to={to}
      height={height}
      formatValue={(v) => `${formatWeight(v)}kg`}
      formatTick={formatWeight}
      yDomain={weightDomain}
    />
  )
}

/** 세로축 범위: 최저~최고에 여백을 두고 0.5kg 단위로 맞춘 눈금 3개. 값이 하나뿐이면 ±1kg. */
function weightDomain(values: number[]): YDomain {
  if (values.length === 0) return { lo: 0, hi: 1, ticks: [] }
  const min = Math.min(...values)
  const max = Math.max(...values)
  const pad = Math.max(0.5, (max - min) * 0.15)
  let lo = Math.floor((min - pad) * 2) / 2
  let hi = Math.ceil((max + pad) * 2) / 2
  // 가운데 눈금도 0.5 단위가 되도록 범위를 1kg 단위로 맞춘다.
  if (((hi - lo) * 2) % 2 !== 0) hi += 0.5
  lo = Math.max(0, lo)
  return { lo, hi, ticks: [lo, (lo + hi) / 2, hi] }
}
