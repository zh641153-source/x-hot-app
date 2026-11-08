import sys,glob,os,shutil
sys.path.insert(0,'/workspace/x-monitor/connector')
from process import read_response,extract
run,name=sys.argv[1],sys.argv[2]
f=max(glob.glob('/workspace/agent-tools/*.txt'),key=os.path.getmtime)
dst=os.path.join(run,name); shutil.copy(f,dst)
o,r=read_response(dst); p,u=extract(o); m=(o or {}).get('meta',{})
print(r, m.get('result_count'), min((x['created_at'] for x in p),default='-'), m.get('next_token') or 'END')
