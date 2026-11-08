# sv3.sh DIR HANDLE CHAIN HASH ... (hash = asset filename without .txt)
A=/cursor/stores/store-0fc0561f-612e-47c2-b2fc-c46c0ff246c0/grok-bot/agents/2ccf2426-0b48-4e85-b901-97010a7c1205/assets
D=$1;H=$2;shift 2;args=()
while [ $# -gt 0 ]; do cp $A/$2.txt /workspace/agent-tools/$2.txt; args+=($1 $2); shift 2; done
bash sv2.sh $D $H "${args[@]}"
