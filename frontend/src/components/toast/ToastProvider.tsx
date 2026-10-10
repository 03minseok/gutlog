import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { Icon } from '../Icon'
import { ToastContext } from './ToastContext'

const DURATION_MS = 1800

/**
 * 시안의 Toast: 하단 탭 위(bottom 108px), 좌우 20, min-h 52, rounded-btn, ink 배경,
 * 흰 글씨 15/500, check_circle 아이콘 #7FD6C9, 1.8초 후 사라짐.
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
      <div aria-live="polite" className="pointer-events-none fixed inset-x-0 bottom-[108px] z-30 mx-auto max-w-[430px] px-5">
        {message && (
          <div
            key={id}
            className="flex min-h-[52px] items-center gap-2.5 rounded-btn bg-ink px-4 text-[15px] font-medium text-white shadow-toast"
          >
            <Icon name="check_circle" size={20} className="text-[#7FD6C9]" />
            {message}
          </div>
        )}
      </div>
    </ToastContext.Provider>
  )
}
