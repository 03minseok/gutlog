---
name: resume-guardian
description: 보호자-환자 연결 작업을 진행 노트에서 이어서 한 항목 진행한다.
disable-model-invocation: true
---

# 보호자 연결 작업 이어가기

## 진행 노트
!`cat docs/guardian/progress.md`

## 최근 커밋
!`git log --oneline -5`

## 순서
1. 위 진행 노트의 작성 규칙을 따른다. 작업 전에 docs/guardian/design.md를 읽는다.
2. 진행 노트의 마지막 기록이 "개발자 구현 대기"면 **리뷰 단계**다.
   - backend/에서 ./gradlew test를 돌린다. 실패해도 src/main은 고치지 않는다.
   - design.md와 CLAUDE.md를 기준으로 개발자가 쓴 코드를 리뷰한다. 코드는 고치지 않는다.
   - 피드백은 힌트부터 준다(어느 부분, design.md 몇 절을 다시 보라). 개발자가 답을 알려달라고 하면 이유와 고치는 방향을 설명한다.
   - 테스트가 통과하고 개발자가 리뷰를 마쳤다고 하면 `passes: true`로 바꾸고, 세션 기록을 남기고, 커밋한다.
3. 아니면 **뼈대 단계**다.
   - docs/guardian/features.json에서 `passes: false`인 첫 항목 하나만 고른다.
   - 기존 테스트가 통과하는지 먼저 확인한다 (backend/에서 ./gradlew test).
   - 계획을 보여주고 확인받은 뒤 뼈대와 테스트를 만든다.
   - 서비스 메서드 본문은 `throw new UnsupportedOperationException("TODO: 개발자 구현");`으로 두고, 위에 design.md의 관련 절 번호만 주석으로 적는다.
   - 진행 노트에 "개발자 구현 대기"와 채울 메서드 목록을 남기고 멈춘다. 커밋하지 않는다.
4. 어느 단계든 완료 조건을 다 못 채우고 끝나면 `passes`는 그대로 두고, 어디까지 했는지 노트에 남긴다.