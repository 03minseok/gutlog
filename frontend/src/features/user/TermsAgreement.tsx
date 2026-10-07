import { Icon } from '../../components/Icon'
import { TERMS, type Agreements } from './terms'

interface TermsAgreementProps {
  value: Agreements
  onChange: (value: Agreements) => void
}

/**
 * 약관 동의 체크 UI. 동의 내역을 저장하는 API가 아직 없어서 서버에는 보내지 않는다. (CLAUDE.md)
 * 약관 문서가 없어서 시안의 상세 보기(chevron_right) 링크는 넣지 않았다.
 */
export function TermsAgreement({ value, onChange }: TermsAgreementProps) {
  const allOn = TERMS.every((term) => value[term.key])

  function toggleAll() {
    const next = !allOn
    onChange(Object.fromEntries(TERMS.map((term) => [term.key, next])) as Agreements)
  }

  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="mb-2 text-[14px] font-semibold text-gray-700">약관 동의</legend>
      <div className="overflow-hidden rounded-tile border-[1.5px] border-gray-200">
        <button
          type="button"
          role="checkbox"
          aria-checked={allOn}
          onClick={toggleAll}
          className={`flex min-h-[60px] w-full items-center gap-3 border-b border-gray-200 px-4 text-left ${allOn ? 'bg-blue-50' : 'bg-white'}`}
        >
          <Icon name="check_circle" size={26} filled className={allOn ? 'text-blue-600' : 'text-gray-300'} />
          <span className="text-[17px] font-bold text-ink">전체 동의</span>
        </button>
        <div className="flex flex-col py-1">
          {TERMS.map((term) => {
            const checked = value[term.key]
            return (
              <button
                key={term.key}
                type="button"
                role="checkbox"
                aria-checked={checked}
                onClick={() => onChange({ ...value, [term.key]: !checked })}
                className="flex min-h-12 w-full items-center gap-3 px-4 text-left"
              >
                <Icon name="check" size={22} className={checked ? 'text-blue-600' : 'text-gray-300'} />
                <span className="text-[15px] leading-[1.4] text-ink">
                  <span className={`font-semibold ${term.required ? 'text-blue-600' : 'text-gray-500'}`}>
                    {term.required ? '[필수]' : '[선택]'}
                  </span>{' '}
                  {term.label}
                </span>
              </button>
            )
          })}
        </div>
      </div>
    </fieldset>
  )
}
