cd /workspace/x-creators; D=raw/20261103_1757; for a in "$@"; do id=${a%%:*}; n=${a##*:}; bash sv_pg.sh /workspace/agent-tools/$id*.txt $D/20_search_${n}_hy.json | sed "s/^/$n /"; done
