import { useEffect, useId, useRef } from 'react'
import { Icon } from '../../components/Icon'
import { MyMedicinesList } from './MyMedicinesPage'

/**
 * 내 약 관리 시트 — 복약 상세에서 "약 관리"를 누르면 아래에서 올라온다. (새 화면으로 넘어가지 않음)
 * - 바탕: ink 40% 막(눌러도 닫힘). 시트: 앱 폭(최대 430), 위 모서리 rounded-sheet, gray-50 면, 높이는 내용만큼(최대 화면 88%, 넘치면 안에서 스크롤).
 * - 헤더: "내 약 관리" 16/700 + 오른쪽 X(44px). X·Esc·바탕을 누르면 그대로 닫힌다(닫힐 때 움직임 없음).
 * - 내용: 내 약 관리 화면과 같은 MyMedicinesList. 등록·수정 화면에 다녀오면 시트가 열린 채로 돌아온다.
 * - 올라오는 움직임은 "움직임 줄이기" 설정이면 끈다(motion-safe).
 */
export function MyMedicinesSheet({ onClose }: { onClose: () => void }) {
  const titleId = useId()
  const closeRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null
    closeRef.current?.focus()
    // 시트가 떠 있는 동안 뒤 화면이 스크롤되지 않게 한다. (html에 scrollbar-gutter가 있어 화면이 옆으로 밀리지 않음)
    const overflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => {
      window.removeEventListener('keydown', onKey)
      document.body.style.overflow = overflow
      opener?.focus()
    }
  }, [onClose])

  return (
    <div className="fixed inset-0 z-40 flex items-end justify-center bg-ink/40 motion-safe:animate-fade-in" onClick={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        onClick={(e) => e.stopPropagation()}
        className="flex max-h-[88dvh] w-full max-w-[430px] flex-col rounded-t-sheet bg-gray-50 shadow-toast motion-safe:animate-sheet-up"
      >
        <header className="flex h-14 flex-none items-center justify-between pr-2 pl-5">
          <h2 id={titleId} className="text-section font-bold">
            내 약 관리
          </h2>
          <button
            ref={closeRef}
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="flex h-11 w-11 items-center justify-center rounded-input text-gray-700 hover:bg-gray-100"
          >
            <Icon name="close" size={22} />
          </button>
        </header>
        <div className="overflow-y-auto overscroll-contain px-4 pb-[max(24px,env(safe-area-inset-bottom))]">
          <MyMedicinesList />
        </div>
      </div>
    </div>
  )
}
