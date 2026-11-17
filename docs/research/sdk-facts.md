# Pleos Playground SDK / 공모전 사실 조사 (2026-11-07 기준, 읽기 전용)

핵심 요약: NaviHelper는 requestRoute / requestRouteWithWaypoints / addWaypoint(최대 2개) / cancelRoute 제공, 단 "경로 변경 사용자 확인 UI·원 목적지 유지"는 문서에 명시 없음(미확인). STT/TTS는 HYBRID/SERVER/ON_DEVICE 모드와 onError(ErrorType: TIMEOUT 등) 제공, 서버 전송·보존·내비 음성 우선순위는 미확인. 이동 상태(주행/정차) 전용 값과 시설 POI 검색 API는 못 찾음. EV는 EvBattery SoC 리스너 있음. 접수 마감 2026-11-15.

Base: https://document.pleos.ai/en/docs/pleos-only/ (이하 B). SDK 버전: NaviHelper v3.0.5, TTS/STT v3.3.1, Vehicle v2.3.2, FusedLocation v1.1.2.

## 1. NaviHelper
| 사실 | 출처 | 확인 |
|---|---|---|
| 목적지 설정: `naviHelper.requestRoute(RouteInfo(latitude, longitude, displayLatitude?, displayLongitude?, poiName, poiId, poiSubId, address))`; 조회 `getDestinationInfo()`→`onDestinationInfo`; 취소 `cancelRoute()`; 재설정은 requestRoute 재호출 | B connect-sdk-pleos/NaviHelper/destinations | 확인 |
| `requestRoute` 성공 호출 != 경로탐색 완료. 결과는 onRouteStarted / onRoutingCompleted / onDestinationChanged / onError 콜백. 조회 API는 값을 직접 반환하지 않고 콜백으로 전달 | 동일 | 확인 |
| 경유지: 최대 2개(WaypointIndex.FIRST/SECOND), `addWaypoint(RequestWaypointInfo(lat, lon, waypointIndex, poiId, poiName, poiSubId, address))`, `removeWaypoint(WaypointIndex)`, ALL로 전체 삭제, `getWaypointInfo()`→onWayPointInfo, 도착 onWayPointArrived | B .../NaviHelper/waypoint-settings | 확인 |
| 목적지+경유지 한 번에: `requestRouteWithWaypoints(RouteWithWaypointsRequest(destination: RouteInfo, waypoints: List<RequestWaypointInfo>))`, 권한 `pleos.car.permission.NAVI_ROUTE_SEARCH` | B .../NaviHelper/route-with-waypoints | 확인 |
| addWaypoint 시 "원 목적지가 유지된다"는 명시 문장 없음. 문서 예시는 requestRoute 후 addWaypoint 순서일 뿐 | waypoint-settings | 미확인(에뮬레이터 실측 필요) |
| 2026-11-16 에뮬레이터 실측(#107): `pleos.car.permission.NAVI_ROUTE_SEARCH` 없이는 `addWaypoint`가 실패한다(앱 `guardDenied`가 `SecurityException`을 삼켜 "길 안내에 전달하지 못했어요"로 보임). 같은 빌드에 권한만 추가하면 경유지 추가 성공(`scripts/e2e-demo.sh` 전 단계 OK). 매니페스트에 유지 | 에뮬레이터 실측 | 확인 |
| 경로 변경 "사용자 확인/취소 프롬프트" API: 문서에 없음. 취소는 `cancelRoute()`, 경로옵션 변경 `changeRouteOption(RouteOption)`, 재탐색 `requestReRoute()`. 활성 경로 있을 때만 호출 권장, 실제 콜백은 내비 앱 상태에 따라 다를 수 있음 | routes, event-listeners | 확인(확인 UI 없음은 미발견) |
| 에러: NaviErrorCode = CAUSE_LOW_BATTERY / CAUSE_SEARCH_ROUTE_OVER_DISTANCE / CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE / CAUSE_SEARCH_ROUTE. 초기 연결 실패는 `initialize(onConnected, onFailed)` | NaviHelper/error-handling | 확인 |
| 기타 이벤트: onNavigationStatus, onDrivingInfo, onTBTInfo, onChargerOperatorInfo, onTripInfo, onRouteStateInfo, onCurrentLocationInfo, onBookmarkInfo, onRecentDestinationInfo 등. 전달 이벤트는 내비 앱 구현/상태에 따라 다를 수 있음 | NaviHelper/event-listeners | 확인(필드 상세 미확인) |

## 2. SpeechToText / TextToSpeech
| 사실 | 출처 | 확인 |
|---|---|---|
| 둘 다 SDK로 제공(v3.3.1). 모드 HYBRID(기본, 서버 우선+네트워크 실패 시 온디바이스) / SERVER / ON_DEVICE(네트워크·앱등록 불필요). 서버 모드는 Pleos Playground My Project 자격증명으로 앱 등록 필요 | B .../TextToSpeech/intro, .../SpeechToText/intro | 확인 |
| STT는 텍스트 변환만, 음성 명령 실행 안 함(실행은 Gleo Interface) | SpeechToText/intro | 확인 |
| STT API: initialize, addListener(ResultListener), setLocale, request(), stop(), sendAudio(음성 데이터 전달 페이지 존재), release. 콜백 onReady/onStartedRecognition/onUpdated(stt, completed)/onEndedRecognition/onError(ErrorType) | SpeechToText/result-listeners, start-and-stop | 확인 |
| TTS API: initialize, addEventListener(onReady/onStart/onUpdatedRms/onDone/onStop/onError), setLocale, `speak(text, isFinal)`(분할 전송 가능), stop(). 서버 모드는 OnServerConnectionListener.onConnected() 이후 speak | TextToSpeech/start-and-stop, event-listeners | 확인 |
| ErrorType: AUDIO_ERROR, INVALID_REQUEST_ERROR, MIC_ERROR, NETWORK_ERROR, NOT_SUPPORTED_API, ON_DEVICE_ENGINE_ERROR, PERMISSION_DENIED, SERVER_INTERNAL_ERROR, TIMEOUT, UNAUTHENTICATED, (TTS) UNAVAILABLE_LANGUAGE, UNKNOWN. TIMEOUT = "시간 내 미완료". 구체 timeout 초 단위 값은 없음 | TextToSpeech/error-code, SpeechToText/error-code | 확인 / 시간값 미확인 |
| 서버 모드에서 음성/텍스트가 외부(서버)로 전송되는지·보존 기간: 문서에 명시 없음. 서버 기반 스트리밍 방식이라는 서술만 있음 | intro 페이지들 | 미확인(전송 자체는 서버 모드 서술로 추정되나 보존 조건은 미확인) |
| 내비 음성과의 우선순위/오디오 포커스: v3.3.1 가이드에 없음. 구버전 검색 스니펫에 `setAudioAttributes`(기본 CONTENT_TYPE_MEDIA) 언급, 현재 가이드에서는 확인 못함 | search snippet pleos.ai/.../connect-sdk/TextToSpeech/intro (구버전) | 미확인 |
| 개인정보/위치 이용 앱은 약관·처리방침 제출, SDK 오남용·무단 사용자 데이터 사용 금지 | B distribute/app-review-guide | 확인 |

## 3. 이동 상태 / EV
| 사실 | 출처 | 확인 |
|---|---|---|
| Vehicle SDK 도메인: Brake, CarInfo, Display, Door, DrivingMode, EvBattery, Fuel, HVAC, Light, Odometer, Safety, Seat, SideMirror, Steeringwheel, Tire, Toll, TurnSignal, Window, Wiper. 조회/콜백(register*) 모두 비동기, capability 확인 API(check*Capabilities)로 지원 여부 확인 | B connect-sdk-pleos/Vehicle/intro | 확인 |
| 이동 상태: 리스너 목록에 BrakePedalStatusListener, DrivingModeStateListener, CruiseControlState/TargetSpeedListener, DistanceDriven/DrivingTime/TripDriving* 있음. "주행 중/정차" 또는 기어(P/D)·속도 값을 직접 주는 API는 목록에서 확인 못함(DrivingMode가 주행모드(ECO 등) 의미인지 "이동 여부"인지 미확인) | B .../Vehicle/vehicle/ai.pleos.playground.vehicle.listener/entry | 미확인(대체 후보: NaviHelper onDrivingInfo/onNavigationStatus, FusedLocation 위치 변화) |
| EV: EvBatteryStateOfChargeListener(SoC), EvBatteryEfficiencyListener, TripEvEfficiencyListener 존재. 권한 CAR_ENERGY(`pleos.car.permission.CAR_ENERGY`). "충전 중 여부" 전용 값은 이름만으론 미확인 | 위 listener 페이지, Vehicle/vehicle-permission | SoC 확인 / 충전중 미확인 |
| 권한: DrivingMode·Brake·Odometer=CAR_DRIVE. 승인되지 않은 권한 선언/호출 시 앱 심사 실패 | vehicle-permission, connect-sdk-pleos(Overview) | 확인 |
| FusedLocation: getLastKnownLocation, registerFusedLocationCallback, isSupported, proximityAlert. LocationData = 위도/경도/고도/방향/confidence/타임스탬프(속도 필드는 언급 없음). 권한 FUSED_LOCATION | FusedLocation/intro | 확인 |

## 4. 시설 검색/POI, 외부 API
| 사실 | 출처 | 확인 |
|---|---|---|
| SDK 5종(Vehicle, NaviHelper, STT, TTS, FusedLocation) 중 식당·화장실·충전소 검색 API 없음(문서상). NaviHelper에는 즐겨찾기/최근목적지 조회, 충전사업자 정보(onChargerOperatorInfo)만 있고 일반 시설 검색은 미발견 | Overview, NaviHelper/event-listeners | 미확인(없음으로 단정 불가, 미발견) |
| 외부 서버 연결 앱은 심사 전 서비스가 접근 가능해야 함(앱 제출 가이드). 외부 API 호출 timeout 권고값: 미발견 | B distribute/app-submission-guide | 일부 확인 / timeout 미확인 |
| 성능(메모리·CPU·배터리) 자원 효율, 운전자 주의분산 최소화(DDO) 가이드 준수 필요 | app-submission-guide, app-review-guide | 확인 |

## 5. 공모전 (공식 접수: https://appdevchallenge.auto-hmg.io, 보도: https://www.hyundaimotorgroup.com/ko/news/hyundai-motor-company-kia-vehicle-app-development-contest)
| 사실 | 출처 | 확인 |
|---|---|---|
| 일정(KST): 접수 2026-10-21~11-15, 온라인 설명회 10-28, 1차 서류 결과 11-23, 2차 최종 PT(오프라인) 12-03~04, 최종 발표 12-18, 전시·시상 2027-02. 변경은 공지사항 | appdevchallenge.auto-hmg.io | 확인 |
| 대상: 대학(원)생·개발자(스타트업 포함), 개인 또는 최대 5인 팀. 분야: 인카 엔터, 모빌리티 라이프 케어, 안전 운전, 패밀리·동승자 케어 등 | 공식 사이트, 현대차그룹 보도 | 확인 |
| 제출: 접수양식(기획서) 이메일 제출 appdevchallenge@hyundai-partners.com. 최종 선정 시 차량 환경 구현 앱 시연. 세부 파일 목록은 "제출물 안내" 링크로만 존재 | 공식 사이트, 현대차그룹 보도 | 확인 / 세부 미확인 |
| 평가 항목(차량특화성, 창의성, 기술 활용성, 사업성)과 가산점(시연 영상, GitHub 소스·APK), 접수양식 구성(한 줄 소개, 기획 배경, 주요 기능·차량특화 요소, 개발 계획) | 2차 자료(WebSearch 결과, 제3자 분석 블로그 계열) | 공식 원문 미확인(제3자 정보) |
| 총상금 5,100만원, 10개 팀(개별 금액 미공개). 당선작 개발자 행사 부스 운영 | 현대차그룹 보도 | 확인 |
| 외부 API·유료 서비스 사용 제한/비용 규정: 공식 사이트·보도자료 본문에 없음. SDK·API 활용 요건만 있음 | 위 두 곳 | 미확인(없음 확인 못함, "심사·시상 상세/제출물 안내" 링크 미열람) |
| 혼동 주의: "플레오스 커넥트 앱 아이디어 공모전"(부산모빌리티쇼)은 별개 | https://www.hyundaimotorgroup.com/ko/story/hyundai-motor-company-pleos-connect-app-idea-contest | 확인 |

## 한계
- 공식 공모 사이트의 "제출물 안내", "심사·시상 상세" 하위 링크 및 DOCX 접수양식은 열람하지 못함. 심사 배점·외부 API 규정은 여기서 확정 불가.
- SDK API Reference(Dokka)의 개별 시그니처/필드는 본문 텍스트가 거의 비어 있어 가이드 페이지 기준으로만 기재.
