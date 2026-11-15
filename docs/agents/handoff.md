# 인수인계 스냅샷 (2026-11-15)

다음 세션이 이어받을 현재 상태다. 날짜가 지나면 이슈·PR·각 문서가 이 파일보다 우선한다.

## 지금 상태

- 2026-11-15 오후: 1차 제출 준비 중(#15). 결정·진행은 [`docs/submission/checklist.md`](../submission/checklist.md).
- 열린 이슈: #15 제출물(마감 **2026-11-15 23:59 KST**), #1·#2 에뮬레이터 실측(`ready-for-human`).
- 앱은 #14로 실제 SDK에 연결됐다. 최종 E2E는 2026-11-14 22:41에 `scripts/e2e-demo.sh` 주간·야간으로 통과했다(#82 병합 직전 빌드). 녹화본은 로컬 `.e2e-final/`(저장소 제외), 화면 증거는 `docs/design-qa/shots/v79/`.

## 최근 병합 (2026-11-09 ~ 11-14)

| PR (이슈) | 내용 |
|---|---|
| #66 (#65) | 정차 입력·추천 화면 개편, 목적지 목록·시각 휠 |
| #68 (#67) | "급해요" 제거, 정차에서 필요한 것 아이콘 타일 |
| #71 (#69, #70) | 경유지 첫 전달 실패 수정. NaviHelper 콜백은 appId당 하나라 인스턴스 둘이 서로 덮어썼다 → `NaviSession` 하나를 공유 |
| #73 (#72) | 정차 입력 3단계 분리: 목적지 → 정차 조건 → 후보 |
| #75~#78 (#74) | AI PR 리뷰 workflow |
| #80 (#79) | 충전 화면: 충전 시간 직접 입력·카운트다운 제거, 차량 충전 정보 표시. 시각 표기를 `formatClock`(오전/오후 h:mm)으로 통일 |
| #82 (#81) | `pleos.car.permission.CAR_DRIVE` 선언(주행 가능 거리) |

## 결정

- **충전 화면은 차량 값만 보여 준다** (#79). SOC·충전 스위치·주행 가능 거리를 Vehicle SDK에서 받아 `정보 없음 / 충전 전 / 충전 중 / 충전 끝`으로 보여 준다. SDK에 완료까지 남은 시간 API가 없어 남은 시간은 추정하지 않는다. 못 받은 배터리 값은 "미확인", 못 받은 주행 가능 거리는 줄을 숨긴다. 충전 스위치는 리스너가 없어 3초마다 읽는다.
- **CAR_DRIVE 선언** (#81). 권한 없이 `Odometer.registerRangeRemainingListener`를 부르면 `SecurityException`으로 앱이 죽었다. 지금은 매니페스트에 선언하고, 런타임에 권한이 없으면 거리를 읽지 않는다. 승인 안 된 권한 선언은 Pleos 심사에서 걸릴 수 있다(`docs/research/sdk-facts.md`).
- **AI 리뷰는 라벨로만 돈다** (#74). `claude-review-request` 라벨을 붙이면 Opus가 PR을 심층 리뷰하고 끝나면 라벨을 뗀다. 1단계는 `claude -p` + `ANTHROPIC_API_KEY`(API 크레딧), 1단계가 실패하고 요약 코멘트도 없을 때만 2단계 `claude-code-action` + `CLAUDE_CODE_OAUTH_TOKEN`(구독). action을 거친 호출은 Claude Code 사용으로 분류돼 API 프로모션 크레딧이 적용되지 않아서 1단계를 직접 실행으로 바꿨다. 중복 방지 마커는 `<!-- ai-review:<head sha> -->`. 상세: `docs/agents/issue-tracker.md`.
- **병합**: gate 통과와 Qodo 지적 처리 뒤 묻지 않고 squash 병합, 브랜치·worktree 삭제(`docs/agents/issue-tracker.md`).

## 남은 일

**제출 (#15)**: 항목별 상태는 [`docs/submission/checklist.md`](../submission/checklist.md).

- 결정 끝: 개인·일반부문, 팀명 FamilyStop, 앱명 Family Stop, 지금 저장소를 그대로 Public 전환(실명 영문 노출 감수), 키스토어 생성(비밀번호는 macOS 키체인 `pleos-release-keystore`).
- 저장소: 서명한 `apk/FamilyStop_FamilyStop_release.apk`, README(에뮬레이터 버전·화면 사양, APK 위치), 기획서 확정본.
- 저장소 밖: 접수양식 DOCX와 시연 영상은 로컬 `.submission/`·`.e2e-final/`(git 제외)에 두고 메일에 첨부한다. 접수양식의 개인정보는 저장소에 올리지 않는다.
- 마지막 단계(메일 발송)는 사용자가 Gmail 임시보관함에서 직접 한다.

**1차 제출 진행 순서 (2026-11-15 16:45 기준, 마감 23:59)**

1. [x] 규정 재확인(Aside): 이메일 제출만, 메일에 저장소 주소·최종 커밋 번호, Collaborator 초대 불필요, 11/16까지 Public 전환 필요(11/23까지 유지), README에 에뮬레이터 버전·화면 사양.
2. [x] 키스토어·서명 APK·README·기획서 확정 → PR #85.
3. [ ] 시연 영상: 백그라운드 에이전트가 녹화·편집 중. 산출물 `.e2e-final/familystop-demo.mp4`(1080p, 20MB 이하, 자막·내레이션). 장면 s1~s4 녹화 끝, s5~s7·편집 남음. 끝나면 [`docs/submission/demo-script.md`](../submission/demo-script.md)를 녹화본 장면에 맞춰 고친다.
4. [ ] 마지막 PR 병합 뒤 main HEAD가 **최종 커밋 번호**다. 그 뒤 저장소 Public 전환(사용자 승인): `gh repo edit HyeokjaeChoi/pleos-family-stop --visibility public --accept-visibility-change-consequences`.
5. [ ] release APK 에뮬레이터 설치·실행 확인(debug를 먼저 `adb uninstall ai.familystop.app`).
6. [ ] 접수양식 최종본: 로컬 `.submission/README.local.md`의 명령으로 `.submission/form/fill_form.py`에 최종 커밋 해시를 넘겨 `.submission/FamilyStop_접수양식.docx`를 만든다. 01·04란(개인정보·동의·서명 이미지)은 이미 채워진다.
7. [ ] Gmail 임시보관 메일: Aside `google-gmail` 스킬(`aside repl` → `googleAccounts.print()`로 uid 확인 → `gmail.openComposer(uid, {to, subject, bodyHtml})` → `input[type=file][name=Filedata]`에 `setInputFiles`로 양식 DOCX·영상 첨부). **보내기는 누르지 않는다.** 받는 사람 appdevchallenge@hyundai-partners.com, 제목 `[차량용 앱 개발 공모전] 성명_성명`(개인 참가), 본문에 저장소 주소·최종 커밋 번호·첨부 목록. Gmail MCP `create_draft`는 첨부를 base64로 넘겨야 해서 영상 첨부에 못 쓴다.
8. [ ] 사용자가 임시보관함에서 확인 후 발송.

주의: 이 Mac의 LibreOffice는 시스템 글꼴을 읽지 못해 PDF 변환 시 한글이 빠진다. 양식은 DOCX로 낸다(DOCX·PDF 모두 허용). 개인정보(성명·연락처·서명)는 저장소·PR·이슈에 쓰지 않는다.

**그 외**

- #1, #2 에뮬레이터 실측.
- Pleos 콘솔에서 `CAR_DRIVE` 승인 여부 확인.
- API 프로모션 크레딧은 2026-12-14 만료. 그 뒤 AI 리뷰는 구독 폴백으로만 돈다.

## 검증 안 된 것

- 충전 카드 "정보 없음": 에뮬레이터에서 값을 끊을 수 없어 단위 테스트와 Figma(#79)로만 확인했다.
- AI 리뷰 workflow의 마지막 수정(마커 `contains`, 라벨 제거 `!cancelled()`)은 병합 뒤 실제 실행으로 확인하지 않았다. 그 전 두 번의 실행은 1단계(API 크레딧)로 성공했다.
- release APK는 아직 빌드·서명·설치하지 않았다.
- 음성 안내(TTS)와 위치 5초 타임아웃 분기는 에뮬레이터 한계로 실기기 확인이 필요하다(`docs/e2e/README.md`).
