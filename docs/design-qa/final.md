# 최종 디자인 검수 (이슈 #61)

2026-11-09. 에뮬레이터 `Pleos_Connect_v2_1_0`(2560×1440), main `908288a`(#60) + 이 PR의 #42 수정 빌드. 도구: `scripts/vconsole.sh`(기어·속도), `adb shell cmd uimode night`, `adb emu geo fix`, `android screen capture`, 내비 UI(목적지 "더현대서울" 안내 시작). 캡처: `shots/final/`.

## 결과

| # | 항목 | 판정 | 증거 / 수정 / 한계 |
|---|---|---|---|
| 1a | 정차 입력·추천 Night/Day | **PASS** | `parked-input-night`, `parked-recommend-night`, `parked-recommend-day`, `parked-input-day` |
| 1b | 주행 카드 Night/Day, 추가 중·성공(취소 가능)·완료 잠금·실패 | **PASS** | `drive-idle-{night,day}`, `drive-adding-night`, `drive-undo-day`(UndoBar "3초 안에 취소할 수 있어요"), `drive-result-night`(성공 시 내비 전면 전환), `drive-A-second-day`, `drive-after-handoff-C-day` |
| 1c | 주행 빈 상태 | **PASS** | 이전 라운드 `docs/e2e/B0` + `DriveScreenshotTest`(7상태). 이번엔 라이브 재캡처 안 함 |
| 1d | 핸드오프 4상태(전달 중·성공·실패·교체) | **PASS** | 전달 중은 내비가 전면에서 재탐색하는 동안이라 앱 화면 캡처 불가(`navi-recalc-waypoint-added-day`: 내비 "경유지 1개 포함"), 성공 `handoff-C-result-day`, 실패 `handoff-failed-night`, 교체 `handoff-replace-E-day`. 전달 중 문구는 `HandoffScreenshotTest`(이전 라운드 `shots/handoff/`) |
| 1e | 입력 검증 칩(Day) | **PASS** | `shots/final/...` 대신 라이브 확인(목적지·시간 오류 칩 표시). 캡처 파일 없음(작업 중 덮어씀) |
| 1f | #60 모션(입장/퇴장/크로스페이드/press 0.97) | **PASS(부분)** | `DriveMotionTest` 2건을 에뮬레이터에서 실제 실행, 통과. 화면 녹화(`screenrecord`)는 찍었으나 프레임 단위 타이밍 판독에 쓸 만한 샷을 못 얻어 **커밋 안 함**(녹화 파일 10MB+). 모션 수치는 `motion.md` 기준 |
| 1g | 주행 중 모션 최소·정차 허용 | **PASS(코드)** | 라이브: 주행 화면은 UndoBar/상태 슬롯 입장 외 모션 없음, 스크롤 없음. 정차 허용 모션(탭·카드 확장)은 `motion.md` 표대로 |
| 1h | 시스템 애니메이션 끔(`animator_duration_scale 0`) 폴백 | **PASS** | 0으로 두고 주행 카드 탭 → 상태 즉시 표시·정상 동작(`anim0-drive-*`). `AppMotion.reduce`가 `ANIMATOR_DURATION_SCALE==0`을 읽음 |
| 2 | 소프트 키보드가 "정차 추천 받기" 버튼을 가리지 않음 | **PASS** | 키보드 상단 y≈676 vs 버튼 y≈511(표시 기준). 목적지·시간 필드 모두, Night/Day: `keyboard-dest-night`, `keyboard-eta-night`, `keyboard-eta-day` |
| 3a | 큰 글자 1.3 / 2.0 | **PASS** | 입력·추천·주행 모두 잘림·겹침 없음: `font1.3-*`, `font2.0-*`. 2.0에서도 카드·버튼이 레이아웃 안에 들어감 |
| 3b | 긴 문구·긴 장소명 | **PASS(부분)** | 입력 필드는 단일 행(긴 목적지는 끝부분 표시·가로 스크롤)이라 깨지지 않음(`long-dest-font2.0-day`, 2.0배). 추천 목록·주행 카드의 **긴 장소명 줄바꿈은 데모 시설 이름이 짧아 라이브로 못 만듦** → 한계 |
| 3c | TalkBack 읽기 순서·역할·상태 안내 | **FAIL → 수정 대기** | 에뮬레이터 이미지에 TalkBack 패키지가 없어 **실제 TalkBack은 구동 못 함**(실기기 한정). 코드 점검: 역할은 `AppToggleChip`(Checkbox/RadioButton)·`AppButton`(Button)·`selectable` 행에 있음. **결함**: 주행 `StatusSlot`과 `HandoffScreen`이 `liveRegion`이 아니어서 추가 중/성공/실패/교체 안내가 스크린리더에 읽히지 않음. 수정은 `liveRegion=Polite` 한 줄씩(시각 변화 0) — UI 경로라 `figma-done` 승인 대기 중 |
| 4 | #42 슬롯 공유 | **PASS(수정)** | 아래 |
| — | Day 모드 전환 시 앱 상태 유지 | **PASS** | `uimode night no` 후 입력·추천 선택 유지(Activity 재생성 후에도 `PickController` Saver 동작) |

## #42 해결

**방안**: 두 흐름이 모두 지나가는 `ShellNavi`가 경유지 장부(`held`)를 가지고, `HandoffController`가 같은 목록을 쓴다. UI 변경 없음(Figma 불필요). 포트(`NaviGateway`)에 조회를 추가하지 않은 이유: 에뮬레이터 Navi는 재탐색 동안 `getWaypointInfo`가 빈 목록으로 흔들려 믿을 수 없다(`docs/agents/emulator.md` §6).

- `ShellNavi.held`: 성공한 add/remove를 기록(주행 카드·핸드오프 공통).
- `HandoffController(held = shellNavi.held)`: 주행 카드가 이미 채운 경유지를 안다.
- 라이브(Day): 정차에서 C(핸드오프)+A(주행 카드)를 넣은 뒤 E 선택 → **"경유지가 가득 찼어요 · C 대신 / A 대신 / 취소"**(`handoff-replace-E-day`). 수정 전에는 게이트웨이가 거절해 "전달하지 못했어요"로 끝났다. 이미 들어간 C를 다시 고르면 게이트웨이 호출 없이 즉시 "추가했어요"(`dup-C-already-held-day`).
- 단위 테스트: `HandoffTest.waypointsAddedElsewhereAreKnown`.
- **한계**: 장부는 이 앱을 통해 넣은 경유지만 안다. 내비 UI에서 직접 넣은 경유지, 앱 재시작 후의 경유지는 모른다(실측: 앱 재시작 직후 내비에 경유지 2개가 남아 있어 추가가 `경유지는 최대 2개까지` 토스트로 거절 → 실패 화면, `handoff-failed-night`). 주행 카드의 "이미 넣은 후보가 계속 표시"는 카드가 잠기고 상태 줄이 결과를 보여 주는 현재 동작을 유지(카드 숨김은 DESIGN.md 주행 카드 규칙·ADR-0003 "한 화면 한 선택"과 충돌해 하지 않음).

## 관찰 (결함 확정 못 함)

- 에뮬레이터 Navi가 주행 카드 추가 성공 직후 앱을 가리고 전면으로 올라오는 경우가 있다(첫 라운드). 두 번째 라운드부터는 올라오지 않았다. 재현 조건 불명.
- 경유지 1개를 넣은 뒤 내비 경로 카드에는 경유지 점이 보이지 않았다(2개일 때는 점 2개). 같은 시점 재탐색 화면은 "경유지 1개 포함"(`navi-recalc-waypoint-added-day`)이라 유실은 아닌 것으로 보이나, 내비 표시 규칙은 확인 못 했다. 앱 로그에 단서가 없어 미확정, 실기기에서 재확인.

## 남은 한계 (실기기 한정)

- TalkBack 실제 낭독·읽기 순서(에뮬레이터 이미지에 TalkBack 없음).
- TTS 낭독(`docs/e2e/README.md` V 항목).
- 실차 Navi와 에뮬레이터 Navi 동작 동일성(재탐색 시간·경유지 이벤트).
- 긴 장소명 줄바꿈(실제 POI 데이터 연동 후).
