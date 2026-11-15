# Family Stop — 가족 여행 통합 정차 도우미

전기차 가족 여행에서 식사·화장실·아이 휴식과 충전을 함께 고려해 정차 후보를 비교하고, 고른 장소를 현재 경로의 경유지로 추가하는 Pleos Connect(AAOS) 앱. 현대·기아 차량용 앱 개발 공모전 출품작.

- 앱명: Family Stop (`ai.familystop.app`)
- 대상: 전기차로 가족 여행하는 운전자·성인 동승자
- 기획서 초안: [`docs/submission/proposal.md`](docs/submission/proposal.md)

> 상태 (2026-11-15): 화면 연결과 실제 SDK 배선(#14)을 마쳤다. `AppGraph`는 Pleos NaviHelper·Vehicle SDK와 Android 위치·TTS를 쓰고, 주행/정차는 AAOS `CarUxRestrictions`로 판별한다. 정차 후보 시설은 시연 구간 데이터(가상 포함, 화면에 출처 표시)다. 에뮬레이터 E2E는 `scripts/e2e-demo.sh`.

## 테스트 환경

| 항목 | 값 |
|---|---|
| 에뮬레이터 | Pleos Connect v2.1.0 (AAOS, API 34) arm64 시스템 이미지, AVD `Pleos_Connect_v2_1_0` |
| 화면 | 2560×1440, mdpi, 가로 (기기 정의 `Pleos_Connect_v2_0`, 이미지 포함 `devices.xml`) |
| 검증 호스트 | Apple Silicon macOS, `-gpu host` |
| 앱 | minSdk 28, targetSdk 34, compileSdk 36 |
| 빌드 | JDK 21 이상, Gradle wrapper(`./gradlew`), Android SDK(`ANDROID_HOME`) |
| SDK 저장소 | `https://nexus-playground.pleos.ai/repository/maven-releases/` (`settings.gradle.kts`에 설정됨) |

SDK 사용 현황 (Pleos My Project 콘솔 확인, 2026-11-07):

| SDK | 상태 | 앱 사용 |
|---|---|---|
| Vehicle State | 사용 가능 | EV 배터리 잔량·충전 상태·주행 가능 거리(`CAR_DRIVE` 권한) |
| NaviHelper | 사용 가능 | 정차 지점을 경유지로 추가·취소 |
| FusedLocation | 준비 중 | 대체: Android `LocationManager` (`LocationSource` 뒤) |
| TextToSpeech | 준비 중 | 대체: Android `TextToSpeech` (`VoiceOutput` 뒤) |
| SpeechToText | 준비 중 | 시연에서 제외 |

주행/정차 판별은 AAOS `CarUxRestrictions`(주의분산 제한 여부)로 한다. 준비 중 SDK는 오픈 후 `:impl:pleos`의 구현으로 교체한다 (ADR-0001, ADR-0002).

## 빌드·테스트

```bash
./gradlew ktlintCheck testDebugUnitTest :app:assembleDebug
```

산출물: `app/build/outputs/apk/debug/app-debug.apk`

## 에뮬레이터에서 실행

시스템 이미지 설치와 AVD 생성은 [`docs/agents/emulator.md`](docs/agents/emulator.md) §1–2. 이후:

```bash
# 터미널 하나를 점유한다. 이 플래그 없이는 부팅 직후 에뮬레이터가 죽는다.
$ANDROID_HOME/emulator/emulator -avd Pleos_Connect_v2_1_0 \
  -no-snapshot -gpu host -no-metrics -no-boot-anim -crash-report-mode disabled

adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ai.familystop.app/ai.familystop.MainActivity
```

차량 상태 조작(기어·배터리·충전·주행 가능 거리·속도)은 Virtual Console을 쓴다:

```bash
scripts/vconsole.sh gear P      # P | R | N | D
scripts/vconsole.sh soc 40      # 0–100
scripts/vconsole.sh speed 60    # kph
scripts/vconsole.sh charge on   # on | off
scripts/vconsole.sh range 300    # 주행 가능 거리 km
adb emu geo fix 127.0276 37.4979   # 현재 위치 (경도 위도)
```

## APK

- 위치: [`apk/FamilyStop_FamilyStop_release.apk`](apk/FamilyStop_FamilyStop_release.apk) (release 빌드, APK Signature Scheme v3, versionName 0.1.0)
- 빌드·서명 절차: [`docs/submission/release-apk.md`](docs/submission/release-apk.md)

```bash
adb uninstall ai.familystop.app   # debug 빌드가 깔려 있으면 서명이 달라 먼저 지운다
adb install -r -g apk/FamilyStop_FamilyStop_release.apk
adb shell am start -n ai.familystop.app/ai.familystop.MainActivity
```

내비에서 목적지 안내를 먼저 시작해 두면 앱 1단계에서 그 목적지가 미리 선택된다. 시연 흐름은 [`docs/submission/demo-script.md`](docs/submission/demo-script.md), 자동 E2E는 `scripts/e2e-demo.sh`.

## 구조

멀티 모듈 Gradle, Google 앱 아키텍처 ([ADR-0006](docs/adr/0006-multi-module-google-app-architecture.md)). 패키지는 `ai.familystop.*`.

| 모듈 | 역할 |
|---|---|
| `:app` | 셸: `MainActivity`, `AppGraph`(조립) |
| `:core:model` / `:core:domain` / `:core:testing` / `:core:designsystem` | 값 타입 / Ports 인터페이스 / fake / 디자인 토큰 |
| `:feature:drive` | 이동 상황 판별, 주행 중 제한 화면 |
| `:feature:input` | 여행 조건·정차 필요·에너지 입력 |
| `:feature:recommend` | 정차 후보 추천·비교 |
| `:feature:activity` | 충전 중 가족 활동 안내 |
| `:feature:fallback` | 권한 거부·데이터 미수신 대체 동작 |
| `:feature:metrics` | 측정 동의·정차 가치 지표 |
| `:impl:pleos` | Pleos SDK 구현(NaviHelper, Vehicle) — SDK 의존은 여기에만 |
| `:impl:platform` | Android 플랫폼 구현(LocationManager, TextToSpeech) |

feature끼리, impl끼리는 서로 의존하지 않고 `:app`이 조립한다.

## 설계 결정

[`docs/adr/`](docs/adr/) — Pleos SDK 우선·외부 데이터는 시설만(0001), 음성 범위(0002), 경유지+주행 중 한 번 탭·10초 취소(0003), 이동 상태 미확인=주행 중(0004), 상시 무료(0005), 멀티 모듈(0006). UI 기준은 [`DESIGN.md`](DESIGN.md).
