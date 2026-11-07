# DESIGN.md — 가족 여행 통합 정차 도우미

Pleos Connect(AAOS) 인포테인먼트 앱, Jetpack Compose. 사용자: 전기차 가족 여행 운전자·성인 동승자.
UI 이슈 #6~#12 착수용 최소 기준이다. 범위를 넓히지 않는다.

- Figma: https://www.figma.com/design/So1tPxF3kWdBwk8WXf4Mfr (페이지 `Foundations`: 토큰·타입·StatusChip·주행 카드 2장 시안)
- 근거 ADR: 0003(주행 중 한 번 탭+10초 취소), 0004(미확인=주행 중). 근거 문서: `docs/research/aaos-dev-notes.md`

## 1. 확정 vs 임시

| 구분 | 항목 |
|---|---|
| 확정(결정 문서 있음) | 주행 중 큰 카드 최대 2개, 비교·펼치기·스크롤 금지, 정차 중에만 상세 비교, 이동 상태 미확인=주행 중, 색만으로 상태 구분 금지, 미확인 값은 '미확인' 표식 |
| **임시(확인 전 확정 금지)** | 글자 크기, 터치 영역 76, 카드 크기, 대비 기준 외 수치 전부. Pleos/AAOS 공식 UX·접근성 지침 확인 후 갱신. 확인 전에는 이 문서의 수치를 "시연용 기본값"으로만 쓴다 |
| 미확인 | 비템플릿 Activity의 다크/라이트 요구 (research상 템플릿 앱만 확인). Day 모드는 선택적 |

## 2. 색 토큰 (Figma 컬렉션 `Color`, 모드 Night 기본 / Day)

기본은 Night. Day는 시연 필요 시에만 쓴다. Compose 이름은 `AppColors.<camelCase>`로 매핑(`bg/base` → `bgBase`).

| 토큰 | Night | Day | 용도 |
|---|---|---|---|
| bg/base | #0F1316 | #F4F6F8 | 화면 배경 |
| bg/surface | #181D21 | #FFFFFF | 카드 |
| bg/raised | #222930 | #E8ECEF | 칩·보조 면 |
| text/primary | #F2F5F7 | #11181D | 본문·제목 |
| text/secondary | #B4BEC6 | #4A5760 | 부가 정보 |
| accent/primary | #5CB8FF | #0A62BA | 주 버튼, 선택 |
| accent/on | #06121C | #FFFFFF | 주 버튼 위 글자 |
| status/ok | #5BD38A | #17703D | 사용 가능 |
| status/warn | #FFC247 | #8A5A00 | 혼잡·주의 |
| status/danger | #FF6B6B | #C62828 | 이용 불가·실패 |
| status/unknown | #9AA7B0 | #5C6B75 | 미확인 |
| border/default | #3A444C | #C3CBD1 | 카드 외곽선 |
| focus/ring | #FFFFFF | #11181D | 포커스(로터리/키 입력용) |

대비(WCAG 계산, Night): 본문/카드 15.5:1, 부가/칩면 7.8:1, 상태색/카드 6.1~10.6:1, 주 버튼 글자 8.8:1. Day는 상태색을 `bg/surface`·`bg/base` 위에서만 쓴다(`bg/raised` 위 일부 4.5 미만이라 금지). Day의 ok/accent 최종값은 조정 후 재계산하지 않았으므로 Day 사용 전 재검증.

## 3. 타이포 (Noto Sans KR, 텍스트 스타일 5개 — 임시)

| 스타일 | 굵기 | 크기/행간 | 쓰는 곳 |
|---|---|---|---|
| drive/title | Bold | 40/52 | 주행 중 카드 제목 |
| drive/body | Medium | 28/40 | 주행 중 카드 부가·버튼 |
| stop/title | Bold | 28/38 | 정차 중 제목 |
| stop/body | Regular | 22/32 | 정차 중 본문·칩 |
| caption | Regular | 18/26 | 보조 설명 (주행 중 사용 금지) |

Compose에서는 `sp`로 옮기되 기기 글자 크기 설정을 따른다. 주행 중 화면에는 `caption`을 쓰지 않는다.

## 4. 간격·모서리·터치 (컬렉션 `Layout`)

- space: xs4 / sm8 / md16 / lg24 / xl32 (dp)
- radius: sm8 / md16 / lg24
- touch/min: 76dp (임시, 공식 수치 확인 전). 주행 중 탭 대상은 카드 전체 또는 버튼 높이 ≥ touch/min.

## 5. 상태 표현 (규칙)

- 상태는 **아이콘 + 문구 + 색** 3중. 색만으로 구분 금지.
- 칩 `StatusChip`(Figma 컴포넌트 4종): 사용 가능 ✓ / 혼잡 ! / 이용 불가 ✕ / 미확인 ?. 아이콘 글리프는 Figma에서 임시 문자. 구현은 Material Icons(Compose 기본 의존)로 대체하고 문구는 그대로 유지.
- 데이터 미수신·권한 거부·값 없음은 빈칸이나 0 대신 **'미확인'** 칩 + 이유 한 줄(#11).
- 오류·실패 문구는 원인과 다음 행동 한 문장. 음성 안내 문구는 화면 문구와 동일 어휘.

## 6. 컴포넌트 (이슈 매핑)

| 컴포넌트 | 설명 | 이슈 |
|---|---|---|
| DriveCard | 큰 카드: 장소명(drive/title) + 거리·핵심 한 줄(drive/body) + StatusChip + 주 버튼 "경유지로 추가". 폭 약 520dp 기준 시안, 2열 | #6 |
| UndoBar | 선택 직후 10초 카운트 "취소" 버튼. 높이 ≥ touch/min | #6 |
| StatusChip | 위 5절 | #6~#12 |
| ConditionForm | 가족 여행 조건·정차 필요 입력, 에너지 수동 입력. 정차 중에만 노출 | #7 |
| CompareList / CompareRow | 후보 비교(거리·충전기·시설·가족 활동). 정차 중에만 | #8, #10 |
| WaypointReplaceDialog | 경유지 2개 초과 시 교체 대상 선택 | #9 |
| EmptyState | 권한 거부·미수신 대체 | #11 |
| VoiceHint | 음성 안내 상태 표시(말하는 중/끔) | #12 |

Figma에는 StatusChip, DriveCard, Button/Primary만 시안으로 있다. 나머지는 구현 시 같은 토큰으로 조립하고 필요하면 Figma에 추가.

## 7. 화면 규칙

### 주행 중 (또는 이동 상태 미확인, ADR-0003·0004)
- `distractionOptimized` Activity. 큰 카드 **최대 2개**, 한 화면에 고정(스크롤·펼치기·탭 전환·비교 금지).
- 허용 상호작용: 카드 한 번 탭 → 즉시 경유지 추가, 이후 10초 UndoBar. 음성 안내 보조.
- 텍스트: drive/title, drive/body만. 카드당 문구 2줄 이내. 입력 필드·키보드·목록 없음.
- 이동 중 자동 재추천 없음. 새 추천은 사용자 요청 시에만.

### 정차 중
- 상세 비교 화면(CompareList), 조건 입력 폼, 에너지 수동 입력, 후보 3개 이상 비교 가능. stop/title·stop/body·caption 사용.
- 스크롤 허용. 단 조작은 touch/min 이상 대상 위주.
- 정차→주행 전환 시 즉시 주행 화면으로 대체(진행 중 입력은 보존하되 화면은 주행 규칙). 미확인이면 주행으로 취급.

## 8. 디자인 소스 기록

- **k-style-ui-ux** (https://github.com/vmc-inner-circle/k-style-ui-ux, 사용자 확인): PRD→한국 앱 스타일 모바일 화면 초안 플러그인. 이번에는 **설치·실행하지 않고** `checklist.md`(비평 체크리스트)만 읽기 전용으로 참조. 반영한 항목: 뜻이 다른 배지는 다른 색(5절 상태 4종), 한 번뿐인 행동은 누른 뒤 모습 필요(UndoBar), 대상마다 시작·끝 화면(#6~#12 화면 점검 시 사용). 한국 앱 팔레트·모바일 레이아웃은 차량 화면과 맞지 않아 쓰지 않음.
- **omd(oh-my-design)**: 로컬에 `omd:*` 스킬이 설치돼 있으나 위 레포와 다른 도구로 확인됨. 이번 작업에서는 사용하지 않음.
- **Mobbin MCP**: 설치·연결됨(`search_screens` 등). iOS 지도·추천 카드·경유지 추가·undo 토스트 레퍼런스 12건 열람. 차량용 화면이 아니라 모바일이라 **구조 참고에만** 사용(큰 카드+지도, 하단 시트 확인/취소, 상태 배지). 참고 앱: Waymo(정차 추가 시트), Tripadvisor(지도+카드), Starbucks(목록의 상태 배지), Structured(Undo 바). 계정 생성·결제 없음.
- 차량 환경 규칙(글자·터치·대비)은 두 소스 모두에서 가져오지 않았다. 공식 Pleos/AAOS 지침 확인 후 3·4절 갱신.

## 9. 다음 확인 사항

1. Pleos/AAOS 공식 최소 글자 크기·터치 영역·대비 수치 (3·4절 갱신).
2. 에뮬레이터에서 실제 화면 크기·DPI 확인 후 DriveCard 폭 확정(#3).
3. Day 모드 필요 여부와 ok/accent 재검증.
