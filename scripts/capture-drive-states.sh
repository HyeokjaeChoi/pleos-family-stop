#!/usr/bin/env bash
# #50: capture DriveScreen Idle/Undo/Failed/Pending/Empty (Night+Day) with a fake NaviGateway, so the
# UndoBar can be verified without Pleos Navi taking the foreground. Needs the Pleos emulator (docs/agents/emulator.md).
# Not `connectedAndroidTest`: AGP uninstalls the app afterwards and the PNGs go with it.
# Usage: scripts/capture-drive-states.sh [out_dir] [name_prefix]   (default docs/design-qa/shots/drive-states; prefix pulls only e.g. handoff-*)
set -euo pipefail
cd "$(dirname "$0")/.."
out=${1:-docs/design-qa/shots/drive-states}
prefix=${2:-}
./gradlew -q :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r -g app/build/outputs/apk/debug/app-debug.apk >/dev/null
adb install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk >/dev/null
U=$(adb shell am get-current-user | tr -d "\r") # Pleos runs the app as user 10, not 0
adb shell run-as ai.familystop.app --user "$U" rm -rf files/shots
adb shell am instrument -w ai.familystop.app.test/androidx.test.runner.AndroidJUnitRunner | tail -4
mkdir -p "$out"
for f in $(adb shell run-as ai.familystop.app --user "$U" ls files/shots | grep "^$prefix" | tr -d '\r'); do
  adb exec-out run-as ai.familystop.app --user "$U" cat "files/shots/$f" > "$out/$f"
done
echo "captured -> $out"
