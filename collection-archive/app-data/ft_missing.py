#!/usr/bin/env python3
"""Print comma-joined batches (<=100) of post ids in site/data.json lacking full text in fulltext.json.
Fetch each batch with x.get_posts_by_ids(ids=..., post.fields='text,note_tweet', write_to_file=true),
move the result files into ft_raw/, run ft_merge.py, then deploy.sh."""
import json,pathlib
D=pathlib.Path('/workspace/x-app'); d=json.load(open(D/'site/data.json'))
ft=json.load(open(D/'fulltext.json')); gone=set(json.load(open(D/'gone.json')))
ids=[p['u'].rstrip('/').split('/')[-1] for c in d['creators'] for p in c['posts']]+[p['u'].rstrip('/').split('/')[-1] for x in d['days'] for p in x['posts'] if p['u']]
m=[i for i in dict.fromkeys(ids) if i not in ft and i not in gone]
for k in range(0,len(m),100): print(','.join(m[k:k+100]))
import sys; print(len(m),'missing',file=sys.stderr)
