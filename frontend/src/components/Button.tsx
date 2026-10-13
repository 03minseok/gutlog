import type { ComponentProps } from 'react'

type Variant = 'primary' | 'outline' | 'secondary' | 'destructive'

// React 19: ref도 일반 prop으로 전달된다.
interface ButtonProps extends ComponentProps<'button'> {
  variant?: Variant
}

const DISABLED = 'disabled:bg-gray-150 disabled:text-gray-500'

const VARIANT: Record<Variant, string> = {
  // h-12 rounded-btn 16/600, 먹색. hover ink-hover, pressed ink-pressed. 비활성: gray-150 / gray-500
  primary: `h-12 w-full rounded-btn bg-ink text-section font-semibold text-white hover:bg-ink-hover active:bg-ink-pressed ${DISABLED}`,
  // 1.5px gray-400 테두리, hover 시 ink
  outline:
    'h-12 flex-none rounded-input border-[1.5px] border-gray-400 bg-white px-4 text-body font-semibold text-ink ' +
    'hover:border-ink disabled:text-gray-500 disabled:hover:border-gray-400',
  // 확인창의 취소 등 보조 동작
  secondary: `h-12 w-full rounded-btn bg-gray-100 text-section font-semibold text-gray-700 hover:bg-gray-150 ${DISABLED}`,
  // 삭제처럼 되돌릴 수 없는 동작. 빨강은 혈변 전용이라 쓰지 않고, 아이콘·문구·확인창으로 구분한다.
  destructive: `flex h-12 w-full items-center justify-center gap-1.5 rounded-btn bg-ink text-section font-semibold text-white hover:bg-ink-hover active:bg-ink-pressed ${DISABLED}`,
}

export function Button({ variant = 'primary', type = 'button', className = '', ...rest }: ButtonProps) {
  return (
    <button
      type={type}
      className={`transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-700 focus-visible:ring-offset-2 ${VARIANT[variant]} ${className}`}
      {...rest}
    />
  )
}
