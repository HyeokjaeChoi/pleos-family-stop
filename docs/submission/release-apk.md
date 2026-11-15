# release APK 빌드·서명 절차

제출 규정: GitHub 저장소 `apk/` 폴더에 `팀명_앱이름_release.apk` (release 빌드). APK 단독 제출 불가. 용량이 너무 크면 GitHub Releases 링크를 제출한다 (`docs/research/contest-rules.md`).

2026-11-08 실측: `:app:assembleRelease`는 서명 설정이 없어 **unsigned APK**(약 25MB)만 만든다. 설치하려면 서명이 필요하므로 빌드 파일을 건드리지 않고 `zipalign` + `apksigner`로 서명한다. 아래 명령은 임시 키로 서명·검증까지 확인했다.

## 1. 키스토어 (1회, 저장소 밖에 보관)

2026-11-15 생성 완료: `~/pleos-release.jks`(별칭 `familystop`, 인증서 `CN=FamilyStop`), 비밀번호는 macOS 키체인 항목 `pleos-release-keystore`에만 있다. 새로 만들 때만 아래 명령을 쓴다.

```bash
keytool -genkeypair -keystore ~/pleos-release.jks -alias familystop \
  -keyalg RSA -keysize 2048 -validity 10000
```

키스토어와 비밀번호는 저장소에 커밋하지 않는다. 인증서 이름(`dname`)에 실명을 넣지 않는다(양식: 개발자 실명 노출 금지) — 프롬프트에 팀명 또는 가명을 쓴다. 같은 키로 서명해야 같은 앱으로 갱신된다. 선정 후 최종 APK(전시용)도 같은 키를 쓴다.

## 2. 빌드·서명

```bash
./gradlew :app:assembleRelease
BT=$(ls -d $ANDROID_HOME/build-tools/* | sort -V | tail -1)
mkdir -p apk
$BT/zipalign -f -P 16 4 app/build/outputs/apk/release/app-release-unsigned.apk build/aligned.apk
KS_PW="$(security find-generic-password -s pleos-release-keystore -w)" \
  $BT/apksigner sign --ks ~/pleos-release.jks --ks-key-alias familystop --ks-pass env:KS_PW --key-pass env:KS_PW \
  --out apk/FamilyStop_FamilyStop_release.apk build/aligned.apk
rm -f apk/*.idsig
$BT/apksigner verify --verbose apk/FamilyStop_FamilyStop_release.apk   # v3 true 이어야 함
```

파일명은 `팀명_앱이름_release.apk` 규칙에 따라 `FamilyStop_FamilyStop_release.apk`다 (팀명 FamilyStop, 앱명 Family Stop).

## 3. 설치 확인

```bash
adb install -r -g apk/FamilyStop_FamilyStop_release.apk
adb shell am start -n ai.familystop.app/ai.familystop.MainActivity
```

debug APK로 설치한 에뮬레이터에는 서명이 달라 `adb install -r`이 실패한다. 먼저 `adb uninstall ai.familystop.app`.

## 주의

- release 빌드는 R8 minify·shrink가 켜져 있다(#105, `app/proguard-rules.pro`). CI `gate`가 `assembleRelease`로 누락 클래스를 잡는다. 서명 후 `scripts/e2e-demo.sh`로 확인하고, 시연 전에 `adb shell cmd package compile -m speed -f ai.familystop.app`를 실행한다(`docs/agents/emulator.md`).
- 1차 제출본은 2026-11-15 main 기준으로 빌드·서명했다. 코드를 바꾸면 다시 빌드·서명해 같은 파일명으로 교체한다.
- `versionCode`/`versionName`은 제출 전 확인(현재 1 / 0.1.0).
