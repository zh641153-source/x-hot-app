#!/bin/bash
set -e; cd /workspace/x-app && python3 build.py && cd site
git add -A && (git commit -qm "update $(date '+%F %H:%M')" || echo nochange) && git push -q origin main
