import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { medicationApi, type UserMedicine } from '../../api/medication'
import { messageOf } from '../../api/types'
import { FieldHint } from '../../components/FieldHint'
import { Icon } from '../../components/Icon'
import { useRecordFormLinkState } from '../../components/recordForm'
import { formatDose, medicineTitle, MY_MEDICINES_PATH, scheduleSummary } from './constants'

type State = { status: 'loading' } | { status: 'ready'; items: UserMedicine[] } | { status: 'error'; message: string }

/**
 * 내 약 관리 화면 — 마이페이지 등에서 여는 따로 된 화면. (하단 탭 있음, gray-50 바탕)
 * 헤더: 뒤로(기록 탭) + "내 약 관리" 16/700. 내용은 MyMedicinesList (복약 상세에서는 같은 내용을 시트로 연다: MyMedicinesSheet)
 */
export function MyMedicinesPage() {
  const navigate = useNavigate()
  return (
    <div className="flex flex-col">
      <header className="flex h-13 items-center gap-1 px-1.5">
        <button
          type="button"
          onClick={() => navigate('/record')}
          aria-label="기록으로 돌아가기"
          className="flex h-11 w-11 items-center justify-center rounded-input text-ink hover:bg-gray-100"
        >
          <Icon name="arrow_back_ios_new" size={20} />
        </button>
        <h1 className="text-section font-bold">내 약 관리</h1>
      </header>
      <div className="px-4 pt-1 pb-6">
        <MyMedicinesList />
      </div>
    </div>
  )
}

/**
 * 지금 먹고 있는 약(끊지 않은 약) 목록. 카드를 누르면 수정, 아래 버튼으로 등록.
 * 등록·수정 화면은 닫거나 저장하면 이 목록을 연 곳(화면 또는 시트)으로 돌아온다.
 */
export function MyMedicinesList() {
  const formLinkState = useRecordFormLinkState()
  const [state, setState] = useState<State>({ status: 'loading' })
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    medicationApi
      .listMyMedicines()
      .then((items) => !cancelled && setState({ status: 'ready', items }))
      .catch((err) => !cancelled && setState({ status: 'error', message: messageOf(err, '약 목록을 불러오지 못했어요') }))
    return () => {
      cancelled = true
    }
  }, [reloadKey])

  function retry() {
    setState({ status: 'loading' })
    setReloadKey((k) => k + 1)
  }

  return (
    <div className="flex flex-col gap-4">
      <p className="text-label text-gray-500">먹고 있는 약을 등록하면 기록 탭에서 바로 체크할 수 있어요.</p>

      {state.status === 'loading' && (
        <div role="status" aria-label="불러오는 중" className="flex flex-col gap-2">
          {[0, 1].map((i) => (
            <div key={i} className="flex h-16 items-center gap-3 rounded-card border border-gray-200 bg-white px-4">
              <span className="h-9 w-9 animate-pulse rounded-input bg-gray-100" />
              <span className="h-4 flex-1 animate-pulse rounded-input bg-gray-100" />
            </div>
          ))}
        </div>
      )}

      {state.status === 'error' && (
        <div className="flex min-h-14 items-center justify-between gap-2 rounded-card border border-error-border bg-error-bg py-1.5 pr-1.5 pl-4">
          <FieldHint tone="error">{state.message}</FieldHint>
          <button
            type="button"
            onClick={retry}
            className="flex h-11 flex-none items-center gap-1 rounded-input px-3 text-label font-semibold text-ink hover:bg-white/70"
          >
            <Icon name="refresh" size={18} />
            다시 시도
          </button>
        </div>
      )}

      {state.status === 'ready' && (
        <section className="flex flex-col gap-3" aria-labelledby="my-medicines-title">
          <div className="flex items-center gap-2 px-1">
            <span className="flex h-7 w-7 items-center justify-center rounded-[8px] bg-med-bg text-med">
              <Icon name="medication" size={16} />
            </span>
            <h2 id="my-medicines-title" className="text-section font-bold">
              먹고 있는 약
            </h2>
            <span className="text-label font-medium text-gray-500 tabular-nums">{state.items.length}개</span>
          </div>

          {state.items.length === 0 ? (
            <div className="rounded-card border border-dashed border-gray-300 px-4 py-6 text-center text-body text-gray-500">
              아직 등록한 약이 없어요
            </div>
          ) : (
            <ul className="flex flex-col gap-2">
              {state.items.map((item) => (
                <li key={item.id}>
                  <MyMedicineCard item={item} />
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {state.status === 'ready' && (
        <>
          <Link
            to={`${MY_MEDICINES_PATH}/new`}
            state={formLinkState}
            className="flex h-12 w-full items-center justify-center gap-1.5 rounded-btn bg-ink text-section font-semibold text-white hover:bg-ink-hover active:bg-ink-pressed"
          >
            <Icon name="add" size={20} />약 등록
          </Link>
          <FieldHint>약을 끊어도 지금까지의 복용 기록은 남아요</FieldHint>
        </>
      )}
    </div>
  )
}

/** 약 카드: 약 이름(제품명 + 제형) 15/600 + 옆에 성분명 12 gray-500, 아래 줄에 복용 방식 · 1회 복용량 · 메모 13 gray-500. */
function MyMedicineCard({ item }: { item: UserMedicine }) {
  const formLinkState = useRecordFormLinkState()
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
      state={formLinkState}
      aria-label={`${medicineTitle(item.medicine)} (${item.medicine.genericName}), ${details}. 수정하기`}
      className="flex items-center gap-3 rounded-card border border-gray-200 bg-white py-3 pr-2 pl-4 hover:border-gray-300"
    >
      <span className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className="flex min-w-0 items-baseline gap-1.5">
          <span className="min-w-0 truncate text-body font-semibold">{medicineTitle(item.medicine)}</span>
          <span className="min-w-0 truncate text-caption text-gray-500">{item.medicine.genericName}</span>
        </span>
        <span className="truncate text-label text-gray-500">{details}</span>
      </span>
      <Icon name="chevron_right" size={20} className="flex-none text-gray-400" />
    </Link>
  )
}
