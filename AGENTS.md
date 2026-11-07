# Agent Instructions

## Agent skills

### Issue tracker

GitHub Issues (`HyeokjaeChoi/pleos-family-stop`) + GitHub Projects kanban board (#3). See `docs/agents/issue-tracker.md`. No direct push to `main`: issue branch `<n>-<slug>` in a worktree → PR (`Closes #n`) → required check `gate` (ktlint + unit test + assemble).

### Triage labels

Default five labels, unchanged. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context (root `GLOSSARY.md` + `docs/adr/`). See `docs/agents/domain.md`.
