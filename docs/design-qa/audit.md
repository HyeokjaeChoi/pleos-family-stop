# 디자인 일괄 점검 (이슈 #50)

2026-11-08. 에뮬레이터 `Pleos_Connect_v2_1_0`(2560×1440 mdpi, 1px=1dp), main `d2ed9ca`+#49 기준 APK 재빌드·재설치. 캡처 `shots/audit/`(이번 신규), `shots/`(#49 기존: Day·충전·동의·폴백). Figma 기준은 `figma/` + [DESIGN.md](../../DESIGN.md). 코드는 바꾸지 않았다.

## 0. 점검 도구 로드 결과

| 요청 | 결과 |
|---|---|
| `stageshow`, `better-interface`(+layout·typography·colors·ui·accessibility·writing), `emil-design-eng`, `impeccable`, `make-interfaces-feel-better`, `ui-ux-pro-max`, `design-taste-frontend`, `apple-design`, `web-design-guidelines`, `design:design-critique`, `design:accessibility-review` | 전부 로드. 단 대부분 **웹/모바일 기준**이라 AAOS 수치(76dp, 24dp, ADR-0003/0004)가 우선. `design-taste-frontend`(랜딩 전용)·`stageshow`(가짜 UI 일러스트)·`web-design-guidelines`(웹 `file:line` 규칙)는 차량 앱에 거의 해당 없어 **참고만**, 항목 근거로 쓰지 않았다 |
| Emil Kowalski 계열 | 설치된 것은 `emil-design-eng` 하나. (`transitions-dev`·`transitions-polish`·`animate-expo`는 설치돼 있으나 Emil 작이라는 근거 없어 미로드) |
| `hallmark`, `k-style-ui-ux` | **미설치**. SearchSkills에도 없음. `k-style-ui-ux`는 DESIGN.md 9절대로 checklist만 읽기 전용 참조한 상태 그대로 |
| `impeccable` | 로드했으나 `context` 스크립트·PRODUCT.md 흐름이 웹 프로젝트용이라 실행하지 않음. critique 관점(위계·밀도)만 사용 |
| 가용하지만 이번엔 안 쓴 것 | `omd-*`(DESIGN.md가 사용 안 함으로 명시), `ui-ux-pro-max` 검색 스크립트(웹 팔레트 DB) |

규칙 충돌 메모: `better-accessibility`·`ui-ux-pro-max`의 터치 44px/8px 기준은 이 프로젝트의 76dp/24dp가 더 엄격하므로 **프로젝트 값을 따랐다**. `make-interfaces-feel-better`의 hover·`scale(0.96)`은 터치 차량 화면에서 hover 무의미, press 피드백만 의미 있음.

## 1. 핵심 요약 (상위 10)

| # | 심각도 | 화면 | 발견 | 근거 스킬 |
|---|---|---|---|---|
| 1 | **HIGH** | 주행 카드 | 카드가 화면 높이 전체(약 930dp)를 채우는데 정보는 상단 24%(약 180dp)에만 몰려 있고, 가운데 약 450dp는 빈 면, 버튼은 맨 아래. 텍스트는 카드 폭의 55%까지만 쓰고 좌상단에 붙음 → "좌측 쏠림·욱여짐" 피드백의 정체 | better-layout(그룹화·정렬), impeccable(density), design-critique(hierarchy) |
| 2 | **HIGH** | 정차 추천 | 후보 카드 한 장이 좌측 열 7줄(칩 2개 줄바꿈+문구 4줄)로 쌓이고 우측 70%가 비며, 두 번째 카드는 하단 바에 잘려 스크롤 필요. 버튼은 폭 140dp짜리 소형(주행은 전폭) → 일관성 없음. Figma의 `CompareRow` 표(3행 한 화면)와 불일치 | better-layout, better-interface, design-critique |
| 3 | **HIGH** | 주행 실패 안내 | 실패 문구가 **빨간 글자만**(아이콘 없음) → 색만으로 상태 구분(DESIGN 6절·escalation trigger). 표시 시 카드 높이가 818→770dp로 줄어 레이아웃이 튄다 (`d-failed-night.png`, `DriveScreen.kt` `PickState.Failed`) | better-accessibility(1.4.1), better-colors |
| 4 | **MEDIUM** | 하단 음성 바 (전 화면) | 모든 화면 하단에 `bg/surface` 큰 카드+전폭 accent "다음 후보" 버튼 상시 노출. 주행 중에는 카드 2장 + 이 카드로 **큰 카드가 3개처럼 보이고**(ADR-0003), 화면마다 주 버튼 2개가 경쟁(better-colors "화면당 주 버튼 1개"). 정차 활동·폴백 화면에도 주행 중 실패 문구("경유지 추가 실패 · 정차 후 확인")가 그대로 남음(`parked-activity-night.png`) | better-colors, better-layout, better-writing |
| 5 | **MEDIUM** | 주행 카드 | Figma의 "다음 충전 정차" 헤더(아이콘+제목)·버튼 `add_location_alt` 아이콘 없음. 카드 문구에 "가상 데이터" 같은 출처가 핵심 정보와 같은 줄(`+2분 · 0.3km · 가상 데이터`) | better-layout, impeccable |
| 6 | **MEDIUM** | 주행 빈 상태 | 후보 없음 = 좌상단 보조색 한 줄 + 나머지 90% 공백(`d-drive-night.png`). `EmptyState`(DESIGN 7절) 미사용 | better-layout, better-writing |
| 7 | **MEDIUM** | 주행 대기 중 | 탭 후 5~9초 재탐색 동안 두 버튼이 회색으로 비활성화될 뿐 진행 표시·문구 변화 없음(`d-pending-night.png`) → 시스템이 멈춘 것처럼 보임 | emil-design-eng(피드백), apple-design(response), better-accessibility |
| 8 | **MEDIUM** | 정차 입력·충전·폴백·동의 | 모든 정차 화면이 좌상단 정렬 + 하단 40~60% 공백. 입력 필드가 1240dp 폭으로 늘어나고 충전 화면 안내문은 한 줄 80자 이상(측정 60~75자 권장 초과) | better-layout, better-typography(measure) |
| 9 | **MEDIUM** | UndoBar | `bg/surface` + accent "취소 (N초)" 버튼. Figma는 `bg/raised` + `undo` 아이콘 + "10초 안에 취소" 문구. 카운트다운 숫자가 라벨에 섞여 폭이 매초 변함 | better-ui(tabular), better-layout |
| 10 | **LOW** | 모션 전반 | 입장·퇴장·누름 피드백이 전혀 없음. UndoBar·상태 메시지는 순간 등장. 차량 화면은 모션 절제가 맞지만 UndoBar 입장 200ms ease-out·누름 `scale(0.97)`·reduce-motion 폴백은 정의 필요 | emil-design-eng, better-ui, make-interfaces-feel-better |

## 2. 화면별 상세

심각도: HIGH=과제 방해·오해·반복 구조 결함, MEDIUM=이해·효율·일관성 저하, LOW=국소 다듬기.

### 2.1 주행 카드 (`feature/drive/DriveScreen.kt`)

캡처: `shots/audit/d-cards-night.png`(기존 `shots/driving-cards-*`와 동일 구조) / Figma `figma/driving-night.png`.

| # | 심각도 | 위치 | 현재 | 문제 | 제안 |
|---|---|---|---|---|---|
| D1 | HIGH | `DriveScreen.kt` Row `weight(1f)` + `CandidateCard` `fillMaxSize()` | 카드 높이 ≈ 930dp. 제목·문구 2줄·칩이 y=0~180에, 버튼이 y=830~900 | 정보 밀도 24%, 빈 면 50%. 관련 정보(이름·거리·상태)와 행동(버튼)이 450dp 떨어져 그룹이 깨짐(근접성). 눈이 위→아래로 450dp 이동해야 탭 | 카드 높이를 콘텐츠 기준(≈ 380~420dp)으로 고정하고 화면 세로 중앙 배치, 또는 카드 내부를 `이름/지표/버튼` 3단으로 균등 배치 |
| D2 | HIGH | 같은 곳 | 텍스트 좌측 정렬, 우측 45% 비어 있음 | 시선이 좌상단에 쏠림 → 사용자 피드백 | 핵심 지표 2개(`+2분`, `0.3km`)를 카드 폭을 쓰는 가로 2칸 큰 숫자(drive/title 40sp)로 승격하고 이름은 위, 칩은 우측 정렬 |
| D3 | MEDIUM | 같은 곳 | 헤더 없음 | Figma "다음 충전 정차"(ev_station 아이콘+제목). 화면이 무엇인지 알려주는 단서 없음 | 헤더 한 줄(아이콘 44 + drive/title) 추가, 카드와 간격 24dp |
| D4 | MEDIUM | `AppButton("경유지로 추가", null, …)` | 아이콘 없음, 글자 중앙 | Figma `add_location_alt` 아이콘+문구. 아이콘은 문구를 읽기 전에 행동을 알려 줌 | 아이콘 36dp + 문구 |
| D5 | MEDIUM | `AppRoot.driveCard()` | 요약 `"$reason\n+N분 · N.Nkm · 가상 데이터"` | 출처(시연 한정 표기)가 핵심 지표와 같은 위계. 칩 "필요 조건 충족"은 무엇을 충족인지 모호 | 출처는 작은 보조 문구로 분리(카드 하단 또는 화면 헤더 우측), 칩은 충족/미충족 항목 이름 |
| D6 | HIGH | `PickState.Failed` → `Text(color = statusDanger)` | 빨간 글자 | 아이콘 없음(색만) + 높이 변화 | `StatusChip(Danger, …)` + 메시지 슬롯 높이 예약 |
| D7 | MEDIUM | `locked` 비활성화만 | 5~9초 회색 버튼 | 진행 표시 없음 | 선택된 카드 버튼 문구를 "추가하는 중…"(+진행 아이콘), 나머지 카드는 비활성 |
| D8 | MEDIUM | `UndoBar` | 위 요약 #9 | Figma와 구조 다름 | Figma대로 `bg/raised`, `undo` 아이콘 버튼 ≥76dp, 남은 시간은 문구 "10초 안에 취소할 수 있어요"는 길어 `취소` + 작은 남은 초(tabular-nums) |
| D9 | LOW | 하단 `VoiceBar` | 3번째 카드처럼 보임 | #4 | 주행 중 `VoiceBar`는 한 줄 상태+보조 버튼으로 축소, 카드 2장이 시각 1순위 |

일치: 면 단계·글자 40/32sp·버튼 ≥76dp·카드 간격 24dp·칩 아이콘+문구(#49).

### 2.2 정차 추천 (`feature/recommend/RecommendScreen.kt`)

캡처 `shots/audit/p-recommend-night.png`, `shots/parked-recommend-*.png` / Figma `figma/parked-night.png`.

| # | 심각도 | 현재 | 문제 | 제안 |
|---|---|---|---|---|
| R1 | HIGH | 후보 = 카드 리스트(제목, 충족 칩 2개를 세로로, 문구 4줄, 작은 버튼) | `CompareList/CompareRow` 미구현(DESIGN 7절). 한 후보가 330dp를 차지해 2개 비교 불가, 하단 카드는 잘림 | Figma `CompareRow`: 한 행 = 이름 · 거리 · 시간 · 충족 칩, 행 3개가 한 화면, 하단에 선택된 행 기준 "경유지로 추가" 1개 |
| R2 | MEDIUM | 버튼 폭 140dp, 좌측 | 주행(전폭)과 다름 | 행 선택 후 하단 전폭 버튼 1개 |
| R3 | MEDIUM | 칩 줄바꿈 세로 쌓임 | 가로 여유가 크므로 `FlowRow` 한 줄 | 행 오른쪽 칩 열 |
| R4 | LOW | "출처: 가상 데이터(시연)" 문구가 카드마다 | 반복 | 화면 상단 한 번 |

### 2.3 정차 입력·충전 활동·동의·폴백

캡처 `shots/audit/p-input-night.png`, `p-input-validation-night.png`, `shots/parked-activity-*`, `parked-consent-*`, `fallback-permission-*`.

| # | 심각도 | 발견 | 제안 |
|---|---|---|---|
| I1 | MEDIUM | 입력: 필드 폭 1240dp 전폭, 하단 280dp 공백, 정렬 좌상단. 시간 필드 `예: 13:30` 힌트가 라벨로만 존재 | 2열(목적지/시간 | 필요·충전 칩) 배치로 폭 활용, 버튼은 폭 제한(≤560dp) 우측 하단 |
| I2 | MEDIUM | 입력 검증 오류 "도착 시간을 00:00 형식으로…" 가 필드 아래가 아니라 최하단(`p-input-validation-night.png`). 아이콘 없음 | 오류는 필드 바로 아래 + StatusChip Danger |
| I3 | MEDIUM | 충전 활동: 안내문 한 줄 85자 이상, 문구 "(자동 확인 아님)" 반복 | 줄 길이 ≤ 60~75자 폭 제한, 문구 한 문장으로 |
| I4 | LOW | 폴백: 좋은 사례. 단 본문 3문장이 별도 단락으로 같은 위계 | 첫 문장만 본문, 나머지 보조 |
| I5 | LOW | 동의 화면: #49 일치 판정. Figma 없음 | 후속에 Figma 화면 추가 |
| I6 | LOW | 앱 셸 탭 `입력/추천/(충전 중 활동)/측정 동의`: "측정 동의"가 단계가 아닌 설정인데 같은 위계 | 우측 정렬 보조 버튼으로 분리 |

### 2.4 접근성·AAOS 제약 체크

| 항목 | 결과 | 근거 |
|---|---|---|
| 터치 ≥76dp | 통과 (카드·버튼·탭 ≥76dp 실측 근사) | 캡처 높이 |
| 간격 ≥24dp | 통과 (탭 사이 24, 카드 사이 24) | 캡처 |
| 상태 = 텍스트+아이콘 | **실패 1건**: 주행 실패 문구(D6). 입력 오류(I2)도 아이콘 없음 | 코드·캡처 |
| 주행 중 큰 카드 2개·스크롤/비교 없음 | 카드 2개 준수. VoiceBar가 3번째 큰 카드처럼 보임(#4) | `d-cards-night.png` |
| 대비 | 토큰 상 통과(ContrastTest). 비활성 버튼 글자 보조색/raised 7.3:1 | DESIGN 2절 |
| 글자 크기 | 24/32/40sp. 실기 4mm 기준은 **미검증**(DPI 확인 필요, DESIGN 10절) | — |
| 키보드 | 입력 시 소프트 키보드가 `정차 추천 받기` 버튼을 가림(`adb input text` 중 확인) → 스크롤/`imePadding` 확인 필요 | 캡처 중 관찰 |
| Day 모드 | 이번 라운드 재캡처 안 함. #49 기존 Day 캡처로 대체 | `shots/*-day.png` |

## 3. 잔여 불일치 (#49) 정리

| 항목 | 상태 | 계획 |
|---|---|---|
| `CompareList`/`CompareRow` 미구현 | 확인됨(R1) | Figma `Components`에 추가 → 코드 |
| 주행 카드 `add_location_alt` 아이콘 | 확인됨(D4) | 코드는 `add_location_alt` 벡터 추가 필요 |
| "다음 충전 정차" 헤더 | 확인됨(D3) | Figma 유지·코드 추가, 문구는 ADR/DESIGN에 맞게 확정(충전이 아닌 일반 정차 후보면 "다음 정차"로 변경 검토 → **사용자 판단**) |
| 카드 높이 | 확인됨(D1). Figma 334 고정 vs 구현 930 | Figma를 실제 2560×1440 영역 기준으로 재작도 |
| UndoBar·핸드오프 | 아래 4절 절차로도 **재현 실패**, 여전히 미검증 | 4절 |

## 4. UndoBar·핸드오프 캡처 절차 (내비 전면 전환 회피) — 시도 결과

내비 전면 전환은 막을 수 없다(`addWaypoint` 성공 시 Pleos 내비가 올라옴, `d-navi-foreground-night.png`). 대신:

1. 정차(`scripts/vconsole.sh gear P`)에서 입력(목적지, 미래 시각, 식사/아이 휴식 선택) → 제출 → **제출 뒤** `adb emu geo fix 127.05 37.30` 두 번(문서 e2e README 재확인).
2. `scripts/vconsole.sh gear D` + `speed 30` → 카드 2장 확인(`d-cards-night.png`).
3. 카드 탭 → 즉시 `adb shell am start -n ai.familystop.app/ai.familystop.MainActivity`로 앱을 다시 올리고 1~2초 간격으로 `screencap`.
4. 결과: 이번 세션에서는 내비에 이전 경로의 경유지 2개가 남아 있어 "경유지는 최대 2개까지" 토스트와 함께 **Failed**로 수렴(`d-failed-night.png`). `pm clear ai.pleos.playground.service.navi`로 내비 데이터를 지운 뒤에도 경로(목적지) 없이는 추가가 실패. **UndoBar는 여전히 미캡처**.
5. 남은 안: (a) 내비에서 목적지를 설정한 상태(경유지 0개)에서 3번 반복 (b) 코드 변경 없이 불가하면 `NaviGateway` fake로 UndoBar를 띄우는 Compose 프리뷰/스크린샷 테스트를 코드 PR에 포함(권장). 이번 단계는 코드 변경 금지라 보류.

## 5. Figma 선행 변경 목록 (2단계에서 반영)

1. `Screen / Driving / *`: 실제 앱 영역 비율(1680×~950, 클러스터 제외)로 카드 높이·간격 재설계(D1·D2), 헤더·아이콘 버튼(D3·D4), 실패/진행 상태 변형(D6·D7), UndoBar 교정(D8).
2. `Screen / Parked / *`: `CompareRow` 컴포넌트와 3행 표 + 하단 단일 버튼(R1·R2).
3. 새 프레임: 주행 빈 상태(EmptyState), 입력 오류 인라인 표시는 코드 PR에서 토큰대로 처리(I2, Figma 변경 범위 밖이면 사용자 판단).

## 5-1. Figma 반영 결과 (2단계, 코드 변경 없음)

파일 [So1tPxF3kWdBwk8WXf4Mfr](https://www.figma.com/design/So1tPxF3kWdBwk8WXf4Mfr) 페이지 `Foundations`, 기존 프레임은 건드리지 않고 **v2를 오른쪽에 추가**했다(1280×720 임시 크기 → 실제 앱 영역 1680×950dp). 모든 면·글자색·모서리·간격은 기존 `Color`/`Layout` 변수에 바인딩, Day는 변수 모드 전환.

| 프레임 | 노드 | 해결한 항목 |
|---|---|---|
| Driving / Night — UndoBar | [19:228](https://www.figma.com/design/So1tPxF3kWdBwk8WXf4Mfr?node-id=19-228) | D1·D2·D3·D4·D8: 카드 높이 콘텐츠 기준(약 440), 이름·칩 한 줄, 지표 2칸(+시간·거리), 아이콘 버튼, 헤더, 그룹 세로 중앙 |
| Driving / Day — UndoBar | [19:294](https://www.figma.com/design/So1tPxF3kWdBwk8WXf4Mfr?node-id=19-294) | 같은 구조 Day 모드 |
| Driving / Night — 실패 | [19:360](https://www.figma.com/design/So1tPxF3kWdBwk8WXf4Mfr?node-id=19-360) | D6: 실패 = 칩(`cancel` 아이콘)+문구, 상태 슬롯 높이 108 고정(레이아웃 점프 방지) |
| Driving / Night — 추가 중 | [19:426](https://www.figma.com/design/So1tPxF3kWdBwk8WXf4Mfr?node-id=19-426) | D7: 선택 카드 "추가하는 중…", 다른 카드 비활성(raised) |
| Driving / Night — 후보 없음 | [19:485](https://www.figma.com/design/So1tPxF3kWdBwk8WXf4Mfr?node-id=19-485) | 주행 빈 상태: 아이콘+제목+안내 문구 |
| Parked / Night — CompareList | [20:402](https://www.figma.com/design/So1tPxF3kWdBwk8WXf4Mfr?node-id=20-402) | R1~R3: 행 3개 한 화면, 열 정렬(이름·시간·거리·칩), 선택 행=accent 면+"선택됨" 텍스트, 하단 액션 바(선택 이름 + 버튼 560) |
| Parked / Day — CompareList | [20:463](https://www.figma.com/design/So1tPxF3kWdBwk8WXf4Mfr?node-id=20-463) | 같은 구조 Day 모드 |
| Components v2 | [19:170](https://www.figma.com/design/So1tPxF3kWdBwk8WXf4Mfr?node-id=19-170) | 신규 컴포넌트: `DriveCard/v2`, `CompareRow/기본·선택`, `StatusBar/Failed`, `Tab/기본·선택`, `VoiceHint` (기존 `Button/Primary`·`UndoBar`·`StatusChip`·`Icon/*` 인스턴스 재사용) |

캡처: `figma/v2/driving-night-pending.png`, `figma/v2/parked-night.png`. 하단 큰 `VoiceBar`는 **한 줄 `VoiceHint`**로 교체(선택지 2 A안 가정, 사용자 판단 대기). 헤더는 기존 문구 "다음 충전 정차" 유지(선택지 3 대기). UndoBar는 기존 `UndoBar` 컴포넌트가 이미 Figma 사양(raised+undo)이라 재사용, 코드를 이에 맞춘다. 입력·충전 활동·폴백·동의(I1~I6)는 이번에 Figma 화면을 추가하지 않았다(범위 밖, 후속).

**미검증**: Figma v2 프레임의 Day 캡처 시각 확인은 샘플 1건(Night)만 했다. 실제 4mm/글자 크기는 실기 DPI 확인 필요.

## 6. 사용자 판단이 필요한 선택지

1. 주행 카드 구성: **A** 카드 높이 고정(약 400dp) 세로 중앙 + 가로 지표 2칸(권장) / **B** 현재처럼 화면 채움 유지하되 내부 균등 분산.
2. 하단 `VoiceBar`: **A** 주행 중 한 줄 상태로 축소, 정차에선 숨김(권장) / **B** 현재 유지.
3. 헤더 문구: "다음 충전 정차" 유지(충전 중심) vs "다음 정차"(식사·휴식 포함).
4. 정차 추천 `CompareRow`의 열: 이름·거리·+시간·충족 칩 4열(권장) vs Figma 기존(충전기·시설 포함, 충전 시나리오 전용).
