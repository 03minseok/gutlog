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
 * h-12, 1.5px gray-400 테두리(흰색 위 3.1:1), rounded-input, 15px.
 * focus: sky-700 테두리 + 3px sky-100 링. 오류: error 테두리.
 * type="password"면 오른쪽에 보기 토글이 생긴다.
 */
export function TextInput({ label, trailing, hint, invalid = false, type = 'text', className = '', ...rest }: TextInputProps) {
  const id = useId()
  const hintId = `${id}-hint`
  const isPassword = type === 'password'
  const [visible, setVisible] = useState(false)

  const border = invalid
    ? 'border-error focus:border-error focus:ring-error-bg'
    : 'border-gray-400 focus:border-sky-700 focus:ring-sky-100'

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-label font-semibold text-gray-700">
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
              `h-12 min-w-0 flex-1 rounded-input border-[1.5px] bg-white px-3.5 text-body text-ink outline-none ` +
              `transition-[border-color,box-shadow] focus:ring-[3px] ${border} ${isPassword ? 'pr-12' : ''} ${className}`
            }
            {...rest}
          />
          {isPassword && (
            <button
              type="button"
              onClick={() => setVisible((v) => !v)}
              aria-label={visible ? '비밀번호 숨기기' : '비밀번호 보기'}
              aria-pressed={visible}
              className="absolute top-0.5 right-0.5 flex h-11 w-11 items-center justify-center rounded-input text-gray-500"
            >
              <Icon name={visible ? 'visibility_off' : 'visibility'} size={20} />
            </button>
          )}
        </div>
        {trailing}
      </div>
      {hint && <div id={hintId}>{hint}</div>}
    </div>
  )
}
