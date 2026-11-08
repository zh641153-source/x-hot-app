cd /workspace/x-creators; D=raw/20261103_1516; i=$(ls $D | grep -c search); f=$(ls -t /workspace/agent-tools/*.txt|head -1); bash sv_pg.sh $f $D/20_search_q$(printf %02d $i)_mikey.json
grep -o "[0-9]* of 1000 today" $f
