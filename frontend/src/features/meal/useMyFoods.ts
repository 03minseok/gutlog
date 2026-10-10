import { useEffect, useRef, useState } from 'react'
import { mealApi } from '../../api/meal'

/**
 * 내 음식 이름 목록(많이 먹은 순). 화면에 들어올 때 한 번만 받는다.
 * - 저장 후에는 목록 화면으로 나갔다가 다시 들어오므로 그때 새로 받는다.
 * - 요청이 실패하면 빈 목록 → 자동완성만 안 뜨고 직접 입력은 그대로 된다.
 * - 개발 모드(StrictMode)에서 effect가 두 번 실행돼도 요청은 한 번만 보내도록 ref로 공유한다.
 */
export function useMyFoods(): string[] {
  const [names, setNames] = useState<string[]>([])
  const request = useRef<Promise<string[]> | null>(null)

  useEffect(() => {
    let active = true
    request.current ??= mealApi
      .listFoods()
      .then((foods) => foods.map((food) => food.name))
      .catch(() => [])
    request.current.then((result) => {
      if (active) setNames(result)
    })
    return () => {
      active = false
    }
  }, [])

  return names
}
