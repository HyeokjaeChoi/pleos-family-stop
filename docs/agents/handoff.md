# 인수인계 스냅샷 (2026-11-21)

다음 세션이 이어받을 현재 상태다. 날짜가 지나면 이슈·PR·각 문서가 이 파일보다 우선한다.

## 지금 상태

- **1차 제출 완료** (#15 닫힘). 2026-11-15 사용자가 메일 발송. 제출한 최종 커밋은 `6781fc5e723204d0300ef50f04e88adfc2463ade`. 접수양식 DOCX·시연 영상은 로컬 `.submission/`(git 제외)에 있다.
- **11/23(1차 발표)까지 저장소 Public 유지.** 그 전에 Private로 돌리지 않는다.
- **다국어 (#93)**: 하위 #94~#102 모두 main 병합. 한국어 기본(`values/`), 영어 `values-en/`, 앱별 언어 전환(`localeConfig`), TTS가 앱 언어를 따른다(#128). 결정은 ADR-0007. 에뮬레이터 앱별 언어 절차는 `docs/agents/emulator.md`(#126).
- **품질 점검 (#104)**: #105~#110 처리, main 71b22eb 기준 `scripts/e2e-demo.sh` 전 단계 통과(결과는 #104 댓글). 이후 #129(androidTest import 수정 + gate에 androidTest 컴파일), #131(주행 화면 대기 중 리컴포지션 20초당 80프레임 → 0) 병합.
- **열린 PR**: #132 NDK 30.0.16248370 고정(`build/ndk-30`, gate 통과, 이 문서 작업과 별개로 연 것).

## 최근 병합 (2026-11-16 ~ 11-21)

| PR (이슈) | 내용 |
|---|---|
| #112 | ADR-0007 다국어 결정 |
| #114 (#94) | 다국어 tracer: 폴백 한/영 + 리소스 기반 |
| #118 (#95), #119 (#101) | 공용 디자인시스템 문구, 앱 셸·위치 오류 한/영 |
| #121~#125 (#100, #98, #97, #99, #96) | 인계·충전 활동·주행·추천·입력 화면 한/영 |
| #126 | 에뮬레이터 앱별 언어 실측 절차 문서 |
| #128 (#102) | TTS가 앱 언어 따르기 |
| #113 (#108) | Pleos SDK 호출 `SecurityException` 방어 |
| #115 (#109) | TTS를 내비 안내 용도로, 말하는 동안 오디오 포커스 |
| #116 (#110) | TimeWheel stale index 수정 |
| #117 (#105) | release R8 minify·shrinkResources |
| #120 (#107) | `allowBackup=false`, NAVI_ROUTE_SEARCH 필요성 실측 기록 |
| #129 (#127) | androidTest import 수정, gate에 androidTest 컴파일 |
| #131 (#130) | Compose remember/derive, 250ms tick 읽기 범위 축소 |

## 결정

- **자율 병합 (2026-11-18 사용자 지시)**: PR은 AI 리뷰(`/code-review`) 지적 없음 + `gate`·`design-gate` 통과면 사용자 확인 없이 squash 병합, 브랜치·worktree 삭제. main은 strict(최신 반영 필수)라 여러 PR은 하나씩 `gh pr update-branch` → `gh pr checks --watch` → 병합 순서로 처리한다(갱신 직후 상태가 잠깐 UNKNOWN이거나 이전 검사 결과가 남아 있으니 무조건 갱신하고 새 검사를 기다린다).
- **모델 분담 (사용자 지시)**: 계획은 Fable 5.2, 정보 수집·설치는 sonnet/haiku 서브에이전트, 코드 작업은 sonnet 서브에이전트. 상위 모델 지정이 안 되면 Opus 5.5로 진행.
- **에뮬레이터는 한 번에 한 세션만** 쓴다. 쓰기 전에 다른 세션에 비어 있는지 확인한다.
- 이전 결정(충전 화면은 차량 값만, CAR_DRIVE 선언, AI 리뷰 라벨 workflow)은 그대로다. 상세: `docs/agents/issue-tracker.md`, `docs/research/sdk-facts.md`.

## 남은 일

- #103 [PT 이후] 한국어 조사 문장 정리 + 영어 넘침 배치 수정.
- 실행 중 앱 언어를 바꿨을 때 TTS 언어가 따라가는지 에뮬레이터 실측(#93 마무리 조건).
- #111 품질 백로그(이번에 실행하지 않음), #104 보고서 이슈는 열어 둠.
- #1, #2 에뮬레이터 실측(`ready-for-human`).
- Pleos 콘솔에서 `CAR_DRIVE` 승인 여부 확인.
- API 프로모션 크레딧 2026-12-14 만료. 그 뒤 AI 리뷰 workflow는 구독 폴백으로만 돈다.
- 다 쓴 worktree 정리: `.claude/worktrees/65-input-overhaul`, `practical-aryabhata-abf7e1`(detached) 등.

## 검증 안 된 것

- 충전 카드 "정보 없음": 단위 테스트와 Figma(#79)로만 확인.
- TTS 소리와 위치 5초 타임아웃 분기는 실기기 확인 필요(`docs/e2e/README.md`).
- #131의 프레임 수치는 품질 세션 보고값이고 메인 세션이 다시 측정하지 않았다.
- 시연 영상은 기계 검사(검은 화면·정지·무음)만 했다.

## 주의

- 개인정보(성명·연락처·서명)는 저장소·PR·이슈에 쓰지 않는다.
- 이 Mac의 iOS 시뮬레이터(`vaultme-*`)는 다른 프로젝트 것이다. PosterBoard 크래시는 이 저장소와 무관하다.
