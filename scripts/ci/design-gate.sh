#!/usr/bin/env bash
# CI design-gate. Env: PR_BODY. stdin: changed file paths (one per line). Needs `gh` (GH_TOKEN, GH_REPO).
# UI path changed => PR body needs a figma.com/design URL with node-id AND the Closes #n issue needs label figma-done.
here="$(cd "$(dirname "$0")/../hooks" && pwd)"
ui=""
while IFS= read -r f; do [ -n "$f" ] && "$here/is-ui-path.sh" "$f" && ui="$ui $f"; done
[ -n "$ui" ] || { echo "design-gate: no UI path changed — pass"; exit 0; }
echo "design-gate: UI paths changed:$ui"
fail() { echo "::error::design-gate: $1"; exit 1; }
printf '%s' "$PR_BODY" | grep -qiE 'figma\.com/design/[^[:space:])>]*[?&]node-id=[0-9]+[-:%A0-9a-fA-F]*' \
  || fail "PR body needs a Figma URL (figma.com/design/...?node-id=...)"
ns="$(printf '%s' "$PR_BODY" | grep -oiE '(close[sd]?|fix(e[sd])?|resolve[sd]?):? +#[0-9]+' | grep -oE '[0-9]+$')"
[ -n "$ns" ] || fail "PR body needs 'Closes #<issue>'"
for n in $ns; do
  labels="$(gh issue view "$n" --json labels --jq '.labels[].name')" || fail "cannot read issue #$n"
  echo "$labels" | grep -qx 'figma-done' || fail "issue #$n lacks label figma-done (re-run this check after labeling)"
done
echo "design-gate: pass (issues: $(echo $ns))"
