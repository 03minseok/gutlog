import { Icon } from '../../components/Icon'

/**
 * 혈변 배지 — 채운 물방울 아이콘 + 문구 + blood 색 (색만으로 전달하지 않는다).
 * 목록 헤더·홈 요약은 blood-bg 면, 이미 blood-bg인 혈변 카드 안에서는 흰 면(onTint)으로 써서 묻히지 않게 한다.
 * 불안을 키우지 않도록 응급 기호나 진한 빨강 면은 쓰지 않는다.
 */
export function BleedingBadge({ label, onTint = false }: { label: string; onTint?: boolean }) {
  return (
    <span
      className={
        'inline-flex items-center gap-1 rounded-full px-2 pt-[3px] pb-px text-label font-semibold text-blood ' +
        (onTint ? 'border border-blood-border bg-white' : 'bg-blood-bg')
      }
    >
      <Icon name="water_drop" size={14} filled />
      {label}
    </span>
  )
}
