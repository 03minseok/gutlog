---
name: add-several-record
description: 하루에 여러 번 기록 가능한 기록 기능을 제작할 때 쓰는 스킬이다. 식사 등이 그 예시이다.
---

# 하루 여러 건 기록 추가 (식사·배변 패턴)

기록 하나가 시각(`LocalDateTime`) 하나를 갖고 id로 구분된다. 기준 구현은 `domain/record/bowel`(단순한 형태)과 `domain/record/meal`(연결 엔티티가 있는 형태)이다. 하루에 한 줄만 있는 기록이면 `add-daily-record`를 쓴다.

## 0. 시작 전에 개발자에게 물을 것
- 둘 곳: 기록 탭 섹션 / 마이페이지
- 필드마다 값의 범위·단위·필수 여부, enum 값과 화면 표시 이름
- 홈 "오늘의 기록"에 넣을지. 지금은 3칸 그리드라 넣으면 디자인을 바꿔야 한다
- 카테고리 색을 새로 둘지 (없으면 수면처럼 회색 면)
- 답을 받으면 backend/CLAUDE.md대로 구현 계획을 보여주고 확인받는다

## 1. 백엔드 (`domain/record/{type}/`)
순서: 엔티티 → 요청 DTO → 응답 DTO → 리포지토리 → 서비스(+ErrorCode) → 컨트롤러
- 엔티티 `{Type}Record extends BaseTimeEntity`: `user`(LAZY, not null), `{type}At`(LocalDateTime), `@Index(user_id, {type}_at)`, 정적 `create(...)`, 전체 교체용 `update(...)`, `isOwnedBy(userId)`
- 요청 DTO는 등록·수정 공용 `{Type}CreateRequest` 하나다. 응답 DTO는 `{Type}Response.fromEntity`
- 리포지토리: `findAllByUserIdAnd{Type}AtGreaterThanEqualAnd{Type}AtLessThanOrderBy{Type}AtAsc`
- 서비스
- ErrorCode: `{TYPE}_RECORD_NOT_FOUND` 한 줄. 코드는 기존과 겹치지 않는 접두사 + 001 (B001, M001 등)
- 컨트롤러 `/api/record/{type}`: POST, GET `/{id}`, GET `?from&to`, PUT `/{id}`, DELETE `/{id}`
- 식사의 음식처럼 연결된 목록이 있으면 추가로 만든다: 연결 엔티티, 자동완성 API, SQL 개수 테스트(`MealRecordServiceQueryCountTest` 참고)

## 2. 테스트 (`src/test/.../domain/record/{type}/`)
네 파일을 만든다. 작성 원칙은 backend/CLAUDE.md 참고.
- `controller/{Type}ControllerTest`: 다섯 API의 성공, 400, 401, 남의 기록, 경계값
- `dto/{Type}CreateRequestTest`: 필드별 검증
- `repository/{Type}RecordRepositoryTest`: 기간 경계(시작 포함, 끝 제외), `isOwnedBy`
- `service/{Type}ServiceTest`: Mockito로 서비스 분기

## 3. 프론트
- `api/{type}.ts`: 타입 + `listByPeriod / get / create / update / remove` (`api/bowel.ts`와 같은 형식)
- `features/{type}/`
  - `{Type}FormPage`: `RecordFormScreen` + `DateTimeFields`
  - `{Type}List`: `RecordSection` + `useDayRecords`, 헤더 `AddRecordLink`, 빈 상태 `EmptyWithLink`
  - `{Type}DetailPage`: `CategoryDetail` 부품 + `useRangeRecords`·`groupByDate`
  - `constants.ts`

## 4. 새 타입을 붙일 때 고칠 곳
- `App.tsx`
  - TabLayout 안: `/record/{type}`
  - RecordFormLayout 안: `/record/{type}/new`, `/record/{type}/:id/edit`
- `features/record/RecordPage.tsx`: 섹션을 추가하고, 위쪽 주석의 섹션 순서도 고친다
- `features/record/detail.ts`: `Category`, `CATEGORY_META`, `DETAIL_PATH`
- `features/record/CategoryDetail.tsx`: `SWITCH_ORDER`. 전환 버튼이 3열 기준이라 열 수도 확인한다
- `features/home/TodaySummaryCard.tsx` (넣기로 했을 때만): 상태·요청·`Column` 추가, `grid-cols-3` 조정
- `index.css` `@theme` (새 색을 쓸 때만): `--color-{type}`, `--color-{type}-bg`
- 마이페이지에 두기로 했으면 위의 기록 탭 항목 대신 `MyPage.tsx`와 `/mypage/{type}` 라우트를 고친다

## 5. 문서
- `docs/design/README.md`: 새 부품·섹션 규칙 행을 추가하고, "기록 탭"·"오늘의 기록"·"기록 종류별 상세" 항목의 타입 목록을 갱신한다
- `docs/STATUS.md`: 진행 상황 한 줄을 추가한다
- CLAUDE.md는 직접 고치지 않는다. "동작 규칙"에 넣을 문구(`**{타입}**` 아래 1~3줄)만 제안하고, 화면 규칙이 있으면 frontend/CLAUDE.md용 문구도 제안한다
- 완료 전 검사 명령은 각 CLAUDE.md 참고
