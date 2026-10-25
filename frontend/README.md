# 것로그 프론트엔드

Vite + React + TypeScript + Tailwind CSS v4

## 실행

```bash
npm install
npm run dev     # http://localhost:5173 — /api 요청은 localhost:8080(백엔드)으로 프록시
npm run build   # 타입 검사 + 빌드
```

백엔드(`../backend`)를 먼저 8080 포트로 띄워야 로그인·회원가입이 동작한다.

## 구조

```
src/
├── api/          axios 클라이언트(인터셉터), 공통 타입, 도메인별 API 함수
├── auth/         토큰 저장, 로그인 상태(AuthProvider), 라우트 가드
├── components/   디자인 시안의 공통 컴포넌트 (Button, TextInput, ...)
├── layouts/      하단 탭 레이아웃
└── features/     도메인별 화면 (user, home, record, stats, mypage)
```

디자인 토큰(색·radius·폰트·그림자)은 `src/index.css`의 `@theme`에 있다. 값은 `docs/design/README.md`와 같다.
