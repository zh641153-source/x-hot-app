#!/bin/bash
# sv3u: same as sv3 but assets under /cursor/stores/user/...
A=/cursor/stores/user/grok-bot/agents/2ccf2426-0b48-4e85-b901-97010a7c1205/assets
# fallback to old store if missing
A2=/cursor/stores/store-0fc0561f-612e-47c2-b2fc-c46c0ff246c0/grok-bot/agents/2ccf2426-0b48-4e85-b901-97010a7c1205/assets
D=$1;H=$2;shift 2;args=()
while [ $# -gt 0 ]; do
  src=$A/$2.txt; [ -f "$src" ] || src=$A2/$2.txt
  cp "$src" /workspace/agent-tools/$2.txt
  args+=($1 $2); shift 2
done
bash /workspace/x-creators/sv2.sh $D $H "${args[@]}"
