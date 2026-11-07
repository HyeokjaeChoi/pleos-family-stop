# Pleos My Project SDK 접근 상태 (2026-11-07, 콘솔 확인)

사용자가 My Project "Family Trip Stop Assistant"를 만들고, aside 세션이 콘솔 SDK 탭을 읽어 확인했다.

| SDK | 상태 | 신청 버튼 |
|---|---|---|
| Vehicle State (조회 11개, EV 배터리 잔량 포함) | 사용 가능 | 신청 불필요 |
| NaviHelper (경로 조회, 경로 탐색 설정, 경유지 최대 2개) | 사용 가능 | 신청 불필요 |
| FusedLocation | 준비 중 | 없음 |
| TextToSpeech | 준비 중 | 없음 |
| SpeechToText | 준비 중 | 없음 |
| Vehicle Control | 준비 중 | 없음 |

- 준비 중인 SDK의 오픈 시점은 콘솔과 문서 어디에도 없다.
- 문서상 STT/TTS `ON_DEVICE` 모드는 앱 등록 없이 동작한다고 되어 있으나, 콘솔의 "준비 중"과 어떻게 맞물리는지는 미확인이다(에뮬레이터 실측 필요).
