import { Icon } from '../../components/Icon'

/**
 * 혈변 배지 (emergency 아이콘 + 문구). 목록 헤더·홈 요약은 연한 배지,
 * 이미 danger-bg인 혈변 카드 안에서는 solid로 써서 묻히지 않게 한다.
 */
export function BleedingBadge({ label, solid = false }: { label: string; solid?: boolean }) {
  return (
    <span
      className={
        'inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-[13px] font-bold ' +
        (solid ? 'bg-danger text-white' : 'bg-danger-bg text-danger')
      }
    >
      <Icon name="emergency" size={16} />
      {label}
    </span>
  )
}
