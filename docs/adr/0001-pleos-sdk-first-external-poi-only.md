# Pleos SDK 우선, 외부 데이터는 시설(POI)만

공모전은 Pleos SDK 활용을 중요하게 보므로 위치(`FusedLocation`)·길 안내(`NaviHelper`)·EV 잔량(Vehicle `EvBatteryStateOfChargeListener`)·음성(STT/TTS)은 Pleos SDK만 쓰고 다른 위치 SDK는 쓰지 않는다. SDK 문서에서 식당·화장실·충전소 검색 API를 찾지 못했으므로(2026-11-07 조사) 시설 후보만 외부 제공처 + 직접 확인한 시연 구간 데이터(실제·가상 혼합)로 채우고, 화면에 출처(실제/가상)를 표시한다.

## Consequences

- 공모전 사이트 전체(가이드·FAQ·공지, 2026-11-07)에 외부 API·외부 SDK·유료 서비스 허용/금지 규정이 없다. Pleos SDK 사용 의무 문장도 없지만 '기술 활용성'이 필수 평가항목이고 기획서 '개발 계획'에 SDK/API 사용계획을 쓰게 되어 있어 SDK 활용이 사실상 전제다. 운영사 서면 답변을 받으면 이 ADR을 재검토한다.
- 출품작은 제3자 권리를 침해하면 안 되므로 외부 시설 데이터는 제공처 이용약관 범위 안에서만 쓴다.
- 추천 응답 목표 3s, 외부 API 타임아웃 5s는 잠정값이며 프로토타입 측정 후 확정한다(초과 시 데이터 미수신 대체 동작).
- Pleos 문서 스냅샷(2026-11-07 조사)에서 FusedLocation·STT·TTS가 My Project SDK 접근 상태 "Not Available Yet / Coming Soon"으로 보인다(Vehicle State·NaviHelper는 "Ready to Use"). My Project에서 실제 상태를 확인하기 전까지 위치·음성의 SDK 사용은 전제가 흔들린다. 근거: `docs/research/aaos-dev-notes.md`.
- 가상 시설 데이터는 공모전 시연 한정이다. Pleos 앱 마켓 심사는 더미 데이터를 허용하지 않으므로 입점 단계에서는 실제 데이터로 바꿔야 한다.
- 콘솔 확인 결과 FusedLocation이 "준비 중"이고, Pleos Connect는 non-GAS AAOS로 보여(`docs/research/location-voice-alternatives.md`) Google `FusedLocationProviderClient`도 쓸 수 없을 가능성이 높다(에뮬 실측 필요). 위치는 `LocationSource` 인터페이스 뒤에 두고, 지금은 Android 플랫폼 `LocationManager`(API 31+ `FUSED_PROVIDER`, 그 외 `GPS_PROVIDER`) 구현을 쓰며 Pleos FusedLocation이 열리면 그 구현으로 바꾼다. 플랫폼 API는 외부 SDK가 아니므로 이 ADR의 원칙과 충돌하지 않는다.
