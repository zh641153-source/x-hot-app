#!/usr/bin/env python3
"""Manually add one post to 简中24h十万热帖.md (today's BJ section, sorted by views) + state.json.
Usage: add_link.py '<json>'  json keys: id, handle, name, created_at (ISO Z), views, text (FULL text: use note_tweet.text when present)
Fetch the fields first with x.get_posts_by_id (MCP) with post.fields incl. note_tweet. Then run /workspace/x-app/deploy.sh."""
import json,sys,re,datetime as dt,pathlib
Z=pathlib.Path('/workspace/x-zh-24h'); MD=Z/'简中24h十万热帖.md'; BJ=dt.timezone(dt.timedelta(hours=8))
p=json.loads(sys.argv[1]); today=dt.datetime.now(BJ).strftime('%Y-%m-%d')
t=dt.datetime.fromisoformat(p['created_at'].replace('Z','+00:00')).astimezone(BJ).strftime('%m-%d %H:%M')
txt=re.sub(r'\s+',' ',p['text']).replace('|','｜'); txt=txt[:60]+('…' if len(txt)>60 else '')
FTP=pathlib.Path('/workspace/x-app/fulltext.json'); ft=json.load(open(FTP)) if FTP.exists() else {}
ft[p['id']]=p.get('full_text') or p['text']; json.dump(ft,open(FTP,'w'),ensure_ascii=False)
url=f"https://x.com/{p['handle']}/status/{p['id']}"
row=f"| {p['views']:,} | {p['name']} @{p['handle']} | {t} | {txt} | [打开]({url}) |"
L=MD.read_text(encoding='utf-8').split('\n')
if any(p['id'] in l for l in L): sys.exit('already present')
hdr=f'## {today}（北京时间）'
if hdr not in L:
    i=next(k for k,l in enumerate(L) if l.startswith('## '))
    L[i:i]=[hdr,'','共 0 条。','','| 浏览量 | 作者 | 发布时间（北京） | 内容摘要 | 链接 |','| ---: | --- | --- | --- | --- |','']
h=L.index(hdr); j=h+1
while j<len(L) and not L[j].startswith('| ---'): j+=1
rows_s=j+1; e=rows_s
while e<len(L) and L[e].startswith('|'): e+=1
rows=L[rows_s:e]+[row]; rows.sort(key=lambda r:-int(r.split('|')[1].strip().replace(',','')))
L[rows_s:e]=rows
for k in range(h,rows_s):
    if L[k].startswith('共 '): L[k]=f'共 {len(rows)} 条。'
MD.write_text('\n'.join(L),encoding='utf-8')
s=json.load(open(Z/'state.json')); s['seen'][p['id']]=today; s.setdefault('added',{})[p['id']]=dt.datetime.now(BJ).strftime('%Y-%m-%d %H:%M:%S'); json.dump(s,open(Z/'state.json','w'),ensure_ascii=False)
print('added',row)
