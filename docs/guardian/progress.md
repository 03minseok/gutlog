# 보호자-환자 연결 진행 노트

기능 목록은 [features.json](features.json)에 있다.

## 작성 규칙
- 세션을 시작할 때 이 노트의 마지막 기록과 `features.json`에서 `passes: false`인 첫 항목을 확인한다.
- 한 세션에는 한 항목만 진행한다.
- 완료 조건을 모두 직접 확인한 항목만 `passes: true`로 바꾼다.
- 개발자가 답한 미정 질문은 질문 번호와 함께 "결정한 것"에 적는다.
- 새 세션 기록은 "세션 기록"의 맨 아래에 추가한다.
- features.json에서는 `passes`만 바꾼다. 항목이나 완료 조건은 고치거나 지우지 않는다. 바꿔야 하면 개발자에게 제안한다.
- "브라우저 확인"은 실제 사용자처럼 화면을 열고 눌러서 확인한다. 확인한 절차(어떤 계정으로, 어디를 눌러서, 무엇을 봤는지)를 세션 기록의 "한 일"에 적은 뒤에만 `passes: true`로 바꾼다.

## 세션 기록 형식

```markdown
### YYYY-MM-DD · G00 항목 이름

**한 일**
-

**결정한 것**
-

**다음 할 일**
-
```

## 세션 기록

### 2026-11-21 · G01 ERD·API 명세 확정(개발자 확인)

**한 일**
- 시작 전 `./gradlew test --rerun` 통과를 확인했다.
- Q1~Q15의 답을 아래에 기록했다.
- 현재 엔티티 코드를 기준으로 [design.md](design.md)에 ERD, API 목록, 동작 규칙을 작성했다.
- CLAUDE.md 추가 규칙과 features.json 완료 조건 변경안을 design.md 6·7장에 적어 개발자에게 제안했다.
- 개발자가 design.md의 ERD·API 목록과 C1~C6을 확인했다.
- 개발자 승인에 따라 features.json의 G04·G14·G19에 완료 조건을 하나씩 추가했다.
- 완료 조건을 모두 확인해 G01을 `passes: true`로 바꿨다.

**결정한 것**
- Q1: 보호자 API는 별도 경로 `/api/guardian/patients/{patientId}/...`로 둔다.
- Q2: 현재 연결과 해제 이력은 두 테이블로 나눈다.
- Q3: 이력에는 연결·해제 시각과 해제한 사람만 남긴다. 공개 범위 변경은 남기지 않는다.
- Q4: 초대 코드는 6자리 영문 대문자+숫자다. 보호자가 3번 틀리면 1분 동안 막고 "1분 뒤 다시 시도"를 안내한다(429).
- Q5: 코드는 원문으로 저장한다.
- Q6: 없는·만료된·사용된 코드는 구분 없이 404, 개수 초과와 이미 연결된 경우는 409다.
- Q7: 보호자가 연결된 환자는 코드를 발급할 수 없고, 해제한 뒤에 가능하다(409).
- Q8: 공개 범위는 전부 비공개로 시작하고, 재연결하면 초기화한다.
- Q9: 비공개·권한 없는 요청은 모두 404로 통일한다. 비공개 종류의 기간 조회도 404다.
- Q10: 보호자가 자기 기록 API를 호출하면 404다. 기존 보호자 계정의 기록은 신경 쓰지 않는다.
- Q11: 복약 "수정 가능"은 복용 체크와 내 약 등록·수정·중단까지 모두 포함한다.
- Q12: "보기만"이면 입력 화면에 들어갈 수 없으니 자동완성도 안 보인다. 보호자가 입력한 음식은 환자의 음식 목록에 쌓인다.
- Q13: "보호자 작성" 표시는 마지막으로 저장한 사람 기준이다.
- Q14: 표시 문구는 "보호자 작성"만 쓰고, 해제 후에도 유지한다.
- Q15: 보호자에게는 환자 닉네임만 보인다.
- C1: 3번째 오답의 응답부터 429로 1분 막는다. 1분이 지나거나 연결에 성공하면 횟수는 0이 된다.
- C2: 서버가 코드의 앞뒤 공백을 지우고 대문자로 바꾼 뒤 검사한다. 6자리 형식이 아니면 400이고 실패 횟수에 넣지 않는다.
- C3: 약 등록·수정은 내 약 목록에 "보호자 작성"으로 표시한다. 중단한 약은 목록에서 사라지므로 표시하지 않는다.
- C4: 환자 쪽 API 경로는 `/api/my-guardian`이다.
- C5: 환자에게는 보호자 닉네임이 보인다.
- C6: 식사·배변·체중·수면은 나중에 저장한 값이 남는다. 내 약도 나중 저장이 남되, 일정이 갈라지지 않게 행 잠금으로 한 명씩 처리한다. 수정 화면을 연 뒤 다른 사람이 고쳤는지 검사하는 409(낙관적 잠금)는 넣지 않는다.

**다음 할 일**
- 개발자가 design.md 6장의 "보호자 연결" 규칙을 CLAUDE.md에 반영할지 정한다. (429는 반영됨)
- 다음 세션은 G02(연결·초대 코드·공개 범위·해제 이력의 엔티티와 리포지토리)를 진행한다.

### 2026-11-24 · G02 연결·초대 코드·공개 범위·해제 이력의 엔티티와 리포지토리

**한 일**
- 시작 전 `./gradlew test --rerun` 통과를 확인했다.
- `com.gutlog.domain.guardian`에 엔티티 `GuardianLink`, `GuardianLinkHistory`, `GuardianInviteCode`, `GuardianCodeAttempt`와 enum `GuardianAccess`, 리포지토리 4개를 만들었다.
- 리포지토리 테스트 17개와 코드 만료 판단 단위 테스트 1개를 작성했다. 저장·조회, 공개 범위 기본값 `PRIVATE`, 유니크 4개(연결 `patient_id`, 코드 `patient_id`·`code`, 시도 `guardian_id`), 보호자 1명–환자 2명 연결 허용, 재발급 후 이전 코드 무효, 해제 후 재연결과 이력 유지를 확인했다.
- `./gradlew test` 통과를 확인했다.
- 기동 확인: 실행 중이던 개발 서버를 개발자 승인 후 종료하고, `data/gutlog.mv.db`를 백업한 뒤 `./gradlew bootRun`으로 띄웠다. 기존 파일 DB에서 새 테이블 4개, 유니크 4개, 인덱스 3개, FK 7개가 생성되고 `Started GutlogApplication`이 찍혔다. `/v3/api-docs`는 200, 토큰 없는 `/api/record/meal`은 401이었다. 확인 후 띄운 서버는 종료했다.

**결정한 것**
- 계획 확인(개발자): 패키지 `com.gutlog.domain.guardian`, enum `GuardianAccess`, `guardian_link.guardian_id`·이력의 `patient_id`·`guardian_id` 인덱스, 이력의 `disconnected_by_id` NOT NULL.
- 실패 횟수 증가·차단·초기화 메서드는 G04에서 규칙과 함께 만든다. 비관적 잠금 쿼리도 G04에서 추가한다.
- 보호자의 연결 목록은 연결 시각 순, 같으면 id 순이다(`findAllByGuardianIdOrderByConnectedAtAscIdAsc`).

**다음 할 일**
- 다음 세션은 G03(환자의 초대 코드 발급·재발급)을 진행한다.
- 개발 서버는 꺼진 상태다. 필요하면 개발자가 다시 띄운다.

### 2026-11-24 · G03 환자의 초대 코드 발급·재발급

**한 일**
- 시작 전 `./gradlew test --rerun` 통과를 확인했다. (408개)
- 계획을 개발자에게 확인받고 뼈대를 만들었다.
  - `MyGuardianController`: `GET`·`POST /api/my-guardian/invite-code`
  - `InviteCodeResponse(code, expiresAt)`
  - `GuardianInviteCodeService`, `InviteCodeGenerator` (본문은 TODO)
  - `ErrorCode`: `GUARDIAN_LINK_NOT_FOUND`(404, G001), `GUARDIAN_ALREADY_LINKED`(409, G002)
- 테스트 28개를 작성했다.
  - `MyGuardianInviteCodeTest`: 발급 시 만료 10분 뒤, 재발급 시 이전 코드 무효, 만료 후 재발급, 만료 경계(9분 59초 보임·10분 null), GUARDIAN·OTHER 404, 토큰 없음 401, Q7(연결 시 409, 연결 삭제 후 발급), 동시 요청(첫 발급·재발급 각 5회 반복)
  - `MyGuardianInviteCodeCollisionTest`: 생성기를 가짜로 바꿔 다른 환자 코드·다른 환자의 만료된 코드·자기 이전 코드와 겹치면 다시 만드는지 확인
  - `InviteCodeGeneratorTest`: 형식(6자리 영문 대문자+숫자)과 매번 같은 값이 아닌지
- `./gradlew test` 결과: 기존 408개 통과. 새 테스트는 토큰 없음 401 1개만 통과하고 27개는 TODO 때문에 실패한다(예상대로).

**결정한 것**
- 역할 불일치 404는 `GUARDIAN_LINK_NOT_FOUND`를 쓴다. ErrorCode는 이번에 쓰는 2개만 추가하고 나머지는 해당 항목에서 추가한다.
- GET `/api/my-guardian/invite-code`도 G03에 포함한다. 유효한 코드가 없으면 `null`이다.
- 코드 겹침 확인은 다른 환자의 만료된 코드와 자기 이전 코드도 포함한다. 서비스는 `InviteCodeGenerator.generate()`로만 코드를 만든다.
- 같은 환자의 동시 발급: 진 요청은 409(`DataIntegrityViolationException` 공통 처리)여도 된다. 500은 안 되고 코드는 1개만 남아야 한다. 잠금은 넣지 않는다.

**개발자 구현 대기**
- `InviteCodeGenerator.generate()` — design.md 4.1
- `GuardianInviteCodeService.getInviteCode(userId)` — design.md 3.1
- `GuardianInviteCodeService.issueInviteCode(userId)` — design.md 3.1, 4.1, Q7

**다음 할 일**
- 개발자가 위 3개 메서드를 구현하면 리뷰 단계를 진행한다. (테스트 실행 → design.md·CLAUDE.md 기준 리뷰 → 통과하면 G03 `passes: true`, 커밋)

### 2026-11-25 · G03 환자의 초대 코드 발급·재발급 (리뷰)

**한 일**
- 개발자가 `InviteCodeGenerator.generate()`, `GuardianInviteCodeService.getInviteCode()`·`issueInviteCode()`를 구현했다.
- 개발자 요청에 따라 서비스 메서드 위에 역할을 한 줄로 적었다. (backend/CLAUDE.md "서비스 로직 위에는 해당 메서드가 어떤 역할을 하는지 간단하게 적는다")
- 리뷰에서 힌트로 짚은 것과 개발자가 고친 것:
  - 코드가 없을 때 `orElseThrow`로 404 → `null`을 돌려주도록 (design 3.1)
  - `LocalDateTime.now()` → `Clock` 기준 (design 4.1)
  - `fromEntity(null)` NPE → `Optional.map`
  - 발급한 코드를 저장하지 않음 → 없으면 `create` 후 저장, 있으면 `reissue` (design 2장)
  - 코드 겹침 확인 누락, 이후 반복 조건이 거꾸로라 무한 루프 → `existsByCode`로 겹칠 때만 다시 만들기 (design 4.1)
- `./gradlew test --rerun` 통과: 기존 408개, G03 28개 모두 통과했다.
- 완료 조건(만료 10분, 재발급 시 이전 코드 무효, PATIENT 외 404, Q7, `./gradlew test`)을 모두 확인하고 개발자가 리뷰를 마쳐 G03을 `passes: true`로 바꿨다.

**결정한 것**
- 남은 다듬을 거리(`orElse` → `orElseGet`, `map` 안의 `reissue`, `expiredAt` 이름)는 개발자 판단으로 그대로 둔다. 동작에는 영향이 없다.

**다음 할 일**
- 다음 세션은 G04(보호자의 코드 입력으로 연결)를 진행한다. design.md 4.2 순서, 5.1(실패 횟수 롤백), 5.2(보호자 행 비관적 잠금)를 함께 본다.

### 2026-11-25 · G04 보호자의 코드 입력으로 연결

**한 일**
- 시작 전 `./gradlew test --rerun` 통과를 확인했다. (436개)
- 계획을 개발자에게 확인받고 뼈대를 만들었다.
  - `GuardianPatientController`: `POST /api/guardian/patients` (목록 조회·해제는 G05)
  - `ConnectPatientRequest(code)`: 생성자에서 앞뒤 공백 제거·대문자 변환 후 `@NotBlank`·`@Pattern("^[A-Z0-9]{6}$")` 검사 (C2)
  - `GuardianPatientResponse(patientId, nickname, connectedAt, access)`, `GuardianAccessResponse(meal, bowel, medication, weight, sleep)`
  - `ConnectResult`: 트랜잭션 결과(성공 응답 또는 ErrorCode)
  - `GuardianConnectService`(트랜잭션 없음, 커밋 뒤 예외) / `GuardianConnectProcessor`(`@Transactional`, 결과만 반환) (본문은 TODO)
  - `GuardianCodeAttempt`: `MAX_FAIL_COUNT = 3`, `BLOCK_MINUTES = 1`, `isBlocked`·`recordFailure`·`reset` (본문은 TODO)
  - 잠금 조회: `UserRepository.findByIdForUpdate`, `GuardianInviteCodeRepository.findByCodeForUpdate` (`@Lock(PESSIMISTIC_WRITE)` + `@Query`)
  - `ErrorCode`: `INVITE_CODE_NOT_FOUND`(404, G003), `INVITE_CODE_BLOCKED`(429, G004), `GUARDIAN_PATIENT_LIMIT`(409, G005), `PATIENT_ALREADY_LINKED`(409, G006)
- 테스트 42개를 작성했다.
  - `GuardianConnectTest`: 연결 성공(응답·공개 범위 전부 PRIVATE·코드 삭제), 같은 코드 재사용 404, 공백·소문자 정규화, 재발급 전 코드 404, 보호자당 2명, 형식 오류 400(실패 횟수 미포함), 만료 경계(9분 59초 성공·10분 404), PATIENT·OTHER 404, 토큰 없음 401, 환자 이미 연결 409, 3번째 환자 409(코드 미확인·횟수 미포함), 3번째 오답 429, 실패 횟수·차단 시각 DB 유지(5.1), 차단 중 맞는 코드도 429, 차단 경계(59초 429·1분 성공), 차단 후 0부터 다시 셈, 성공 시 초기화, 보호자별 횟수, 동시성(보호자 5명이 같은 코드 → 연결 1개, 한 보호자가 코드 3개 → 연결 2개, 각 3회 반복)
  - `GuardianCodeAttemptTest`: 초기 상태, 1·2번째 실패, 3번째 실패 시 1분 차단, 차단 경계, 초기화
  - `UserRepositoryTest`, `GuardianInviteCodeRepositoryTest`: 잠금 조회 메서드 각 1개
- `./gradlew test --rerun` 결과: 기존 436개 통과. 새 테스트는 형식 오류 400, 토큰 없음 401, 잠금 조회 리포지토리 테스트 11개만 통과하고 31개는 TODO 때문에 실패한다(예상대로).

**결정한 것**
- 5.1 처리 구조는 두 클래스로 나눈다. `GuardianConnectProcessor`(트랜잭션 안, 결과만 반환)와 `GuardianConnectService`(트랜잭션 밖, 커밋 뒤 예외).
- `GuardianCodeAttempt`의 실패 기록·차단 판단·초기화 메서드는 개발자가 직접 구현한다.
- 4.2의 6단계(환자가 이미 연결됨) 409는 실패 횟수에 넣지 않는다. 유효한 코드를 넣었기 때문이다.
- 역할 불일치 404는 G03과 같이 `GUARDIAN_LINK_NOT_FOUND`를 쓴다.
- 형식 오류 문구: 빈 값 "초대 코드를 입력해주세요.", 형식 "초대 코드는 영문 대문자와 숫자 6자리입니다."
- 개발자 요청에 따라 `process()`의 리뷰 지적 3개(4단계 ErrorCode `GUARDIAN_PATIENT_LIMIT`, 6단계 ErrorCode `PATIENT_ALREADY_LINKED`, 3번째 오답 429)는 Claude가 고치고 단계별 한 줄 주석을 달았다.

**개발자 구현 대기**
- `GuardianCodeAttempt.isBlocked(now)` — design.md 1장 C1, 4.2의 3단계
- `GuardianCodeAttempt.recordFailure(now)` — design.md 1장 C1, 4.2의 5단계
- `GuardianCodeAttempt.reset()` — design.md 1장 C1, 4.2의 7단계
- `GuardianConnectProcessor.process(guardianId, code)` — design.md 4.2, 5.1, 5.2, C1, Q6, Q8
- `GuardianConnectService.connect(userId, code)` — design.md 3.2, 5.1

**다음 할 일**
- 개발자가 위 5개 메서드를 구현하면 리뷰 단계를 진행한다. (테스트 실행 → design.md·CLAUDE.md 기준 리뷰 → 통과하면 G04 `passes: true`, 커밋)

### 2026-11-26 · G04 보호자의 코드 입력으로 연결 (리뷰)

**한 일**
- 개발자가 `GuardianCodeAttempt.isBlocked()`·`recordFailure()`·`reset()`, `GuardianConnectProcessor.process()`, `GuardianConnectService.connect()`를 구현했다.
- 리뷰에서 짚은 것과 고친 것:
  - `isBlocked`: `blockedUntil`이 null일 때 NPE, 횟수(`failCount > 3`)로 차단을 판단함 → 시각만으로 판단 (C1)
  - "1분이 지나면 0부터"가 없음 → `isBlocked` 안에서 초기화했다가 1·2번째 실패도 지워지는 문제 → `recordFailure`에서 차단이 끝난 뒤에만 초기화. `isAfter`로 정확히 1분째가 빠지는 경계 → `isEqual || isAfter` (C1)
  - `process`: 만료 `filter` 조건이 거꾸로, `findByCode` → `findByCodeForUpdate` (4.2의 5단계, 5.2), 새 시도 행 미저장, 차단 확인이 코드 조회 뒤에 있음 → 4.2 순서대로, 4단계(개수)·6단계(환자 이미 연결)·코드 삭제·`return` 누락
  - 개발자 요청으로 Claude가 고친 것: 4단계 ErrorCode `GUARDIAN_PATIENT_LIMIT`, 6단계 ErrorCode `PATIENT_ALREADY_LINKED`, 3번째 오답 429, `process()` 단계별 한 줄 주석
- `./gradlew test --rerun` 통과: 478개 (기존 436개, G04 42개) 모두 통과했다.
- 완료 조건(유효 코드 연결·재사용 불가, 만료·역할 404, 환자당 1명·보호자당 2명, 동시성 2종, 3번 실패 429·1분 뒤 재입력, `./gradlew test`)을 모두 확인하고 개발자가 리뷰를 마쳐 G04를 `passes: true`로 바꿨다.

**결정한 것**
- 처음 조회·역할·개수(4단계)·환자 연결(6단계) 실패는 지킬 변경이 없어 트랜잭션 안에서 예외를 던져도 된다. 실패 횟수를 바꾸는 5단계만 `ConnectResult`로 돌려준다. (5.1)
- 남은 다듬을 거리(`MAX_GUARDIAN_COUNT` 이름·`static`, `findAll().size()` → `countByGuardianId`, `orElse` → `orElseGet`)는 그대로 둔다. 동작에는 영향이 없다.

**다음 할 일**
- 다음 세션은 G05(연결 조회와 해제, 해제 이력 저장)를 진행한다.

### 2026-11-28 · G05 연결 조회와 해제, 해제 이력 저장

**한 일**
- 시작 전 `./gradlew test --rerun` 통과를 확인했다. (478개)
- 계획을 개발자에게 확인받고 뼈대를 만들었다.
  - `MyGuardianController`: `GET`·`DELETE /api/my-guardian`
  - `GuardianPatientController`: `GET /api/guardian/patients`, `DELETE /api/guardian/patients/{patientId}`
  - `MyGuardianResponse(guardianNickname, connectedAt, access)` (보호자 쪽 목록은 기존 `GuardianPatientResponse`)
  - `GuardianLinkService` (메서드 4개, 본문은 TODO)
  - 잠금 조회: `GuardianLinkRepository.findByPatientIdForUpdate` (`@Lock(PESSIMISTIC_WRITE)` + `@Query`)
  - 새 ErrorCode는 없다. 역할 불일치·연결 없음은 `GUARDIAN_LINK_NOT_FOUND`(404)다.
- 테스트 35개를 작성했다.
  - `GuardianLinkTest`: 조회(환자 null·보호자 정보, 보호자 빈 배열·연결 순서, 남의 연결 안 보임), 역할 404(GET·DELETE 양쪽, 연결된 환자가 보호자 쪽 해제를 불러도 404), 토큰 없음 401, 환자 해제(data null, 연결 삭제, 이력의 연결·해제 시각·해제한 사람, 양쪽에서 사라짐, 다른 환자 유지, 연결 없음 404, 두 번째 404), 보호자 해제(같은 확인, 제3자·미연결·없는 id·환자 아닌 id 404), 재연결(해제 뒤 코드 발급 가능 Q7, 같은·다른 보호자와 재연결, 이력 2줄, 2명 제한 풀림), 동시 해제(환자+보호자 5회, 환자 2번+보호자 2번 3회 → 200 1개·나머지 404·이력 1줄)
  - `GuardianLinkRepositoryTest`: 잠금 조회 1개
- `./gradlew test --rerun` 결과: 513개 중 480개 통과. 기존 478개와 잠금 조회 리포지토리 테스트, 토큰 없음 401이 통과하고, 33개는 TODO 때문에 500이 나서 실패한다(예상대로).

**결정한 것**
- 동시 해제(개발자): 연결 행을 `PESSIMISTIC_WRITE`로 잠근다. 늦은 요청은 기다렸다가 연결이 없어 404다. 잠그지 않으면 늦은 쪽 DELETE가 0줄이 되어 Hibernate 충돌 예외로 500이 난다.
- 서비스는 `GuardianLinkService` 하나로 둔다(개발자). 메서드 역할 주석은 개발자가 단다.
- 해제 응답 `data`는 `null`이다. 해제 시각은 `Clock` 기준이다.

**개발자 구현 대기**
- `GuardianLinkService.getMyGuardian(userId)` — design.md 3.1, C5
- `GuardianLinkService.disconnectMyGuardian(userId)` — design.md 2장(guardian_link_history), 3.1, Q2, Q3
- `GuardianLinkService.getPatients(userId)` — design.md 3.2, Q15
- `GuardianLinkService.disconnectPatient(userId, patientId)` — design.md 2장(guardian_link_history), 3.2, Q2, Q3, Q9

**다음 할 일**
- 개발자가 위 4개 메서드를 구현하면 리뷰 단계를 진행한다. (테스트 실행 → design.md·CLAUDE.md 기준 리뷰 → 통과하면 G05 `passes: true`, 커밋)

### 2026-11-28 · G05 연결 조회와 해제, 해제 이력 저장 (리뷰)

**한 일**
- 개발자가 `GuardianLinkService.getMyGuardian()`·`disconnectMyGuardian()`·`getPatients()`·`disconnectPatient()`를 구현했다. 보호자 쪽 잠금 조회 `GuardianLinkRepository.findByPatientIdAndGuardianIdForUpdate`도 개발자가 추가했다.
- 리뷰에서 짚은 것과 개발자가 고친 것:
  - `disconnectMyGuardian`: 이력만 저장하고 연결을 지우지 않음 → `delete` 추가 (2장)
  - `disconnectPatient`: 환자 id로만 찾아 제3자도 해제됨 → 환자·보호자로 찾기. 잠금 없는 조회로 바뀌어 동시 해제가 500 → 보호자 쪽도 잠금 조회 (G05 결정). `@Param` 오타(`parientId`)로 500 → 수정
  - `getMyGuardian`: 연결이 없을 때 404 → `map(...).orElse(null)` (3.1). 개발자 요청으로 예시 코드를 보여줬다
  - 역할 확인 누락(`getMyGuardian`·`getPatients`·`disconnectPatient`) → 요청한 사람의 역할 확인, 불일치는 `GUARDIAN_LINK_NOT_FOUND` (3.1, 3.2)
  - 경로 `patientId`가 없는 사용자일 때 "사용자를 찾을 수 없습니다." → `GUARDIAN_LINK_NOT_FOUND`로 통일 (Q9)
- `./gradlew test --rerun` 통과: 513개 (기존 478개, G05 35개) 모두 통과했다.
- 완료 조건(환자·보호자 각자 조회, 양쪽 해제와 제3자 404, 해제 후 이력과 재연결, `./gradlew test`)을 모두 확인하고 개발자가 리뷰를 마쳐 G05를 `passes: true`로 바꿨다.

**결정한 것**
- 남은 다듬을 거리는 그대로 둔다. 동작에는 영향이 없다.
  - `disconnectPatient`의 `patient` 조회와 역할 확인: 잠금 조회가 이미 걸러 준다
  - `findByPatientIdAndGuardianIdForUpdate` 설명 주석

**다음 할 일**
- 다음 세션은 G06(환자의 기록 종류별 공개 범위 조회·변경)을 진행한다. Q8(연결·재연결 시 전부 비공개)과 3.1의 `PUT /api/my-guardian/access`를 함께 본다.
