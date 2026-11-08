#!/usr/bin/env bash
# exit 0 if the repo-relative path ($1) is a UI path guarded by the Figma gate, else 1.
# UI = design system, string resources, or feature/app Kotlin that is/has a @Composable (new files: *Screen*/*Bar* name).
p="${1#./}"
case "$p" in
  */src/test/*|*/src/androidTest/*) exit 1 ;;
  core/designsystem/*) exit 0 ;;
  */res/values*/strings*.xml) exit 0 ;;
  feature/*.kt|app/*.kt)
    [ -f "$p" ] && grep -q '@Composable' "$p" && exit 0
    case "$(basename "$p")" in *Screen*|*Bar*) exit 0 ;; esac ;;
esac
exit 1
