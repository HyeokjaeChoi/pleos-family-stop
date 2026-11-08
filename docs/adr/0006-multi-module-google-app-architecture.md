# 멀티 모듈 + Google 앱 아키텍처로 전환한다

단일 모듈 `:app`을 Gradle 멀티 모듈로 나누고 Google 앱 아키텍처를 따른다. 병렬 작업(#5~#13)이 같은 파일·패키지에서 충돌하는 일을 모듈 경계로 줄이고, Pleos SDK 의존(ADR-0001)을 `:impl:pleos` 한 곳에 가두기 위해서다. 사용자가 직접 결정했다. 패키지명(`ai.familystop`)과 동작은 바꾸지 않고 파일 이동·모듈 분리만 한다.

## 아키텍처 (Google 앱 아키텍처)

- **UI 레이어** (`:feature:*`): Compose 화면 + `ViewModel`이 `StateFlow<UiState>`를 노출한다. 상태는 아래로, 이벤트는 위로.
- **domain 레이어** (`:core:domain`): Ports 인터페이스(`LocationSource`, `VoiceOutput`, `NaviGateway`, `VehicleEnergySource`)와 유스케이스. 필요한 것만 둔다(단순 위임 유스케이스는 만들지 않는다).
- **data 레이어** (`:impl:*`): Ports 구현(data source). 상태를 가진 repository가 필요해지면 `:core:domain`에 인터페이스, `:impl:*`에 구현.

## 모듈 맵

| 모듈 | 역할 |
|---|---|
| `:app` | 셸: `MainActivity`, `AppGraph`(조립), 네비게이션, 매니페스트 |
| `:core:model` | 순수 Kotlin 도메인 값 타입(`GeoPoint`, `StopPlace` 등). Android·Compose 의존 없음 |
| `:core:domain` | Ports 인터페이스 + 유스케이스. `:core:model`에만 의존 |
| `:core:designsystem` | DESIGN.md 토큰·공용 Compose 컴포넌트 |
| `:feature:drive` | 이동 상황 판별 + 주행 중 제한 화면 (#6) |
| `:feature:input` | 여행 조건·정차 필요·에너지 입력 (#7) |
| `:impl:pleos` | `NaviGateway`(NaviHelper), `VehicleEnergySource`(Vehicle) (#4) |
| `:impl:platform` | `LocationSource`(LocationManager), `VoiceOutput`(TextToSpeech) (#5) |

테스트용 fake(`fake/`)는 `:core:domain`의 `testFixtures`가 아니라 우선 `:app`에 둔다. 다른 모듈 테스트가 fake를 필요로 할 때 `:core:testing`으로 분리한다.

새 기능 화면(#8~#13)은 `:feature:<name>` 모듈로 추가한다. 이름 규칙: 소문자 단일 단어, 패키지는 `ai.familystop.feature.<name>`.

## 의존 방향

```
:app ──▶ :feature:* ──▶ :core:domain ──▶ :core:model
  │          └────────▶ :core:designsystem
  └─────▶ :impl:* ────▶ :core:domain
```

- `feature → core:domain, core:designsystem`; `impl → core:domain`; `:app`이 모두 조립한다.
- **feature끼리, impl끼리, feature↔impl은 서로 의존 금지.** feature 간 이동은 `:app` 네비게이션이, 공유가 필요한 타입은 `:core:model`/`:core:domain`이 맡는다.
- Pleos SDK(`ai.pleos.playground:*`) 의존은 `:impl:pleos`에만 둔다.

## 하지 않는 것 (과설계 방지)

- `build-logic` 컨벤션 플러그인: 모듈별 build 파일의 중복이 눈에 띄면 그때 도입한다.
- 버전 카탈로그: 아직 없다. 모듈이 늘어 같은 버전을 반복하면 `gradle/libs.versions.toml`을 추가한다.
- DI 프레임워크(Hilt 등): `AppGraph` 수동 조립 유지.

## 이동 순서

1. 이 ADR + 계획 PR(#27).
2. 열린 PR #24(drive), #26(input), #25(pleos)가 모두 병합된 뒤, 병합된 main 기준으로 코드를 이동한다(충돌 방지).
3. #5(impl/platform)는 미착수이므로 새 구조 기준으로 `:impl:platform`에서 시작한다.
4. 이동 PR에서 CI `gate`·`AGENTS.md`·`docs/agents/`·열린 이슈의 경로 안내를 함께 갱신한다.
