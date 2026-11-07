# DESIGN.md — 가족 여행 통합 정차 도우미

Pleos Connect(AAOS) 인포테인먼트 앱, Jetpack Compose. 사용자: 전기차 가족 여행 운전자·성인 동승자.
UI 이슈 #6~#12 착수용 최소 기준이다. 범위를 넓히지 않는다.

- Figma: https://www.figma.com/design/So1tPxF3kWdBwk8WXf4Mfr (페이지 `Foundations`: `Foundations / Night`·`Day`: 토큰·타입·StatusChip·카드, `Icons`, `Components`, `Screen / Driving·Parked / Night·Day`)
- 근거 ADR: 0003(주행 중 한 번 탭+10초 취소), 0004(미확인=주행 중). 근거 문서: `docs/research/aaos-dev-notes.md`

## 1. 확정 vs 확인 필요

| 구분 | 항목 |
|---|---|
| 확정(결정 문서) | 주행 중 큰 카드 최대 2개, 비교·펼치기·스크롤 금지, 정차 중에만 상세 비교, 이동 상태 미확인=주행 중, 색만으로 상태 구분 금지, 미확인 값은 '미확인' 표식 |
| 확정(공식 문서, Google Design for Driving) | 터치 대상 ≥76×76dp, 주요 글자 32dp·보조 24dp 이상, 항목당 120자·주행 중 1~3줄, 글자·아이콘 대비 ≥4.5:1, 버튼↔배경 ≥3:1, 터치 대상 간격 ≥24dp 권장, 야간은 어두운 배경+밝은 글자 필수, 아이콘 44/36/24dp·최소 36dp |
| 확정(Pleos 가이드) | 대비 ≥4.5:1, 핵심 정보 글자 높이 ≥4mm·일반 ≥3mm, 낮/밤 모드에서 UI 밝기·대비 자동 조정, 시스템 폰트 Asta Sans(한/영) |
| 확인 필요 | Pleos 자체 터치·글자 dp 값(페이지에 없음, 도형 이미지에만 있을 수 있음), 에뮬레이터 실제 화면 크기·DPI, 낮/밤 전환의 실제 트리거(5절), 라이트 테마 면 단계 공식 값 |

## 2. 색 토큰 (Figma 컬렉션 `Color`, 모드 Night / Day)

**border-less**: 외곽선 토큰을 두지 않는다. 카드·칩 구분은 면 단계(`bg/base` < `bg/surface` < `bg/raised`)와 간격으로만 한다. 근거: AAOS 색 가이드는 어두운 배경에서 그림자가 안 보이므로 회색 단계로 깊이를 표현하고, 불필요한 외곽선·색 구분을 피하라고 한다(https://docs.partner.android.com/drivingux/automotive-os/design-system/color). 눈부심 환경에서는 면 차이가 먼저 사라지므로 라이트 면 단계를 넉넉히 벌렸다.

Night/Day 둘 다 지원. Compose: `AppColors.bgBase`식 camelCase.

| 토큰 | Night | Day | 용도 |
|---|---|---|---|
| bg/base | #080A0C | #DDE3E8 | 화면 배경 |
| bg/surface | #1B2126 | #FFFFFF | 카드 |
| bg/raised | #272E34 | #EEF1F4 | 칩·보조 면 |
| text/primary | #F2F5F7 | #11181D | 제목·핵심 |
| text/secondary | #B4BEC6 | #46535C | 부가 정보 |
| accent/primary | #5CB8FF | #0A62BA | 주 버튼 |
| accent/on | #06121C | #FFFFFF | 주 버튼 글자 |
| status/ok | #5BD38A | #13673A | 사용 가능 |
| status/warn | #FFC247 | #7A4F00 | 혼잡·주의 |
| status/danger | #FF6B6B | #B71C1C | 이용 불가·실패 |
| status/unknown | #9AA7B0 | #4F5E68 | 미확인 |
| focus/ring | #FFFFFF | #11181D | 포커스 표시(로터리·키 입력용, 외곽선 아님) |

대비 계산(WCAG): 
- Night: 본문/배경 17.8, 보조/칩면 7.3, 상태색/카드 6.2~10.7, 상태색/칩면 최저 5.0(danger), 주 버튼 글자 8.8, 버튼/카드 8.0.
- Day: 본문/카드 17.9, 보조/칩면 7.0, 상태색/카드 ≥6.6, 상태색/배경 최저 5.1, 주 버튼 글자·버튼/카드 6.05.
- 면 단계 명도비는 카드/배경 Night 1.22·Day 1.29, 칩/카드 Night 1.18·Day 1.13. 규정 대비가 아니라 구분용이며, 이것만으로 정보를 전달하지 않는다(문구·아이콘 병행). 에뮬레이터·실기에서 눈부심 확인 전까지 칩/카드 단계가 약하면 `bg/raised`를 더 벌린다.

## 3. 타이포 (텍스트 스타일 4개)

Figma는 대체 폰트 Noto Sans KR 사용. 구현은 Compose 기본 폰트(Pleos 시스템 폰트 Asta Sans)를 쓴다.

| 스타일 | 굵기 | 크기/행간 | 쓰는 곳 |
|---|---|---|---|
| drive/title | Bold | 40/52 | 주행 중 카드 제목 |
| drive/body | Medium | 32/44 | 주행 중 핵심 정보·버튼 |
| stop/title | Bold | 32/44 | 정차 중 제목 |
| stop/body | Regular | 24/34 | 정차 중 본문·칩·보조 |

공식 최소 24dp(보조)·32dp(주요) 미만 스타일은 두지 않는다. 본문 글자를 줄이려면 문구를 줄인다. 기기 글자 크기 설정은 `sp`로 따른다. 항목당 120자·주행 중 1~3줄.

## 4. 간격·모서리·터치 (컬렉션 `Layout`)

- space: xs4 / sm8 / md16 / lg24 / xl32 (dp). **터치 대상 사이 간격 ≥ lg(24)**. 화면 가장자리 안전 여백 ≥24 (공식 SHOULD).
- radius: sm8 / md16 / lg24
- touch/min: **76dp** (확정). 주행 중 탭 대상은 카드 전체 또는 높이 ≥76dp 버튼. 폭을 줄일 때는 면적 유지하고 높이를 늘린다(차량 진동 대응).
- 아이콘: 주 44 / 보조 36 / 3차 24dp, 지도 마커 최소 36dp.

## 5. 낮/밤(Light/Dark) 전환

- 앱은 테마를 스스로 정하지 않고 **시스템 낮/밤 상태를 따른다**. Compose에서는 `isSystemInDarkTheme()`(`Configuration.uiMode`)로 읽고 Night 토큰/Day 토큰을 매핑한다. 앱 설정에 별도 테마 토글을 두지 않는다.
- **터널 등 갑자기 어두운 곳 진입도 반영**한다(사용자 요구, 네이버 지도 동작 참고 — 사용자 설명이며 직접 확인하지 않음). 앱이 하는 일: 시스템 테마 변경 이벤트가 오면 즉시 재구성하고, 전환 중 깜빡임·긴 애니메이션을 두지 않는다. Activity 재생성으로 화면 상태(주행 중 선택·10초 UndoBar 타이머)를 잃지 않도록 `configChanges="uiMode"` 처리 또는 상태 보존을 한다.
- **미확인**: AAOS/Pleos가 어떤 신호(조도 센서·전조등·시간)로, 터널처럼 짧은 어두운 구간에도 시스템 테마를 바꾸는지 공식 문서에서 확인하지 못했다(Pleos 문서는 "조명 조건에 따라 자동 조정"까지만). 에뮬레이터 `adb shell cmd uimode night yes/no`로 토글해 반응하는지 먼저 확인하고, 시스템이 터널에서 전환하지 않으면 별도 조도 입력 사용 여부를 결정한다(이슈 필요).
- 규칙: 밤에는 반드시 어두운 배경+밝은 글자(공식 MUST). 낮은 라이트·다크 모두 허용. 지도 표시가 생기면 시스템이 지시하는 라이트/다크 지도 사용.
- 토큰 두 모드는 Figma `Color` 컬렉션에 모두 있다. 프레임 `Foundations / Night`, `Foundations / Day`로 확인.

## 6. 상태 표현

- 상태는 **아이콘 + 문구 + 색** 3중. 색만으로 구분 금지. 외곽선으로 구분하지 않는다(border-less).
- `StatusChip`(Figma 컴포넌트 4종, `bg/raised` 면 + 상태색 아이콘 24dp + 본문색 문구): 사용 가능 `check_circle` / 혼잡 `warning` / 이용 불가 `cancel` / 미확인 `help`. 아이콘만 단독으로 쓰지 않고 항상 문구와 함께 쓴다.

### 아이콘 세트 (Material Symbols)

- 세트: **Material Symbols Rounded, Fill 1, weight 400** (Apache-2.0, `google/material-design-icons`). 근거: 코드(Compose `material-icons`)와 같은 글리프라 매핑이 필요 없고, AAOS Car UI 가이드의 vector drawable 요건과 맞으며, Rounded 모양이 카드 모서리(radius 16~24)·소프트한 글자와 어울린다. Fill 1로 먼 거리·눈부심에서 식별성을 높인다(화살표·닫기류는 Fill 영향 없음).
- 크기: 주 44 / 보조 36 / 3차 24dp. 최소 36dp(지도 마커). 칩 안은 24, 단독 터치 아이콘은 터치 대상 76dp 안에 44로 중앙 배치.
- 색: 토큰에 바인딩(기본 `text/primary`, 상태는 `status/*`, 버튼 위는 `accent/on`). 하드코딩 금지, Night/Day 자동 대응.
- 이름 매핑(Figma `Icon/<이름>` = Compose `Icons.Rounded.<PascalCase>`): `check_circle`, `warning`, `cancel`, `help`, `add_location_alt`(경유지 추가), `undo`(취소), `ev_station`(충전), `mic`·`volume_up`(음성), `close`, `arrow_back`. 구현 시 vector drawable로 가져오며, `material-icons-extended` 의존성 크기가 부담이면 필요한 아이콘만 SVG→vector drawable로 추가한다.
- 새 아이콘은 같은 세트·같은 설정에서만 추가한다. 다른 세트가 필요하면 선 굵기·모서리 스타일을 맞추고 이 문서에 기록한다. Figma Community 아이콘 파일은 쓰지 않았다.
- Figma: 페이지 `Foundations`의 `Icons (Material Symbols Rounded…)` 프레임에 컴포넌트 11개(44 기준, 스케일 제약 설정).
- 뜻이 다른 배지는 다른 색(k-style-ui-ux 체크리스트 항목).
- 데이터 미수신·권한 거부·값 없음은 빈칸·0 대신 **'미확인'** 칩 + 이유 한 줄(#11).
- 오류·실패 문구는 원인+다음 행동 한 문장. 음성 안내 문구는 화면 문구와 같은 어휘.

## 7. 컴포넌트 (이슈 매핑)

| 컴포넌트 | 설명 | 이슈 |
|---|---|---|
| DriveCard | 큰 카드(`bg/surface`, radius lg): 장소명(drive/title) + 거리·핵심 한 줄(drive/body) + StatusChip + 주 버튼 "경유지로 추가". 2열 | #6 |
| UndoBar | 선택 직후 10초 카운트 "취소". 높이 ≥76dp | #6 |
| StatusChip | 6절 | #6~#12 |
| ConditionForm | 가족 여행 조건·정차 필요 입력, 에너지 수동 입력. 정차 중에만 | #7 |
| CompareList / CompareRow | 후보 비교. 정차 중에만 | #8, #10 |
| WaypointReplaceDialog | 경유지 2개 초과 시 교체 대상 선택 | #9 |
| EmptyState | 권한 거부·미수신 대체 | #11 |
| VoiceHint | 음성 안내 상태 | #12 |

Figma(페이지 `Foundations`)에 있는 컴포넌트: StatusChip 4종, Icon 11종, Button/Primary(아이콘+문구, 최소 높이 76), UndoBar(문구+취소 버튼, 버튼 ≥76), DriveCard(주행 중 큰 카드). 대표 화면(1280×720은 **임시 크기**, 에뮬레이터 확인 후 확정 #3): `Screen / Driving / Night·Day`(카드 2장+UndoBar), `Screen / Parked / Night·Day`(후보 3개 비교+버튼). 나머지 컴포넌트(ConditionForm, WaypointReplaceDialog, EmptyState, VoiceHint 등)는 해당 구현 이슈에서 같은 토큰으로 조립한다.

## 8. 화면 규칙

### 주행 중 (또는 이동 상태 미확인, ADR-0003·0004)
- `distractionOptimized` Activity. 큰 카드 **최대 2개**, 한 화면 고정(스크롤·펼치기·탭 전환·비교 금지).
- 허용 상호작용: 카드 한 번 탭 → 즉시 경유지 추가, 이후 10초 UndoBar. 음성 보조.
- 텍스트: drive/title, drive/body만. 카드당 문구 1~3줄. 입력 필드·키보드·목록 없음.
- 이동 중 자동 재추천 없음. 새 추천은 사용자 요청 시에만.

### 정차 중
- 상세 비교(CompareList), 조건 입력 폼, 에너지 수동 입력, 후보 3개 이상 비교 가능. stop/title·stop/body 사용. 스크롤 허용. 터치 대상 ≥76dp.
- 정차→주행 전환 시 즉시 주행 화면으로 대체(입력 중 값은 보존하되 화면은 주행 규칙). 미확인이면 주행으로 취급.

## 9. 디자인 소스 기록

- **k-style-ui-ux** (https://github.com/vmc-inner-circle/k-style-ui-ux, 사용자 확인): PRD→한국 앱 스타일 모바일 화면 초안 플러그인. **설치·실행하지 않고** `checklist.md`만 읽기 전용 참조. 반영: 뜻이 다른 배지는 다른 색, 한 번뿐인 행동은 누른 뒤 모습(UndoBar), 대상마다 시작·끝 화면. 한국 앱 팔레트·모바일 레이아웃은 차량 화면과 맞지 않아 쓰지 않음.
- **omd(oh-my-design)**: 로컬에 설치돼 있으나 위 레포와 다른 도구. 이번 작업에서 사용하지 않음.
- **아이콘**: Figma Community 검색(Material 계열, 차량 특화 `Car & Equipment`·`Icons for driving` 등, Phosphor)으로 후보를 조사했으나 상세 페이지·라이선스는 열지 않았고, 코드 1:1 대응을 이유로 Google 공식 레포 Material Symbols를 선택(Apache-2.0 확인).
- **Mobbin MCP**: 연결됨. iOS 레퍼런스 약 28건 열람, 모바일이라 구조 참고만. border-less 카드 참고: Waymo 차량 제어 카드, Bevel 위젯, Perplexity 다크 카드, corner 장소 카드. 계정 생성·결제 없음.
- **Aside 웹 조사**(출처 URL은 위 본문에 표기): Google Design for Driving(색·터치·글자·대비), AAOS 시스템 경험 개요(낮/밤 테마), Pleos Connect UI Design Guide(intro·colors·typography·layout·basic-button 페이지: 대부분 수치가 이미지라 텍스트에는 없음). Polestar·Tesla·Volvo 카드 스타일과 CarPlay 수치는 확인 못 함.

## 10. 다음 확인 사항

1. 에뮬레이터에서 화면 크기·DPI, 낮/밤 전환 트리거(`cmd uimode night`와 터널형 조도 변화 반응) 확인(#3).
2. Pleos 가이드의 이미지 값(color 역할 hex, 타입 스케일, 버튼 크기)을 사람이 열어 확인해 3·4절과 대조.
3. Day 모드 실제 눈부심 조건에서 `bg/raised` 단계 시인성 확인.
