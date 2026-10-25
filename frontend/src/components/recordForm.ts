import { useLocation, useNavigate, type Location } from 'react-router'

/**
 * 기록 입력(식사·배변) 화면으로 가는 링크와 닫기.
 * - 입력 화면으로 가는 Link는 state에 { from: 지금 위치 }를 담는다. (useRecordFormLinkState)
 * - 닫기(X)는 보던 화면으로 돌아간다. 주소로 바로 들어왔으면(from 없음) 기록 탭으로 간다.
 */

export interface RecordFormLinkState {
  from: Location
}

/** 입력 화면으로 가는 Link에 넘길 state */
export function useRecordFormLinkState(): RecordFormLinkState {
  const location = useLocation()
  return { from: location }
}

/**
 * 입력 화면 닫기.
 * - 앱 안에서 열었으면 뒤로 가서 보던 화면으로 돌아간다.
 * - 저장·삭제 뒤(date를 넘김) 보던 화면이 기록 탭이면, 그 날짜의 기록 탭을 보여준다.
 * - 주소로 바로 들어왔으면 기록 탭(date ?? fallbackDate)으로 간다.
 */
export function useCloseRecordForm(fallbackDate: string) {
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as Partial<RecordFormLinkState> | null)?.from

  return (date?: string) => {
    const showDateOnRecordTab = date !== undefined && from?.pathname === '/record'
    if (from && !showDateOnRecordTab) {
      navigate(-1)
      return
    }
    navigate(`/record?date=${date ?? fallbackDate}`, { replace: true })
  }
}
