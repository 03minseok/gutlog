import { useId, useState } from 'react'
import type { Medicine } from '../../api/medication'
import { Icon } from '../../components/Icon'
import { CATEGORY_LABEL, medicineTitle } from './constants'

interface MedicinePickerProps {
  /** 활성 약 전체 (화면 진입 시 한 번 받는다) */
  medicines: Medicine[]
  /** 이미 복용 중인 약 id. 같은 약을 두 번 등록할 수 없어서 고를 수 없게 한다. */
  takenIds: ReadonlySet<number>
  value: Medicine | null
  onChange: (medicine: Medicine | null) => void
}

/** 약 고르기: 성분명·제품명 일부로 거르고(서버 요청 없이), 고르면 선택한 약 카드로 바뀐다. */
export function MedicinePicker({ medicines, takenIds, value, onChange }: MedicinePickerProps) {
  const inputId = useId()
  const [query, setQuery] = useState('')

  if (value) {
    return (
      <div className="flex flex-col gap-2">
        <span className="text-body font-bold">약</span>
        <div className="flex items-center gap-3 rounded-tile border-2 border-ink bg-sky-50 py-3 pr-2 pl-4">
          <span className="flex min-w-0 flex-1 flex-col gap-0.5">
            <span className="truncate text-body font-bold text-sky-700">{medicineTitle(value)}</span>
            <span className="truncate text-label text-gray-700">
              {value.genericName} · {CATEGORY_LABEL[value.category]}
            </span>
          </span>
          <button
            type="button"
            onClick={() => onChange(null)}
            className="h-11 flex-none rounded-input px-3 text-label font-semibold text-sky-700 hover:bg-white/60"
          >
            변경
          </button>
        </div>
      </div>
    )
  }

  const keyword = query.trim().toLowerCase()
  const filtered = keyword
    ? medicines.filter((m) => m.genericName.toLowerCase().includes(keyword) || m.brandName.toLowerCase().includes(keyword))
    : medicines

  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={inputId} className="text-body font-bold">
        어떤 약을 먹나요?
      </label>
      <div className="flex h-12 items-center gap-2 rounded-input border-[1.5px] border-gray-400 px-3.5 focus-within:border-sky-700 focus-within:ring-[3px] focus-within:ring-sky-100">
        <Icon name="search" size={20} className="text-gray-500" />
        <input
          id={inputId}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="성분명이나 제품명 (예: 메살라진, 펜타사)"
          autoComplete="off"
          className="min-w-0 flex-1 bg-transparent text-body text-ink outline-none"
        />
      </div>
      <ul role="listbox" aria-label="약 목록" className="flex max-h-[320px] flex-col overflow-y-auto rounded-input border border-gray-200">
        {filtered.length === 0 && <li className="px-4 py-6 text-center text-label text-gray-500">찾는 약이 없어요</li>}
        {filtered.map((medicine) => {
          const taken = takenIds.has(medicine.id)
          return (
            <li key={medicine.id} className="border-b border-gray-100 last:border-b-0">
              <button
                type="button"
                role="option"
                aria-selected={false}
                disabled={taken}
                onClick={() => onChange(medicine)}
                className="flex min-h-12 w-full items-center gap-3 px-4 py-2.5 text-left hover:bg-gray-50 disabled:cursor-not-allowed disabled:hover:bg-transparent"
              >
                <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                  <span className={`truncate text-body font-semibold ${taken ? 'text-gray-400' : 'text-ink'}`}>{medicineTitle(medicine)}</span>
                  <span className="truncate text-label text-gray-500">
                    {medicine.genericName} · {CATEGORY_LABEL[medicine.category]}
                  </span>
                </span>
                {taken && <span className="flex-none text-caption font-semibold text-gray-500">복용 중</span>}
              </button>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
