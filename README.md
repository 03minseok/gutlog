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

## 사용한 기술·개념과 이유

### 인증·인가
| 개념 | 어디에 | 왜 |
|---|---|---|
| JWT (stateless) | `JwtProvider`, `JwtFilter` | 프론트(React)와 백엔드가 분리돼 있고 모바일 확장도 고려해서, 서버 세션 대신 토큰으로 사용자를 증명 |
| Spring Security 필터 체인 | `SecurityConfig` | 컨트롤러에 닿기 전에 인증·인가를 한곳에서 처리. 401(인증 실패)과 403(권한 없음)을 구분해서 응답 |
| 회원 유형과 권한 분리 | `Role` / `Authority` | 회원 유형은 사용자가 고르는 값이라, 여기에 ADMIN을 섞으면 가입 요청으로 관리자가 될 위험이 있음. 관리자는 DB에서 직접 지정 |
| 권한을 토큰 claim에 | `JwtProvider.createToken` | 요청마다 DB를 조회하지 않음. 대신 권한 변경은 다시 로그인해야 반영된다(트레이드오프) |

### API 설계
| 개념 | 어디에 | 왜 |
|---|---|---|
| 공통 응답 + 에러 코드 | `ApiResponse`, `ErrorCode`, `GlobalExceptionHandler` | 프론트가 성공·실패를 같은 모양으로 처리하고, 에러 문구를 그대로 화면에 보여줄 수 있게 |
| 요청 DTO 검증 | `@Valid`, `@AssertTrue` | 형식 검증은 컨트롤러 진입 전에 끝냄. 필드끼리의 조합(예: 매일 먹는 약만 하루 횟수 입력)도 DTO에서 검사 |
| 소유권 검사는 404 | 각 서비스의 `findMy...` | 남의 기록에 403을 주면 "그 기록이 존재한다"는 정보가 새어 나감 |
| 기간 조회 `>= from` / `< to+1일` | 기간 조회 쿼리 | `between`과 시각 경계(23:59:59…) 문제 없이 날짜 단위로 조회 |
| 체크는 토글 API 하나 | `POST .../toggle` | "이미 체크된 회차를 또 체크"하는 요청 자체를 만들 수 없게 |

### JPA·데이터 설계
| 개념 | 어디에 | 왜 |
|---|---|---|
| N+1 방지 | `join fetch`, `@EntityGraph`, IN 쿼리 | 목록 조회에서 약·음식 이름을 꺼낼 때 행마다 쿼리가 나가지 않게. 날짜별 복약 화면은 쿼리 3번으로 고정 |
| 더티 체킹 | 수정·소프트 딜리트 | 조회한 엔티티의 필드만 바꾸면 커밋 때 UPDATE. 불필요한 `save()` 없음 |
| 유니크 제약 + 인덱스 | 음식(사용자+이름), 복용 기록(약+날짜+회차) 등 | 서비스 검사만으로는 동시 요청을 못 막음. DB 제약이 마지막 방어선(위반 시 409) |
| 소프트 딜리트 | 내 복용 약, 복용 기록, 약 마스터(비활성) | 과거 기록이 그 행을 가리키고 있어서 지우면 기록이 깨짐. 취소했던 회차를 다시 체크하면 새로 넣지 않고 되살림 |
| `@SQLRestriction`을 쓰지 않음 | `UserMedicine` | 붙이면 끊은 약을 JPA가 아예 못 읽어서, 남아 있는 복용 기록에서 약 정보를 꺼낼 때 에러가 남. 필요한 쿼리에만 `deleted = false`를 직접 건다 |
| 이력 테이블 (기간별 일정) | `MedicationSchedule` (`effectiveFrom` ~ `effectiveTo`) | 복용 횟수를 바꿔도 지난 날짜는 그때 일정으로 보여야 함. 수정하면 지금 일정을 닫고 오늘부터 새 일정 |
| 초기 데이터 | `MedicineDataInitializer` (CSV + `ApplicationRunner`) | 약 목록은 관리자가 관리하지만 처음엔 비어 있으면 안 됨. 비어 있을 때만 채우고, 형식이 틀리면 서버 시작을 멈춤 |
| `Clock` 주입 | `ClockConfig` | "오늘"을 서울 시간 기준으로 구하고, 테스트에서 날짜를 고정할 수 있게 |

### 테스트
| 종류 | 도구 | 무엇을 |
|---|---|---|
| 단위 | Mockito | 서비스 규칙(소유권, 회차 순서, 일정 이력) |
| 슬라이스 | `@DataJpaTest` | 쿼리 조건·경계·unique 제약을 실제 H2에서 |
| 통합 | `@SpringBootTest` + MockMvc | 요청부터 응답까지. 실제 요청처럼 테스트에 `@Transactional`을 걸지 않고 직접 정리 |
| 실제 서버 | `RANDOM_PORT` | MockMvc가 흉내 내지 않는 `/error` 재디스패치까지 (403→401 버그) |

백엔드 테스트 291개.

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
