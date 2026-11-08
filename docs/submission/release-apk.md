# release APK 빌드·서명 절차

제출 규정: GitHub 저장소 `apk/` 폴더에 `팀명_앱이름_release.apk` (release 빌드). APK 단독 제출 불가. 용량이 너무 크면 GitHub Releases 링크를 제출한다 (`docs/research/contest-rules.md`).

2026-11-08 실측: `:app:assembleRelease`는 서명 설정이 없어 **unsigned APK**(약 25MB)만 만든다. 설치하려면 서명이 필요하므로 빌드 파일을 건드리지 않고 `zipalign` + `apksigner`로 서명한다. 아래 명령은 임시 키로 서명·검증까지 확인했다.

## 1. 키스토어 (1회, 저장소 밖에 보관)

```bash
keytool -genkeypair -keystore ~/pleos-release.jks -alias familystop \
  -keyalg RSA -keysize 2048 -validity 10000
```

키스토어와 비밀번호는 저장소에 커밋하지 않는다. 인증서 이름(`dname`)에 실명을 넣지 않는다(양식: 개발자 실명 노출 금지) — 프롬프트에 팀명 또는 가명을 쓴다. 같은 키로 서명해야 같은 앱으로 갱신된다. 선정 후 최종 APK(전시용)도 같은 키를 쓴다.

## 2. 빌드·서명

```bash
./gradlew :app:assembleRelease
BT=$ANDROID_HOME/build-tools/36.1.0
mkdir -p apk
$BT/zipalign -f -P 16 4 app/build/outputs/apk/release/app-release-unsigned.apk build/aligned.apk
$BT/apksigner sign --ks ~/pleos-release.jks --out "apk/팀명_앱이름_release.apk" build/aligned.apk
$BT/apksigner verify --verbose "apk/팀명_앱이름_release.apk"   # v3 true 이어야 함
```

`팀명_앱이름`은 확정 전 placeholder다 (`checklist.md`).

## 3. 설치 확인

```bash
adb install -r -g "apk/팀명_앱이름_release.apk"
adb shell am start -n ai.familystop.app/ai.familystop.MainActivity
```

debug APK로 설치한 에뮬레이터에는 서명이 달라 `adb install -r`이 실패한다. 먼저 `adb uninstall ai.familystop.app`.

## 주의

- release 빌드는 현재 minify·shrink 없음(`app/build.gradle.kts`에 `buildTypes` 없음). 용량 문제가 없어 그대로 둔다.
- #14(통합) 이후에 다시 빌드해야 실제 제출본이 된다. 지금 만든 APK는 fake 기반 셸이라 제출하지 않는다.
- `versionCode`/`versionName`은 제출 전 확인(현재 1 / 0.1.0).
