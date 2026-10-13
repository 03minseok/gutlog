import { useState } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { toDateParam } from '../../lib/date'
import { TodaySummaryCard } from './TodaySummaryCard'

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

function greeting(hour: number) {
  if (hour < 11) return '좋은 아침이에요'
  if (hour < 17) return '오늘 하루 어떠세요'
  return '오늘도 수고하셨어요'
}

/** 홈. 오늘 날짜, 인사말, 오늘의 기록 카드. */
export function HomePage() {
  const { user } = useAuth()
  // 화면에 들어온 시각 기준으로 고정한다. (렌더마다 바뀌지 않게)
  const [now] = useState(() => new Date())
  const dateLabel = `${now.getMonth() + 1}월 ${now.getDate()}일 ${WEEKDAYS[now.getDay()]}요일`

  return (
    <div className="flex flex-col gap-5 px-4 pt-5 pb-6">
      <div className="flex flex-col gap-1">
        <p className="text-label font-semibold text-sky-700">{dateLabel}</p>
        <h1 className="text-title font-bold tracking-[-0.015em]">
          {greeting(now.getHours())}
          {user && (
            <>
              <br />
              {user.nickname}님
            </>
          )}
        </h1>
      </div>
      <TodaySummaryCard today={toDateParam(now)} />
    </div>
  )
}
