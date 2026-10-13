> **보관용 (v1, 2026-09)** — 현재 디자인 기준은 `docs/design/README.md`(v2, 모노 + 하늘)다.
> 이 문서의 색·크기·radius 값은 더 이상 쓰지 않는다. 통계 화면의 구성·데이터 규칙(5·6절)만 참고한다.
> 이 문서 안의 `designs/` 경로는 지금의 `archive/` 폴더를 가리킨다.

# Handoff: 것로그(gutlog) — UC·크론병 증상 기록 앱

## Overview
궤양성대장염(UC)·크론병 환자를 위한 모바일 증상 기록·분석 앱. 식사·복약·배변을 최소 단계로 기록하고, 기간별 요약과 "음식 → 다음날 혈변" 경향을 보여준다.
대상 화면: 로그인, 회원가입, 홈, 배변 기록 입력, 통계(기록 충분 / 기록 부족).

## About the Design Files
`designs/` 폴더의 파일은 **HTML로 만든 디자인 레퍼런스**다. 모양과 동작을 보여주는 프로토타입이며 그대로 배포할 코드가 아니다.
작업 목표: 이 디자인을 **React + Tailwind CSS**로 재구현한다. (권장: Vite + React + TypeScript + Tailwind v3/v4, 라우팅은 react-router.)
`.dc.html` 파일은 같은 폴더의 `support.js`와 함께 브라우저에서 바로 열린다. 스타일은 모두 인라인이므로 각 요소의 정확한 값은 소스에서 확인할 수 있다.

## Fidelity
**High-fidelity.** 색상·타이포·간격·radius·상호작용이 최종값이다. 픽셀 단위로 재현할 것.

## Platform & Global Rules
- 모바일 세로, 375–430px 기준 (디자인 캔버스 390×844). 데스크톱에서는 `max-w-[430px] mx-auto`로 가운데 정렬.
- 화면 좌우 여백 20px (로그인/회원가입은 24px).
- 최소 터치 영역 48px, 주요 버튼 56px.
- 본문 대비 4.5:1 이상. 색만으로 의미를 전달하지 않고 아이콘·라벨을 함께 쓴다.
- 숫자는 `font-variant-numeric: tabular-nums` (`tabular-nums` 클래스).
- 하단 탭 네비게이션: 홈 / 기록 / 통계 / 마이페이지 (로그인·회원가입·기록 입력 화면에는 없음).

## Design Tokens

### Tailwind config (theme.extend)
```js
colors: {
  ink: '#16202E',
  gray: { 700: '#3F4A5A', 500: '#6B7686', 400: '#8A94A3', 300: '#C9D0DA', 200: '#E3E7ED', 150: '#E9EDF2', 100: '#F1F3F6', 50: '#F7F9FB' },
  blue: { 800: '#16408C', 700: '#1E4FA8', 600: '#2B63CC', 300: '#8FB0EC', 100: '#E6EEFB', 50: '#F3F7FD' },
  bowel: { DEFAULT: '#0B6E8C', mid: '#5BB5D0', soft: '#9ED3E3', bg: '#E3F3F8' },
  meal:  { DEFAULT: '#1F7A4D', mid: '#6DBF93', bg: '#E5F4EC' },
  med:   { DEFAULT: '#5B3FB8', mid: '#A392E4', bg: '#EEEAFB' },
  danger: { DEFAULT: '#B42D32', bg: '#FCEAEA', border: '#F2C4C6' },   // 혈변
  warn:   { DEFAULT: '#A34B0B', bar: '#D9772B', bg: '#FDF0E4' },       // 주의
  ok:     { DEFAULT: '#136B61', bg: '#E3F3F1' },                       // 양호
  kakao: '#FEE500', kakaoInk: '#191600', naver: '#03C75A',
},
borderRadius: { input: '12px', btn: '14px', tile: '16px', card: '20px', sheet: '24px' },
fontFamily: { sans: ['"Pretendard Variable"', 'Pretendard', 'system-ui', 'sans-serif'] },
boxShadow: {
  seg: '0 1px 3px rgba(22,32,46,0.12)',
  toast: '0 8px 20px rgba(22,32,46,0.2)',
  fab: '0 6px 16px rgba(43,99,204,0.28)',
},
```
- Primary = `blue-600 #2B63CC`, hover `blue-700`, pressed `blue-800`.
- 화면 배경: 탭이 있는 화면 `gray-50 #F7F9FB`, 폼 화면 `#FFFFFF`.
- 카테고리 색은 항상 (텍스트/아이콘 = DEFAULT, 배경 = bg) 쌍으로 사용.

### Typography (Pretendard 단일 서체)
CDN: `https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.min.css`
| 역할 | size / weight | 기타 |
|---|---|---|
| Display | 40 / 700 | tracking -0.02em |
| 화면 제목 (통계) | 24 / 700 | -0.015em |
| 인사말 (홈) | 26 / 700, lh 1.35 | -0.015em |
| 큰 수치 | 32 / 700 (보조 28, 22) | -0.02em, 단위는 15–16 / 500 gray-500 |
| 섹션 제목 | 17 / 700 | |
| 폼 섹션 라벨 | 15 / 700 | 입력 라벨은 14 / 600 gray-700 |
| 버튼 | 17 / 600 | |
| 본문 | 16 / 400–500, lh 1.5–1.6 | |
| 캡션 | 13 / 500 gray-500 | 최소 12 (탭 라벨, 배지) |

### Spacing
4px 단위: 4 · 6 · 8 · 10 · 12 · 14 · 16 · 20 · 24 · 28 · 32. 카드 패딩 18–20, 섹션 간 간격 28 (홈·기록), 12 (통계 카드).

## Shared Components
- **StatusBar (목업용)**: 48px. 실제 앱에서는 safe-area로 대체.
- **BottomTabBar**: 흰 배경, `border-t gray-200`, 4열 grid, padding `6px 4px 26px`, 각 항목 min-h 56, 아이콘 24 + 라벨 12. 활성 = blue-600 + FILL 아이콘 + 700, 비활성 = gray-500 + 500.
  아이콘: home / edit_note / bar_chart / person.
- **Button**
  - Primary: h-14 rounded-btn bg-blue-600 text-white 17/600, hover blue-700.
  - Disabled: bg-gray-200 text-gray-500, `disabled` 속성. 라벨을 "무엇이 필요한지"로 바꿀 수 있음 (예: "배변 양상을 선택하세요").
  - Outline: h-12(또는 14) border-[1.5px] gray-300 bg-white, hover border-blue-600.
- **TextInput**: h-14, border-[1.5px] gray-200, rounded-input, px-4, 17px. focus: border-blue-600 + `ring-[3px] ring-blue-100`. placeholder gray-400.
  비밀번호: 오른쪽 48×48 아이콘 버튼 (visibility / visibility_off), input 오른쪽 padding 56.
- **SegmentedControl**: 트랙 bg `gray-100`(통계는 `gray-150`), p-1, rounded-btn, gap-1. 항목 h-11~12, rounded-[10px]. 선택 = bg-white text-blue-700 700 shadow-seg, 비선택 = transparent text-gray-700 500.
- **SelectTile**: min-h 56 (큰 것 88), rounded-btn/tile. 비선택 `border-[1.5px] gray-200 bg-white`, 선택 `border-2 blue-600 bg-blue-50 text-blue-700` + check 아이콘.
- **Toggle (switch)**: 60×34, p-[3px], thumb 28 흰색 + shadow. off = gray-300, on = 문맥색 (혈변은 danger). `role="switch" aria-checked`.
- **Card**: bg-white border gray-200 rounded-card p-5. 그림자는 사용하지 않음 (토스트·FAB만).
- **Toast**: 하단 탭 위(bottom 108px), 좌우 20, min-h 52, rounded-btn, bg ink, 흰 글씨 15/500, check_circle 아이콘 `#7FD6C9`. 1.8초 후 사라짐.
- **Badge/Pill**: px-2.5 py-1 rounded-full 13/600, 상태색 bg/텍스트 쌍.

## Screens

### 1. 로그인 (`Gutlog Login.dc.html` #1a)
- 흰 배경, 콘텐츠 padding `56px 24px 32px`, 세로 flex, 그룹 간 gap 40.
- 로고: 72×72 rounded-[22px] bg-blue-600, 흰 "g" 38/700. 아래 "것로그" 28/700, "매일의 장 건강을 기록해요" 15 gray-500.
- 아이디 / 비밀번호 TextInput (라벨 포함, gap 12). 비밀번호 보기 토글.
- 로그인 버튼(mt-2): 아이디·비밀번호 모두 입력 시에만 활성.
- "아이디 찾기 | 비밀번호 찾기" 15 gray-500, 구분선 1×12 gray-300.
- 하단(mt-auto): "간편 로그인" 구분선 → 카카오 버튼(bg #FEE500, text #191600, 말풍선 로고) → 네이버 버튼(bg #03C75A, 흰 N 로고) — 둘 다 h-14 rounded-btn 17/600. 카카오/네이버 공식 브랜드 가이드 로고로 교체 권장.
- "아직 회원이 아니신가요? **회원가입**"(blue-600 700) → 회원가입 화면.

### 2. 회원가입 (`Gutlog Login.dc.html` #1b)
- 헤더 56px: 뒤로(arrow_back_ios_new 48×48) + "회원가입" 17/600.
- 스크롤 영역 padding `12px 24px 24px`, gap 22. 하단 고정 버튼 영역 `12px 24px 32px`, border-t gray-100.
- 필드 순서:
  1. **아이디** + "중복 확인" outline 버튼(h-14). 확인 성공 시 ok색 "사용할 수 있는 아이디예요". 아이디를 다시 수정하면 확인 상태 초기화. (프로토타입은 4자 이상이면 성공 처리 → 실제 API 연결)
  2. **비밀번호**: placeholder "8자 이상, 영문·숫자 포함". 아래 힌트 13px — 조건 미충족 gray-500 + info, 충족 ok색 + check_circle. 규칙: 길이 ≥ 8 && /[a-zA-Z]/ && /\d/.
  3. **닉네임**: placeholder "앱에서 불릴 이름".
  4. **회원 유형**: SegmentedControl 환자 / 보호자 / 기타 (초기 미선택).
  5. **질환 종류**: 회원 유형이 **환자일 때만** 표시. 2열 SelectTile(min-h 88): "UC / 궤양성대장염", "크론병 / Crohn's disease", 우상단 radio_button_unchecked ↔ check_circle. 안내 13px "선택한 질환에 맞춰 기록 항목이 구성돼요. 나중에 바꿀 수 있어요."
  6. **약관 동의**: border-[1.5px] gray-200 rounded-tile 박스.
     - 상단 "전체 동의"(min-h 60, 17/700, check_circle FILL 26px; 전체 선택 시 blue-600 + 행 bg-blue-50). 누르면 전부 토글.
     - 항목(min-h 48): check 아이콘 22(선택 blue-600 / 미선택 gray-300) + "[필수]"(blue-600 600) 또는 "[선택]"(gray-500) + 라벨 15. 약관 문서가 있는 항목은 오른쪽 chevron_right 44×44 링크.
     - 항목: [필수] 만 14세 이상입니다(문서 없음) · [필수] 서비스 이용약관 · [필수] 개인정보 수집·이용 동의 · [필수] 민감정보(건강정보) 처리 동의 · [선택] 알림·혜택 정보 수신.
- **가입하기 활성 조건**: 아이디 중복확인 완료 && 비밀번호 규칙 충족 && 닉네임 입력 && 회원 유형 선택 && (환자면 질환 선택) && 필수 약관 모두 동의.

### 3. 홈 (`Gutlog Home.dc.html`)
- 배경 gray-50, 스크롤 영역 padding `20px 20px 24px`, gap 28. 하단 탭(홈 활성).
- 날짜: "9월 24일 목요일" 15/600 blue-600 (오늘 날짜 자동).
- 인사말 26/700: 시간대별 — 11시 전 "좋은 아침이에요", 17시 전 "오늘 하루 어떠세요", 이후 "오늘도 수고하셨어요" + 줄바꿈 + "{닉네임}님".
- **오늘의 기록 카드**: 제목 17/700. 3열 grid(열 사이 border-l gray-100): 식사 N회 / 복약 N/총 / 배변 N회. 각 열: 카테고리색 아이콘 18 + 라벨 14/600, 수치 32/700 + 단위.
- **빠른 기록**: 제목 17/700. 3열 grid gap 10, 각 타일 h-[76px] rounded-tile, 카테고리 bg + 카테고리 텍스트색, 흰 34×34 rounded-[10px] 아이콘 박스(아이콘 22) + 라벨 15/700, gap 4. 순서: 식사 · 복약 · 배변.
  - 동작: 각 기록 입력 화면으로 이동. (프로토타입은 즉시 +1 하고 토스트 표시 — 복약이 총량에 도달하면 "오늘 복약을 모두 마쳤어요")
- **준비 중 카드**: 2열 grid gap 10. "정보 제공 / 질환·식단·생활 정보"(menu_book), "커뮤니티 / 환우·보호자 이야기"(forum). 흰 카드 rounded-[18px] p-4, 회색 아이콘 박스 40×40 bg gray-100, 우상단 "준비 중" 배지(gray-100/gray-500 12/600). 비활성(aria-disabled), 탭 동작 없음.

### 4. 배변 기록 입력 (`Gutlog Bowel Record.dc.html`)
- 흰 배경, 하단 탭 없음. 헤더 56px(border-b gray-100): 닫기(close) + 28×28 bowel 아이콘 박스 + "배변 기록" 17/700.
- 스크롤 영역 padding `20px 20px 24px`, gap 28.
  1. **언제였나요?** — 2열(1.2fr / 1fr): 날짜(calendar_today + `input type=date`), 시간(schedule + `input type=time`). h-14 border gray-200 rounded-input, 값 16/600. 기본값 = 현재 시각. (모바일에서는 네이티브 피커 사용 권장)
  2. **배변 양상** — 2열 SelectTile(min-h 56), 라벨 16 + 설명 12 gray-500, 선택 시 check:
     딱딱함(덩어리·힘줘야 나옴) · 보통(부드럽고 형태 있음) · 무름(형태가 약간 풀림) · 진흙 같음(형태 거의 없음) · 물설사(액체 상태) · 점액만(변 없이 점액).
  3. **혈변** — 행 전체 카드(min-h 64, rounded-tile, border-[1.5px]): emergency 아이콘 + "혈변" 16/700 + 보조문구 13. 오른쪽 Toggle.
     off: 흰 배경/gray-200 테두리/아이콘 gray-300, "피가 보이지 않았어요". on: bg danger-bg, border danger-border, 아이콘·제목·토글 danger, "피가 섞여 있었어요".
  4. **통증 정도** — 제목 옆 오른쪽에 "{n}(24/700) / 10 · {라벨}". 라벨/색: 0 없음, 1–3 약함 (blue-700), 4–6 중간 (warn), 7–10 심함 (danger).
     숫자 버튼 11개를 6열 grid 두 줄(0–5, 6–10), h-12 rounded-input. 비선택 bg gray-100 text gray-700, 선택 bg blue-600 흰색 700. 아래 13px "0 통증 없음" ↔ "10 참을 수 없음". 기본값 0.
  5. **메모 (선택)** — textarea 3줄, placeholder "급박감, 잔변감, 점액 등 특이사항".
- 하단 고정 저장 버튼(`12px 20px 32px`, border-t): 배변 양상 미선택 시 비활성 + 라벨 "배변 양상을 선택하세요", 선택 시 "저장". 저장 → 토스트 "배변 기록을 저장했어요" → 이전 화면.

### 5. 통계 (`Gutlog Stats.dc.html` #4a — 기록 충분)
- 배경 gray-50, 하단 탭(통계 활성).
- 고정 헤더(padding `12px 20px`): "통계" 24/700 + 기간 SegmentedControl 1주일 / 1개월 / 3개월 (기본 1개월).
- 스크롤 영역(padding `4px 20px 24px`, gap 12):
  - 기간 설명 13 gray-500: "8월 26일 – 9월 24일 · 26일 기록".
  - **배변 카드**: 헤더 bowel 아이콘 박스 30×30 + "배변" 15/700, 우측 danger 배지 "혈변 N일". 좌측 "하루 평균" + 32/700 수치, 우측 미니 막대그래프(높이 최대 44px, radius 5, 과거 bowel-soft, 마지막 막대 bowel). 1주일=요일별 7개(14px 폭), 1개월=주별 4개(22px), 3개월=월별 3개(26px). 라벨 11px.
  - **식사 / 복약 카드** 2열: 식사 "하루 평균" 28/700. 복약 "복용률 · 54/60" + 28/700 % + 6px 진행 바(med-bg 트랙, med 채움).
  - **음식과 다음날 혈변 카드** (mt-2): 제목 17/700, 설명 13 "먹은 다음날과 먹지 않은 다음날의 혈변 비율을 비교했어요".
    - 항목(border-t gray-100, py-4): "{음식} · N일 섭취" + 관련도 배지. 3열 grid(72px / 1fr / 40px): "먹은 다음날" 막대(관련도색) + %, "안 먹은 날" 막대(gray-300) + %. 막대 h 10 rounded-full, 트랙 gray-100.
    - 관련도: ratio = 먹은 날% / 안 먹은 날%. ≥2 "관련 높음"(danger / 막대 #B42D32), ≥1.4 "관련 있음"(warn / 막대 #D9772B), 그 외 "뚜렷한 관련 없음"(gray / 막대 #8A94A3). ratio 내림차순 정렬.
    - 하단 안내 박스(bg gray-50 rounded-input p-3, info 아이콘): "기록에서 찾은 경향이며 원인을 뜻하지 않아요. 식단이나 치료를 바꾸기 전에 주치의와 상의하세요."
- 예시 데이터는 `Gutlog Stats.dc.html`의 logic class `D` 객체 참고 (API 응답 형태 설계에 활용).

### 6. 통계 — 기록 부족 (#4b)
- 조건: 선택 기간 내 기록일 < 7일.
- 헤더·기간 선택은 동일.
- 안내 카드(흰, p `28px 24px`, 가운데 정렬, gap 16): insights 아이콘 64×64 bg blue-50 → "분석하려면 기록이 더 필요해요" 20/700 → "7일 이상 기록하면 식사·복약·배변 요약과 음식별 혈변 경향을 보여드려요." 15 gray-700 → 진행 박스(bg gray-50 rounded-btn p-4): "기록한 날 **3일** / 7일" + 7칸 세그먼트 바(h 8, 채움 blue-600 / 빈칸 gray-200) → Primary "오늘 기록하기" (→ 기록 화면).
- "지금까지의 기록" 13/600 gray-500 + 3열 소형 카드(식사 8회 / 복약 5/6 / 배변 10회), 수치 22/700.

## State Management (제안)
- `auth`: user { id, nickname, role: 'patient'|'guardian'|'other', disease?: 'uc'|'cd' }.
- `records`: { meals[], meds[], bowels[] } — bowel = { datetime, shape, blood: boolean, pain: 0–10, memo }.
- `medSchedule`: 하루 총 복약 횟수 (홈의 N/총).
- 통계는 기간(`'1w'|'1m'|'3m'`)별 서버 집계 또는 클라이언트 계산. 상관관계는 최소 기록일 조건 충족 시에만 노출.
- 폼 상태는 각 화면 로컬(useState / react-hook-form + zod 권장).

## Assets
- 아이콘: Google **Material Symbols Rounded** (weight 500, opsz 24). 활성 탭은 `FILL 1`. React에서는 웹폰트 또는 `@material-symbols/svg-500` 패키지 사용.
  사용 아이콘: home, edit_note, bar_chart, person, restaurant, medication, water_drop(배변), emergency, check, check_circle, radio_button_unchecked, info, insights, calendar_today, schedule, close, arrow_back_ios_new, chevron_right, visibility, visibility_off, menu_book, forum.
- 카카오/네이버 로고는 임시 SVG — 각 사 공식 리소스로 교체.
- 이미지 에셋 없음.

## Files
`designs/` — 브라우저에서 바로 열 수 있음 (support.js 동봉):
- `Gutlog Design System.dc.html` — 컬러·타이포·버튼·카드·입력·탭바 규격
- `Gutlog Login.dc.html` — 로그인(#1a), 회원가입(#1b)
- `Gutlog Home.dc.html` — 홈
- `Gutlog Bowel Record.dc.html` — 배변 기록 입력
- `Gutlog Stats.dc.html` — 통계 (#4a 충분, #4b 부족)

## 아직 디자인되지 않은 화면
식사 기록, 복약 기록, 기록 탭(목록/캘린더), 마이페이지, 약관 상세, 아이디·비밀번호 찾기. 위 토큰과 컴포넌트 규칙을 그대로 따라 구현.
