import { useAuth } from '../../auth/AuthContext'
import { Link } from 'react-router'
import { Button } from '../../components/Button'
import { Icon } from '../../components/Icon'
import { TabHeader } from '../../layouts/TabLayout'
import { ADMIN_MEDICINES_PATH } from '../admin/constants'
import { MY_MEDICINES_PATH } from '../medication/constants'
import { IBD_TYPE_OPTIONS, ROLE_OPTIONS } from '../user/constants'

/** 마이페이지. 내 정보(/me), 내 약 관리, (관리자만) 약 관리, 로그아웃. */
export function MyPage() {
  const { user, logout } = useAuth()
  const roleLabel = ROLE_OPTIONS.find((r) => r.value === user?.role)?.label
  const ibdLabel = IBD_TYPE_OPTIONS.find((t) => t.value === user?.ibdType)?.label

  return (
    <>
      <TabHeader title="마이페이지" />
      <div className="flex flex-col gap-3 px-4 pt-1 pb-6">
        {user && (
          <section className="flex flex-col gap-1 rounded-card border border-gray-200 bg-white p-4">
            <p className="text-section font-bold">{user.nickname}</p>
            <p className="text-label font-medium text-gray-500">
              {user.username} · {roleLabel}
              {ibdLabel && ` · ${ibdLabel}`}
            </p>
          </section>
        )}
        <MenuLink to={MY_MEDICINES_PATH} icon="medication" label="내 약 관리" />
        {user?.authority === 'ADMIN' && <MenuLink to={ADMIN_MEDICINES_PATH} icon="admin_panel_settings" label="약 관리 (관리자)" />}
        <Button variant="outline" onClick={logout} className="w-full">
          로그아웃
        </Button>
      </div>
    </>
  )
}

function MenuLink({ to, icon, label }: { to: string; icon: string; label: string }) {
  return (
    <Link to={to} className="flex min-h-12 items-center gap-3 rounded-card border border-gray-200 bg-white px-4 py-3 hover:border-gray-300">
      <Icon name={icon} size={22} className="text-gray-700" />
      <span className="flex-1 text-body font-semibold">{label}</span>
      <Icon name="chevron_right" className="text-gray-400" />
    </Link>
  )
}
