# Pleos Connect 에뮬레이터 (Apple Silicon) 셋업 · E2E 절차

2026-11-08 Apple Silicon(arm64) Mac에서 실측. 이슈 #3. 근거: `docs/research/aaos-dev-notes.md`.

## 결론

- **Pleos Connect v2.1.0 arm64 이미지는 Apple Silicon에서 부팅·렌더링된다.** `-gpu host`(Metal/MoltenVK)로 동작하며 문서의 외장 GPU(RTX/RX) 요구는 Mac에 적용되지 않았다.
- 이 저장소 APK(`ai.familystop.app`)는 설치·실행되고 Pleos Vehicle SDK로 배터리 SOC를 읽는다(화면에 `배터리 62%`).
- 에뮬레이터는 **`-crash-report-mode disabled` 없이는 부팅 15초 뒤 스스로 죽는다**(아래 함정 1). 이 플래그 필수.
- E2E 조합: **`adb` + `android` CLI**(스크린샷·UI 덤프)로 결정적 테스트. `google/artemis`는 실재하며(아래) LLM 키가 필요한 자연어 탐색 테스트용 선택지.
- Virtual Console REST는 떠 있지만 `emulator_inactive`라 기어/SOC를 아직 못 바꾼다 → #1·#2 실측 보류(아래 "미해결").

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
| 차량 값 조작 | Virtual Console `http://127.0.0.1:8088` (`adb forward tcp:8088 tcp:8088` 후) | 미해결(아래) |

`android layout`은 최초 호출 때 기기에 `com.android.cli.interact.instrumentation`(3rd-party 패키지)을 설치한다.

### artemis

`google/artemis`는 실재한다: https://github.com/google/artemis (Apache-2.0, Python 3.12+, MCP 서버 내장). 자연어로 안드로이드 기기를 조작·스크린샷·logcat 수집하며 Claude Code에 MCP로 연결할 수 있다. 에이전트 구동에 멀티모달 모델 API 키가 필요하고, `./start.sh`가 ADB/scrcpy/FFmpeg 설치와 전역 MCP 설정 변경을 제안하므로 **설치하지 않았다**(사용자 몫). 설치 시 `git clone https://github.com/google/artemis.git && cd artemis && ./start.sh`. critical path E2E의 기본은 위 `adb`+`android` CLI, artemis는 탐색적 시나리오에 선택 사용.

## 5. 함정

1. **에뮬레이터 hang detector**: Pleos 이미지 부팅 후 QEMU 스레드가 15초 무응답으로 보고되고 에뮬레이터가 SIGSEGV(`CrashReporter::GenerateDumpAndDie`)로 스스로 종료된다(`-gpu host`·`swangle_indirect` 모두). `-crash-report-mode disabled`로 해결.
2. 크래시 직후 재실행하면 "crashdialog to get consent" GUI 대기에 걸려 프로세스는 있는데 adb에 안 잡힌다. 남은 덤프 삭제: `find /tmp/android-$USER -name '*.dmp' -delete`, 이전 qemu 프로세스 종료 후 재실행.
3. 앱 창이 검게 보인다: 현재 `MainActivity`는 배경·글자색 지정 없이 `Text`만 그려서 검은 글씨가 검은 창에 묻힌다. 텍스트는 UI 덤프로 확인된다.
4. `adb shell cmd car_service inject-vhal-event/get-property-value/emulate-driving-state`는 user 빌드라 막힘. `cmd hmg_car_service`(문서의 `driving_regulation`)도 서비스가 없다.

## 6. 미해결

- **Virtual Console 제어**: 포트 8088 `/health`는 정상(`vehicle_api_dds_emulator`)이나 `/vehicle/state`가 `emulator_active:false`·`bridge_ready:false`이고 `PUT /vehicle/gear`, `/vehicle/ev-battery?soc=`가 `{"error":"emulator_inactive"}`를 반환한다. 원인 미확인(DDS 브리지 미연결 추정). 풀기 전에는 기어·SOC·속도 조작이 안 된다. Pleos에 문의하거나 Android Studio 에뮬 경로에서 같은지 비교 필요.
- **#1 addWaypoint 후 원 목적지 유지, #2 주행/정차 판별 근거**: 위 조작 불가와, 현재 `main`의 앱이 NaviHelper/주행 상태 화면을 아직 호출하지 않아 이번에 실측하지 못했다.
- 에뮬 주행 상태(UX restrictions)와 FusedLocation/STT/TTS 동작은 미확인(SDK "Coming Soon").
