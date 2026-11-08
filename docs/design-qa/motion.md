# 모션 점검: transitions.dev 패턴 채택/보류/기각 (이슈 #59)

2026-11-09. 출처: https://transitions.dev 라이브러리 43개(Essential 24, AI Agents 5, Effects 11, Texts 4)를 Aside로 전수 열람, 로그인 없음. **Pro 12개는 설명만 보이고 코드·수치가 가려져 있어** 전부 기각(구현 근거 확보 불가). 수치는 각 패턴 CSS 탭 기준. 기본 easing `cubic-bezier(0.22, 1, 0.36, 1)` = **std**(ease-out 계열).

판정 기준: ADR-0003(주행 중 주의 분산 최소·카드 2개·스크롤/비교 금지), ADR-0004(미확인=주행 중), DESIGN.md §8, 키워드 150~250ms·일상 UI bounce 금지. 이 앱은 웹이 아니라 Compose이므로 CSS 값은 **참고 수치**이고 구현은 `animate*AsState`/`AnimatedContent`/`Crossfade`.

범례: **채택**(주행 중 허용) / **정차**(정차 중만 채택, 주행 중 정지) / **보류**(해당 화면이 생기면) / **기각**.

## 1. 패턴 표

### Essential
| 패턴 | 원값 | 판정 | 근거 / 적용 |
|---|---|---|---|
| Card resize | 300ms std | **정차** | CompareRow 선택 시 같은 오브젝트 확장(250ms로 단축). 주행 중 펼치기 금지(ADR-0003) |
| Notification badge | 260/500ms 스프링 팝 | 기각 | 배지 없음. 스프링 오버슈트 = bounce |
| Text states swap | 150ms, 4px, blur 2px | **채택**(blur 제외) | StatusChip 문구·상태 변경 크로스페이드 150ms. blur는 저사양 헤드유닛 비용 대비 이득 낮아 제외 |
| Menu dropdown | 250/150ms, scale .97 | 기각 | 드롭다운 UI 없음 |
| Modal open/close | 250/150ms, scale .96 | **정차** | WaypointReplaceDialog(정차·경유지 초과 시)에만. 퇴장 150ms. 주행 중 모달 없음 |
| Panel reveal | 400/350ms | 기각 | 패널 없음, 400ms는 상한 초과 |
| Page side-by-side | 250ms 슬라이드+페이드 | 보류 | 정차 탭 전환은 슬라이드 대신 크로스페이드(§2). 화면 이동이 생기면 재검토 |
| Icon swap | 250ms, scale .25→1, blur | **채택**(단순화) | 아이콘 상태 전환(check/warning/cancel/help)은 크로스페이드 150ms, scale·blur 없음 |
| Success check | 500ms 회전·bob·blur | **채택**(단순화) | 경유지 추가 성공 확인. 회전·bob 제거, 체크 아이콘 크로스페이드+1회 pulse(§2), 총 ≤250ms |
| Error state shake | 80/60ms 스텝 6px | 기각 | 주행 중 흔들림 = 주의 분산·멀미. 실패는 칩(아이콘+문구) 전환 + 음성/문구로만 |
| Skeleton loader and reveal | 1000ms 펄스, 400ms 리빌 | **정차** | 정차 추천 로딩. 펄스 1000ms는 느리므로 reduce 시 정지. 주행 중 카드는 로딩 칩(고정) |
| Tabs sliding | 250ms std | **정차** | 앱 셸 탭 인디케이터 이동. 주행 중 탭 전환 자체가 금지 |
| Shimmer text | 2000ms 무한 | 기각 | 무한 반복 모션은 주행 중 시선 유도. 로딩은 정적 칩+문구 |
| Tooltip open/close | 150/50ms | 기각 | 툴팁 없음(차량 터치) |
| Dropdown menu morph | 350ms, 오버슈트 | 기각 | 메뉴 없음, 오버슈트 |
| Accordion | 250ms | 기각 | 주행 중 펼치기 금지. 정차 펼침은 Card resize로 대체 |
| Toast open/close | 350/250ms, rise 16px, scale .97 | **채택**(변형) | **UndoBar** 입장 200ms / 퇴장 160ms(입장보다 20% 짧게), 아래서 12dp 이동+페이드, 트리거 카드에서 가까운 하단 |
| Learn more hover | hover | 기각 | 호버 없음(터치) |
| Checkbox check | 150/350ms 패스 드로 | 보류 | 칩 선택 체크(정차 입력). 체크 드로 대신 크로스페이드로 충분 |
| Toggle | 350ms 이중 바운스 | 기각 | bounce |
| Banner stacking | 350/250ms | 기각 | 배너 누적 없음, 주행 중 알림 스택 금지 |
| Organic shimmer·Pro gradient text·Image generation placeholder (Pro) | 비공개 | 기각 | 수치 미확인 + 장식 |

### AI Agents
| 패턴 | 판정 | 근거 |
|---|---|---|
| Thinking states (hold 2s, swap 150ms) | **정차** | 정차 추천 "찾는 중" 문구 교체 150ms. 주행 중 로딩 문구는 고정 |
| Reasoning stream | 기각 | 스크롤 흐름 = 스크롤 금지 |
| Streaming text | 기각 | 문구 점진 노출은 읽기 지연. 주행 중 즉시 표시 |
| Matrix dot loader | 기각 | 장식. 로딩은 진행 칩 |
| Spinner to check morph (Pro) | 기각 | 비공개. 대신 칩 크로스페이드 |

### Effects
| 패턴 | 판정 | 근거 |
|---|---|---|
| Avatar group hover | 기각 | 호버, 바운스 |
| Input clear with dissolve | 기각 | 입력 필드는 정차뿐, 효과 불필요 |
| 3D tilt | 기각 | 포인터 없음, 멀미 |
| Like button | 기각 | 파티클·오버슈트 |
| Confetti·Gooey plus·Card stack hover·Drag&drop physics·Image open tilt·Smoky dissolve·Get Pro (Pro) | 기각 | 비공개 + 장식·물리효과 |

### Texts
| 패턴 | 판정 | 근거 |
|---|---|---|
| Number pop-in (600ms, 3D 드럼) | 기각 | 600ms 초과·바운스 |
| Texts reveal (500ms, 40ms stagger) | 기각 | 500ms 초과. 리스트 stagger는 30ms 자체 규칙으로 |
| Spinning counter | 기각 | 3D 롤, 130ms×자리수 |
| Number subtle change (455ms 크로스페이드) | **정차** | 지표 숫자 변경. 200ms로 단축, 항상 `tabular-nums`(폭 흔들림 방지) |

**요약**: 채택 6(Text swap, Icon swap, Success check, Toast→UndoBar 변형 포함) · 정차 전용 7 · 보류 3 · 기각 나머지. 기각 사유는 대부분 ① 블러·오버슈트·바운스(키워드 위배) ② 호버·메뉴·툴팁 등 이 앱에 없는 UI ③ 무한 반복·스크롤·펼침(ADR-0003) ④ Pro 비공개.

## 2. 키워드 체크리스트 → 화면·상태 매핑

모드: **D**=주행 중(미확인 포함, ADR-0004) **P**=정차. 지속시간은 `DESIGN.md §11` 토큰.

| 키워드 | 적용 | D | P | 비고 |
|---|---|---|---|---|
| 입장 ease-out | UndoBar, 칩 상태, 다이얼로그 | ✔ | ✔ | `Enter` easing (decelerate) |
| 150~250ms | 모든 전환 | ✔ | ✔ | 기본 200ms, 상한 250ms |
| 퇴장은 입장보다 20% 짧게 | UndoBar 200→160ms, 다이얼로그 250→200ms | ✔ | ✔ | 토큰 `exit = enter×0.8` |
| 누름 scale 0.97 | `AppButton`, DriveCard 전체, 칩 버튼 | ✔ | ✔ | 100ms, 복귀 150ms. 터치 대상 크기는 불변(graphicsLayer라 레이아웃 영향 없음) |
| 성공 확인 pulse | 경유지 추가 성공 칩/카드 | ✔ | ✔ | 1회 1.0→1.02→1.0, 총 200ms, 오버슈트 없음 |
| 이전·이후 오버랩/크로스페이드 | 카드 상태(대기→처리 중→성공/실패), StatusChip, 핸드오프 4상태 | ✔ | ✔ | `AnimatedContent`+fade, 레이아웃 크기 고정(점프 금지) |
| 아이콘 크로스페이드+blur | 상태 아이콘 | ✔(blur 제외) | ✔(blur 제외) | blur 제외 사유 위 표 |
| 이동 ease-in-out | 탭 인디케이터, 카드 확장 | ✘ | ✔ | 250ms |
| 리스트 30ms stagger | 정차 추천 목록·CompareRow 입장 | ✘ | ✔ | 최대 5행, 총 ≤ 250+4×30. 주행 카드 2개는 동시 입장 |
| 같은 오브젝트 확장(shared element) | CompareRow 선택→상세 | ✘ | ✔ | `animateContentSize`/`AnimatedContent` 크기 변환 250ms |
| 트리거에서 입장/퇴장 | UndoBar는 탭한 카드 쪽 하단에서 | ✔ | ✔ | 이동 12dp |
| bounce 금지 | 전체 | ✔ | ✔ | 스프링은 `noBounce`(dampingRatio=NoBouncy)만 |
| 탭 숫자 `tabular-nums` | 거리·충전량·카운트다운 `fontFeatureSettings="tnum"` | ✔ | ✔ | 모션 아님, 흔들림 방지(UndoBar 10→0초) |
| 로딩 상태 | 주행: 정적 로딩 칩 / 정차: 스켈레톤 펄스 1000ms | ✔(정적) | ✔ | reduce 시 정적 |
| 유용한 피드백 | 누름·성공 pulse·칩 전환 | ✔ | ✔ | 장식 금지, 상태 변화 있을 때만 |
| 중첩 radius, 눈 정렬, CTA, 아이콘 일관성, 대비, 그룹 간격 | #50 점검에서 처리 | — | — | 본 이슈 범위 아님(모션 외) |

## 3. 주행 중 모션 원칙 (ADR-0003 준수 확인 기준)
1. 한 번의 사용자 행동에 모션은 **하나의 연속 전환(≤250ms)**, 반복·무한 모션 금지.
2. 레이아웃을 움직이지 않는다(위치 이동 없는 페이드·scale 0.97~1.02만; UndoBar만 12dp 이동). 카드 2개 한 화면 고정은 유지.
3. 시선을 끄는 효과(흔들림·깜빡임·진동 시각화) 금지. 실패는 칩 전환+음성/문구.
4. 시스템 애니메이션 비활성(`Settings.Global.ANIMATOR_DURATION_SCALE == 0`) 시 모든 전환은 즉시 교체(duration 0), 상태 정보는 동일하게 전달.
5. 미확인 이동 상태 = 주행 규칙(정차 전용 모션 쓰지 않음).

## 4. 구현·검증 결과 (2026-11-09)
구현: `Motion.kt`(`AppMotion` 토큰, `pressScale`, reduce-motion) · `StatusChip`(크로스페이드 150ms+성공 pulse 1회) · `AppButton`/`AppCard`(press 0.97, 버튼 배경 250ms) · `CompareRow`(선택 배경 250ms) · `DriveScreen` 상태 줄(UndoBar 입장 200ms+12dp / 퇴장 160ms) · `HandoffScreen`(4상태 크로스페이드 150ms) · 전역 `tnum`.
- 반영 안 함(보류): 정차 리스트 stagger·CompareRow 실제 크기 확장·탭 콘텐츠 슬라이드 — 탭 전환은 버튼 배경 전환만 적용.
- 검증: `ktlintCheck testDebugUnitTest :app:assembleDebug` 통과. 에뮬레이터 계측 27/27 통과(신규 `DriveMotionTest` 2: 모션 켬/끔 모두 카드 위치 불변, UndoBar 표시). `animator_duration_scale=0`에서도 27/27 통과, 이후 1.0 복구.
- 한계: reduce 경로의 "즉시 교체"는 snap spec 사용으로 코드상 보장하나, 계측으로는 위치 불변·표시만 확인(알파 단정 불가). `HandoffScreenshotTest.sendingNight`가 1회 Activity 종료 NPE로 실패 후 재실행 통과(기존 테스트 flake, 모션 무관로 판단).
