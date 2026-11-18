import { useEffect, useRef, useState, type KeyboardEvent, type PointerEvent } from 'react'
import type { WeightRecord } from '../../api/weight'
import { daysBetween, formatDateWithDay, formatShortDate } from '../../lib/date'
import { formatWeight } from './weight'

interface WeightChartProps {
  /** 날짜순 기록 */
  records: WeightRecord[]
  /** 가로축 범위 (둘 다 포함) */
  from: string
  to: string
  height?: number
}

const PAD = { top: 12, right: 12, bottom: 24, left: 40 }
/** 점이 이보다 많으면 점은 생략하고 선만 그린다 (마우스를 올린 점은 그대로 보인다). */
const MAX_DOTS = 31

/**
 * 날짜별 체중 선 그래프. 한 계열이라 범례 없이 카드 제목이 이름을 대신한다.
 * - 가로축은 실제 날짜 간격: 기록이 없는 날은 점이 없고, 앞뒤 기록을 선으로 잇는다(값을 만들어 넣지 않는다).
 * - 선 2px ink, 점 8px + 흰 테두리. 격자는 gray-200 가로선 3개.
 * - 마우스·터치를 대면 가장 가까운 날짜의 점에 세로선과 값이 뜬다. 키보드는 ← → 로 옮긴다.
 * 같은 값은 아래 목록(표 보기)에서도 볼 수 있다.
 */
export function WeightChart({ records, from, to, height = 160 }: WeightChartProps) {
  const wrapRef = useRef<HTMLDivElement>(null)
  const [width, setWidth] = useState(0)
  // 고른 점은 날짜로 기억한다. 기간이 바뀌어 그 날짜가 없어지면 저절로 풀린다.
  const [activeDate, setActiveDate] = useState<string | null>(null)

  useEffect(() => {
    const el = wrapRef.current
    if (!el) return
    const observer = new ResizeObserver(([entry]) => setWidth(entry.contentRect.width))
    observer.observe(el)
    return () => observer.disconnect()
  }, [])

  // 기간을 바꾸는 중에는 이전 기간 기록이 잠깐 남아 있을 수 있어 범위 안만 그린다.
  const inRange = records.filter((r) => r.date >= from && r.date <= to)
  const found = activeDate === null ? -1 : inRange.findIndex((r) => r.date === activeDate)
  const active = found === -1 ? null : found
  const setActive = (update: number | null | ((prev: number | null) => number | null)) => {
    const next = typeof update === 'function' ? update(active) : update
    setActiveDate(next === null ? null : (inRange[next]?.date ?? null))
  }

  const span = Math.max(1, daysBetween(from, to))
  const plotW = Math.max(0, width - PAD.left - PAD.right)
  const plotH = height - PAD.top - PAD.bottom

  const values = inRange.map((r) => r.weight)
  const { lo, hi, ticks } = yDomain(values)
  const x = (date: string) => PAD.left + (daysBetween(from, date) / span) * plotW
  const y = (weight: number) => PAD.top + (1 - (weight - lo) / (hi - lo)) * plotH

  const points = inRange.map((r) => ({ ...r, cx: x(r.date), cy: y(r.weight) }))
  const path = points.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.cx.toFixed(1)},${p.cy.toFixed(1)}`).join(' ')
  const showDots = points.length <= MAX_DOTS

  const nearest = (clientX: number) => {
    const rect = wrapRef.current?.getBoundingClientRect()
    if (!rect || points.length === 0) return null
    const px = clientX - rect.left
    let best = 0
    for (let i = 1; i < points.length; i++) {
      if (Math.abs(points[i].cx - px) < Math.abs(points[best].cx - px)) best = i
    }
    return best
  }

  const onPointerMove = (e: PointerEvent<SVGSVGElement>) => setActive(nearest(e.clientX))
  const onKeyDown = (e: KeyboardEvent<SVGSVGElement>) => {
    if (points.length === 0) return
    if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') {
      e.preventDefault()
      const step = e.key === 'ArrowRight' ? 1 : -1
      setActive((prev) => Math.min(points.length - 1, Math.max(0, prev === null ? points.length - 1 : prev + step)))
    } else if (e.key === 'Escape') {
      setActive(null)
    }
  }

  const current = active !== null ? points[active] : null
  const summary =
    inRange.length === 0
      ? '기록 없음'
      : `${formatShortDate(from)}부터 ${formatShortDate(to)}까지 ${inRange.length}개 기록, ` +
        `최저 ${formatWeight(Math.min(...values))}kg, 최고 ${formatWeight(Math.max(...values))}kg`

  return (
    <div ref={wrapRef} className="relative w-full select-none" style={{ height }}>
      {width > 0 && (
        <svg
          width={width}
          height={height}
          role="group"
          aria-label={`체중 추이 그래프. ${summary}. 좌우 화살표로 날짜별 값을 들을 수 있어요.`}
          tabIndex={0}
          onPointerMove={onPointerMove}
          onPointerDown={onPointerMove}
          onPointerLeave={(e) => e.pointerType === 'mouse' && setActive(null)}
          onKeyDown={onKeyDown}
          onBlur={() => setActive(null)}
          className="block touch-pan-y rounded-input outline-none focus-visible:ring-2 focus-visible:ring-sky-700"
        >
          {/* 가로 격자 + 세로축 값 */}
          {ticks.map((t) => (
            <g key={t}>
              <line x1={PAD.left} x2={width - PAD.right} y1={y(t)} y2={y(t)} className="stroke-gray-200" strokeWidth={1} />
              <text x={PAD.left - 6} y={y(t)} textAnchor="end" dominantBaseline="middle" className="fill-gray-500 text-caption tabular-nums">
                {formatWeight(t)}
              </text>
            </g>
          ))}
          {/* 가로축: 처음·끝 날짜 */}
          <text x={PAD.left} y={height - 6} textAnchor="start" className="fill-gray-500 text-caption">
            {formatShortDate(from)}
          </text>
          <text x={width - PAD.right} y={height - 6} textAnchor="end" className="fill-gray-500 text-caption">
            {formatShortDate(to)}
          </text>

          {current && <line x1={current.cx} x2={current.cx} y1={PAD.top} y2={PAD.top + plotH} className="stroke-gray-400" strokeWidth={1} />}

          {points.length > 1 && <path d={path} fill="none" className="stroke-ink" strokeWidth={2} strokeLinejoin="round" strokeLinecap="round" />}
          {points.map((p, i) =>
            showDots || points.length === 1 || i === active ? (
              <circle
                key={p.date}
                cx={p.cx}
                cy={p.cy}
                r={i === active ? 5 : 4}
                className={i === active ? 'fill-sky-700 stroke-white' : 'fill-ink stroke-white'}
                strokeWidth={2}
              />
            ) : null,
          )}
        </svg>
      )}

      {current && (
        <div
          aria-live="polite"
          className="pointer-events-none absolute z-10 -translate-x-1/2 rounded-input bg-ink px-2.5 py-1.5 text-label whitespace-nowrap text-white shadow-toast"
          style={{
            left: Math.min(Math.max(current.cx, 64), width - 64),
            top: Math.max(0, current.cy - 52),
          }}
        >
          <span className="font-medium">{formatDateWithDay(current.date)}</span>
          <span className="ml-1.5 font-bold tabular-nums">{formatWeight(current.weight)}kg</span>
        </div>
      )}
    </div>
  )
}

/** 세로축 범위: 최저~최고에 여백을 두고 0.5kg 단위로 맞춘 눈금 3개. 값이 하나뿐이면 ±1kg. */
function yDomain(values: number[]): { lo: number; hi: number; ticks: number[] } {
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
