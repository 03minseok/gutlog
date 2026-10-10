import { createContext, useContext } from 'react'

export interface ToastContextValue {
  /** 하단 탭 위에 1.8초 동안 문구를 띄운다. 화면을 이동해도 유지된다. */
  showToast: (message: string) => void
}

export const ToastContext = createContext<ToastContextValue | null>(null)

export function useToast(): ToastContextValue {
  const value = useContext(ToastContext)
  if (!value) throw new Error('useToast는 ToastProvider 안에서만 사용할 수 있어요.')
  return value
}
