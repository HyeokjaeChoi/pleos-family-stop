# Agent Instructions

## Code layout

Multi-module Gradle (ADR-0006): `:app` (shell, `AppGraph`, `MainActivity`), `:core:model`, `:core:domain` (Ports), `:core:testing` (fakes), `:core:designsystem`, `:feature:<name>`, `:impl:pleos`, `:impl:platform`. Package names stay `ai.familystop.*`. A new feature = new `:feature:<name>` module. Features never depend on other features or impls; impls never depend on other impls; `:app` wires everything. Pleos SDK deps live only in `:impl:pleos`. Run everything with `./gradlew ktlintCheck testDebugUnitTest :app:assembleDebug`.

## Agent skills

### Issue tracker

GitHub Issues (`HyeokjaeChoi/pleos-family-stop`) + GitHub Projects kanban board (#3). See `docs/agents/issue-tracker.md`. No direct push to `main`: issue branch `<n>-<slug>` in a worktree → PR (`Closes #n`) → required check `gate` (ktlint + unit test + assemble).

### Triage labels

Default five labels, unchanged. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context (root `GLOSSARY.md` + `docs/adr/`). See `docs/agents/domain.md`.

### Design tokens

feature/app 화면은 `:core:designsystem`(`App.colors/type/space`, `AppCard`·`AppButton`·`StatusChip`)만 쓴다. 하드코딩 색·sp·dp는 `scripts/check-design-tokens.sh`(gate)가 막는다.

### Emulator / E2E

Pleos Connect AAOS 에뮬레이터(Apple Silicon) 셋업·E2E 절차와 함정: `docs/agents/emulator.md`.

### Figma gate

UI 화면 구현·수정(문구 수정 포함)은 Figma 선행 필수, 이슈에 `figma-done` 라벨 전에는 UI 파일 편집 불가 (hook + CI `design-gate`). 상세: `docs/agents/figma-gate.md`.

### 인수인계

현재 상태·결정·남은 일 스냅샷: `docs/agents/handoff.md` (날짜 기준, 이슈·PR이 우선).
