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
1. 위 진행 노트의 작성 규칙을 따른다.
2. docs/guardian/features.json에서 `passes: false`인 첫 항목 하나만 고른다.
3. 시작 전에 기존 테스트가 통과하는지 먼저 확인한다 (backend/에서 ./gradlew test).
4. 그 항목의 계획을 보여주고 확인받은 뒤 구현한다.
5. 완료 조건을 모두 확인하면 `passes`를 true로 바꾸고, 진행 노트에 세션 기록을 남기고, 커밋한다.
6. 완료 조건을 다 못 채웠으면 `passes`는 그대로 두고, 어디까지 했는지 노트에 남긴다.