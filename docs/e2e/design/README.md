# 디자인 시스템 적용 에뮬레이터 스크린샷 (#45)

Pleos Connect AAOS 에뮬레이터, `adb shell cmd uimode night yes|no` + `scripts/vconsole.sh gear P|D`, `speed 30`.

| 파일 | 화면 | 확인 |
|---|---|---|
| `parked-input-night.png` / `-day.png` | 정차 · 입력(TripInputScreen) | 토큰 색(bg/base·surface·raised), border-less 필드·칩, 선택 칩 체크 아이콘, 큰 주 버튼 |
| `driving-night.png` / `-day.png` | 주행 · 후보 없음 안내(DriveScreen) | drive/body 32sp, bg/base |

미검증(한계): 주행 중 큰 카드 2개·UndoBar, 추천 비교, 폴백, 핸드오프, 음성 바, 동의/활동 화면은 에뮬레이터 위치 입력(Fake 위치 주입)이 없어 이번에 화면으로 캡처하지 못했다.
이들은 코드가 같은 컴포넌트(`AppCard`/`AppButton`/`StatusChip`)로 렌더링되며, Figma `Screen / Driving·Parked` 와의 픽셀 비교는 하지 않았다. 후속 E2E 세션에서 캡처 필요.
