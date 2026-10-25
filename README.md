# 것로그 (gutlog)

궤양성대장염·크론병 등 염증성 장질환(IBD) 환자를 위한 증상 기록·분석 앱.
식사·복약·배변을 기록하고, 행동과 증상 사이의 패턴을 찾는 것을 목표로 한다.

> 개발자 본인이 궤양성대장염 환자다. 다른 환우분들뿐 아니라 나도 매일 쓰고 싶은 앱을 만드는 것이 출발점이다.
> 기획: [docs/PLANNING.md](docs/PLANNING.md)

---

## Phase 구분

| | Phase 1 (완료) | Phase 2 (예정) |
|---|---|---|
| 목표 | Java/Spring 기본기로 기록 기능을 직접 구현 | 여러 AI 관련 기술을 적용해 기능 확장 |
| 방식 | 핵심 백엔드 코드를 직접 작성, 기능마다 "왜 이렇게 했는지" 기록 | 미정 |

### Phase 1 작업 방식
학습이 목적이라 역할을 나눠서 진행했다. (규칙은 [claude.md](claude.md))

| 영역 | 담당 |
|---|---|
| 기획·ERD·API 설계, 핵심 판단(삭제 방식, 유니크 조건 등) | 직접 |
| 백엔드 `src/main`: 회원·인증·공통 기반, 식사·배변 기록, 복용 일정 이력 리팩터링 | 직접 작성 |
| 백엔드 `src/main`: 복약 기록 1차 구현 | 직접 정한 설계대로 Claude가 구현 → 직접 리팩터링 |
| 백엔드 테스트 `src/test`, 프론트엔드 `frontend/` | Claude 작성, 직접 검토 |

---

## Phase 1에서 구현한 것

### 회원·인증
- 회원가입: 아이디 중복 확인, 회원 유형(환자·보호자·기타), 환자일 때만 질환 종류 선택
- 로그인: JWT 발급, 내 정보 조회
- 권한: 일반 사용자 / 관리자 분리, 관리자 API는 관리자만 접근

### 기록
| 기록 | 내용 |
|---|---|
| 식사 | 먹은 시각 + 음식 여러 개. 음식 자동완성(많이 먹은 순) |
| 배변 | 브리스톨 대변 척도 1~7형, 혈변 여부, 통증(0~10), 메모 |
| 복약 | 내 약 등록(매일 n회 / n일 간격 / 필요할 때), 회차별 복용 체크, 복용 일정 이력 |

- 공통: 등록·단건 조회·기간 조회·수정·삭제, 남의 기록은 404
- 약 마스터: 성분명·제품명·제형별 약 목록. 의약품안전나라로 확인한 초기 데이터 33건을 서버 시작 시 채운다.
- 관리자 약 관리: 약 추가·수정·활성/비활성

### 화면 (React)
로그인·회원가입, 홈(오늘의 기록 요약), 기록 탭(날짜 이동), 기록 상세·검색, 식사·배변 입력, 복약 체크, 내 약 관리, 관리자 약 관리

### 아직 하지 않은 것
- **분석:** 특정 음식을 먹은 다음날 혈변·복통 빈도 집계. Phase 1의 핵심 목표였지만 아직 구현하지 않았다.
- 약관·개인정보·민감정보 동의 내역 저장
- PostgreSQL 전환, JWT 비밀키 환경변수 주입, Swagger 공개 범위 결정

---

## 기술 스택

| 영역 | 기술 |
|---|---|
| 백엔드 | Java 21, Spring Boot 4.1, Spring Security, Spring Data JPA, Bean Validation, jjwt 0.12, springdoc-openapi |
| DB | H2 (개발) → PostgreSQL (배포 예정) |
| 프론트엔드 | React 19, TypeScript, Tailwind CSS 4, Vite, axios, React Router |
| 테스트 | JUnit 5, Mockito, AssertJ, MockMvc, `@DataJpaTest` |

---

## 다룬 기술·개념
자세한 내용과 이유는 [블로그 글](#관련-글-spring-security-파헤치기)에 정리했다.

- **인증·인가:** JWT, Spring Security 필터 체인, 회원 유형(Role)과 권한(Authority) 분리
- **API 설계:** 공통 응답·에러 코드, 요청 DTO 검증, 소유권 검사 404
- **JPA·데이터 설계:** N+1 방지(fetch join), 더티 체킹, 유니크 제약, 소프트 딜리트, 이력 테이블

---

## 핵심 설계 결정·트러블슈팅

| 주제 | 내용 |
|---|---|
| 403이 실제 서버에서만 401로 바뀜 | `sendError(403)` 뒤 `/error`로 재디스패치될 때 인증 정보가 없어서 생긴 문제. 에러 디스패치를 인가 검사에서 제외하고, 실제 포트로 띄우는 테스트로 고정 |
| ADMIN인데 403 | 필터에서 인증 정보를 등록한 뒤, 남아 있던 예전 코드가 빈 권한으로 덮어씀. "허용되는 경우" 테스트가 있어서 잡음 |
| 기존 DB에 NOT NULL 컬럼 추가 실패 | `ddl-auto: update`는 기존 행이 있으면 NOT NULL 컬럼 추가에 실패하고 조용히 넘어감. `@ColumnDefault`로 DB 기본값을 두거나 데이터 이전이 필요 |
| 복용 횟수를 바꾸면 과거 기록이 이상해짐 | 일정을 `UserMedicine`에 두면 수정이 과거까지 덮어씀 → 기간별 이력 테이블로 분리 |
| 혈변 여부가 빠지면 `false`로 저장됨 | 기본형 `boolean`은 값이 없어도 `false`가 됨 → `Boolean` + `@NotNull`로 "입력 안 함"과 "없음"을 구분 |

---

## 관련 글 (Spring Security 파헤치기)
- [1부 — 인증, 인가, 그리고 JWT](https://velog.io/@03minseok/Spring-Security-%ED%8C%8C%ED%97%A4%EC%B9%98%EA%B8%B0-1%EB%B6%80-%EC%9D%B8%EC%A6%9D-%EC%9D%B8%EA%B0%80-%EA%B7%B8%EB%A6%AC%EA%B3%A0-JWT)
- [2부 — ?userId=1을 JWT로 바꾸기](https://velog.io/@03minseok/Spring-Security-%ED%8C%8C%ED%97%A4%EC%B9%98%EA%B8%B0-2%EB%B6%80-userId1%EC%9D%84-JWT%EB%A1%9C-%EB%B0%94%EA%BE%B8%EA%B8%B0)
- [2.5부 — JWT 토큰 만료 리팩터링](https://velog.io/@03minseok/JWT-%ED%86%A0%ED%81%B0-%EB%A7%8C%EB%A3%8C-%EB%A6%AC%ED%8C%A9%ED%84%B0%EB%A7%81-...-Spring-Security-2.5%EB%B6%80)
- 3.1부 — 소셜 로그인 도입 리팩터링 (링크 추가 예정)
- 4부 — 권한(인가) 붙이기 (작성 중)

---

## 설계 문서
- 기획: [docs/PLANNING.md](docs/PLANNING.md)
- ERD: [docs/V1-2Erd.png](docs/V1-2Erd.png)
- 디자인: [docs/design/](docs/design/)
- API 명세: 백엔드 실행 후 `http://localhost:8080/swagger-ui/index.html`

## 실행 방법
```bash
# 백엔드 (http://localhost:8080) — H2 파일 DB(backend/data)를 쓰고, 처음 켤 때 약 목록을 채운다
cd backend
./gradlew bootRun

# 테스트
./gradlew test

# 프론트엔드 (http://localhost:5173) — /api 요청은 8080으로 프록시
cd frontend
npm install
npm run dev
```

관리자는 회원가입으로 만들 수 없다. DB에서 직접 지정한 뒤 다시 로그인한다.
```sql
update users set authority = 'ADMIN' where username = '아이디';
```

---

## Phase 2 계획
여러 AI 관련 기술을 적용해 기능을 확장할 예정이다. (구체적인 내용은 정해지면 추가)
- Phase 1에서 하지 못한 **분석**(음식 → 다음날 증상)을 어떻게 풀지
- v2 로드맵: UC 관련 정보 제공, 보호자-환자 연결 ([docs/PLANNING.md](docs/PLANNING.md))
