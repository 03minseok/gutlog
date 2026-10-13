import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { adminMedicineApi, type Medicine } from '../../api/medication'
import { messageOf } from '../../api/types'
import { FieldHint } from '../../components/FieldHint'
import { Icon } from '../../components/Icon'
import { FormLoadStatus, RecordFormScreen } from '../../components/RecordFormScreen'
import { SegmentedControl } from '../../components/SegmentedControl'
import { CATEGORY_LABEL, medicineTitle } from '../medication/constants'
import { ADMIN_MEDICINES_PATH } from './constants'

type State = { status: 'loading' } | { status: 'ready'; items: Medicine[] } | { status: 'error'; message: string }
type Filter = 'all' | 'active' | 'inactive'

const FILTER_OPTIONS = [
  { value: 'all', label: '전체' },
  { value: 'active', label: '활성' },
  { value: 'inactive', label: '비활성' },
] as const

/**
 * 관리자 약 관리. 비활성 약도 모두 보이고, 활성/비활성을 바로 바꾼다.
 * 비활성 약은 검색·새 등록에서 빠지지만, 이미 복용 중인 사용자는 계속 보고 체크한다. (되돌릴 수 있어 확인창 없음)
 */
export function AdminMedicinesPage() {
  const navigate = useNavigate()
  const [state, setState] = useState<State>({ status: 'loading' })
  const [query, setQuery] = useState('')
  const [filter, setFilter] = useState<Filter>('all')
  const [pendingId, setPendingId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    adminMedicineApi
      .list()
      .then((items) => !cancelled && setState({ status: 'ready', items }))
      .catch((err) => !cancelled && setState({ status: 'error', message: messageOf(err, '약 목록을 불러오지 못했어요') }))
    return () => {
      cancelled = true
    }
  }, [])

  async function toggleActive(medicine: Medicine) {
    if (pendingId !== null) return
    setPendingId(medicine.id)
    setError(null)
    try {
      const updated = medicine.active ? await adminMedicineApi.deactivate(medicine.id) : await adminMedicineApi.activate(medicine.id)
      setState((s) => (s.status === 'ready' ? { ...s, items: s.items.map((m) => (m.id === updated.id ? updated : m)) } : s))
    } catch (err) {
      setError(messageOf(err, '바꾸지 못했어요. 다시 시도해 주세요.'))
    } finally {
      setPendingId(null)
    }
  }

  const keyword = query.trim().toLowerCase()
  const visible =
    state.status === 'ready'
      ? state.items.filter(
          (m) =>
            (filter === 'all' || (filter === 'active') === m.active) &&
            (!keyword || m.genericName.toLowerCase().includes(keyword) || m.brandName.toLowerCase().includes(keyword)),
        )
      : []

  return (
    <RecordFormScreen category="medication" title="약 관리 (관리자)" onClose={() => navigate('/mypage')}>
      {state.status !== 'ready' ? (
        <FormLoadStatus error={state.status === 'error' ? state.message : null} />
      ) : (
        <>
          <div className="flex flex-none flex-col gap-3 border-b border-gray-100 px-4 py-3">
            <div className="flex h-11 items-center gap-2 rounded-input border-[1.5px] border-gray-400 px-3.5 focus-within:border-sky-700 focus-within:ring-[3px] focus-within:ring-sky-100">
              <Icon name="search" size={20} className="text-gray-500" />
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                aria-label="성분명이나 제품명으로 찾기"
                placeholder="성분명이나 제품명"
                className="min-w-0 flex-1 bg-transparent text-body text-ink outline-none"
              />
            </div>
            <SegmentedControl label="상태" options={FILTER_OPTIONS} value={filter} onChange={setFilter} />
            {error && <FieldHint tone="error">{error}</FieldHint>}
          </div>

          <div className="flex-1 overflow-y-auto bg-gray-50 px-4 pt-3 pb-6">
            <p className="px-1 pb-2 text-label text-gray-500 tabular-nums">
              {visible.length}개 · 전체 {state.items.length}개
            </p>
            <ul className="flex flex-col gap-2">
              {visible.map((medicine) => (
                <li key={medicine.id} className="flex items-center gap-2 rounded-card border border-gray-200 bg-white py-3 pr-2 pl-4">
                  <Link to={`${ADMIN_MEDICINES_PATH}/${medicine.id}/edit`} className="flex min-w-0 flex-1 flex-col gap-0.5">
                    <span className="flex items-center gap-1.5">
                      <span className={`truncate text-body font-bold ${medicine.active ? 'text-ink' : 'text-gray-400'}`}>
                        {medicineTitle(medicine)}
                      </span>
                      {!medicine.active && (
                        <span className="flex-none rounded-full bg-gray-100 px-2 py-0.5 text-caption font-semibold text-gray-500">비활성</span>
                      )}
                    </span>
                    <span className="truncate text-label text-gray-500">
                      {medicine.genericName} · {CATEGORY_LABEL[medicine.category]}
                    </span>
                  </Link>
                  <button
                    type="button"
                    onClick={() => toggleActive(medicine)}
                    disabled={pendingId !== null}
                    className={
                      'h-11 flex-none rounded-input px-3 text-label font-semibold transition-colors disabled:opacity-50 ' +
                      (medicine.active ? 'bg-gray-100 text-gray-700 hover:bg-gray-150' : 'bg-sky-50 text-sky-700 hover:bg-sky-100')
                    }
                  >
                    {pendingId === medicine.id ? '바꾸는 중…' : medicine.active ? '비활성화' : '활성화'}
                  </button>
                </li>
              ))}
            </ul>
          </div>

          <div className="flex flex-none border-t border-gray-100 px-4 pt-3 pb-8">
            <Link
              to={`${ADMIN_MEDICINES_PATH}/new`}
              className="flex h-12 w-full items-center justify-center gap-1.5 rounded-btn bg-ink text-section font-semibold text-white hover:bg-ink-hover active:bg-ink-pressed"
            >
              <Icon name="add" size={22} />약 추가
            </Link>
          </div>
        </>
      )}
    </RecordFormScreen>
  )
}
