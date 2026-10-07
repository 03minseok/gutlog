import { useAuth } from '../../auth/AuthContext'
import { Button } from '../../components/Button'
import { TabHeader } from '../../layouts/TabLayout'
import { IBD_TYPE_OPTIONS, ROLE_OPTIONS } from '../user/constants'

/** 마이페이지. 지금은 내 정보(/me)와 로그아웃만 있다. */
export function MyPage() {
  const { user, logout } = useAuth()
  const roleLabel = ROLE_OPTIONS.find((r) => r.value === user?.role)?.label
  const ibdLabel = IBD_TYPE_OPTIONS.find((t) => t.value === user?.ibdType)?.label

  return (
    <>
      <TabHeader title="마이페이지" />
      <div className="flex flex-col gap-3 px-5 pt-1 pb-6">
        {user && (
          <section className="flex flex-col gap-1 rounded-card border border-gray-200 bg-white p-5">
            <p className="text-[17px] font-bold">{user.nickname}</p>
            <p className="text-[13px] font-medium text-gray-500">
              {user.username} · {roleLabel}
              {ibdLabel && ` · ${ibdLabel}`}
            </p>
          </section>
        )}
        <Button variant="outline" onClick={logout} className="w-full">
          로그아웃
        </Button>
      </div>
    </>
  )
}
