# Pleos Connect 에뮬레이터 (Apple Silicon) 셋업 · E2E 절차

2026-11-08 Apple Silicon(arm64) Mac에서 실측. 이슈 #3. 근거: `docs/research/aaos-dev-notes.md`.

## 결론

- **Pleos Connect v2.1.0 arm64 이미지는 Apple Silicon에서 부팅·렌더링된다.** `-gpu host`(Metal/MoltenVK)로 동작하며 문서의 외장 GPU(RTX/RX) 요구는 Mac에 적용되지 않았다.
- 이 저장소 APK(`ai.familystop.app`, 멀티 모듈 전환 후 main 기준)는 `./gradlew :app:assembleDebug`로 빌드되어 설치·실행된다. 화면의 `배터리 62%`는 `AppGraph`가 아직 `FakeVehicleEnergySource`(기본 62)를 쓰기 때문이며 **Pleos Vehicle SDK 값이 아니다**. `PleosVehicleEnergySource`를 임시로 배선하면 콘솔 SOC가 화면에 반영되는 것은 확인했다(§6).
- 에뮬레이터는 `-crash-report-mode disabled` 없이는 부팅 직후 스스로 죽었다(아래 함정 1). 이 플래그를 붙인 실행은 1회 재현 성공(원인 추정은 hang detector, 이중 확인은 안 함).
- E2E 조합: **`adb` + `android` CLI**(스크린샷·UI 덤프)로 결정적 테스트. `google/artemis`는 실재하며(아래) LLM 키가 필요한 자연어 탐색 테스트용 선택지.
- Virtual Console REST로 기어·SOC·속도를 조작할 수 있다(`scripts/vconsole.sh`, §4). 쓰기는 브라우저 하트비트가 있어야 한다(함정 5). #1·#2 실측 결과는 §6.

## 1. 시스템 이미지 설치 (1회, 4.67GB)

공식 경로는 Android Studio의 SDK Update Site 등록이다. CLI만으로 설치하려면:

```bash
# update site: https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml
# 패키지: Pleos Connect v2.1.0 APIs arm64 System Image (API 34), 42dot SDK License Agreement 동의 필요
mkdir -p ~/pleos-dl && cd ~/pleos-dl
curl -fL -C - -O https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp_car_arm64_emulator.zip
shasum crp_car_arm64_emulator.zip   # 24d28eb2f18eba5e87c861a7eb2bdccebb049419
D=$ANDROID_HOME/system-images/android-34/android-automotive/arm64-v8a
mkdir -p $D && unzip -q crp_car_arm64_emulator.zip -d $D
mv $D/crp_avd_out_arm64-v8a/* $D/ && rmdir $D/crp_avd_out_arm64-v8a
```

디렉터리는 반드시 `android-automotive/arm64-v8a`여야 한다(`pleos_car_emulator`로 두면 avdmanager가 "inconsistent location"으로 거부).

## 2. AVD 생성

```bash
echo no | $ANDROID_HOME/cmdline-tools/latest/bin/avdmanager create avd -n Pleos_Connect_v2_1_0 \
  -k "system-images;android-34;android-automotive;arm64-v8a" -d Pleos_Connect_v2_0
C=~/.android/avd/Pleos_Connect_v2_1_0.avd/config.ini
sed -i '' -e 's/^hw.ramSize=.*/hw.ramSize=4096/' -e 's/^hw.gpu.enabled=.*/hw.gpu.enabled=yes/' \
  -e 's/^hw.gpu.mode=.*/hw.gpu.mode=host/' -e 's/^fastboot.forceColdBoot=.*/fastboot.forceColdBoot=yes/' \
  -e 's/^fastboot.forceFastBoot=.*/fastboot.forceFastBoot=no/' $C
```

디바이스 `Pleos_Connect_v2_0`("Pleos Connect v2.1.0", 2560x1440 mdpi)는 이미지에 포함된 `devices.xml`에서 온다. RAM 4GB, Cold boot(quick boot는 VHAL 꺼짐).

## 3. 부팅 · 빌드 · 설치 · 실행

```bash
# 터미널 하나를 점유한다(백그라운드 실행). 약 10~20초 후 sys.boot_completed=1
caffeinate -i $ANDROID_HOME/emulator/emulator -avd Pleos_Connect_v2_1_0 \
  -no-snapshot -gpu host -no-metrics -no-boot-anim -crash-report-mode disabled

./gradlew :app:assembleDebug
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ai.familystop.app/ai.familystop.MainActivity
```

`android emulator start`는 이 플래그를 넘기지 못할 수 있어 쓰지 않았다(미검증).

## 4. E2E 도구

| 용도 | 명령 | 상태 |
|---|---|---|
| 스크린샷 | `android screen capture -o out.png` | 검증 |
| UI 덤프(JSON) | `android layout --full --flat` | 검증. `--full` 없이는 앱 창이 안 나왔다 |
| 입력 | `adb shell input tap/swipe/text/keyevent` | adb 표준 |
| 입력(선택자) | `android input tap/text/key` | 미검증 |
| 앱 설치/실행 | `adb install`, `adb shell am start` | 검증 (`android run`/`install`은 미검증) |
| 로그 | `adb logcat --pid=$(adb shell pidof ai.familystop.app)` | 검증 |
| 차량 값 조작 | `scripts/vconsole.sh gear P\|R\|N\|D`, `soc 0-100`, `charge on\|off`, `range <km>`, `speed <kph>`, `state` (Virtual Console REST, 아래) | 검증 |

`android layout`은 최초 호출 때 기기에 `com.android.cli.interact.instrumentation`(3rd-party 패키지)을 설치한다.

### Virtual Console REST

`adb forward tcp:8088 tcp:8088` 후 `http://127.0.0.1:8088`. **쓰기(`PUT`)는 브라우저 하트비트가 살아 있을 때만 된다**: 웹 페이지가 1초마다 `POST /browser/session?client=<id>`를 보내며 타임아웃은 3초다. 하트비트가 없으면 `/vehicle/state`가 `emulator_active:false`이고 모든 PUT이 `{"error":"emulator_inactive"}`다. 스크립트(`scripts/vconsole.sh`)가 하트비트를 돌려 준다.

| 조작 | 호출 |
|---|---|
| 기어 | `PUT /vehicle/gear?gear=Park\|Reverse\|Neutral\|Drive` |
| 배터리 SOC | `PUT /vehicle/ev-battery?field=soc&value=0..100` |
| 충전 스위치 | `PUT /vehicle/ev-battery?field=charge_switch&value=true\|false` (앱은 3초마다 `getEvChargeSwitchState`로 읽음) |
| 주행 가능 거리 | `PUT /vehicle/odometer?remaining_range_m=<m>` (앱은 `CAR_DRIVE` 권한으로 읽음, #81) |
| 속도 | `PUT /vehicle/controls?speed_kph=0..240` (`accelerator`, `brake`, `steering`도 같은 엔드포인트) |
| 상태 | `GET /vehicle/state`, 전체 항목 `GET /vehicle/catalog` |

설정한 값은 하트비트가 끊겨도 유지된다. 기어·SOC·속도 변경은 앱의 Pleos SDK 콜백과 AAOS `CarDrivingStateService`에 반영되는 것을 확인했다.

### artemis

`google/artemis`는 실재한다: https://github.com/google/artemis (Apache-2.0, Python 3.12+, MCP 서버 내장). 자연어로 안드로이드 기기를 조작·스크린샷·logcat 수집하며 Claude Code에 MCP로 연결할 수 있다. 에이전트 구동에 멀티모달 모델 API 키가 필요하고, `./start.sh`가 ADB/scrcpy/FFmpeg 설치와 전역 MCP 설정 변경을 제안하므로 **설치하지 않았다**(사용자 몫). 설치 시 `git clone https://github.com/google/artemis.git && cd artemis && ./start.sh`. critical path E2E의 기본은 위 `adb`+`android` CLI, artemis는 탐색적 시나리오에 선택 사용.

## 5. 함정

1. **에뮬레이터 hang detector**: Pleos 이미지 부팅 후 QEMU 스레드가 15초 무응답으로 보고되고 에뮬레이터가 SIGSEGV(`CrashReporter::GenerateDumpAndDie`)로 스스로 종료된다(`-gpu host`·`swangle_indirect` 모두). `-crash-report-mode disabled`로 해결.
2. 크래시 직후 재실행하면 "crashdialog to get consent" GUI 대기에 걸려 프로세스는 있는데 adb에 안 잡힌다. 남은 덤프 삭제: `find /tmp/android-$USER -name '*.dmp' -delete`, 이전 qemu 프로세스 종료 후 재실행.
3. 앱 창이 검게 보인다: 현재 `MainActivity`는 배경·글자색 지정 없이 `Text`만 그려서 검은 글씨가 검은 창에 묻힌다. 텍스트는 UI 덤프로 확인된다.
4. `adb shell cmd car_service inject-vhal-event/get-property-value/emulate-driving-state`는 user 빌드라 막힘. `cmd hmg_car_service`(문서의 `driving_regulation`)도 서비스가 없다. 대신 Virtual Console을 쓴다.
5. Virtual Console에 `curl`로만 PUT하면 `emulator_inactive`다. 하트비트가 필요하다(위 REST 절).
6. Pleos SDK 호출은 권한이 없으면 앱이 `SecurityException`으로 죽는다: 기어 `GearSelectionListener`는 `pleos.car.permission.CAR_DRIVE`, `NaviHelper.getDestinationInfo`/`getWaypointInfo`/`getRouteStateInfo`는 `pleos.car.permission.NAVI_ROUTE`. 현재 매니페스트는 `NAVI_ROUTE_SEARCH`, `NAVI_ROUTE`, `CAR_ENERGY`, `CAR_DRIVE`(#81, 주행 가능 거리)를 선언한다.
7. 에뮬레이터 현재 위치는 `adb emu geo fix <lon> <lat>`로 설정한다(서울 예: `127.0276 37.4979`).

## 6. 실측 결과 (2026-11-08, #31)

임시 프로브(`impl:pleos`에 리스너·`am broadcast` 트리거를 넣은 비커밋 코드)로 Pleos SDK 콜백을 직접 관찰했다. SOC는 콘솔에서 77→18로 바꿀 때마다 `PleosVehicleEnergySource`를 통해 앱 화면(`배터리 N%`)에 3초 내 반영됐다(`AppGraph`에 임시 배선).

**#1 `addWaypoint` 후 원 목적지** (목적지 DEST-Seoul 37.5665,126.978 요청 → 경유지 추가):
- 경유지 1개·2개 추가 후 `getDestinationInfo()`는 계속 DEST-Seoul, `getWaypointInfo()`는 추가한 순서대로 반환. 목적지는 유지된다.
- 단, 추가 직후 재탐색 동안(약 5~9초) `onNavigationStatus`가 `FREE_DRIVE`로, `onDestinationInfo`가 빈 값(이름 없음, 0.0/0.0)으로, `onWayPointInfo`가 빈 목록으로 흔들린 뒤 `onWaypointChanged(ADD)`와 `ROUTE_DRIVE`로 복구된다. 이 이벤트의 `routeStartInfo.name`은 목적지가 아니라 경유지 이름이다. 완료 이벤트 전의 목적지 값은 믿지 말 것.
- 이미 찬 인덱스(`FIRST`)에 다시 `addWaypoint`하면 오류도 이벤트도 없이 슬롯이 그대로다(무시). 3번째 경유지는 못 넣는다.
- 위 일시 `FREE_DRIVE` 구간이 자기 상태 판단(경로 유무)에 쓰이면 잘못된 분기가 날 수 있다.

**#2 주행/정차 판별 근거**:
- `DrivingModeStateListener`는 주행 *모드*(`NORMAL`/`ECO`/`SPORT`)이지 이동 여부가 아니다.
- Vehicle SDK `getGear().registerGearSelection(GearSelectionListener)`(권한 `CAR_DRIVE`)가 콘솔 기어 변경을 `GEAR_PARK/DRIVE/NEUTRAL/REVERSE`로 즉시 전달한다(속도와 무관).
- Vehicle SDK에는 속도 API가 없다(`api` 목록에 Speed 없음). 속도는 AAOS `CarDrivingStateService`로 확인된다: Park=0(정차), Drive+속도0=1(IDLING), Drive+속도>0=2(MOVING), Neutral+속도>0=2. 앱에서는 `android.car` `CarDrivingStateManager`(권한 `CAR_DRIVING_STATE`)로 읽을 수 있다. 앱 코드에서는 미검증.
- `onDrivingInfo`는 이번에 호출 경로를 만들지 않아 미검증. `onNavigationStatus`는 경로 유무(`FREE_DRIVE`/`ROUTE_DRIVE`)이지 이동 여부가 아니다.
- ADR-0004의 결정(미확인=주행 중)은 바꾸지 않았다. 판별 후보는 기어(`GEAR_PARK` = 정차)이며, 이는 현재 결정과 충돌하지 않는다.

**남은 것**: FusedLocation/STT/TTS는 SDK "Coming Soon"이라 미확인. 실차와 에뮬레이터 Navi 동작이 같다는 보장은 없다(에뮬 Navi는 경로 시간·거리가 고정값에 가깝다).

## E2E 녹화 (`scripts/e2e-demo.sh <outdir>`)

입력 → 추천 → 경유지 추가 → 활동 → 체크 → 충전(차량 값: Virtual Console로 충전 스위치 on, SOC 35→60→80, off) → 자동 충전 끝을 `uiautomator`로 구동하고, `screenrecord` 녹화 + `ffmpeg` 압축(1280px/15fps, 약 0.3MB)까지 한다. 단계별 OK/FAIL은 `<outdir>/log.txt`. 사전 조건: 내비에서 더현대서울 안내 중(경로 없음이면 목적지 선택·전달이 달라진다). 내비는 앱 재시작 후에도 경유지를 유지하므로, 앱은 연결 직후 `getWaypointInfo`로 슬롯을 동기화한다(#69).

## 시연 전 AOT 컴파일 (R8 release, #105)

시연 전에 `adb shell cmd package compile -m speed -f ai.familystop.app`를 실행한다. 이 에뮬레이터는 sideload APK의 ProfileInstaller 프로필을 쓰지 못한다(broadcast 결과 `4` NOT_WRITABLE, dexopt는 `status=verify` 그대로). 컴파일하지 않으면 앱이 JIT로만 돈다.
