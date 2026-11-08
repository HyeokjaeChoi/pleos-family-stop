# Figma gate (UI는 Figma 선행)

UI 화면 구현·수정은 **문구 수정 포함 예외 없이** Figma 작업이 먼저다. 이슈에 `figma-done` 라벨이 붙기 전에는 UI 파일을 고칠 수 없고, 라벨 없는 PR은 병합할 수 없다.

## UI 경로 (`scripts/hooks/is-ui-path.sh`가 유일한 정의)
- `core/designsystem/**`
- `**/res/values*/strings*.xml`
- `feature/**/*.kt`, `app/**/*.kt` 중 `@Composable`이 있는 파일 (새 파일은 이름에 `Screen`/`Bar` 포함)
- `src/test`, `src/androidTest`는 제외

## 절차
1. 이슈에 Figma 링크(`node-id` 포함)를 적는다.
2. Figma 작업 완료 후 이슈에 `figma-done` 라벨을 붙인다: `gh issue edit <n> --add-label figma-done`.
3. PR 본문에 `Closes #<n>`과 `figma.com/design/...?node-id=...` URL을 적는다.

## 게이트 두 겹
- **CI `design-gate`** (`.github/workflows/design-gate.yml`, `scripts/ci/design-gate.sh`): PR diff에 UI 경로가 있으면 (a) PR 본문에 `node-id` 있는 Figma design URL, (b) `Closes #n` 이슈의 `figma-done` 라벨을 요구. UI 변경이 없으면 통과. 라벨을 뒤늦게 붙였으면 PR 본문을 한 번 수정하거나 체크를 re-run.
- **Claude hook** (`.claude/settings.json` PreToolUse `Edit|Write` → `scripts/hooks/figma-gate.sh`): UI 경로 편집 시 브랜치 `<n>-<slug>`의 이슈 n에 `figma-done`이 없으면 exit 2로 차단. 브랜치에서 이슈 번호를 못 찾거나 `gh`로 라벨을 못 읽어도 차단.

## 알려진 우회 경로 (솔직히)
- hook은 `Edit`/`Write`만 본다. `Bash`(`sed -i`, `cat >`, `git apply` 등)·IDE·수동 편집은 hook을 통과한다. 그 경우 CI `design-gate`가 마지막 방어선.
- 파일 경로에 `@Composable`이 없는 UI 문구 파일(예: 문구 상수 전용 `.kt`)은 경로 규칙에 안 걸린다. 필요하면 `is-ui-path.sh`에 경로를 추가.
- 관리자는 `main` 보호의 `enforce_admins`가 꺼져 있어 required check를 우회해 병합할 수 있다 (의도적으로 남긴 통로).
- 라벨·PR 본문은 사람이 쓴다. Figma URL의 실재 여부는 검사하지 않는다 (선택 확장: `FIGMA_TOKEN`으로 Figma REST API에서 node 존재 확인, 현재 미구현).
- CI는 라벨 변경에 재실행되지 않는다 (위 re-run 필요).

## 테스트
`scripts/hooks/test-figma-gate.sh` — 가짜 `gh`로 hook/CI/경로 판별의 통과·실패 케이스 실행.

## required check 등록
`design-gate`를 `main` required checks에 추가하는 것은 UI 파일을 대량 수정 중인 PR이 정리된 뒤 별도로 한다 (`enforce_admins`는 켜지 않는다).
