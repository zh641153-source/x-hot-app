#!/bin/bash
# usage: sv.sh SRC DST
cp "$1" "$2" && python3 - "$2" <<'P'
import sys,re,json
t=open(sys.argv[1]).read()
rc=re.search(r'"result_count":\s*(\d+)',t); nt=re.search(r'"next_token":\s*"([^"]+)"',t)
left=re.findall(r'[Rr]eads? left[^\n]*',t)
print(rc.group(1) if rc else '-', nt.group(1) if nt else 'END', ('429' if '429' in t[:300] else ''), left[-1][:80] if left else '')
P
