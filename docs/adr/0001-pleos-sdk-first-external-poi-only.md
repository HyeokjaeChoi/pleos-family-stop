# Pleos SDK 우선, 외부 데이터는 시설(POI)만

공모전은 Pleos SDK 활용을 중요하게 보므로 위치(`FusedLocation`)·길 안내(`NaviHelper`)·EV 잔량(Vehicle `EvBatteryStateOfChargeListener`)·음성(STT/TTS)은 Pleos SDK만 쓰고 다른 위치 SDK는 쓰지 않는다. SDK 문서에서 식당·화장실·충전소 검색 API를 찾지 못했으므로(2026-11-07 조사) 시설 후보만 외부 제공처 + 직접 확인한 시연 구간 데이터(실제·가상 혼합)로 채우고, 화면에 출처(실제/가상)를 표시한다.

## Consequences

- 공모전 요강의 외부 API 사용 규정은 공식 원문에서 확인하지 못했다. 요강 원문(제출물 안내·심사 상세)을 확인하면 이 ADR을 재검토한다.
- 추천 응답 목표 3s, 외부 API 타임아웃 5s는 잠정값이며 프로토타입 측정 후 확정한다(초과 시 데이터 미수신 대체 동작).
