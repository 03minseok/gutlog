interface IconProps {
  name: string
  size?: number
  filled?: boolean
  className?: string
}

/** Google Material Symbols Rounded. 장식용이라 스크린리더에서는 숨긴다. */
export function Icon({ name, size = 24, filled = false, className = '' }: IconProps) {
  return (
    <span
      aria-hidden="true"
      className={`ms ${className}`}
      style={{ fontSize: size, fontVariationSettings: filled ? "'FILL' 1" : undefined }}
    >
      {name}
    </span>
  )
}
