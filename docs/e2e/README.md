# E2E 보고서: 통합 앱 (#14) — Pleos Connect AAOS 에뮬레이터

2026-11-08. 에뮬레이터 `Pleos_Connect_v2_1_0`(API 34, arm64) + `adb` + `android screen capture` + `scripts/vconsole.sh`. 빌드: main `e85b18a`(#41) + 이 PR의 `PleosNaviGateway` 수정. 데이터는 시연용 가상 시설(화면에 "가상 데이터" 표시).

## 결과

| # | 시나리오 | 결과 | 증거 |
|---|---|---|---|
| A | 정차(P) 입력 → 추천(3개 비교 카드) → 정차 지점 선택 → 길 안내 경유지 전달 → 결과 표시 | **PASS** | A1~A6. 내비에 경유지 "가상 휴게소 C" 반영(A5), 앱에 "경유지로 추가했어요"(A6) |
| B0 | 주행(D)인데 추천 없음 | PASS | B0 "정차 후 입력하면 추천 후보가 여기에 나타나요" |
| B | 주행 전환 → 큰 카드 2개만, 스크롤·펼치기 없음 → 한 번 탭 → 처리 중 카드 잠금 → "경유지 추가했습니다 / 취소 (N초)" | **PASS** | B2, B3, B4 (취소 카운트다운 표시). 두 번째 경유지는 아래 결함 1 수정 후 |
| B1 | 충전 필요 선택 시 "충전 중 활동" 탭 → 자동 확인 불가 안내·수동 입력 | PASS | B1 |
| C | 주행 후 정차 복귀 시 입력 초안 유지 | **PASS** | C3 (Busan, 23:30, 체크 유지) |
| C' | 기어 P 후 2초 안정화 | 미측정 | `vconsole.sh`가 PUT 뒤 2초 대기해 스크린샷으로 구분 불가. 로직은 `DriveLogicTest`가 검증 |
| D | 권한 거부 폴백 | **미검증** | `pm revoke` 후에도 다이얼로그 없이 위치가 수신됨(에뮬레이터 정책 추정). `FallbackTest` 단위 테스트만 근거 |
| D' | 위치 미수신 타임아웃 폴백 | 미검증 | 에뮬레이터가 위치를 계속 공급해 재현 못함 |
| E | 지표 동의: 운전자 확인 → 동의 → 철회 화면 → 철회 | **PASS** | E1~E4 (기록 파일 생성은 확인 못함) |
| V | 음성 안내 | 부분 | TTS 엔진 인증 오류(`UNAUTHENTICATED`)로 낭독 실패, 앱이 한 줄 상태("경유지 추가됨", "후보 2개 · 정차 후 확인")로 대체 |

## 발견한 결함과 조치

1. **경유지 요청 타임아웃 5초가 너무 짧음 → 성공한 추가를 실패로 처리** (수정됨, #41). 에뮬레이터는 추가 후 재탐색에 5~12초가 걸려 `onWaypointChanged(ADD)`가 늦게 오고, 앱은 실패로 표시한 뒤 정리용 remove로 경유지를 지웠다. 15초로 상향.
2. **두 번째 경유지 ADD 이벤트가 `index=FIRST`로 도착해 게이트웨이가 매칭하지 못함** (이 PR에서 수정). 요청은 `SECOND`인데 내비가 `FIRST`로 알린다. ADD는 인덱스 비교 없이 진행 중 요청을 완료하도록 변경(요청은 한 번에 하나).
3. **전제 조건**: 내비에 활성 경로(목적지)가 없으면 `CAUSE_SEARCH_ROUTE`로 경유지 추가 거절. 이때 "전달하지 못했어요 → 다시 시도"로 안내되지만 사유(경로 없음)는 구분하지 않는다. 후속 개선 여지.
4. **실패한 add + 실패한 remove가 슬롯을 고정**: 위 3의 상태에서 재시도하면 `waypoints full or duplicate`로 add가 나가지 않고 remove만 나간다(경로가 생긴 뒤 remove가 성공해야 해제). 경로 설정 후 재시도로 복구됨.
5. 주행 카드에 이미 경유지로 넣은 후보가 계속 표시된다(중복 제거 없음). 주행 화면과 정차 handoff의 슬롯 공유는 [#42](https://github.com/HyeokjaeChoi/pleos-family-stop/issues/42).

## 재현 절차 요약

```bash
./gradlew :app:assembleDebug && adb install -r -g app/build/outputs/apk/debug/app-debug.apk
scripts/vconsole.sh gear P; scripts/vconsole.sh soc 62
# 내비: 목적지(최근 목록의 DEST-Seoul) 설정 후 "안내 시작" — 경로가 없으면 경유지 추가가 거절된다
adb shell am start -n ai.familystop.app/ai.familystop.MainActivity
# 앱에서 목적지·시간·필요 입력 → 제출 → `adb emu geo fix 127.05 37.30`(제출 뒤에 다시 보내야 수신) → 후보 선택
```

입력란 첫 `adb shell input text`는 IME 초기화 때문에 누락될 수 있어 탭 후 1초 대기가 필요하다. 좌표는 2560x1440 기준.
