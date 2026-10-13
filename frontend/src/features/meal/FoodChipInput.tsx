import { useId, useRef, useState, type KeyboardEvent } from 'react'
import { FieldHint } from '../../components/FieldHint'
import { Icon } from '../../components/Icon'

interface FoodChipInputProps {
  label: string
  foods: string[]
  /** 아직 칩이 되지 않은 입력 중인 글자 */
  draft: string
  error: string | null
  /** 자동완성 후보 (부모가 filterFoodSuggestions로 거른 결과) */
  suggestions: string[]
  onDraftChange: (draft: string) => void
  /** 콤마·엔터·후보 선택으로 칩을 만들 때. 콤마 뒤에 남은 글자는 remainder로 넘긴다. */
  onCommit: (text: string, remainder?: string) => void
  onRemove: (index: number) => void
}

/**
 * 음식 칩 입력 + 자동완성 (ARIA combobox).
 * - 콤마나 엔터로 입력한 글자 그대로 칩을 만든다.
 * - 후보를 누르거나 ↑↓로 고르고 엔터를 치면 그 후보를 칩으로 만든다.
 * - 한글 조합 중에는 엔터·방향키를 무시한다. (조합 중 엔터를 처리하면 글자가 반만 들어가거나 두 번 추가된다)
 * - 입력칸이 비어 있고 목록이 닫혀 있으면 "자주 먹은 음식"을 버튼으로 보여준다. (누르면 바로 칩으로 추가)
 */
export function FoodChipInput({
  label,
  foods,
  draft,
  error,
  suggestions,
  onDraftChange,
  onCommit,
  onRemove,
}: FoodChipInputProps) {
  const inputId = useId()
  const hintId = useId()
  const listId = useId()
  const inputRef = useRef<HTMLInputElement>(null)

  const [focused, setFocused] = useState(false)
  // Esc로 닫으면 다시 입력하거나 포커스할 때까지 닫아둔다.
  const [dismissed, setDismissed] = useState(false)
  const [activeIndex, setActiveIndex] = useState(-1)

  const open = focused && !dismissed && suggestions.length > 0
  const active = open && activeIndex < suggestions.length ? activeIndex : -1
  const query = draft.trim()
  const optionId = (index: number) => `${listId}-option-${index}`

  function handleChange(value: string) {
    setDismissed(false)
    setActiveIndex(-1)
    // 콤마가 들어오면(타이핑·붙여넣기 모두) 마지막 콤마 앞까지 칩으로 만들고 뒤는 계속 입력 중으로 둔다.
    const lastComma = value.lastIndexOf(',')
    if (lastComma === -1) {
      onDraftChange(value)
      return
    }
    onCommit(value.slice(0, lastComma), value.slice(lastComma + 1))
  }

  function select(name: string) {
    onCommit(name)
    setActiveIndex(-1)
  }

  function handleKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.nativeEvent.isComposing || e.keyCode === 229) return

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        if (!open) {
          setDismissed(false)
          return
        }
        setActiveIndex(active < suggestions.length - 1 ? active + 1 : 0)
        return
      case 'ArrowUp':
        if (!open) return
        e.preventDefault()
        setActiveIndex(active > 0 ? active - 1 : suggestions.length - 1)
        return
      case 'Enter':
        e.preventDefault() // 폼 제출 방지
        if (active >= 0) select(suggestions[active])
        else if (query !== '') onCommit(draft)
        return
      case 'Escape':
        if (!open) return
        e.preventDefault()
        setDismissed(true)
        setActiveIndex(-1)
        return
      case 'Backspace':
        if (draft === '' && foods.length > 0) onRemove(foods.length - 1)
        return
    }
  }

  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={inputId} className="text-body font-bold">
        {label}
      </label>
      <div
        onClick={() => inputRef.current?.focus()}
        className={
          'flex min-h-12 cursor-text flex-wrap items-center gap-1.5 rounded-input border-[1.5px] bg-white px-2 py-1 ' +
          'transition-[border-color,box-shadow] focus-within:ring-[3px] ' +
          (error
            ? 'border-error focus-within:ring-error-bg'
            : 'border-gray-400 focus-within:border-sky-700 focus-within:ring-sky-100')
        }
      >
        <ul className="contents" aria-label="입력한 음식">
          {foods.map((food, index) => (
            <li
              key={food}
              className="flex h-9 max-w-full items-center rounded-full bg-meal-bg pl-3 text-body font-semibold text-meal"
            >
              <span className="truncate">{food}</span>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation()
                  onRemove(index)
                }}
                aria-label={`${food} 삭제`}
                // 칩(36px)보다 크게 44×44 누르는 영역을 둔다.
                className="-my-1 flex h-11 w-11 flex-none items-center justify-center rounded-full hover:bg-white/60"
              >
                <Icon name="close" size={16} />
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
          onFocus={() => {
            setFocused(true)
            setDismissed(false)
          }}
          onBlur={() => {
            setFocused(false)
            setActiveIndex(-1)
          }}
          role="combobox"
          aria-expanded={open}
          aria-controls={listId}
          aria-autocomplete="list"
          aria-activedescendant={active >= 0 ? optionId(active) : undefined}
          enterKeyHint="enter"
          autoComplete="off"
          placeholder={foods.length === 0 ? '예: 떡볶이, 우유' : '음식 추가'}
          aria-invalid={error ? true : undefined}
          aria-describedby={hintId}
          className="h-9 min-w-[120px] flex-1 bg-transparent px-1.5 text-body text-ink outline-none"
        />
      </div>

      {/* 후보 목록: 아래 내용을 밀어내며 펼친다. (스크롤 영역에 잘리거나 키보드에 가리지 않게) */}
      <div hidden={!open} className="overflow-hidden rounded-input border border-gray-200 bg-white py-1">
        {query === '' && <p className="px-3.5 pt-2 pb-1 text-caption font-semibold text-gray-500">자주 먹은 음식</p>}
        <ul id={listId} role="listbox" aria-label="음식 추천">
          {open &&
            suggestions.map((name, index) => (
              <li
                key={name}
                id={optionId(index)}
                role="option"
                aria-selected={index === active}
                // 누르는 순간 입력칸 포커스가 빠지면 목록이 닫혀 클릭이 사라진다 → 기본 동작을 막아 포커스를 유지한다.
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => select(name)}
                onMouseEnter={() => setActiveIndex(index)}
                className={
                  'flex min-h-11 cursor-pointer items-center gap-2.5 px-3.5 text-body ' +
                  (index === active ? 'bg-sky-100 text-ink' : 'text-ink')
                }
              >
                <Icon name={query === '' ? 'history' : 'search'} size={18} className="flex-none text-gray-500" />
                <span className="min-w-0 truncate">
                  <Highlight text={name} query={query} />
                </span>
              </li>
            ))}
        </ul>
      </div>

      <div id={hintId}>
        {error ? (
          <FieldHint tone="error">{error}</FieldHint>
        ) : (
          <FieldHint>콤마(,)나 엔터로 여러 개 입력할 수 있어요</FieldHint>
        )}
      </div>

      {/* 자주 먹은 음식: 입력칸을 누르기 전에도 보여서 한 번에 추가할 수 있다. 입력칸이 열려 있으면 위 목록이 대신한다. */}
      {!open && query === '' && suggestions.length > 0 && (
        <div className="mt-2 flex flex-col gap-2">
          <p className="text-caption font-semibold text-gray-500">자주 먹은 음식</p>
          <ul className="flex flex-wrap gap-2" aria-label="자주 먹은 음식">
            {suggestions.map((name) => (
              <li key={name} className="max-w-full">
                <button
                  type="button"
                  onClick={() => select(name)}
                  aria-label={`${name} 추가`}
                  className="flex min-h-11 max-w-full items-center gap-1 rounded-full border border-gray-300 bg-white pr-3.5 pl-2.5 text-body text-ink hover:border-gray-400"
                >
                  <Icon name="add" size={16} className="flex-none text-gray-500" />
                  <span className="truncate">{name}</span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

/** 입력한 글자와 일치하는 부분을 굵게 */
function Highlight({ text, query }: { text: string; query: string }) {
  const start = query ? text.toLowerCase().indexOf(query.toLowerCase()) : -1
  if (start === -1) return <>{text}</>
  const end = start + query.length
  return (
    <>
      {text.slice(0, start)}
      <strong className="font-bold">{text.slice(start, end)}</strong>
      {text.slice(end)}
    </>
  )
}
