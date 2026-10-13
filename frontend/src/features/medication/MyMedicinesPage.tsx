import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { medicationApi, type UserMedicine } from '../../api/medication'
import { messageOf } from '../../api/types'
import { FormLoadStatus, RecordFormScreen } from '../../components/RecordFormScreen'
import { Icon } from '../../components/Icon'
import { formatDose, medicineTitle, MY_MEDICINES_PATH, scheduleSummary } from './constants'

type State = { status: 'loading' } | { status: 'ready'; items: UserMedicine[] } | { status: 'error'; message: string }

/** 내 복용 약 목록 (끊지 않은 약). 누르면 수정, 아래 버튼으로 등록. 하단 탭 없음. */
export function MyMedicinesPage() {
  const navigate = useNavigate()
  const [state, setState] = useState<State>({ status: 'loading' })

  useEffect(() => {
    let cancelled = false
    medicationApi
      .listMyMedicines()
      .then((items) => !cancelled && setState({ status: 'ready', items }))
      .catch((err) => !cancelled && setState({ status: 'error', message: messageOf(err, '약 목록을 불러오지 못했어요') }))
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <RecordFormScreen category="medication" title="내 약 관리" onClose={() => navigate('/record')}>
      {state.status !== 'ready' ? (
        <FormLoadStatus error={state.status === 'error' ? state.message : null} />
      ) : (
        <>
          <div className="flex flex-1 flex-col gap-2 overflow-y-auto bg-gray-50 px-4 pt-5 pb-6">
            {state.items.length === 0 ? (
              <p className="rounded-card border border-gray-200 bg-white px-4 py-8 text-center text-body text-gray-500">
                등록한 약이 없어요. 먹고 있는 약을 등록하면 기록 탭에서 바로 체크할 수 있어요.
              </p>
            ) : (
              <ul className="flex flex-col gap-2">
                {state.items.map((item) => (
                  <li key={item.id}>
                    <MyMedicineCard item={item} />
                  </li>
                ))}
              </ul>
            )}
            <p className="px-1 pt-2 text-label text-gray-500">
              약을 끊어도 지금까지의 복용 기록은 남아요.
            </p>
          </div>
          <div className="flex flex-none border-t border-gray-100 px-4 pt-3 pb-8">
            <Link
              to={`${MY_MEDICINES_PATH}/new`}
              className="flex h-12 w-full items-center justify-center gap-1.5 rounded-btn bg-ink text-section font-semibold text-white hover:bg-ink-hover active:bg-ink-pressed"
            >
              <Icon name="add" size={22} />약 등록
            </Link>
          </div>
        </>
      )}
    </RecordFormScreen>
  )
}

function MyMedicineCard({ item }: { item: UserMedicine }) {
  const details = [
    scheduleSummary(item.scheduleType, item.dailyDoseCount, item.intervalDays),
    formatDose(item.doseAmount, item.medicine.doseUnit),
    item.doseMemo,
  ]
    .filter(Boolean)
    .join(' · ')
  return (
    <Link
      to={`${MY_MEDICINES_PATH}/${item.id}/edit`}
      aria-label={`${medicineTitle(item.medicine)}, ${details}. 수정하기`}
      className="flex items-center gap-3 rounded-card border border-gray-200 bg-white py-4 pr-2 pl-4 hover:border-gray-300"
    >
      <span className="flex h-11 w-10 flex-none items-center justify-center rounded-[10px] bg-med-bg text-med">
        <Icon name="medication" size={22} />
      </span>
      <span className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className="truncate text-body font-bold">{medicineTitle(item.medicine)}</span>
        <span className="truncate text-label text-gray-500">
          {item.medicine.genericName} · {details}
        </span>
      </span>
      <Icon name="chevron_right" className="flex-none text-gray-400" />
    </Link>
  )
}
