import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { ApiError } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { Button } from '../../components/Button'
import { FieldHint } from '../../components/FieldHint'
import { TextInput } from '../../components/TextInput'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/'

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const canSubmit = username !== '' && password !== '' && !submitting

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!canSubmit) return
    setSubmitting(true)
    setError(null)
    try {
      await login(username, password)
      navigate(from, { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : '로그인하지 못했어요. 다시 시도해 주세요.')
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-[430px] flex-col gap-9 bg-white px-5 pt-16 pb-[max(24px,env(safe-area-inset-bottom))]">
      <div className="flex flex-col gap-4">
        {/* 로고 자리: 앱 이름 첫 글자. 출시 전 이름이 바뀌면 함께 바꾼다. */}
        <div
          aria-hidden="true"
          className="flex h-12 w-12 items-center justify-center rounded-[14px] bg-sky-100 text-heading font-bold text-ink"
        >
          것
        </div>
        <div className="flex flex-col gap-1">
          <h1 className="text-title font-bold tracking-[-0.015em]">것로그</h1>
          <p className="text-body text-gray-500">매일의 장 건강을 가볍게 기록해요</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-3" noValidate>
        <TextInput
          label="아이디"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoComplete="username"
          autoCapitalize="none"
          spellCheck={false}
        />
        <TextInput
          label="비밀번호"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
        />
        {error && (
          <div className="rounded-input border border-error-border bg-error-bg px-3 py-2.5">
            <FieldHint tone="error">{error}</FieldHint>
          </div>
        )}
        <Button type="submit" disabled={!canSubmit} className="mt-2">
          로그인
        </Button>
      </form>

      {/* 카카오·네이버 로그인, 아이디·비밀번호 찾기는 API가 없어 표시하지 않는다. (CLAUDE.md) */}
      <div className="mt-auto flex items-center justify-center gap-1.5 pt-1 text-body text-gray-500">
        아직 회원이 아니신가요?
        <Link to="/signup" className="inline-flex min-h-11 items-center px-1 font-bold text-sky-700 hover:text-sky-800 hover:underline">
          회원가입
        </Link>
      </div>
    </div>
  )
}
