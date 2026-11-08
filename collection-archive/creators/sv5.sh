#!/bin/bash
# usage: sv5.sh NAME1 hash1 NAME2 hash2 ...
D=/workspace/x-creators/raw/20261105_1914
A=/cursor/stores/store-0fc0561f-612e-47c2-b2fc-c46c0ff246c0/grok-bot/agents/2ccf2426-0b48-4e85-b901-97010a7c1205/assets
while [ $# -gt 1 ]; do n=$1; h=$2; shift 2; echo -n "$n "; bash /workspace/x-creators/sv.sh $A/$h.txt $D/$n; done
