import json,sys
h=sys.argv[1] if len(sys.argv)>1 else 'iam678'
d=json.load(open('state/posts.json'));p=[x for x in d.values() if x.get('handle')==h]
v=lambda x:(x.get('metrics') or {}).get('impression_count') or 0
hot=[x for x in p if v(x)>=100000]
print(h,'posts',len(p),'hot',len(hot),'top',max(map(v,hot),default=0),'oldest',min((x['created_at'] for x in p),default='-'))
