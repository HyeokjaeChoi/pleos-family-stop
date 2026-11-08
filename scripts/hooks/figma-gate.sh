#!/usr/bin/env bash
# Claude PreToolUse hook (Edit|Write): block UI edits until the branch's issue has label `figma-done`.
# stdin: hook JSON. exit 2 = block (stderr shown to Claude). See docs/agents/figma-gate.md.
here="$(cd "$(dirname "$0")" && pwd)"
file="$(sed -n 's/.*"file_path"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1)"
[ -n "$file" ] || exit 0
d="$(dirname "$file")"; while [ ! -d "$d" ]; do d="$(dirname "$d")"; done   # new files: nearest existing dir
root="$(git -C "$d" rev-parse --show-toplevel 2>/dev/null)" || exit 0
rel="$(git -C "$d" rev-parse --show-prefix)${file#"$d"/}"   # prefix-based: symlink-safe
cd "$root" || exit 0
"$here/is-ui-path.sh" "$rel" || exit 0

block() { echo "figma-gate: BLOCKED $rel — $1 (docs/agents/figma-gate.md)" >&2; exit 2; }
branch="$(git -C "$root" branch --show-current)"
n="${branch%%-*}"
case "$n" in ''|*[!0-9]*) block "branch '$branch' is not <issue#>-<slug>; create the issue and an issue branch first" ;; esac
labels="$(gh issue view "$n" --json labels --jq '.labels[].name' 2>&1)" || block "cannot read issue #$n labels: $labels"
echo "$labels" | grep -qx 'figma-done' || block "UI change needs Figma first: finish the Figma work, then add label figma-done to issue #$n"
exit 0
