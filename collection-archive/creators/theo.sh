# usage: theo.sh CHAIN SRC  -> saves next page file for chain, prints count/next/oldest
D=raw/20261103_theo; n=$(ls $D | grep -c "_${1}p"); bash sv_pg.sh $2 $D/30_search_${1}p$(printf %02d $n)_theo.json; grep -o "[0-9]* of 1000 today" $2
