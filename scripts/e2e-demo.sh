#!/bin/bash
# usage: e2e.sh <outdir>  — runs full scenario on emulator, records video, saves step screenshots + result log
# 사전 조건: 에뮬레이터 부팅, 앱 설치, 내비에서 더현대서울로 안내 시작(경로 있음). NIGHT=yes 로 Night 실행.
O=$1; mkdir -p $O; 
dump(){ adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb exec-out cat /sdcard/ui.xml; }
tap_text(){ # tap center of first node whose text contains $1
  dump | python3 -c "
import sys,re
x=sys.stdin.read(); t=sys.argv[1]
m=re.search(r'text=\"'+re.escape(t)+r'\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"',x)
if not m: print('NOTFOUND'); sys.exit(1)
a,b,c,d=map(int,m.groups()); print((a+c)//2,(b+d)//2)" "$1" | { read X Y; if [ "$X" = NOTFOUND ]; then echo "tap_text NOTFOUND: $1" >> $O/log.txt; return 1; fi; adb shell input tap $X $Y; }
}
shot(){ adb exec-out screencap -p > $O/$1.png; }
has(){ dump | grep -q "$1"; }
log(){ echo "$@" | tee -a $O/log.txt; }
: > $O/log.txt
adb shell cmd uimode night ${NIGHT:-no} >/dev/null
adb shell rm -f /sdcard/demo.mp4
adb shell am force-stop ai.familystop.app; adb shell am start -n ai.familystop.app/ai.familystop.MainActivity >/dev/null; sleep 7
(adb shell screenrecord --time-limit 170 --bit-rate 4000000 /sdcard/demo.mp4 >/dev/null 2>&1 &); sleep 2
shot 01-step1-destination
has "어디로 가나요" && log "OK step1 destination list" || log "FAIL step1 not shown"
tap_text "다음"; sleep 2
shot 02-step2-conditions
has "정차에서 필요한 것" && log "OK step2 conditions" || log "FAIL step2 not shown"
has "이 시각으로 정하기" && { tap_text "이 시각으로 정하기"; sleep 1; }
tap_text "식사"; sleep 1; tap_text "화장실"; sleep 1; tap_text "필요"; sleep 1
shot 03-step2-filled
tap_text "후보 보기"; sleep 5
shot 04-step3-candidates
has "정차 후보 비교" && log "OK step3 candidates" || log "FAIL step3 not shown"
tap_text "경유지로 추가"; log "tapped add"
ok=0
for i in $(seq 1 30); do sleep 3
  D=$(dump)
  if echo "$D" | grep -q "전달하지 못했어요"; then log "FAIL handoff failed at ~$((i*3))s"; shot 05-handoff-fail; break; fi
  if echo "$D" | grep -q "정차에서 할 일"; then log "OK handoff success at ~$((i*3))s"; ok=1; break; fi
  if ! echo "$D" | grep -q "familystop"; then log "app not foreground at ~$((i*3))s (navi in front)"; adb shell am start -n ai.familystop.app/ai.familystop.MainActivity >/dev/null 2>&1; fi
done
if [ $ok = 0 ]; then log "retry"; tap_text "다시 시도"; for i in $(seq 1 30); do sleep 3; if has "정차에서 할 일"; then log "OK retry success at ~$((i*3))s"; ok=1; break; fi; done; fi
adb shell am start -n ai.familystop.app/ai.familystop.MainActivity >/dev/null 2>&1; sleep 4
shot 06-activity
has "정차에서 할 일" && log "OK activity shown" || log "FAIL activity not shown"
tap_text "식사"; sleep 2; shot 07-checked
has "1/2" && log "OK checklist 1/2" || log "FAIL checklist"
tap_text "충전 시작했어요"; sleep 4; shot 08-countdown
has "충전 끝까지" && log "OK countdown" || log "FAIL countdown"
tap_text "5분 연장"; sleep 2
tap_text "충전 끝났어요"; sleep 3; shot 09-done
has "충전 끝" && log "OK charge done" || log "FAIL charge done"
sleep 2; adb shell pkill -2 screenrecord; sleep 3
adb pull /sdcard/demo.mp4 $O/raw.mp4 >/dev/null 2>&1
ffmpeg -y -loglevel error -i $O/raw.mp4 -vf "scale=1280:-2,fps=15" -c:v libx264 -crf 32 -preset slow -pix_fmt yuv420p -movflags +faststart -an $O/demo.mp4
rm -f $O/raw.mp4; ls -lh $O/demo.mp4 | awk '{print "video",$5}' | tee -a $O/log.txt
