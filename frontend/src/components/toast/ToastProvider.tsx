import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { Icon } from '../Icon'
import { ToastContext } from './ToastContext'

const DURATION_MS = 1800

/**
 * 토스트: 화면 위쪽(safe-area + 12px), 좌우 16, min-h 48, rounded-btn, ink 배경,
 * 흰 글씨 15/500, check_circle 아이콘 sky-300, 1.8초 후 사라짐.
 * 위쪽에 띄워 방금 저장한 목록 카드나 하단 버튼을 가리지 않는다.
 */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [message, setMessage] = useState<string | null>(null)
  // 같은 문구를 연달아 띄워도 타이머가 다시 시작되도록 id를 둔다.
  const [id, setId] = useState(0)
  const timer = useRef<number | undefined>(undefined)

  const showToast = useCallback((next: string) => {
    setMessage(next)
    setId((n) => n + 1)
  }, [])

  useEffect(() => {
    if (message === null) return
    window.clearTimeout(timer.current)
    timer.current = window.setTimeout(() => setMessage(null), DURATION_MS)
    return () => window.clearTimeout(timer.current)
  }, [message, id])

  const value = useMemo(() => ({ showToast }), [showToast])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div aria-live="polite" className="pointer-events-none fixed inset-x-0 top-[max(12px,env(safe-area-inset-top))] z-50 mx-auto max-w-[430px] px-4">
        {message && (
          <div
            key={id}
            className="flex min-h-12 items-center gap-2 rounded-btn bg-ink px-4 text-body font-medium text-white shadow-toast"
          >
            <Icon name="check_circle" size={20} filled className="flex-none text-sky-300" />
            {message}
          </div>
        )}
      </div>
    </ToastContext.Provider>
  )
}
