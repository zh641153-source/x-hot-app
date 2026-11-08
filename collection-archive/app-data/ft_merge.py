#!/usr/bin/env python3
"""Merge get_posts_by_ids results (files in ft_raw/ or args) into fulltext.json {id: full_text}. note_tweet wins over text."""
import json,sys,glob,re,pathlib
D=pathlib.Path('/workspace/x-app'); F=D/'fulltext.json'
ft=json.load(open(F)) if F.exists() else {}
files=sys.argv[1:] or glob.glob(str(D/'ft_raw/*.txt'))
errs=set(); n=0; left=None
for f in files:
    s=open(f,encoding='utf-8').read()
    m=re.findall(r'(\d+) of 1000 today',s); left=m[-1] if m else left
    i=s.find('{'); obj,_=json.JSONDecoder().raw_decode(s[i:])
    for p in obj.get('data',[]):
        t=(p.get('note_tweet') or {}).get('text') or p.get('text') or ''
        if t: ft[p['id']]=t; n+=1
    for e in obj.get('errors',[]):
        if e.get('resource_id') or e.get('value'): errs.add(e.get('resource_id') or e.get('value'))
json.dump(ft,open(F,'w'),ensure_ascii=False)
G=D/'gone.json'; g=set(json.load(open(G))) if G.exists() else set(); g|=errs; g-=set(ft); json.dump(sorted(g),open(G,'w'))
print('merged',n,'errors',len(errs),'total',len(ft),'reads_left',left)
