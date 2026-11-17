import { Icon } from '../../components/Icon'
import { TabHeader } from '../../layouts/TabLayout'

/** 통계 탭. 분석은 v2 기능이라 지금은 "준비 중" 안내만 보여준다. 분석 API가 생기면 채운다. */
export function StatsPage() {
  return (
    <>
      <TabHeader title="통계" />
      <div className="px-4 pt-1 pb-6">
        <div className="flex flex-col items-center gap-2 rounded-card border border-dashed border-gray-300 px-4 py-8 text-center">
          <Icon name="bar_chart" size={32} className="text-gray-400" />
          <p className="text-body font-semibold text-gray-700">통계는 준비 중이에요</p>
          <p className="text-label text-gray-500">
            지금처럼 식사·배변·복약을 기록해 두면,
            <br />
            쌓인 기록으로 분석해 드릴게요.
          </p>
        </div>
      </div>
    </>
  )
}
