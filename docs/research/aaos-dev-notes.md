# AAOS / Pleos Playground 개발 조사 노트 (읽기 전용 조사, 2026-11-07)

방법: `aside guide` 읽고 `aside exec`(Aside 에이전트)로 공개 문서만 열람. 로그인/제출/다운로드/설치 없음.
표기: [확인] = 페이지에서 직접 확인, [미확인] = 문서에서 못 찾음(추측 안 함).
Pleos 기준 URL 접두: `https://document.pleos.ai/en` / AAOS: `https://developer.android.com`
주의: 요청하신 `https://document.pleos.ai/en/docs/pleos-only/` 루트 자체는 "Page Not Found"(404). sitemap 하위 페이지로 조사. `/docs/pleos-only/infolink-emulator/...`도 `/en`에서 404.

## 핵심 요약
- 에뮬레이터: Android Studio SDK Manager에 update site를 등록해 "Pleos Connect System Image"(API 34, 디바이스 "Pleos Connect v2.1.0") 설치. 가용 [확인].
- Apple Silicon: Mac용 별도 arm64 이미지 URL 존재 [확인] / "Apple Silicon 지원"이라고 직접 명시한 문장은 [미확인]. 호스트 요구에 외장 GPU(NVIDIA RTX 3060 Laptop 6GB / AMD RX 6600M 8GB 이상)가 적혀 있어 Mac 적용 여부 [미확인] -> 실제 설치로 검증 필요.
- 주행 시뮬레이션: Pleos Connect Virtual Console(`adb forward tcp:8088 tcp:8088` -> http://127.0.0.1:8088/)에서 기어/브레이크/배터리 SOC 등 조작 [확인]. 주행 제한 on/off adb 명령 있음 [확인]. 속도 설정법·GPS/내비 시뮬레이션은 [미확인].
- 리스크: My Project의 SDK 접근 상태에서 FusedLocation/TTS/STT/Vehicle Control이 "Not Available Yet / Coming Soon"으로 표시(문서 스냅샷, 오래됐을 수 있음) -> Pleos 측 확인 필요.

## 1. Pleos Playground 개발 환경

| 항목 | 내용 | 상태 | 출처(/en 이하) |
|---|---|---|---|
| 에뮬레이터 이미지 | "Pleos Connect System Image" API 34, AVD 디바이스 "Pleos Connect v2.1.0"(Automotive form factor) | 확인 | /docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator |
| 설치 방법 | Android Studio > SDK Manager > SDK Update Sites에 URL 추가 > Device Manager에서 AVD 생성 | 확인 | 위와 동일 |
| Update site (Mac) | `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml` | 확인 | 위와 동일 |
| Update site (Win/Linux) | `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml` | 확인 | 위와 동일 |
| AVD 설정 | Cold boot, RAM 4GB. quick boot는 VHAL 비활성화 | 확인 | /docs/connect/guide/getting-started/application-development/configure-pleos-connect-emulator |
| 호스트 요구 | 최신 Android Studio, SDK API 28+, Gradle 8.0+, Java/Kotlin, 외장 GPU 필수(최소 RTX 3060 Laptop 6GB 또는 RX 6600M 8GB, 권장 RTX 4060/5060 Laptop 8GB) | 확인 | setup/configure 두 페이지 |
| Apple Silicon | arm64 이미지 URL 존재. "지원" 명시 문장 없음. GPU 요구의 Mac 적용 여부 불명 | 부분 확인/미확인 | 위와 동일 |
| 에뮬 기타 | UI 기본 한국어(Vehicle Settings > General > Language). 블루투스 동글 미지원. 2.1.0에서 LLM SDK/CRN 지원 제거. minSdk 28 | 확인 | /docs/pleos-only/release-note/emulator-release-note, /docs/pleos-only/getting-started |
| gradle 저장소 | `settings.gradle(.kts)`에 `https://nexus-playground.pleos.ai/repository/maven-releases/` | 확인 | /docs/pleos-only/getting-started |
| 좌표 | `ai.pleos.playground:Vehicle:2.3.1`(quick-start, release note) 또는 `2.3.2`(Vehicle import 페이지) - 문서끼리 불일치. `ai.pleos.playground:FusedLocation:1.1.2`. NaviHelper v3.0.5, TTS/STT v3.3.1은 버전만 확인, 좌표 미확인 | 확인/일부 미확인 | /docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup, .../FusedLocation/project-setup, /docs/pleos-only/release-note/sdk-release-note |
| My Project 등록 | Pleos 계정 가입 > Workspace > My Project > New Project (Project name, Developer name(회사), Category, 설명) | 확인 | /docs/pleos-only/create-my-project |
| 자격증명 | Project Information에 Project ID(Key), Client ID, Client Secret. CRN은 에뮬 2.1.0부터 불필요. Project ID는 App Market Console 심사 요청에, Client ID/Secret은 TTS/STT `registerApp()`에 사용 | 확인 | 위와 동일 |
| SDK 접근 신청 | SDK > [SDK name]에서 신청. Vehicle State, NaviHelper는 "Ready to Use". FusedLocation, TTS, STT, Vehicle Control은 "Not Available Yet / Coming Soon"(스냅샷). 일부는 제휴 약정 필요 | 확인(최신성 불명) | 위와 동일 |
| 권한 선언 | AndroidManifest.xml에 예: `pleos.car.permission.CAR_DOORS` | 확인 | 위와 동일 |
| 차량 데이터 시뮬 | Virtual Console(에뮬 2.1.0+): `adb forward tcp:8088 tcp:8088` 후 http://127.0.0.1:8088/ . Motion(기어 P/R/N/D, 브레이크, 조향각, 주행거리), ADAS(크루즈), Convenience(문/창/조명), Safety, Energy(Battery SOC 0-100%, 에너지, 주행가능거리, 충전상태), Climate | 확인 | /docs/connect/guide/getting-started/application-development/use-vehicle-emulator, /docs/pleos-only/app-test-guide/virtual-console-guide |
| 속도 설정 | setup 페이지는 "속도 변경 가능"이라 하나 Motion 카드 목록에 속도 없음 | 미확인 | 위 두 페이지 |
| 주행 제한 토글 | `adb shell cmd hmg_car_service --service drivingstate driving_regulation on` / 끝을 `off`로. `/dev/vcs_simulator_rx` 바이트 주입 명령도 있으나 해석 안 함 | 확인 | /docs/pleos-only/vehicle-app-developer-guide/adb-command-reference-guide |
| GPS/위치/내비 시뮬 | Virtual Console 문서에 location/GPS/route 언급 없음 | 미확인 | - |
| 폼팩터 테스트 | 가상 디스플레이 생성. 에뮬을 별도 창(Android Studio 임베드/headless 아님)으로 실행해야 함 | 확인 | /docs/pleos-only/app-test-guide/form-factor-test-guide |

SDK별 setup 관련 메모
- FusedLocation v1.1.2 [확인]: `pleos.car.permission.FUSED_LOCATION`(dangerous), `isSupported()` 먼저 호출, 콜백 null/`onError` 처리. (/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro, .../declaring-permissions)
- NaviHelper v3.0.5 [확인]: 경유지 최대 2개(`WaypointIndex.FIRST`, `SECOND`), `removeWaypoint(ALL)`, `requestRouteWithWaypoints()`는 `pleos.car.permission.NAVI_ROUTE_SEARCH` 필요, 결과는 `NaviHelperEventListener`(`onRouteStarted`, `onRoutingCompleted`, `onError`), Pleos Connect Navigation 앱으로 전달. (/docs/pleos-only/connect-sdk-pleos/NaviHelper/route-with-waypoints, .../waypoint-settings, .../declaring-permissions)
- EV SoC [확인]: `getEvBatteryStateOfCharge` 사용(FAQ), `registerEvBatteryStateOfCharge`는 Vehicle SDK v2.3.1 신규. 에뮬 Battery SOC 카드로 값 설정. (/docs/pleos-only/vehicle-app-developer-guide/vehicle-app-faq)
- STT/TTS [확인]: 모드 HYBRID(기본)/SERVER/ON_DEVICE. ON_DEVICE는 네트워크·앱 등록 불필요. SERVER와 HYBRID의 서버 경로는 `registerApp()`(Client ID/Secret) 필요. STT 마이크는 에뮬 Extended Controls에서 설정. (/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro, .../app-registration, /TextToSpeech/intro)

## 2. AAOS 앱 일반 유의사항 (developer.android.com 공식)

| 항목 | 내용 | 상태 | 출처 |
|---|---|---|---|
| 앱 카테고리 | 미디어/POI/IoT/날씨는 주행·주차 모두. 비디오/게임/브라우저는 주차 카테고리(게임 주차 전용, 브라우저는 "labs"로 Internal Testing 트랙만). 메시징/통화는 Android Auto 전용 | 확인 | /training/cars |
| 구현 방식 | 미디어: `MediaBrowserService`+`MediaSession`. 주차 앱: Views/Compose. 템플릿 앱: Car App Library | 확인 | /training/cars |
| Car App Library on AAOS | `com.android.automotive` meta-data + `automotive_app_desc.xml`(`<uses name="template"/>`), `CarAppService`, `CarAppActivity`(singleTask, MAIN/LAUNCHER), 의존성 `androidx.car.app:app-automotive`, min SDK 29, `uses-feature android.software.car.templates_host required=true` | 확인 | /training/cars/apps/automotive-os |
| distractionOptimized | 주행 중 UX 제한 시 Activity 실행 불가. `<meta-data android:name="distractionOptimized" android:value="true"/>`로 예외. Play에서는 Car App Library의 `CarAppActivity`만 허용, 다른 Activity에 표시하면 거절 사유 | 확인 | /training/cars/platforms/automotive-os |
| 주차 앱 | `distractionOptimized` 넣으면 안 됨. `onPause`에서 재생 중지. 주행 중 실행 불가·재생 재개 안 됨 확인 테스트 | 확인 | /training/cars/parked/automotive-os |
| 템플릿 vs 일반 Activity 비교 | 한 페이지에서 대조한 문서 | 미확인 | - |
| UX Restrictions | OEM이 규칙 결정(지역·디스플레이별 상이). 기어/속도로 추론 금지. `Car.createCar(context)` > `getCarManager(Car.CAR_UX_RESTRICTION_SERVICE)` as `CarUxRestrictionsManager`, `currentCarUxRestrictions`/`registerListener`, `isRequiresDistractionOptimization`, Gradle `useLibrary("android.car")` | 확인 | /training/cars/platforms/automotive-os, /training/cars/parked/automotive-os, /reference/android/car/drivingstate/CarUxRestrictionsManager |
| 주행 상태 권한 | `android.car.permission.CAR_DRIVING_STATE` (AOSP) | 확인 | https://source.android.com/docs/automotive/driver_distraction/consume |
| 매니페스트 | `<uses-feature android:name="android.hardware.type.automotive" android:required=.../>`(트랙에 따라 값 선택). wifi/portrait/landscape/camera 요구는 제거 또는 `required=false`. `location.network` 없는 차 많음(coarse 위치는 지원). 내비 앱은 `APP_MAPS` 카테고리 + `androidx.car.app.action.NAVIGATE`(geo) | 확인 | /training/cars/platforms/automotive-os, /training/cars/apps/automotive-os |
| touchscreen required | AAOS 문서에서 언급 못 찾음 | 미확인 | - |
| 화면/입력 | 방향 고정, 세로/가로 둘 다 지원 필요, 태블릿류 큰 화면 가이드 적용. 뒤로가기 어포던스가 없을 수 있어 앱 내 네비 필요. 시스템 바 위치 가변(window insets 사용). 컷아웃은 `NEVER`/`ALWAYS`(DEFAULT, SHORT_EDGES 지양). 로터리는 키보드 포커스 API와 동일, 에뮬 "Car rotary" 컨트롤로 테스트. `car` 리소스 한정자로 큰 터치 타깃 | 확인 | /training/cars/platforms/automotive-os, /training/cars/parked/automotive-os, /training/cars/testing/emulator |
| 다크테마 | Quality TH-1: Car App Library 1.9+ 컴포넌트에 커스텀 테마 시 라이트/다크 지원 필수(내비/POI/IOT/날씨/메시징 템플릿). 비템플릿 일반 가이드 | 확인(템플릿만)/미확인(비템플릿) | /docs/quality-guidelines/car-app-quality |
| 오디오 포커스 | 내비 음성: `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` + `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`. DD-1: 내비 오디오 채널은 내비 앱의 안내 전용. AAOS는 대체로 고정 볼륨. AOSP는 `CarAudioContext` 상호작용으로 포커스 처리, Android 11부터 delayed focus | 확인 | /training/cars/apps/navigation, quality guideline 페이지, https://source.android.com/docs/automotive/audio/audio-focus |
| TTS용 usage attribute | developer.android.com에서 지정 페이지 못 찾음 | 미확인 | - |
| 차량 권한 | Car App Library `CarInfo`: 에너지 레벨 리스너에 `android.car.permission.CAR_ENERGY`, `CAR_ENERGY_PORTS`, `READ_CAR_DISPLAY_UNITS`. 속도 `CAR_SPEED`, 모델 `CAR_INFO`, 위치 `ACCESS_FINE_LOCATION`. 주행거리는 Play 설치 AAOS 앱에 미제공 | 확인 | /training/cars/apps/library/car-hardware-api |
| 권한 요청 방식 | 사용자 승인 필요. `CarContext.requestPermissions()`(AA/AAOS 공통, 별도 Activity 불필요). `Car.PERMISSION_ENERGY`는 dangerous(런타임), `PERMISSION_ENERGY_PORTS`는 normal | 확인 | /training/cars/apps/library/request-permissions, /reference/android/car/Car.html |
| CarPropertyManager | 권한 없으면 `SecurityException` 가능. `Car`는 `FEATURE_AUTOMOTIVE` 기기 전용 | 확인 | /reference/android/car/hardware/property/CarPropertyManager |
| 공식 에뮬레이터 | Android Studio AVD Manager > Automotive 프로필 > Automotive 시스템 이미지. arm64-v8a 이미지 목록 존재(API 33 "Android Automotive with Google APIs ARM 64 v8a", API 34-ext9, 35-ext15 등). 35-ext15는 Car App Library 1.9 | 확인 | /training/cars/testing/emulator |
| 공식 에뮬 주행 시뮬 | Extended controls "Car data"로 VHAL 속성(`PERF_VEHICLE_SPEED` 등) 편집. 속도 > 0 + 기어 P 아님 = 주행, 기어 P = 주차 | 확인 | 위와 동일 |
| 파킹 차단 테스트 | Play Store 없는 이미지 또는 Compatibility mode 사용. 커스텀 하드웨어 프로필도 Play Store 없는 이미지에서만 | 확인 | /training/cars/parked/automotive-os, 에뮬 페이지 |
| Apple Silicon 성능/최소 버전 | 미검증 | 미확인 | - |

## 3. Pleos 앱 심사/배포 가이드 요점

| 영역 | 요점 | 상태 | 출처(/en/docs/pleos-only 이하) |
|---|---|---|---|
| 주의분산 | 운전자 주의분산 최소화 설계, 안전 위험 시 반려 가능 | 확인 | /distribute/app-review-guide/review-driver-distraction-elements, /vehicle-app-planning-guide/driver-distraction-guideline |
| 미디어 앱 | 비디오/게임/브라우저 기능 앱은 정차 중에만 사용, 주행 시작 시 UI 숨김·사용 중단, 헤즈업 알림 금지 | 확인 | 위와 동일 |
| UX 제한 구현 | `CarUxRestrictions`, `onUxRestrictionsChanged()` 적용 필수(예: `UX_RESTRICTIONS_NO_VIDEO` 시 영상 정지) | 확인 | /vehicle-app-developer-guide/driving-restriction-development-guide |
| DO 선언 | DO(Distraction Optimization) 미선언 앱은 주차 중만 허용, 주행 중 불가 | 확인 | /vehicle-app-planning-guide/driving-restiction-policy |
| 지역 규정 | 북미는 주행 중 키보드·다이얼패드 잠금, 검색은 음성만. 유럽 등은 다름 | 확인 | 위와 동일 |
| 성능 심사 | 입력 후 응답 0.25초 이하, 실행 10초 이내, 콘텐츠 로드 10초 이내, 버튼 반응 2초(아니면 진행 표시). 크래시/프리즈/UI 깨짐/호환 모드 불가, safe zone 준수, 더미 데이터 불가 | 확인 | /distribute/app-review-guide/review-functionality-performance-stability |
| 자원 | CPU: 내비 앱 주행 중 15% 이하, 기타 앱 주행 중 5% 미만, 주차 중 전체 25% 이하. RAM 400MB 이하 권장 | 확인 | /vehicle-app-planning-guide/performance-resource-mgmt |
| 권한 | Vehicle SDK 권한은 Pleos App Market과 사전 협의·승인된 것만 선언 가능, 미승인 선언 시 심사 불가. 최소 권한, 명시적 동의와 철회, 개인정보처리방침 필수, 악성코드/보안 우회는 반려 | 확인 | /distribute/app-review-guide/review-functionality-performance-stability, /review-user-privacy-security |
| 제출 | Pleos App Market Console, APK 600MB 이하. 필수: 대상 플랫폼(Connect/InfoLINK), 카테고리, 연령 등급, 개인정보 처리, 위치정보 사용, App Permissions, SDK 승인 필요 시 Project ID, 개인정보처리방침 URL(대상 지역 모든 언어), IARC 등급 근거, 전 기능 접근 가능한 심사용 계정, 서버 구성요소 가동 상태 | 확인 | /distribute/app-submission-guide |

## 4. 초보자 함정/체크리스트

| 함정 | 대응 | 상태 | 출처 |
|---|---|---|---|
| quick boot로 VHAL 꺼짐 | AVD를 Cold boot로 | 확인 | configure-pleos-connect-emulator |
| 부팅 완료 전 앱 실행 | 부팅 완료 후 실행. 값 못 읽으면 minSdk 28, 권한 선언, Gradle sync, `onFailed` 예외 메시지 확인 | 확인 | /vehicle-app-developer-guide/vehicle-app-faq |
| 에뮬에서 CCS adb 명령 | 실행 금지 | 확인 | 위와 동일 |
| `Build.DEVICE`로 플랫폼 판별 | 실차에서만 동작, 사용 금지 | 확인 | 위와 동일 |
| 에뮬 미디어 코덱이 실차와 다름 | 실차 가정 금지 | 확인 | 위와 동일 |
| GPS 속도와 클러스터 속도 차이 | 클러스터 일치는 `CarPropertyManager`로 `PERF_VEHICLE_SPEED` 읽기 | 확인 | 위와 동일 |
| 내비 설치 여부 감지법 | 비공개, partnership@pleos.ai 문의 | 확인 | 위와 동일 |
| Vehicle SDK 버전 불일치 | 2.3.1 vs 2.3.2 문서 불일치, 실제 resolve되는 버전 확인 | 확인 | getting-started vs Vehicle/project-setup |
| `/connect-sdk-service-status` | 해당 경로는 "콘텐츠 없음" 안내만 있음 | 확인 | /docs/pleos-only/connect-sdk-service-status |

## 첫 주 셋업 체크리스트 (순서대로)

1. Pleos 계정 가입 > My Project 생성. Project ID / Client ID / Client Secret 기록(Secret은 코드/저장소에 커밋 금지).
2. My Project > SDK 탭에서 Vehicle State, NaviHelper 접근 확인. FusedLocation, STT, TTS 상태("Coming Soon" 여부) 확인, 막히면 Pleos(partnership@pleos.ai 등 문서 안내 채널)에 가용 시점 문의. 2차 PT 일정 때문에 최우선.
3. Android Studio 최신 + SDK API 28+ + Gradle 8.0+ 확인. SDK Manager > SDK Update Sites에 Mac용 arm64 URL(`.../crp-arm64-sys-img.xml`) 추가.
4. "Pleos Connect System Image"(API 34) 다운로드, AVD "Pleos Connect v2.1.0" 생성(Cold boot, RAM 4GB). Apple Silicon M칩에서 부팅/렌더링 되는지 최초 검증(외장 GPU 요구 문구와 충돌 여부 확인). 안 되면 대안(Windows/Linux GPU PC, Pleos 문의)을 바로 결정.
5. 에뮬 부팅 후 Virtual Console 접속(`adb forward tcp:8088 tcp:8088`, http://127.0.0.1:8088/). Battery SOC, 기어 조작 확인.
6. 빈 프로젝트에 `https://nexus-playground.pleos.ai/repository/maven-releases/` 저장소와 `ai.pleos.playground:Vehicle` 추가, minSdk 28, 매니페스트 권한 선언. Vehicle SDK 버전(2.3.1/2.3.2) 실제 resolve 확인.
7. 스모크 테스트 앱: EV SoC 읽기(`getEvBatteryStateOfCharge`) -> 콘솔에서 SOC 변경 -> 화면 반영. 값을 못 읽으면 FAQ 체크리스트(부팅 완료, minSdk, 권한, sync, `onFailed`) 순서로.
8. 주행/주차 전환 확인: 기어 P vs D, `driving_regulation on/off` adb 명령, `CarUxRestrictionsManager`/`onUxRestrictionsChanged()` 리스너로 UI 전환(주행 중 최소 UI) 구현.
9. NaviHelper 경유지 호출(`requestRouteWithWaypoints`, 최대 2개, `NAVI_ROUTE_SEARCH`) 동작 확인. 에뮬에서 내비 앱 연동·위치 시뮬 방법은 문서에 없으므로(미확인) 직접 테스트 후 Pleos에 문의.
10. FusedLocation(`isSupported()`, `FUSED_LOCATION` 권한)과 STT/TTS ON_DEVICE 확인. 위치 주입 방법 불명이므로 에뮬 Extended Controls(Location) 사용 가능한지 직접 확인(문서상 미확인). ON_DEVICE는 `registerApp()` 불필요.
11. 오디오: TTS 출력과 내비 음성 충돌 방지 위해 오디오 포커스(내비 안내 usage는 내비 앱 전용 취지이므로 TTS에 임의로 쓰지 말 것) 설계. Pleos 문서에서 TTS 권장 usage는 이번 조사에서 미확인.
12. 심사 기준에 맞춰 초기 설계 고정: 응답 0.25초, 실행 10초, CPU/RAM 한도, 최소 권한, 개인정보처리방침, 주행 중 UI 제한, 더미 데이터 금지. 사용할 Vehicle 권한은 사전 협의 목록에 포함시킬 것.
13. (보조) 공식 AAOS 에뮬(Android Studio Automotive arm64 이미지)을 Pleos 이미지가 Mac에서 안 될 때의 임시 개발 환경으로 확보. 단 Pleos SDK(`pleos.car.permission.*`)는 Pleos 이미지에서만 동작할 가능성이 높음(추정, 미확인).
