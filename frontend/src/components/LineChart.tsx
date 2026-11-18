import { useEffect, useRef, useState, type KeyboardEvent, type PointerEvent } from 'react'
import { daysBetween, formatDateWithDay, formatShortDate } from '../lib/date'

export interface LinePoint {
  /** 'yyyy-MM-dd' */
  date: string
  value: number
}

export interface YDomain {
  lo: number
  hi: number
  ticks: number[]
}

interface LineChartProps {
  /** 그래프 이름 (화면 낭독기용, 예: '체중 추이') */
  label: string
  /** 날짜순 점 */
  points: LinePoint[]
  /** 가로축 범위 (둘 다 포함) */
  from: string
  to: string
  /** 말풍선·요약에 쓰는 값 (예: '62.1kg', '7시간 30분') */
  formatValue: (value: number) => string
  /** 세로축 눈금 글자 (예: '62', '7시간') */
  formatTick: (value: number) => string
  /** 세로축 범위와 눈금 3개 */
  yDomain: (values: number[]) => YDomain
  height?: number
  /** 세로축 눈금 글자 자리 (기본 40px) */
  padLeft?: number
}

const PAD = { top: 12, right: 12, bottom: 24 }
/** 점이 이보다 많으면 점은 생략하고 선만 그린다 (마우스를 올린 점은 그대로 보인다). */
const MAX_DOTS = 31

/**
 * 날짜별 값 선 그래프 (체중·수면). 한 계열이라 범례 없이 카드 제목이 이름을 대신한다.
 * - 가로축은 실제 날짜 간격: 기록이 없는 날은 점이 없고, 앞뒤 기록을 선으로 잇는다(값을 만들어 넣지 않는다).
 * - 선 2px ink, 점 8px + 흰 테두리. 격자는 gray-200 가로선 3개.
 * - 마우스·터치를 대면 가장 가까운 날짜의 점에 세로선과 값이 뜬다. 키보드는 ← → 로 옮긴다.
 * 같은 값은 아래 목록(표 보기)에서도 볼 수 있다.
 */
export function LineChart({ label, points: data, from, to, formatValue, formatTick, yDomain, height = 160, padLeft = 40 }: LineChartProps) {
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
  const inRange = data.filter((p) => p.date >= from && p.date <= to)
  const found = activeDate === null ? -1 : inRange.findIndex((p) => p.date === activeDate)
  const active = found === -1 ? null : found
  const setActive = (update: number | null | ((prev: number | null) => number | null)) => {
    const next = typeof update === 'function' ? update(active) : update
    setActiveDate(next === null ? null : (inRange[next]?.date ?? null))
  }

  const span = Math.max(1, daysBetween(from, to))
  const plotW = Math.max(0, width - padLeft - PAD.right)
  const plotH = height - PAD.top - PAD.bottom

  const values = inRange.map((p) => p.value)
  const { lo, hi, ticks } = yDomain(values)
  const x = (date: string) => padLeft + (daysBetween(from, date) / span) * plotW
  const y = (value: number) => PAD.top + (1 - (value - lo) / (hi - lo)) * plotH

  const points = inRange.map((p) => ({ ...p, cx: x(p.date), cy: y(p.value) }))
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
        `최저 ${formatValue(Math.min(...values))}, 최고 ${formatValue(Math.max(...values))}`

  return (
    <div ref={wrapRef} className="relative w-full select-none" style={{ height }}>
      {width > 0 && (
        <svg
          width={width}
          height={height}
          role="group"
          aria-label={`${label} 그래프. ${summary}. 좌우 화살표로 날짜별 값을 들을 수 있어요.`}
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
              <line x1={padLeft} x2={width - PAD.right} y1={y(t)} y2={y(t)} className="stroke-gray-200" strokeWidth={1} />
              <text x={padLeft - 6} y={y(t)} textAnchor="end" dominantBaseline="middle" className="fill-gray-500 text-caption tabular-nums">
                {formatTick(t)}
              </text>
            </g>
          ))}
          {/* 가로축: 처음·끝 날짜 */}
          <text x={padLeft} y={height - 6} textAnchor="start" className="fill-gray-500 text-caption">
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
            left: Math.min(Math.max(current.cx, 72), width - 72),
            top: Math.max(0, current.cy - 52),
          }}
        >
          <span className="font-medium">{formatDateWithDay(current.date)}</span>
          <span className="ml-1.5 font-bold tabular-nums">{formatValue(current.value)}</span>
        </div>
      )}
    </div>
  )
}
