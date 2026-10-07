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
    <div className="mx-auto flex min-h-dvh w-full max-w-[430px] flex-col gap-10 bg-white px-6 pt-14 pb-8">
      <div className="flex flex-col items-center gap-3.5">
        <div className="flex h-[72px] w-[72px] items-center justify-center rounded-[22px] bg-blue-600 text-[38px] font-bold tracking-[-0.02em] text-white">
          g
        </div>
        <div className="flex flex-col items-center gap-1">
          <h1 className="text-[28px] font-bold tracking-[-0.02em]">것로그</h1>
          <p className="text-[15px] text-gray-500">매일의 장 건강을 기록해요</p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-3" noValidate>
        <TextInput
          label="아이디"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="아이디 입력"
          autoComplete="username"
          autoCapitalize="none"
          spellCheck={false}
        />
        <TextInput
          label="비밀번호"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="비밀번호 입력"
          autoComplete="current-password"
        />
        {error && <FieldHint tone="error">{error}</FieldHint>}
        <Button type="submit" disabled={!canSubmit} className="mt-2">
          로그인
        </Button>
      </form>

      {/* 카카오·네이버 로그인, 아이디·비밀번호 찾기는 API가 없어 표시하지 않는다. (CLAUDE.md) */}
      <div className="mt-auto flex items-center justify-center gap-1.5 pt-1 text-[15px] text-gray-500">
        아직 회원이 아니신가요?
        <Link to="/signup" className="px-1 py-3 font-bold text-blue-600 hover:text-blue-700 hover:underline">
          회원가입
        </Link>
      </div>
    </div>
  )
}
