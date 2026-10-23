import { useId, useState, type ReactNode } from 'react'
import { Icon } from '../../components/Icon'
import { ToolBarButton } from '../record/CategoryDetail'
import { BRISTOL_OPTIONS, painLevelInfo } from './constants'
import { filterSummary, isFilterActive, PAIN_FILTER_LEVELS, type BowelFilterValue } from './filter'

interface BowelFiltersProps {
  value: BowelFilterValue
  onChange: (next: BowelFilterValue) => void
}

/**
 * 배변 필터 — 달력 아래 도구 칸(식사 검색창·복약 내 약 관리와 같은 크기, h-12 꽉 찬 너비).
 * - 칸: tune 아이콘 + "필터: 혈변 · 통증 · 형태"(조건이 없을 때 gray-500) 또는 "혈변 · 통증 4 이상 · 5·6형" + 오른쪽 ⌄/⌃.
 *   조건이 있으면 오른쪽 끝에 X(필터 지우기, 44px).
 * - 칸을 누르면 아래 흰 칸이 열린다: 혈변(물방울 + "혈변 있는 기록만", 켜면 ink) · 통증 "1 이상 경미 … 5 매우 심함"(하나) · 형태 1형~7형(여러 개).
 *   조건은 모두 만족해야 한다(AND). 켜고 끌 때마다 바로 거른다.
 */
export function BowelFilters({ value, onChange }: BowelFiltersProps) {
  const [open, setOpen] = useState(false)
  const active = isFilterActive(value)

  return (
    <div className="flex flex-col gap-2">
      <ToolBarButton
        icon="tune"
        label={active ? filterSummary(value) : <span className="text-gray-500">필터: 혈변 · 통증 · 형태</span>}
        onClick={() => setOpen((v) => !v)}
        expanded={open}
        indicator={open ? 'expand_less' : 'expand_more'}
        trailing={
          active && (
            <button
              type="button"
              onClick={() => onChange({ bleeding: false, pain: null, types: [] })}
              aria-label="필터 지우기"
              className="flex h-11 w-11 flex-none items-center justify-center rounded-input text-gray-500"
            >
              <Icon name="close" size={18} />
            </button>
          )
        }
      />

      {open && (
        <div className="flex flex-col gap-3 rounded-card border border-gray-200 bg-white p-3">
          <FilterRow label="혈변">
            <button
              type="button"
              aria-pressed={value.bleeding}
              onClick={() => onChange({ ...value, bleeding: !value.bleeding })}
              className={`flex h-11 w-full items-center justify-center gap-1.5 rounded-input text-label font-semibold ${value.bleeding ? 'bg-ink text-white' : 'bg-gray-100 text-gray-700'}`}
            >
              <Icon name="water_drop" size={16} filled className={value.bleeding ? '' : 'text-blood'} />
              혈변 있는 기록만
            </button>
          </FilterRow>

          <FilterRow label="통증">
            <div className="grid grid-cols-5 gap-1.5">
              {PAIN_FILTER_LEVELS.map((level) => {
                const on = value.pain === level
                return (
                  <button
                    key={level}
                    type="button"
                    aria-pressed={on}
                    onClick={() => onChange({ ...value, pain: on ? null : level })}
                    className={`flex h-12 flex-col items-center justify-center rounded-input leading-tight ${on ? 'bg-ink text-white' : 'bg-gray-100 text-gray-700'}`}
                  >
                    <span className="text-label font-semibold tabular-nums">{level === 5 ? '5' : `${level} 이상`}</span>
                    <span className={`text-caption ${on ? 'text-white' : 'text-gray-500'}`}>{painLevelInfo(level).label}</span>
                  </button>
                )
              })}
            </div>
          </FilterRow>

          <FilterRow label="형태">
            <div className="grid grid-cols-7 gap-1">
              {BRISTOL_OPTIONS.map((option) => {
                const on = value.types.includes(option.value)
                return (
                  <button
                    key={option.value}
                    type="button"
                    aria-pressed={on}
                    aria-label={`${option.type} ${option.description}`}
                    onClick={() =>
                      onChange({ ...value, types: on ? value.types.filter((t) => t !== option.value) : [...value.types, option.value] })
                    }
                    className={`flex h-11 items-center justify-center rounded-input text-label font-semibold tabular-nums ${on ? 'bg-ink text-white' : 'bg-gray-100 text-gray-700'}`}
                  >
                    {option.type}
                  </button>
                )
              })}
            </div>
            <p className="px-0.5 pt-1 text-caption text-gray-500">
              {value.types.length === 0
                ? '여러 개를 고를 수 있어요'
                : BRISTOL_OPTIONS.filter((o) => value.types.includes(o.value))
                    .map((o) => `${o.type} ${o.description}`)
                    .join(' · ')}
            </p>
          </FilterRow>
        </div>
      )}
    </div>
  )
}

/** 필터 칸 안의 한 묶음: 이름 13/600 gray-700 + 고르는 버튼들 */
function FilterRow({ label, children }: { label: string; children: ReactNode }) {
  const id = useId()
  return (
    <div role="group" aria-labelledby={id} className="flex flex-col gap-1.5">
      <span id={id} className="px-0.5 text-label font-semibold text-gray-700">
        {label}
      </span>
      {children}
    </div>
  )
}
