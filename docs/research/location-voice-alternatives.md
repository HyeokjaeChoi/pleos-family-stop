# 위치·음성 대체 수단 조사 (2026-11-07, 읽기 전용)

방법: `aside guide` 후 `aside exec` 3건(공개 문서 열람만, 로그인·제출·다운로드·설치 없음). 기존 노트(aaos-dev-notes.md, sdk-facts.md)와 중복되는 SDK 기본 사실은 생략.
표기: 확인 = 페이지에서 직접 확인 / 미확인 = 못 찾음(추측 안 함). Pleos 기준 `https://document.pleos.ai/en/docs/pleos-only/`(= P). `/ko`는 이번에 미조사.

## 핵심 결론
- GMS/GAS 탑재: 미확인. Pleos 문서가 "Pleos Connect non-GAS AAOS 환경"이라고 한 줄 언급(확인). 에뮬레이터 문서에는 Google/Play Store 언급 없음. 뉴스·보도자료에도 Google built-in 탑재 근거 없음.
- `FusedLocationProviderClient`: 사용 가능 여부 미확인(Pleos 문서에 해당 클래스·LocationManager 언급 0건). non-GAS이면 불가능할 가능성이 높으나 이는 추정. 에뮬레이터 실측 필요.
- 내장 음성 대체: Gleo AI(42dot LLM 기반) 내장(확인). 서드파티는 Gleo Interface(App Schema, function calling)로 연동 가능, 문서 공개, Pleos 스키마 심사 필요(확인). 플랫폼 `TextToSpeech`/`SpeechRecognizer` 동작 여부는 미확인.

## 1. 위치 (FusedLocation 대체)

### 1-1. Pleos 문서
| 사실 | 출처 | 확인 |
|---|---|---|
| "Pleos Connect non-GAS AAOS 환경에서는 Google 기반 앱 데이터 자동 백업을 현재 사용하지 않음"(보안 가이드 한 줄) | P app-registration-guide/app-security-guide | 확인(1줄, 맥락은 백업 항목) |
| SDK는 "Google Android Automotive OS(AAOS) 기반" | P connect-sdk-pleos/ | 확인 |
| 결제 마이그레이션 페이지: Google Play Billing 등 외부 의존성 제거, 차량 전용 환경에 맞추라고 안내 | P in-prep/_payment-system-migration | 확인 |
| 에뮬레이터 = "Pleos Connect System Image" API 34, v2.1.0, Automotive. 에뮬 설치·설정 페이지에 GMS/Play Store/Google 언급 없음 | https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator , .../configure-pleos-connect-emulator | 확인(언급 없음) / 에뮬의 GMS 탑재 여부 자체는 미확인 |
| 영문 문서 106개 페이지(connect-mobile·API reference·디자인 컴포넌트 제외)에서 `FusedLocationProviderClient`, `LocationManager`, `GPS_PROVIDER`, `FUSED_PROVIDER` 검색 결과 0건 | document.pleos.ai/en/docs 전체 | 확인(부재). 단 `/ko` 미조사 |
| Pleos FusedLocation SDK는 별도 아티팩트 `ai.pleos.playground:FusedLocation:1.1.2`, 권한 `pleos.car.permission.FUSED_LOCATION`(dangerous) | P connect-sdk-pleos/FusedLocation/project-setup, .../declaring-permissions | 확인 |
| `ACCESS_COARSE/FINE/BACKGROUND_LOCATION` 동작은 "별도로 보장되지 않음". "Google Fused Location SDK 문서를 Pleos Connect 동작 보장으로 해석하지 말 것" | P connect-sdk-pleos/FusedLocation/intro | 확인 |
| 위치 API 사용 전 `isSupported()` 호출, 미지원이면 위치 기능 비활성/숨김. `getLastKnownLocation`은 새 fix 계산 안 함, 콜백 null 가능 | 위 intro | 확인 |
| 차량 개인정보 설정으로 위치가 차단되면 `showLocationAccessBlockedPopup()` | P .../FusedLocation/declaring-permissions | 확인 |
| 지도/내비 앱: 위치 권한 필수, 최소 권한만 허용, 마이크는 음성 입력용만, 불필요 권한은 반려. 위치 데이터 암호화·HTTPS·개인정보처리방침 필수, 불필요한 위치 저장 금지, 비공식 센서/API 사용 금지 | P vehicle-app-developer-guide/navigation-app-development-guide | 확인 |

### 1-2. Android 공식 문서
| 사실 | 출처 | 확인 |
|---|---|---|
| Google built-in 차량은 Google Play, Gemini, Assistant, Maps 포함 | https://developer.android.com/training/cars/platforms/automotive-os | 확인 |
| AAOS에서 `LocationManager`와 Play services `FusedLocationProviderClient`(FLP) 둘 다 선택지. "많은 자동차 앱"이 FLP 사용 | https://developer.android.com/training/cars/platforms/automotive-os/coarse-location | 확인 |
| `GPS_PROVIDER`/`NETWORK_PROVIDER`로 정밀 위치를 받으려면 `ACCESS_FINE_LOCATION` 필요. 거친 권한만 있어도 `GPS_PROVIDER`는 동작하나 프레임워크가 정확도를 낮춤 | 위와 동일 | 확인 |
| `FUSED_PROVIDER`는 Android 12(API 31)+에서 `LocationManager`로 접근 가능(이전엔 Play services 경유만) | 위와 동일, https://developer.android.com/reference/android/location/LocationManager ("Added in API level 31") | 확인 |
| AAOS의 FLP는 내부적으로 `GPS_PROVIDER`를 소스로 사용 | 위 coarse-location | 확인 |
| 많은 AAOS 기기에 `android.hardware.location.network`가 없어 `required="false"` 선언 | 위와 동일 | 확인 |
| `com.google.android.gms.location`은 Google built-in AAOS 차량용 Google API 목록에 포함 | https://developer.android.com/training/cars/platforms/automotive-os/google-play/google-services | 확인 |
| 공식 AAOS 에뮬 "Android Automotive with Google APIs"(API 33~35) 이미지는 표에서 Play Store 항목 없음, "Google Play" 이미지는 Play Store 표시. 일반 이미지에 Play services 포함이라고 서술 | https://developer.android.com/training/cars/testing/emulator | 확인(표에는 Play Store 여부만 표기) |
| GAS 미탑재 AAOS 기기에서 FLP 동작 | 못 찾음 | 미확인 |
| Pleos Connect에서 `LocationManager`의 `GPS_PROVIDER`/`FUSED_PROVIDER` 노출 여부 | 못 찾음 | 미확인 |

### 1-3. Pleos 심사: 위치·개인정보
| 사실 | 출처 | 확인 |
|---|---|---|
| 권한 최소화, 법적 고지(수집 항목·목적·보유기간·거부권)와 명시적 동의 절차, 이해하기 쉬운 개인정보처리방침 | P distribute/app-review-guide/review-user-privacy-security | 확인 |
| "위치 데이터를 저장·이용하는 서비스는 서비스 국가 법령상 등록/신고 확인을 요구받을 수 있음", 서류 허위·불충족 시 판매 중지 가능 | 위와 동일 | 확인 |
| 동의는 명확한 동의/거부 선택 필요(뒤로가기·빈 곳 탭은 동의 아님), 언제든 철회 가능, 철회 시 모든 데이터 경로 비활성·저장 데이터 즉시 삭제 | 위와 동일 | 확인 |
| 개인/위치 데이터 사용 앱은 이용약관·데이터 처리방침 제출 | P distribute/app-review-guide/review-app-registration-info | 확인 |
| 위치 필요 사유를 명확히 설명, 동의 흐름 단순화, 로그의 PII 마스킹 | P vehicle-app-planning-guide/security-privacy | 확인 |

참고: 요청 경로 `document.pleos.ai/en/distribute/...`는 404, 실제는 `/en/docs/pleos-only/distribute/app-review-guide/...`.

## 2. 음성 (STT/TTS 대체)

### 2-1. Gleo AI / Gleo Interface
| 사실 | 출처 | 확인 |
|---|---|---|
| Gleo AI = 개인화 모빌리티 어시스턴트, 대화로 답변·기능 실행. 음성 상태(Sleep/Listening/Thinking/Asking/Speaking) | https://document.pleos.ai/en/docs/connect/guide/docs-design/Experiences/gleo-ai | 확인 |
| Gleo AI는 "42dot의 LLM 기반"(Gemini 아님) | https://document.pleos.ai/en/docs/connect/guide/connect | 확인 |
| 제품 페이지 "Gleo AI" 섹션 제목: Speech SDK API Guide, Gleo AI SDK API Guide, App Integration with Gleo AI, LLM API guide, PEFT API Guide | https://document.pleos.ai/en/product-gleoai | 확인(제목만, 본문 미열람) |
| Gleo Interface = Gleo AI와 서드파티 앱이 차량 음성 명령을 처리하는 방식. function calling: LLM이 앱이 정의한 함수를 호출하고 앱 응답으로 답변 생성 | P app-integration-guide/guide/core-concepts | 확인 |
| 앱은 App Schema(JSON)로 호출 가능 함수 등록, `actionCalls` 최대 3개, 각각 `startActivity` 또는 `sendBroadcast`로 호출 | P app-integration-guide/guide/app-capability | 확인 |
| 앱의 Callback: `functionName`, `status`(success/asking/fail), `message`, 선택 `mode`(voice/beep/silent), 선택 `data` | P app-integration-guide/guide/action_callback | 확인 |
| 전달 절차: `assets/schema.json` + `AndroidManifest.xml`에 함수별 intent-filter action. Pleos가 스키마 심사, 승인 후 자동 활성화. Gleo AI가 부팅·설치/삭제 시 schema.json 스캔 | P app-integration-guide/guide/deliver-schema | 확인 |
| 공개 여부: 페이지 열람에 로그인 불필요(공개). 사용 조건 = 스키마 심사 + 가이드라인. "추가 안내는 문의" 문구, 가이드라인은 계속 갱신 예정 | 위 URL들 | 확인 |
| STT SDK는 텍스트 변환만, 음성 명령 실행은 Gleo Interface | P connect-sdk-pleos/SpeechToText/intro | 확인 |
| LLM SDK 존재(API명 `generateContent`, `startChat`, `sendMessage`, `registerApp`, `setServerType`, 온디바이스/서버 모드). 개요 페이지 본문 없음 | P connect-sdk-pleos/LLM/entry, https://document.pleos.ai/en/sitemap.xml | 확인(API명만). 참고: 에뮬 2.1.0에서 LLM SDK 지원 제거(기존 노트) |
| Google Assistant/Gemini가 Pleos Connect에 있다는 문서 | 못 찾음 | 미확인 |
| 서드파티용 `VoiceInteractionService` 훅 | 못 찾음 | 미확인 |

### 2-2. 플랫폼 TextToSpeech / SpeechRecognizer
| 사실 | 출처 | 확인 |
|---|---|---|
| `android.speech.tts.TextToSpeech`, `SpeechRecognizer`의 Pleos Connect/에뮬 동작(엔진·인식 서비스 탑재 여부) | Pleos 문서에 없음 | 미확인 |
| Pleos 에뮬이 TTS/STT SDK를 지원하는지: ON_DEVICE 오류가 "현재 모드나 런타임 환경"에서 날 수 있다는 서술만 있고 에뮬 언급 없음 | P connect-sdk-pleos/SpeechToText/error-code | 미확인 |
| AAOS VIA는 `VoiceInteractionService`와 `RecognitionService`를 둘 다 포함해야 하며 앱은 `SpeechRecognizer` API로 사용. 이미지별 탑재는 OEM 몫 | https://source.android.com/docs/automotive/voice/voice_interaction_guide | 확인(AAOS 일반) |
| VIA는 `TextToSpeech` API 구현이 선택 사항, `BIND_VOICE_INTERACTION`은 시스템 서명 권한 | https://source.android.com/docs/automotive/voice/voice_interaction_guide/app_development | 확인(AAOS 일반) |
| Pleos TTS/STT SDK v3.3.1, HYBRID(기본)/SERVER/ON_DEVICE, ON_DEVICE는 네트워크·등록 불필요. 릴리스 2026-10-19 | P release-note/sdk-release-note, P .../TextToSpeech/app-registration | 확인 |

## 3. Google built-in 탑재 여부 (뉴스/공식 발표)
| 사실 | 출처 | 확인 |
|---|---|---|
| 현대차그룹 보도(2026-04-29~30): Pleos Connect를 신형 그랜저(국내, 5월 출시), 유럽 첫 모델 IONIQ 3에 탑재, 2030년까지 약 2천만 대 목표. Gleo AI는 LLM 기반 음성 어시스턴트. 기사에서 Google/Gemini/GAS 언급은 스캔상 없음 | https://www.hyundai.com/worldwide/en/newsroom/detail/0000001168 | 확인(스캔 범위 내) |
| 2027 TUCSON이 미국 첫 Pleos Connect 적용 차종. "AAOS 기반", Gleo AI·Pleos App Market, 폰 미러링 없이 네이티브 서드파티 앱 | https://www.hyundai.com/worldwide/en/newsroom/detail/0000001280 | 확인 |
| WardsAuto(2026-10-06): "Google의 오픈 AAOS 기반". 내비는 Here Maps + Google Places, 무선 CarPlay/Android Auto 지원. 발췌에 Google Maps/Assistant/Gemini/Google Play 언급 없음 | https://www.wardsauto.com/news/hyundais-pleos-connect-infotainment-debuts-for-us-in-2027-tucson/832162 | 확인(검색 발췌만) |
| Pleos 25(2025-03-28): Pleos Connect 2026년 2분기 탑재 예정. Google 직원이 "AAOS와 Google 서비스 통합" 발표(컨퍼런스 세션이며 GAS 탑재 발표 아님) | https://www.hyundai.com/worldwide/en/newsroom/detail/0000000921 | 확인 |
| Hyundai Connected Mobility: "Android Automotive OS 기반", Pleos App Market·Pleos ID. Google built-in 언급 없음 | https://connected-mobility.hyundai.com/what-we-do/pleos | 확인 |
| Google "Cars with Google built-in" 정의: Gemini, Assistant, Maps, Play 제공 차종은 제조사별 상이 | https://support.google.com/built-in/answer/9858395 | 정의만 확인 / Hyundai·Kia·Genesis Pleos 포함은 미확인 |
| Electribe(2026-07-30, 2차 출처): IONIQ 3에 Pleos Connect, 앱스토어·Gleo AI | https://www.electribe.io/news/hyundais-pleos-connect-brings-app-stores-and-ai-to-the-ioniq-3-ee42a32e | 확인(2차) |
| Pleos Connect가 GAS/Play Store/Google Maps/Assistant/Gemini와 함께 출시된다는 발표, Hyundai-Google Gemini 제휴 발표 | 못 찾음 | 미확인 |
| Kia/Genesis 실차의 Pleos Connect 탑재 | 2030 목표 언급 외 못 찾음 | 미확인 |

한계: 한국어 보도·`/ko` 문서·sitemap 전수는 미조사. Gleo Interface 하위 페이지의 세부 제약(심사 기준, 비용)은 미확인.

## 추천 대체 경로 (사실 근거만)
1. 위치: Pleos 문서가 지원 경로로 제시하는 것은 Pleos FusedLocation SDK뿐이고 "준비 중" 상태가 리스크. Pleos 이미지 에뮬레이터에서 `LocationManager`(`GPS_PROVIDER`, API 31+ `FUSED_PROVIDER`)가 위치를 주는지와 `FusedLocationProviderClient` 클래스 존재 여부를 직접 실측해야 함(AAOS 공식 문서상 `LocationManager`는 유효한 경로, Pleos는 보장 문구 없음). 미지원이면 `isSupported()` 분기로 위치 기능 비활성 폴백 설계.
2. 음성 출력/입력: Pleos TTS/STT SDK의 `ON_DEVICE` 모드(등록·네트워크 불필요)가 "준비 중"이 아닌지 먼저 확인. 대안 실측은 `TextToSpeech.getEngines()`, `SpeechRecognizer.isRecognitionAvailable()`.
3. 음성 명령: Gleo Interface(App Schema, `actionCalls` 최대 3개, Pleos 스키마 심사)는 공개 문서가 있어 서드파티 음성 연동의 공식 경로. 단 심사 승인 필요, 에뮬 동작 여부는 미확인이라 Pleos 문의(문서의 "contact us") 권장.
