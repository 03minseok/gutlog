import { Link } from 'react-router'
import type { DailyMedicationItem } from '../../api/medication'
import { EmptyWithLink, RecordSection } from '../record/RecordSection'
import type { DayRecordsState } from '../record/useDayRecords'
import { MY_MEDICINES_PATH } from './constants'
import { MedicationDayCard } from './MedicationDayCard'
import { useDailyMedication } from './useDailyMedication'

/**
 * 기록 탭의 복약 섹션. 그날 복용 중이던 약과 그날 기록이 있는 끊은 약을 보여주고, 바로 체크한다.
 * 헤더의 n / m은 DAILY 약의 회차만 센다. (간격 투여·필요할 때 약은 세지 않는다)
 */
export function MedicationSection({ date }: { date: string }) {
  const { state, refresh } = useDailyMedication(date)
  const sectionState: DayRecordsState<DailyMedicationItem> =
    state.status === 'ready' ? { status: 'ready', items: state.data.medicines } : state
  const countLabel =
    state.status === 'ready' && state.data.dailyTotalCount > 0
      ? `${state.data.dailyTakenCount} / ${state.data.dailyTotalCount}`
      : ''

  return (
    <RecordSection
      title="복약"
      icon="medication"
      panel="bg-med-bg"
      iconColor="text-med"
      collapseAfter={2}
      state={sectionState}
      onRetry={refresh}
      countLabel={countLabel}
      action={
        <Link to={MY_MEDICINES_PATH} className="-mr-1 flex h-11 items-center rounded-input px-2 text-label font-semibold text-sky-700">
          내 약 관리
        </Link>
      }
      emptyMessage={<EmptyWithLink message="이 날 복용 중이던 약이 없어요" to={`${MY_MEDICINES_PATH}/new`} label="먹는 약 등록하기" />}
      detailTo="/record/medication"
      getKey={(item) => item.userMedicineId}
      renderItem={(item) => <MedicationDayCard item={item} date={date} onChanged={refresh} />}
    />
  )
}
