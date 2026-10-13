import { useEffect, useId, useRef } from 'react'
import { Button } from './Button'
import { Icon } from './Icon'

interface ConfirmDialogProps {
  open: boolean
  title: string
  description?: string
  confirmLabel: string
  cancelLabel?: string
  /** 확인 버튼 앞 아이콘 (기본: 삭제) */
  confirmIcon?: string
  /** 확인 요청 중이면 버튼을 막는다. */
  pending?: boolean
  onConfirm: () => void
  onCancel: () => void
}

/**
 * 되돌릴 수 없는 동작 전에 띄우는 확인창. 화면 가운데에 뜬다. (흰 면, rounded-sheet, 좌우 여백 24)
 * 빨강은 혈변 전용이라, 위험한 동작은 아이콘 + 문구 + 확인 단계로 구분한다.
 */
export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  cancelLabel = '취소',
  confirmIcon = 'delete',
  pending = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const titleId = useId()
  const descId = useId()
  const cancelRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (!open) return
    // 실수로 확인되지 않도록 기본 포커스는 취소에 둔다.
    cancelRef.current?.focus()
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !pending) onCancel()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, pending, onCancel])

  if (!open) return null

  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-ink/40 px-6" onClick={() => !pending && onCancel()}>
      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={description ? descId : undefined}
        onClick={(e) => e.stopPropagation()}
        className="flex w-full max-w-[340px] flex-col gap-5 rounded-sheet bg-white px-5 pt-6 pb-5 shadow-toast"
      >
        <div className="flex flex-col gap-1.5">
          <h2 id={titleId} className="text-heading font-bold">
            {title}
          </h2>
          {description && (
            <p id={descId} className="text-body text-gray-700">
              {description}
            </p>
          )}
        </div>
        <div className="grid grid-cols-2 gap-2">
          <Button ref={cancelRef} variant="secondary" onClick={onCancel} disabled={pending}>
            {cancelLabel}
          </Button>
          <Button variant="destructive" onClick={onConfirm} disabled={pending}>
            <Icon name={confirmIcon} size={20} />
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  )
}
