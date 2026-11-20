---
name: add-daily-record
description: 하루에 한 번만 기록할 수 있는 기록 기능을 제작할 때 쓰는 스킬이다. 수면 시간 등이 그 예시이다.
---

# 하루 한 줄 기록 추가 (체중·수면 패턴)

날짜(`LocalDate`)당 한 줄만 있고, id 대신 날짜가 키다. 기준 구현은 `domain/record/weight`와 `domain/record/sleep`이다. 두 서비스는 이름과 값 필드만 다르므로, 둘 중 하나를 그대로 옮겨 시작한다. 하루에 여러 건이면 `add-several-record`를 쓴다.

## 0. 시작 전에 개발자에게 물을 것
- 둘 곳: 기록 탭 (수면처럼) / 마이페이지 (체중처럼)
- 값의 범위·단위·간격과 Java 타입 (예: 체중 1~300kg, 소수 둘째 자리, BigDecimal / 수면 30~1440분, 30분 단위, Integer)
- 날짜가 무엇을 뜻하는지 (예: 수면은 일어난 날)
- 홈 "오늘의 기록"에 넣을지. 넣으면 수면처럼 카드 아래 한 줄로 둔다
- 답을 받으면 backend/CLAUDE.md대로 구현 계획을 보여주고 확인받는다

## 1. 백엔드 (`domain/record/{type}/`)
순서: ErrorCode → 엔티티 → DTO → 리포지토리 → 서비스 → 컨트롤러
- 엔티티: `@UniqueConstraint(name = "uk_{type}_record_user_date", columnNames = {"user_id", "record_date"})`
  - `recordDate`(LocalDate)와 값 한 칸, `create(user, date, value)`, `update{Value}(value)`. userId로 조회하므로 `isOwnedBy`는 없다
- 요청 DTO에는 값만 넣는다(날짜는 경로로 받는다). 30분 단위처럼 애너테이션으로 안 되는 검증은 `@JsonIgnore @AssertTrue` 메서드로 하되, 값이 없거나 범위 밖이면 true를 돌려 오류 문구가 하나만 나오게 한다. 명세에 보이도록 `@Schema(multipleOf = …)`도 붙인다
- 응답 DTO는 `{ date, 값 }`이다 (id는 내보내지 않는다)
- 리포지토리: `findByUserIdAndRecordDate`, `findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc`
- 서비스 (`Clock` 주입)
  - 저장: `date.isAfter(LocalDate.now(clock))`면 기존 `FUTURE_DATE_NOT_ALLOWED`. 그날 기록이 있으면 값만 바꾸고, 없으면 user를 조회해 create
  - 삭제: 그날 기록이 없으면 `{TYPE}_RECORD_NOT_FOUND` / 기간 조회: `from`이 `to`보다 뒤면 `INVALID_DATE_RANGE`
- 컨트롤러 `/api/record/{type}`: GET `?from&to`, PUT `/{date}`, DELETE `/{date}` 세 개뿐이다. **POST와 단건 GET은 없다**
- ErrorCode: `{TYPE}_RECORD_NOT_FOUND` 한 줄 (W001, SL001처럼 새 접두사)

## 2. 테스트 (`src/test/.../domain/record/{type}/`)
작성 원칙은 backend/CLAUDE.md 참고.
- `service/`: Clock을 고정한다. 오늘은 허용, 미래는 거부(저장 시도도 없어야 함), 한국 시간 기준, 그날 기록이 있으면 수정·없으면 생성
- `controller/`: 같은 날짜로 다시 보내도 한 줄 유지, 다른 사용자는 같은 날짜에 따로 기록, 경계값, 날짜 형식 오류 400
- `repository/`: 유니크 위반, `Between`이 양 끝을 포함하는지
- `dto/`: 값 검증
- 커스텀 검증이 있으면 `{Type}ApiDocsTest`로 `/v3/api-docs`에 제약이 보이는지 확인한다 (`SleepApiDocsTest` 참고)

## 3. 프론트
- `api/{type}.ts`: `listByPeriod`, `save(date, value)`(PUT), `remove(date)` (`api/sleep.ts`와 같은 형식)
- 이미 있는 부품을 다시 쓴다: `LineChart`, `TrendBody`, `RecordFormScreen`의 `DateField`, `record/detail.ts`의 `useRangeRecords`. 공통 부품을 새로 뽑아냈으면 체중·수면도 그 부품을 쓰도록 같이 고친다
- `features/{type}/` (입력 화면 라우트 `/new`·`/:id/edit`는 만들지 않는다)
  - `{type}.ts`: 범위·간격 상수, `PATH`, 기간(7·30·90일), 값 포맷 함수
  - `{Type}EntryForm`(날짜 + 값 입력, 저장·삭제), `{Type}Chart`(`LineChart`를 감싼다)
  - 상세 화면: 입력 → 추이 → 날짜별 목록(수정·삭제)
  - 기록 탭이면 `{Type}Section`(`RecordSection` 대신 흰 카드 안에 EntryForm을 바로 둔다), 마이페이지면 `{Type}Card`

## 4. 새 타입을 붙일 때 고칠 곳
- 기록 탭에 둘 때 (수면처럼)
  - `App.tsx`: TabLayout 안 `/record/{type}`
  - `RecordPage.tsx`: 섹션 추가, 주석의 섹션 순서 수정
  - `TodaySummaryCard.tsx` (넣기로 했을 때만): 요청과 `SleepRow` 같은 한 줄
  - `detail.ts`의 `Category`와 `SWITCH_ORDER`에는 넣지 않는다 (수면도 넣지 않았다). 넣고 싶으면 개발자에게 묻는다
- 마이페이지에 둘 때 (체중처럼): `App.tsx` `/mypage/{type}`, `MyPage.tsx`에 카드. 홈과 기록 탭은 손대지 않는다

## 5. 문서
- `docs/design/README.md`: 선 그래프 행에 새 차트를 추가하고, 섹션·상세 규칙 행을 추가하고, "기록 탭"·"오늘의 기록"의 타입 목록을 갱신한다
- `docs/STATUS.md`: 진행 상황 한 줄을 추가한다
- CLAUDE.md는 직접 고치지 않는다. "동작 규칙"에 넣을 문구만 제안한다. 형식은 이렇게 맞춘다: "날짜당 한 줄이다. `PUT /api/record/{type}/{date}`는 등록·수정 겸용이다." + 날짜의 의미 + 값의 단위·범위
- 완료 전 검사 명령은 각 CLAUDE.md 참고
