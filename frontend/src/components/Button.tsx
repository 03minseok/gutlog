import type { ButtonHTMLAttributes } from 'react'

type Variant = 'primary' | 'outline'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
}

const VARIANT: Record<Variant, string> = {
  // h-14 rounded-btn 17/600, hover blue-700, pressed blue-800. 비활성: gray-200 / gray-500
  primary:
    'h-14 w-full rounded-btn bg-blue-600 text-[17px] font-semibold text-white hover:bg-blue-700 active:bg-blue-800 ' +
    'disabled:bg-gray-200 disabled:text-gray-500',
  // 1.5px gray-300 테두리, hover 시 blue-600
  outline:
    'h-14 flex-none rounded-input border-[1.5px] border-gray-300 bg-white px-4 text-[15px] font-semibold text-ink ' +
    'hover:border-blue-600 disabled:text-gray-400 disabled:hover:border-gray-300',
}

export function Button({ variant = 'primary', type = 'button', className = '', ...rest }: ButtonProps) {
  return (
    <button
      type={type}
      className={`transition-colors focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-blue-100 ${VARIANT[variant]} ${className}`}
      {...rest}
    />
  )
}
