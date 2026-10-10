import { useId, useState, type InputHTMLAttributes, type ReactNode } from 'react'
import { Icon } from './Icon'

interface TextInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  label: string
  /** 입력칸 오른쪽에 붙는 요소 (예: 중복 확인 버튼) */
  trailing?: ReactNode
  /** 입력칸 아래 안내 문구 (FieldHint) */
  hint?: ReactNode
  invalid?: boolean
}

/**
 * h-14, 1.5px gray-200 테두리, rounded-input, 17px.
 * focus: blue-600 테두리 + 3px blue-100 링. type="password"면 오른쪽에 보기 토글이 생긴다.
 */
export function TextInput({ label, trailing, hint, invalid = false, type = 'text', className = '', ...rest }: TextInputProps) {
  const id = useId()
  const hintId = `${id}-hint`
  const isPassword = type === 'password'
  const [visible, setVisible] = useState(false)

  const border = invalid
    ? 'border-danger focus:border-danger focus:ring-danger-bg'
    : 'border-gray-200 focus:border-blue-600 focus:ring-blue-100'

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-[14px] font-semibold text-gray-700">
        {label}
      </label>
      <div className="flex gap-2">
        <div className="relative flex min-w-0 flex-1">
          <input
            id={id}
            type={isPassword && visible ? 'text' : type}
            aria-invalid={invalid || undefined}
            aria-describedby={hint ? hintId : undefined}
            className={
              `h-14 min-w-0 flex-1 rounded-input border-[1.5px] bg-white px-4 text-[17px] text-ink outline-none ` +
              `transition-[border-color,box-shadow] focus:ring-[3px] ${border} ${isPassword ? 'pr-14' : ''} ${className}`
            }
            {...rest}
          />
          {isPassword && (
            <button
              type="button"
              onClick={() => setVisible((v) => !v)}
              aria-label={visible ? '비밀번호 숨기기' : '비밀번호 보기'}
              aria-pressed={visible}
              className="absolute top-1 right-1 flex h-12 w-12 items-center justify-center rounded-[10px] text-gray-500"
            >
              <Icon name={visible ? 'visibility_off' : 'visibility'} />
            </button>
          )}
        </div>
        {trailing}
      </div>
      {hint && <div id={hintId}>{hint}</div>}
    </div>
  )
}
