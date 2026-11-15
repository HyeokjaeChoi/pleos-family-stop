# 제출 체크리스트 (이슈 #15)

마감 **2026-11-15 23:59 KST** (접수양식 원문 기준; 이메일 마감 이후 서류 불인정). 마감 전 수정 제출은 제목 앞 `[수정제출]`, 전체 자료 재첨부, 마지막 제출본이 최종본.

## 완료 (이 저장소)

- [x] 기획서 초안 → [proposal.md](proposal.md)
- [x] README (빌드·실행·에뮬레이터·구조)
- [x] release APK 빌드·서명 절차 → [release-apk.md](release-apk.md)
- [x] 시연 영상 시나리오 초안 → [demo-script.md](demo-script.md)

## 이 저장소에서 할 것 (#14 완료, 2026-11-15 갱신)

- [x] README "상태" 문단 갱신
- [ ] README APK 위치 기입 (APK 빌드 후)
- [ ] release APK 빌드·서명·`apk/팀명_앱이름_release.apk` 커밋 (또는 GitHub Releases). 키스토어와 팀명·앱명이 먼저 정해져야 한다
- [x] 에뮬레이터 E2E (debug 빌드): `scripts/e2e-demo.sh` 주간·야간 통과 (2026-11-14)
- [ ] release APK 설치·실행 확인 (서명 후)
- [ ] 시연 영상: E2E 녹화본은 로컬 `.e2e-final/`(저장소 제외)에 있다. [demo-script.md](demo-script.md) 기준 편집본 녹화·업로드와 링크 확보는 미완
- [ ] 기획서를 실제 구현에 맞게 수정. 낡은 줄:
  - 3절 "이동 상황 판별"의 "Vehicle SDK 기어로": 실제 판별은 AAOS `CarUxRestrictions`(`feature/drive/.../Movement.kt`)
  - 4절 5번 "충전이 끝날 때까지 쓸 수 있는 시간 안에서": #79 이후 남은 시간을 추정하지 않는다
  - 5절 SDK 표 Vehicle State·NaviHelper 행의 "앱 연결은 #14", "기어 판별은 … 앱 구현 예정": 연결은 끝났고 기어 판별은 쓰지 않는다. Vehicle State 용도에 충전 상태·주행 가능 거리가 빠져 있다

## 사용자 승인·직접 처리 필요 (이 세션은 하지 않음)

- [ ] 접수양식 DOCX 작성: 01 참가신청서, 03 개인정보 동의, 04 팀원별 서명 (이미 `~/Downloads/contest-application-consent.docx`에 있음). 기획서 본문(02)은 [proposal.md](proposal.md)를 옮겨 쓴다
- [ ] **결정 값 입력**: 팀명, 대표자명, 제출용 앱명, 참가 단위·부문. `팀명_앱이름_release.apk`·메일 제목 `[차량용 앱 개발 공모전] 팀명_대표자명`에 쓰인다
- [ ] **사업성 수치**: 예상 MAU·Retention 목표를 쓸지, "측정 후 설정"으로 쓸지 (현재 PRD에 목표값 없음)
- [ ] 외부 데이터 사용 허용 여부 운영사 문의 메일 (appdevchallenge@hyundai-partners.com) — 규정이 사이트에 없음. 문의할지, 위험을 안고 진행할지 결정 (ADR-0001)
- [ ] 키스토어 생성·비밀번호 보관 ([release-apk.md](release-apk.md) §1)
- [ ] 심사용 GitHub 계정 Collaborator 초대: 계정명을 공지에서 확인해야 함 (현재 미확인). private 저장소는 Collaborator에 쓰기 권한이 부여됨. 마감 전 접근 확인
- [ ] **실명 노출 점검**: 양식은 공유 링크·첨부에 개발자 실명 노출 금지를 요구한다. 저장소 URL(`HyeokjaeChoi/...`), 커밋 작성자, Figma 파일, 영상에 실명이 보이는지 확인하고 대응 결정
- [ ] 이메일 발송: 제목, 첨부(양식·영상·APK는 불가 시 링크), 팀 참가 시 팀원 전원 CC (CC 주소가 서로 보임을 팀원에게 안내). **발송은 사용자가 직접**
- [ ] 숏폼 링크 제출 여부 (가산점 없음, 별도 특별상)
