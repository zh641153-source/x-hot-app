# usage: sv_pg.sh SRC DEST ; strips wrapper, prints count/next/oldest
python3 - "$1" "$2" <<'P'
import sys,json;s=open(sys.argv[1]).read();i=s.find('{');j=s.rfind('}');t=s[i:j+1];open(sys.argv[2],'w').write(t)
d=json.loads(t);m=d.get('meta',{});ds=d.get('data',[])
print(m.get('result_count'),m.get('next_token'),ds[-1]['created_at'] if ds else None)
P
