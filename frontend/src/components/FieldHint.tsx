import type { ReactNode } from 'react'
import { Icon } from './Icon'

type Tone = 'neutral' | 'ok' | 'error'

// 오류는 빨강(혈변 전용) 대신 황토색 + error 아이콘으로 표시한다.
const TONE: Record<Tone, { color: string; icon: string }> = {
  neutral: { color: 'text-gray-500', icon: 'info' },
  ok: { color: 'text-ok', icon: 'check_circle' },
  error: { color: 'text-error', icon: 'error' },
}

interface FieldHintProps {
  tone?: Tone
  id?: string
  children: ReactNode
}

/** 입력 아래 13/500 안내 문구. 색만으로 구분하지 않도록 아이콘을 함께 쓴다. */
export function FieldHint({ tone = 'neutral', id, children }: FieldHintProps) {
  const { color, icon } = TONE[tone]
  return (
    <span
      id={id}
      role={tone === 'error' ? 'alert' : undefined}
      className={`flex items-start gap-1 text-label font-medium ${color}`}
    >
      <Icon name={icon} size={16} filled={tone !== 'neutral'} className="mt-px flex-none" />
      {children}
    </span>
  )
}
