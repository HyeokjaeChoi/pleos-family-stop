# 현대·기아 차량용 앱 개발 공모전 공식 요강 (조사일 2026-11-07, 읽기 전용)

핵심: 외부 API·외부 SDK(지도/POI)·유료 서비스 허용/금지 규정은 공식 사이트에서 찾지 못함(미확인). Pleos SDK 사용을 명문 의무로 적진 않았지만 "기술 활용성"이 필수 평가 항목이라 사실상 전제. 마감 시각(시:분)도 미확인(날짜 11-15만 명시).

기준 URL: https://appdevchallenge.auto-hmg.io  (하위: /guide/overview, /guide/schedule, /guide/submission, /guide/development, /guide/judging-awards, /guide/rules, /guide/shortform, /guide/help, /faq, /notice)
공지: /notice/c0a1a000-2026-4923-8000-000000000006 (출품 유의사항), ...0001 (모집공고), ...0005 (설명회)
조사 방법: aside exec 에이전트가 사이트 열람. 접수양식 DOCX는 열지 않음(다운로드 필요). 아래는 원문 요약(저작권상 인용 최소화).

## 1. 제출물·방법·마감  (/guide/submission, /faq, /guide/rules)
| 항목 | 내용 |
|---|---|
| 기획서(접수양식) | 필수. 인용: "접수양식이 곧 기획서입니다." 항목 5번 '개발 계획'에 Pleos Connect 구현 방식·개발 범위·SDK/API 사용계획 작성. 팀원정보, 개인정보 동의·서명·날짜, 숏폼 링크란 있음. 전체 항목은 미확인 |
| 시연 영상 | 선택. 목업·프로토타입·화면녹화 인정 |
| GitHub 저장소 | 선택. 소스+APK 포함. Private 가능(공지된 심사용 계정을 Collaborator 초대, 마감 전 접근 확인; 계정명 미확인). 개인계정 private은 Collaborator에 쓰기권한도 부여되니 확인 |
| APK | 선택. GitHub 저장소 `apk/` 폴더에 `팀명_앱이름_release.apk`(release 빌드). APK 단독 제출 불가. 용량 초과 시 GitHub Releases 링크 제출 |
| README.md | 앱명·한줄소개, 실행방법, 테스트 환경·설정, APK 위치/Release 링크 |
| 가산점 | 시연영상·GitHub·APK 제출 수준에 따라 "단계별 가산점". 구체 점수/단계 기준 미확인. APK 단독 가산 조건은 명시 없음 |
| 숏폼 | 가산점 없음, 별도 특별상 50만원 |
| 제출 방법 | 이메일 appdevchallenge@hyundai-partners.com. 제목 `[차량용 앱 개발 공모전] 팀명(개인은 성명)_대표자명`. 팀원 전원(대표 포함 최대 5인) 동의·서명, 대표자가 전원 CC |
| 수정 제출 | 마감 전, 제목 앞에 `[수정제출]`, 전체 자료+서명 동의서 재첨부, 마지막 제출본이 최종본 |
| 마감 | 접수 2026.10.21(수)~11.15(일), KST. 마감 시각 미확인. 마감 이후 제출 서류는 불인정(공지) |
| DOCX | https://appdevchallenge.auto-hmg.io/downloads/contest-application-consent.docx , 파일명 contest-application-consent.docx, 55,208 bytes(약 54KB). 다운로드 안 함 |

## 2. 심사 (/guide/judging-awards, /faq, 공지)
| 항목 | 내용 |
|---|---|
| 1차 서류심사 | 4개 필수 평가항목, 세부 배점 비공개 |
| 차량특화성 | 모바일 대비 차량 환경 고유 가치, 주행·정차 맥락·차량 기능과 결합 (공식) |
| 창의성 | 독창성, 운전자·동승자 Pain Points & Needs 정의와 해결 (공식) |
| 기술 활용성 | Pleos Playground 제공 SDK·API를 핵심 기능·사용 경험에 적절히 활용했는가 (공식) |
| 사업성 | 타깃 고객, 예상 MAU, Retention, 확대 계획 등 (공식) |
| 가산점 | 개발 산출물(시연영상, GitHub+APK), 선택 |
| 2차 PT 기준 | "추후 안내 예정" - 미확인 |
| 규모 | 1차 합격 20팀, 최종 10팀, 총상금 5,100만원 |

## 3. SDK/API·외부 서비스 (/guide/development, /faq)
| 항목 | 내용 |
|---|---|
| Pleos SDK 사용 | 명문 의무 문장 없음. 기술 활용성이 필수 평가항목 + 기획서에 SDK/API 사용계획 기재 -> 사실상 전제(해석) |
| 제공 SDK 5종 | Vehicle, NaviHelper, TextToSpeech, SpeechToText, FusedLocation. 제공 상태·사용 조건은 공식 문서 참조(외부 문서는 미열람, 미확인) |
| 개발 환경 | Android API 28+, Gradle 8.0+, Java/Kotlin, Pleos Connect SDK |
| 외부 API/SDK(지도·POI)/유료 서비스 | 허용·제한 규정 미확인. 사이트에 언급 없음. 마감 전 이메일 문의 권장 |
| 주제 | 예시(음성메모, 안전운전 점수, 배달기사 앱)는 참고용, 주제 제한 없음 |

## 4. 2차 PT·시연·IP (/guide/schedule, 공지 ...0006)
| 항목 | 내용 |
|---|---|
| 일정·장소 | 2026.12.03~04, 오프라인, 현대자동차 강남대로사옥 1층 UX스튜디오 서울 |
| 시연 환경 | 실제 동작 앱 시연 필수: 에뮬레이터 구동 화면 또는 기기 시연. 실차 필수 언급 없음. 주최측 제공 장비 미확인 |
| 산출물 | 1차 제출물은 PT 전까지 자유롭게 고도화 가능. PT 제출·시연 결과 기준으로 순위 확정 |
| 선정 후 | 10팀 전시용 최종 APK 필수, 2027.02 그룹 행사 시연·시상식 참석 의무. Pleos App Market 탑재 준비 지원(입점 보장 없음) |
| 지식재산권 | 저작권·디자인권 등은 응모자 귀속. 수상작에 대해 주최측이 통상실시권을 가질 수 있음(조건 협의), 결과발표일로부터 4개월 우선협상권 가질 수 있음. 주최측 비영리 복제·전송 및 수익 창출 이용 가능, 범위 초과 시 별도 합의 |
| 기타 조건 | 순수 창작물, 제3자 권리 비침해, 타 공모전 수상작 제외, 현대차그룹 임직원 참가 가능·시상 제외, 제출자료 2027.02.28 이후 폐기(수상작 2년 보관) |
