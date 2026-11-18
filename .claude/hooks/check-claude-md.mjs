import { execSync } from 'node:child_process'
import { readFileSync } from 'node:fs'

// 훅이 이미 한 번 막은 뒤 다시 멈추려는 경우는 통과시킨다. (무한 반복 방지)
const input = JSON.parse(readFileSync(0, 'utf8') || '{}')
if (input.stop_hook_active) process.exit(0)

const changed = execSync('git status --porcelain -- CLAUDE.md backend/CLAUDE.md frontend/CLAUDE.md', {
  encoding: 'utf8',
}).trim()

if (changed) {
  console.error(
    'CLAUDE.md에 커밋되지 않은 변경이 있다:\n' + changed + '\n' +
      'CLAUDE.md는 개발자만 수정한다. 네가 고친 것이라면 되돌리고, 추가하고 싶은 규칙은 개발자에게 제안만 해라. ' +
      '네가 고친 것이 아니라면 개발자에게 알려라.',
  )
  process.exit(2)
}