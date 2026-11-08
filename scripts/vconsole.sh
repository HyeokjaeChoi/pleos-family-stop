#!/usr/bin/env bash
# Pleos Virtual Console helper (docs/agents/emulator.md §4). Usage: scripts/vconsole.sh <gear P|R|N|D | soc N | speed N | state>
# The console only accepts writes while a "browser session" heartbeat is alive (3 s timeout), so we beat in the background.
set -euo pipefail
B=http://127.0.0.1:8088
adb forward tcp:8088 tcp:8088 >/dev/null
curl -s -m3 -X POST "$B/browser/session?client=cli" >/dev/null
(while curl -s -m2 -X POST "$B/browser/session?client=cli" >/dev/null; do sleep 1; done) &
HB=$!; trap 'kill $HB 2>/dev/null' EXIT
put() { curl -s -X PUT "$B$1"; echo; }
case "${1:-state}" in
  gear) case "$2" in P) g=Park;; R) g=Reverse;; N) g=Neutral;; D) g=Drive;; *) echo "gear P|R|N|D"; exit 2;; esac
        put "/vehicle/gear?gear=$g" >/dev/null ;;
  soc) put "/vehicle/ev-battery?field=soc&value=$2" >/dev/null ;;
  speed) put "/vehicle/controls?speed_kph=$2" >/dev/null ;;
  state) ;;
  *) echo "usage: $0 gear P|R|N|D | soc 0-100 | speed kph | state"; exit 2 ;;
esac
sleep 2 # let the guest apply it before the heartbeat stops (a gear/speed set outlives the heartbeat; verify with state)
curl -s "$B/vehicle/state" | python3 -I -c "import json,sys;d=json.load(sys.stdin);print('gear',(d['gear'].get('actual') or {}).get('value'),'speed',d['controls']['speed_kph'],'soc',d['ev_battery']['state_of_charge'])"
echo "driving state: $(adb shell dumpsys car_service --services CarDrivingStateService | grep 'Current Driving' | tr -d '\r')  (0 parked, 1 idling, 2 moving)"
