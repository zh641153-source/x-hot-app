# sv2.sh DIR HANDLE CHAIN UUID ...  (pairs CHAIN UUID)
D=$1; H=$2; shift 2; while [ $# -gt 0 ]; do c=$1; u=$2; shift 2; n=$(ls $D|grep -c "_${c}p"); echo -n "$c: "; bash sv_pg.sh /workspace/agent-tools/$u.txt $D/30_search_${c}p$(printf %02d $n)_$H.json; last=$u; done; grep -o "[0-9]* of 1000 today" /workspace/agent-tools/$last.txt
