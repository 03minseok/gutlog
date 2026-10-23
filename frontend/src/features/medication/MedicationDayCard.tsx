import { useState } from 'react'
import { medicationApi, type DailyMedicationItem } from '../../api/medication'
import { messageOf } from '../../api/types'
import { FieldHint } from '../../components/FieldHint'
import { Icon } from '../../components/Icon'
import { daysBetween, formatShortDate } from '../../lib/date'
import { formatDose, medicineTitle, scheduleSummary } from './constants'
import { canToggleDose, visibleDoses } from './doses'

interface MedicationDayCardProps {
  item: DailyMedicationItem
  /** 보고 있는 날짜 'yyyy-MM-dd' */
  date: string
  /** 체크·취소가 끝나면 날짜별 현황을 다시 받는다. (n/m 갱신) */
  onChanged: () => void
  /** true면 카드 테두리·면·여백 없이 내용만 (다른 카드 안에 넣을 때) */
  plain?: boolean
}

/**
 * 그날 약 하나의 복용 체크 카드.
 * DAILY: 회차 버튼 (순서대로 체크, 역순으로 취소) / INTERVAL: 투여 체크 + 마지막·다음 예정일 / AS_NEEDED: 먹었어요 체크
 * 끊은 약은 기록만 보여주고 바꿀 수 없다.
 */
export function MedicationDayCard({ item, date, onChanged, plain = false }: MedicationDayCardProps) {
  // 서버 응답으로 바로 반영하고, onChanged로 받은 새 현황이 오면 그 값을 따른다.
  const [taken, setTaken] = useState<{ source: number[]; doses: Set<number> }>(() => ({
    source: item.takenDoses,
    doses: new Set(item.takenDoses),
  }))
  const takenDoses = taken.source === item.takenDoses ? taken.doses : new Set(item.takenDoses)
  const [pendingDose, setPendingDose] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  const { medicine } = item
  const dose = formatDose(item.doseAmount, medicine.doseUnit)
  // 성분명은 약 이름 옆에 작게, 두 번째 줄에는 복용 방식 · 1회 복용량 · 메모만
  const subtitle = [scheduleSummary(item.scheduleType, item.dailyDoseCount, item.intervalDays), dose, item.doseMemo]
    .filter(Boolean)
    .join(' · ')
  const maxDose = item.scheduleType === 'DAILY' ? (item.dailyDoseCount ?? 1) : 1

  async function toggle(doseNumber: number) {
    // 응답이 오기 전에는 이 약의 다른 버튼도 막는다. (순서 규칙이 서로 얽혀 있어서)
    if (pendingDose !== null || item.stopped) return
    setPendingDose(doseNumber)
    setError(null)
    try {
      const result = await medicationApi.toggle(item.userMedicineId, date, doseNumber)
      const next = new Set(takenDoses)
      if (result.taken) next.add(doseNumber)
      else next.delete(doseNumber)
      setTaken({ source: item.takenDoses, doses: next })
      onChanged()
    } catch (err) {
      setError(messageOf(err, '기록하지 못했어요. 다시 시도해 주세요.'))
      onChanged() // 다른 기기에서 바뀌었을 수 있으니 최신 상태로 맞춘다.
    } finally {
      setPendingDose(null)
    }
  }

  return (
    <article className={`flex flex-col gap-3 ${plain ? '' : 'rounded-card border border-gray-200 bg-white p-4'} ${item.stopped ? 'opacity-80' : ''}`}>
      <div className="flex items-start gap-3">
        <div className="flex min-w-0 flex-1 flex-col gap-0.5">
          <span className="flex min-w-0 items-baseline gap-1.5">
            <h3 className="min-w-0 truncate text-body font-bold">{medicineTitle(medicine)}</h3>
            <span className="min-w-0 truncate text-caption text-gray-500">{medicine.genericName}</span>
            {item.stopped && (
              <span className="flex-none self-center rounded-full bg-gray-100 px-2 py-0.5 text-caption font-semibold text-gray-500">복용 중단</span>
            )}
          </span>
          <p className="truncate text-label text-gray-500">{subtitle}</p>
        </div>
      </div>

      {item.scheduleType === 'DAILY' && (
        <div role="group" aria-label={`${medicine.brandName} 회차`} className="grid grid-cols-3 gap-1.5">
          {visibleDoses(maxDose, [...takenDoses]).map((n) => {
            const checked = takenDoses.has(n)
            const enabled = !item.stopped && pendingDose === null && canToggleDose(n, takenDoses, maxDose)
            return (
              <DoseButton
                key={n}
                label={`${n}회차`}
                checked={checked}
                disabled={!enabled}
                pending={pendingDose === n}
                outOfRange={n > maxDose}
                onClick={() => toggle(n)}
              />
            )
          })}
        </div>
      )}

      {item.scheduleType === 'INTERVAL' && (
        <div className="flex flex-col gap-2">
          <IntervalStatus item={item} date={date} takenToday={takenDoses.has(1)} />
          <DoseButton
            label={takenDoses.has(1) ? '이 날 투여했어요' : '투여했어요'}
            checked={takenDoses.has(1)}
            disabled={item.stopped || pendingDose !== null}
            pending={pendingDose === 1}
            onClick={() => toggle(1)}
          />
        </div>
      )}

      {item.scheduleType === 'AS_NEEDED' && (
        <DoseButton
          label={takenDoses.has(1) ? '이 날 먹었어요' : '먹었어요'}
          checked={takenDoses.has(1)}
          disabled={item.stopped || pendingDose !== null}
          pending={pendingDose === 1}
          onClick={() => toggle(1)}
        />
      )}

      {item.stopped && <FieldHint>복용을 중단한 약이라 기록을 바꿀 수 없어요</FieldHint>}
      {error && <FieldHint tone="error">{error}</FieldHint>}
    </article>
  )
}

interface DoseButtonProps {
  label: string
  checked: boolean
  disabled: boolean
  pending: boolean
  /** 하루 복용 횟수를 줄여서 범위 밖에 남은 회차 */
  outOfRange?: boolean
  onClick: () => void
}

/** 체크 토글 버튼: 체크 med 배경 + 흰 글자, 미체크 gray-100. 버튼 안 아이콘으로도 상태를 보여준다. */
function DoseButton({ label, checked, disabled, pending, outOfRange = false, onClick }: DoseButtonProps) {
  return (
    <button
      type="button"
      aria-pressed={checked}
      onClick={onClick}
      disabled={disabled}
      className={
        'flex h-11 items-center justify-center gap-1.5 rounded-input text-body font-semibold transition-colors ' +
        (checked ? 'bg-med text-white' : 'bg-gray-100 text-gray-700') +
        (outOfRange ? ' border-[1.5px] border-dashed border-med-mid' : '') +
        (disabled && !pending ? ' cursor-not-allowed opacity-50' : '')
      }
    >
      <Icon name={checked ? 'check_circle' : 'radio_button_unchecked'} size={20} filled={checked} />
      {pending ? '저장 중…' : label}
    </button>
  )
}

/** INTERVAL: '마지막 10월 1일 · 다음 10월 15일 (4일 남음)'. 기록이 없으면 첫 투여를 안내한다. */
function IntervalStatus({ item, date, takenToday }: { item: DailyMedicationItem; date: string; takenToday: boolean }) {
  if (!item.lastTakenDate || !item.nextDueDate) {
    return <p className="text-label text-gray-500">아직 투여 기록이 없어요</p>
  }
  const remaining = daysBetween(date, item.nextDueDate)
  const due = takenToday
    ? null
    : remaining > 0
      ? { text: `${remaining}일 남음`, color: 'text-gray-500' }
      : remaining === 0
        ? { text: '오늘 예정', color: 'text-med font-bold' }
        : { text: `${-remaining}일 지남`, color: 'text-error font-bold' }
  return (
    <p className="text-label text-gray-700">
      마지막 {formatShortDate(item.lastTakenDate)} · 다음 {formatShortDate(item.nextDueDate)}
      {due && <span className={`ml-1 ${due.color}`}>({due.text})</span>}
    </p>
  )
}
