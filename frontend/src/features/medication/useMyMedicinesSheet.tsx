import { useCallback } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router'
import { MyMedicinesSheet } from './MyMedicinesSheet'

/**
 * 내 약 관리 시트를 여는 곳(복약 상세·기록 탭·마이페이지)에서 함께 쓰는 훅.
 * 열린 상태는 주소의 ?sheet=medicines에 둔다. 그래서
 * - 시트에서 약 등록·수정 화면에 다녀오면(저장·닫기) 시트가 열린 채로 돌아오고,
 * - 휴대폰 뒤로 가기로도 닫힌다.
 * 열 때 기록을 하나 쌓고(state.sheet), 닫을 때 그 기록을 되돌린다. 주소로 바로 들어왔으면 주소에서 지우기만 한다.
 * 쓰는 법: const { openSheet, sheet } = useMyMedicinesSheet() → 버튼 onClick={openSheet}, 화면 어딘가에 {sheet}
 */
export function useMyMedicinesSheet() {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const location = useLocation()
  const isOpen = searchParams.get('sheet') === 'medicines'
  const openedInApp = (location.state as { sheet?: boolean } | null)?.sheet === true

  function openSheet() {
    const params = new URLSearchParams(searchParams)
    params.set('sheet', 'medicines')
    setSearchParams(params, { state: { sheet: true } })
  }

  const closeSheet = useCallback(() => {
    if (openedInApp) {
      navigate(-1)
      return
    }
    setSearchParams(
      (prev) => {
        const params = new URLSearchParams(prev)
        params.delete('sheet')
        return params
      },
      { replace: true },
    )
  }, [openedInApp, navigate, setSearchParams])

  return { openSheet, sheet: isOpen ? <MyMedicinesSheet onClose={closeSheet} /> : null }
}
