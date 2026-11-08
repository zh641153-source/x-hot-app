#!/bin/bash
# 重建 APK（仅改 APP 时需要；每日数据更新走 deploy.sh，无需重装）
set -e; source /workspace/tools/env.sh
cp /workspace/x-app/site/data.json /workspace/x-app/android/app/src/main/assets/
cd /workspace/x-app/android && gradle --no-daemon -q assembleRelease
mkdir -p ../dist && cp app/build/outputs/apk/release/app-release.apk "../dist/X热帖_v${1:-1.0}.apk" && ls -l ../dist
