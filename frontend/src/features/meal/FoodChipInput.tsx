import { useId, useRef, type KeyboardEvent } from 'react'
import { FieldHint } from '../../components/FieldHint'
import { Icon } from '../../components/Icon'

interface FoodChipInputProps {
  label: string
  foods: string[]
  /** 아직 칩이 되지 않은 입력 중인 글자 */
  draft: string
  error: string | null
  onDraftChange: (draft: string) => void
  /** 콤마·엔터로 draft를 칩으로 만들 때. 콤마 뒤에 남은 글자는 remainder로 넘긴다. */
  onCommit: (text: string, remainder?: string) => void
  onRemove: (index: number) => void
}

/**
 * 음식 칩 입력. 콤마나 엔터로 구분해 칩을 만든다.
 * 한글 조합 중 엔터는 무시한다. (안 그러면 마지막 글자가 칩으로 한 번 더 생긴다)
 */
export function FoodChipInput({ label, foods, draft, error, onDraftChange, onCommit, onRemove }: FoodChipInputProps) {
  const inputId = useId()
  const hintId = useId()
  const inputRef = useRef<HTMLInputElement>(null)

  function handleChange(value: string) {
    // 콤마가 들어오면(타이핑·붙여넣기 모두) 마지막 콤마 앞까지 칩으로 만들고 뒤는 계속 입력 중으로 둔다.
    const lastComma = value.lastIndexOf(',')
    if (lastComma === -1) {
      onDraftChange(value)
      return
    }
    onCommit(value.slice(0, lastComma), value.slice(lastComma + 1))
  }

  function handleKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.nativeEvent.isComposing || e.keyCode === 229) return
    if (e.key === 'Enter') {
      e.preventDefault() // 폼 제출 방지
      if (draft.trim() !== '') onCommit(draft)
    } else if (e.key === 'Backspace' && draft === '' && foods.length > 0) {
      onRemove(foods.length - 1)
    }
  }

  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={inputId} className="text-[15px] font-bold">
        {label}
      </label>
      <div
        onClick={() => inputRef.current?.focus()}
        className={
          'flex min-h-14 cursor-text flex-wrap items-center gap-2 rounded-input border-[1.5px] bg-white px-3 py-2.5 ' +
          'transition-[border-color,box-shadow] focus-within:ring-[3px] ' +
          (error
            ? 'border-danger focus-within:ring-danger-bg'
            : 'border-gray-200 focus-within:border-blue-600 focus-within:ring-blue-100')
        }
      >
        <ul className="contents" aria-label="입력한 음식">
          {foods.map((food, index) => (
            <li
              key={food}
              className="flex h-9 max-w-full items-center gap-0.5 rounded-full bg-meal-bg pr-1 pl-3 text-[15px] font-semibold text-meal"
            >
              <span className="truncate">{food}</span>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation()
                  onRemove(index)
                }}
                aria-label={`${food} 삭제`}
                className="flex h-8 w-8 flex-none items-center justify-center rounded-full hover:bg-white/60"
              >
                <Icon name="close" size={18} />
              </button>
            </li>
          ))}
        </ul>
        <input
          ref={inputRef}
          id={inputId}
          value={draft}
          onChange={(e) => handleChange(e.target.value)}
          onKeyDown={handleKeyDown}
          enterKeyHint="enter"
          autoComplete="off"
          placeholder={foods.length === 0 ? '예: 떡볶이, 우유' : '음식 추가'}
          aria-invalid={error ? true : undefined}
          aria-describedby={hintId}
          className="h-9 min-w-[120px] flex-1 bg-transparent text-[17px] text-ink outline-none"
        />
      </div>
      <div id={hintId}>
        {error ? (
          <FieldHint tone="error">{error}</FieldHint>
        ) : (
          <FieldHint>콤마(,)나 엔터로 여러 개 입력할 수 있어요</FieldHint>
        )}
      </div>
    </div>
  )
}
