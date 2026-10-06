# CLAUDE.md

이 파일은 Claude Code가 이 프로젝트를 도울 때 참고하는 안내서다.

## 프로젝트 개요
- **이름**: 것로그(gutlog)
- **무엇**: UC(궤양성대장염) 등 염증성 장질환 환자를 위한 증상 기록·분석 앱
- **핵심 기능(v1)**: 식사·복약·배변을 기록하고, 행동과 증상의 상관관계를 집계 분석
- **v1 목표**: 2026년 10월
- 기획: `docs/PLANNING.md` / 데이터 설계: `docs/ERD.md` / 디자인: `docs/design/`

## 저장소 구조
```
gutlog/
├── backend/    Spring Boot (개발자 담당)
├── frontend/   React (Claude 담당)
└── docs/       기획·설계·디자인 문서
```

## ⚠️ 역할 분담 — 반드시 지킬 것
이 프로젝트는 학습이 목적이며, 역할이 명확히 나뉜다.

### 백엔드 (`backend/src/main`) — 개발자가 직접 작성
- Claude는 `backend/src/main` 아래 코드를 **작성하거나 수정하지 않는다.**
- 백엔드에 대해서는 질문 답변, 개념 설명, 문제 진단, 개선 방향 제시만 한다.
- 코드 리뷰는 개발자가 명시적으로 요청할 때만 한다.

### 백엔드 테스트 (`backend/src/test`) — Claude가 작성
- 테스트 코드는 Claude가 작성한다.
- 테스트가 실패하면 **`src/main` 코드를 고쳐서 통과시키지 않는다.** 실패 원인만 보고한다.
- 각 테스트는 서로 영향 없이 독립적으로 실행되게 만든다.

### 프론트엔드 (`frontend/`) — Claude가 작성
- React / TypeScript / Tailwind로 화면과 API 연동을 구현한다.
- 백엔드 API에 맞춰 만든다. 백엔드에 없는 API를 가정하지 않는다. 필요하면 개발자에게 묻는다.

## 기술 스택
- **백엔드**: Spring Boot, Spring Security, JPA, JWT(jjwt), H2(개발) → PostgreSQL(배포)
- **프론트엔드**: React, TypeScript, Tailwind

## 백엔드 API 규칙 (프론트가 알아야 할 것)

### 공통 응답 형태
모든 응답은 아래 형태로 감싸져 온다.
```json
{ "success": true, "data": { ... }, "message": null }
{ "success": false, "data": null, "message": "에러 안내 문구" }
```
- 에러 시 `message`는 사용자에게 그대로 보여줘도 되는 한국어 문구다.
- 단, 401/403은 본문 없이 상태 코드만 올 수 있다. 상태 코드로 판단한다.

### 상태 코드
| 코드 | 의미 | 프론트 처리 |
|---|---|---|
| 400 | 입력값 검증 실패 | `message`를 폼에 표시 |
| 401 | 토큰 없음·만료·위조, 또는 로그인 실패 | 아래 인증 규칙 참고 |
| 403 | 권한 없음 (남의 데이터 접근) | 안내만, 로그아웃하지 않음 |
| 409 | 중복 (아이디 등) | `message` 표시 |
| 500 | 서버 오류 | 일반 오류 안내 |

### 구현된 API
| 기능 | 메서드 | 경로 | 인증 |
|---|---|---|---|
| 아이디 중복 확인 | GET | `/api/users/check-username?username=` | X |
| 회원가입 | POST | `/api/users/signup` | X |
| 로그인 | POST | `/api/users/login` | X |
| 내 정보 조회 | GET | `/api/users/me` | O |

- 중복 확인은 `data`가 `true`면 **사용 가능**, `false`면 이미 사용 중이다.
- 회원가입 요청: `username`, `password`, `nickname`, `role`, `ibdType`
  - `role`: `PATIENT` / `GUARDIAN` / `OTHER`
  - `ibdType`: 환자(`PATIENT`)일 때만 필수. 보호자·기타는 보내지 않는다.
  - enum 값은 백엔드 코드(`Role`, `IbdType`)를 기준으로 한다.
- 로그인 응답: `{ accessToken, user }`

## 프론트엔드 작업 규칙
- 백엔드 API와 필드 이름을 정확히 맞춘다. (예: 백엔드가 `bowelTime`이면 프론트도 `bowelTime`)
- 컴포넌트는 기능(도메인)별 폴더로 구성한다.
- API 호출은 공통 클라이언트(axios)를 통하며, 개별 화면에 흩뿌리지 않는다.
- 개발 서버에서는 `/api` 요청을 `http://localhost:8080`으로 프록시한다.
- 무엇을 만들기 전에 위 API 규칙과 관련 디자인 시안을 먼저 확인한다.

### 인증 처리
- 로그인 응답의 `accessToken`을 저장한다.
- **요청 인터셉터**: 모든 요청에 `Authorization: Bearer {accessToken}` 헤더를 자동으로 붙인다.
- **응답 인터셉터**: 401을 받으면 저장된 토큰을 지우고 로그인 화면으로 보낸다.
  - 단, 로그인 요청(`/api/users/login`)의 401은 제외한다. 비밀번호 오류이므로 폼에 문구만 표시한다.
  - 403은 로그아웃하지 않는다.
- 로그인이 필요한 화면은 토큰이 없으면 로그인 화면으로 보낸다. (화면 흐름 제어용이며, 실제 보안은 백엔드가 담당)

### 회원가입 화면
- 회원 유형(환자/보호자/기타)을 먼저 고른다.
- 환자를 골랐을 때만 질환 종류 선택을 보여준다.
- 아이디 옆 "중복 확인" 버튼은 `check-username` API를 사용한다.

## 디자인
- 디자인 핸드오프: `docs/design/README.md`
- 화면 시안: `docs/design/designs/*.dc.html`
- 시안을 High-fidelity로 재현한다. (색·간격·radius 값 그대로)
- 시안과 API가 다른 경우(예: 선택지 개수) **API를 기준**으로 하고, 다른 점을 개발자에게 알린다.

## 브랜치와 커밋
- 기능마다 `feature/기능명` 브랜치에서 작업하고, PR로 `main`에 합친다.
- 커밋 형식: `타입: 설명` (예: `feat: 식사 기록 화면 구현`)
- 타입: `feat`(기능), `fix`(버그), `docs`(문서), `refactor`(리팩터링), `style`(포맷), `test`(테스트), `chore`(설정·잡일)

## 현재 상태
- 기획·설계·디자인 완료
- 백엔드: 공통 기반(응답·예외·Security) 완료, User 인증(가입·로그인·JWT) 완료
- 다음 백엔드 작업: 배변 기록 → 식사 기록 → 복약 기록 → 분석
- 프론트엔드: 미착수. 로그인·회원가입 화면부터 시작 가능