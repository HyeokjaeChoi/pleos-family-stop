#!/usr/bin/env bash
# Pleos Virtual Console helper (docs/agents/emulator.md §4). Usage: scripts/vconsole.sh <gear P|R|N|D | soc N | charge on|off | range KM | speed N | state>
# The console only accepts writes while a "browser session" heartbeat is alive (3 s timeout), so we beat in the background.
set -euo pipefail
B=http://127.0.0.1:8088
adb forward tcp:8088 tcp:8088 >/dev/null
curl -s -m3 -X POST "$B/browser/session?client=cli" >/dev/null
(while curl -s -m2 -X POST "$B/browser/session?client=cli" >/dev/null; do sleep 1; done) >/dev/null 2>&1 &
HB=$!; trap 'pkill -P $HB 2>/dev/null; kill $HB 2>/dev/null' EXIT
put() { # fail loudly: the console answers {"error":...} with a 200
  local r; r=$(curl -s -X PUT "$B$1")
  case "$r" in *'"error"'*) echo "console error: $r" >&2; exit 1;; esac
}
num() { [[ "${2:-}" =~ ^[0-9]+$ ]] && (( $2 <= $1 )) || { echo "value must be an integer 0..$1" >&2; exit 2; }; }
case "${1:-state}" in
  gear) case "${2:-}" in P) g=Park;; R) g=Reverse;; N) g=Neutral;; D) g=Drive;; *) echo "gear P|R|N|D"; exit 2;; esac
        put "/vehicle/gear?gear=$g" ;;
  soc) num 100 "${2:-}"; put "/vehicle/ev-battery?field=soc&value=$2" ;;
  charge) case "${2:-}" in on) v=true;; off) v=false;; *) echo "charge on|off"; exit 2;; esac
          put "/vehicle/ev-battery?field=charge_switch&value=$v" ;;
  range) num 1000 "${2:-}"; put "/vehicle/odometer?remaining_range_m=$(( $2 * 1000 ))" ;;
  speed) num 240 "${2:-}"; put "/vehicle/controls?speed_kph=$2" ;;
  state) ;;
  *) echo "usage: $0 gear P|R|N|D | soc 0-100 | charge on|off | range km | speed kph | state"; exit 2 ;;
esac
sleep 2 # let the guest apply it before the heartbeat stops (a gear/speed set outlives the heartbeat; verify with state)
curl -s "$B/vehicle/state" | python3 -I -c "import json,sys;d=json.load(sys.stdin);print('gear',(d['gear'].get('actual') or {}).get('value'),'speed',d['controls']['speed_kph'],'soc',d['ev_battery']['state_of_charge'],'charging',d['ev_battery'].get('charging_activated'),'range_m',d.get('odometer',{}).get('remaining_range_m'))"
echo "driving state: $(adb shell dumpsys car_service --services CarDrivingStateService | grep 'Current Driving' | tr -d '\r')  (0 parked, 1 idling, 2 moving)"
