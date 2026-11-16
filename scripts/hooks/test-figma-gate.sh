#!/usr/bin/env bash
# Local tests for is-ui-path / figma-gate hook / ci design-gate with a fake `gh`. Run: scripts/hooks/test-figma-gate.sh
cd "$(dirname "$0")/../.." || exit 1
root="$PWD"; fails=0
bin="$(mktemp -d)"; trap 'rm -rf "$bin"' EXIT
cat > "$bin/gh" <<'G'
#!/usr/bin/env bash
case "$3" in 1) echo figma-done; echo bug ;; 2) echo bug ;; *) echo "not found" >&2; exit 1 ;; esac
G
chmod +x "$bin/gh"; export PATH="$bin:$PATH"
t() { # name expected-exit cmd...
  local name="$1" want="$2"; shift 2
  "$@" >/dev/null 2>&1; local got=$?
  [ "$got" = "$want" ] && echo "ok   $name" || { echo "FAIL $name (want $want got $got)"; fails=$((fails+1)); }
}
ui() { scripts/hooks/is-ui-path.sh "$1"; }
t ui-screen 0 ui feature/input/src/main/java/ai/familystop/feature/input/TripInputScreen.kt
t ui-compose-component 0 ui feature/voice/src/main/java/ai/familystop/feature/voice/VoiceBar.kt
t ui-designsystem 0 ui core/designsystem/src/main/java/Theme.kt
t ui-strings 0 ui app/src/main/res/values/strings.xml
t ui-strings-en 0 ui feature/fallback/src/main/res/values-en/strings.xml
t ui-plurals 0 ui feature/fallback/src/main/res/values/plurals.xml
t ui-plurals-en 0 ui feature/fallback/src/main/res/values-en/plurals.xml
t nonui-controller 1 ui feature/drive/src/main/java/ai/familystop/feature/drive/PickController.kt
t nonui-test 1 ui feature/input/src/test/java/TripInputScreenTest.kt
t nonui-md 1 ui docs/agents/figma-gate.md

hook() { # branch-less: run in a temp repo on branch $1 with file $2
  local d; d="$(mktemp -d)"; git -C "$d" init -q -b "$1"; mkdir -p "$d/$(dirname "$2")"; : > "$d/$2"
  (cd "$d" && printf '{"tool_input":{"file_path":"%s/%s"}}' "$d" "$2" | "$root/scripts/hooks/figma-gate.sh"); local rc=$?
  rm -rf "$d"; return $rc
}
S=feature/x/src/main/XScreen.kt
t hook-labeled-pass 0 hook 1-foo $S
t hook-unlabeled-block 2 hook 2-foo $S
t hook-no-issue-branch-block 2 hook main $S
t hook-unknown-issue-block 2 hook 99-foo $S
t hook-nonui-pass 0 hook main README.md

U=$'feature/a/FooScreen.kt\nREADME.md'
FIG='https://www.figma.com/design/AbC/Name?node-id=12-34&t=x'
gate() { printf '%s\n' "$2" | PR_BODY="$1" scripts/ci/design-gate.sh; }
t ci-no-ui-pass 0 gate "" README.md
t ci-ui-ok 0 gate "Closes #1 $FIG" "$U"
t ci-ui-no-figma 1 gate "Closes #1" "$U"
t ci-ui-figma-no-node 1 gate "Closes #1 https://figma.com/design/AbC/Name" "$U"
t ci-ui-no-label 1 gate "Closes #2 $FIG" "$U"
t ci-ui-second-closes-unlabeled 1 gate "Closes #1 Closes #2 $FIG" "$U"
t ci-ui-no-closes 1 gate "$FIG" "$U"
exit $fails
