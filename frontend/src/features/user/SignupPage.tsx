import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { ApiError } from '../../api/types'
import { userApi, type IbdType, type Role } from '../../api/user'
import { useAuth } from '../../auth/AuthContext'
import { Button } from '../../components/Button'
import { FieldHint } from '../../components/FieldHint'
import { Icon } from '../../components/Icon'
import { SegmentedControl } from '../../components/SegmentedControl'
import { SelectTile } from '../../components/SelectTile'
import { TextInput } from '../../components/TextInput'
import {
  IBD_TYPE_OPTIONS,
  ROLE_OPTIONS,
  RULES,
  isValidNickname,
  isValidPassword,
  isValidUsername,
} from './constants'
import { TermsAgreement } from './TermsAgreement'
import { EMPTY_AGREEMENTS, hasRequiredAgreements, type Agreements } from './terms'

type UsernameCheck =
  | { state: 'idle' }
  | { state: 'checking' }
  | { state: 'available' }
  | { state: 'error'; message: string }

export function SignupPage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [usernameCheck, setUsernameCheck] = useState<UsernameCheck>({ state: 'idle' })
  const [password, setPassword] = useState('')
  const [nickname, setNickname] = useState('')
  const [role, setRole] = useState<Role | null>(null)
  const [ibdType, setIbdType] = useState<IbdType | null>(null)
  const [agreements, setAgreements] = useState<Agreements>(EMPTY_AGREEMENTS)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  const isPatient = role === 'PATIENT'
  const passwordOk = isValidPassword(password)
  const nicknameTooLong = nickname.trim().length > RULES.nickname.max

  const canSubmit =
    usernameCheck.state === 'available' &&
    passwordOk &&
    isValidNickname(nickname) &&
    role !== null &&
    (!isPatient || ibdType !== null) &&
    hasRequiredAgreements(agreements) &&
    !submitting

  function handleUsernameChange(value: string) {
    setUsername(value)
    // 아이디를 바꾸면 중복 확인을 다시 해야 한다.
    setUsernameCheck({ state: 'idle' })
  }

  async function checkUsername() {
    if (!isValidUsername(username)) {
      setUsernameCheck({ state: 'error', message: RULES.username.message })
      return
    }
    setUsernameCheck({ state: 'checking' })
    try {
      const available = await userApi.checkUsername(username)
      setUsernameCheck(available ? { state: 'available' } : { state: 'error', message: '이미 사용 중인 아이디예요' })
    } catch (err) {
      setUsernameCheck({ state: 'error', message: err instanceof ApiError ? err.message : '중복 확인에 실패했어요' })
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!canSubmit || role === null) return
    setSubmitting(true)
    setSubmitError(null)
    try {
      await userApi.signup({
        username,
        password,
        nickname: nickname.trim(),
        role,
        ...(isPatient && ibdType ? { ibdType } : {}),
      })
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        // 확인 후 그사이 다른 사람이 같은 아이디로 가입한 경우 등
        setUsernameCheck({ state: 'error', message: err.message })
      }
      setSubmitError(err instanceof ApiError ? err.message : '가입하지 못했어요. 다시 시도해 주세요.')
      setSubmitting(false)
      return
    }

    // 가입 API는 토큰을 주지 않으므로 같은 아이디·비밀번호로 로그인한다.
    try {
      await login(username, password)
      navigate('/', { replace: true })
    } catch {
      navigate('/login', { replace: true })
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      noValidate
      className="mx-auto flex h-dvh w-full max-w-[430px] flex-col bg-white"
    >
      <header className="flex h-14 flex-none items-center px-2">
        <Link to="/login" aria-label="뒤로" className="flex h-12 w-12 items-center justify-center rounded-input text-ink">
          <Icon name="arrow_back_ios_new" />
        </Link>
        <h1 className="text-[17px] font-semibold">회원가입</h1>
      </header>

      <div className="flex flex-1 flex-col gap-[22px] overflow-y-auto px-6 pt-3 pb-6">
        <TextInput
          label="아이디"
          value={username}
          onChange={(e) => handleUsernameChange(e.target.value)}
          placeholder="영문 소문자·숫자 4~16자"
          autoComplete="username"
          autoCapitalize="none"
          spellCheck={false}
          invalid={usernameCheck.state === 'error'}
          trailing={
            <Button variant="outline" onClick={checkUsername} disabled={username === '' || usernameCheck.state === 'checking'}>
              중복 확인
            </Button>
          }
          hint={
            usernameCheck.state === 'available' ? (
              <FieldHint tone="ok">사용할 수 있는 아이디예요</FieldHint>
            ) : usernameCheck.state === 'error' ? (
              <FieldHint tone="error">{usernameCheck.message}</FieldHint>
            ) : null
          }
        />

        <TextInput
          label="비밀번호"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="8자 이상, 영문·숫자 포함"
          autoComplete="new-password"
          hint={<FieldHint tone={passwordOk ? 'ok' : 'neutral'}>{RULES.password.message}</FieldHint>}
        />

        <TextInput
          label="닉네임"
          value={nickname}
          onChange={(e) => setNickname(e.target.value)}
          placeholder="앱에서 불릴 이름"
          autoComplete="nickname"
          invalid={nicknameTooLong}
          hint={nicknameTooLong ? <FieldHint tone="error">{RULES.nickname.message}</FieldHint> : null}
        />

        <div className="flex flex-col gap-2">
          <span className="text-[14px] font-semibold text-gray-700">
            회원 유형
          </span>
          <SegmentedControl label="회원 유형" options={ROLE_OPTIONS} value={role} onChange={setRole} />
        </div>

        {isPatient && (
          <div className="flex flex-col gap-2">
            <span className="text-[14px] font-semibold text-gray-700">질환 종류</span>
            <div role="radiogroup" aria-label="질환 종류" className="grid grid-cols-2 gap-2">
              {IBD_TYPE_OPTIONS.map((option) => (
                <SelectTile
                  key={option.value}
                  title={option.label}
                  description={option.description}
                  selected={ibdType === option.value}
                  onSelect={() => setIbdType(option.value)}
                />
              ))}
            </div>
            <span className="text-[13px] text-gray-500">선택한 질환에 맞춰 기록 항목이 구성돼요. 나중에 바꿀 수 있어요.</span>
          </div>
        )}

        <TermsAgreement value={agreements} onChange={setAgreements} />
      </div>

      <div className="flex flex-none flex-col gap-2 border-t border-gray-100 px-6 pt-3 pb-8">
        {submitError && <FieldHint tone="error">{submitError}</FieldHint>}
        <Button type="submit" disabled={!canSubmit}>
          가입하기
        </Button>
      </div>
    </form>
  )
}
