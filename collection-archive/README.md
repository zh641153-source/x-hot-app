# collection-archive · X 采集项目归档（快照 2026-11-08 16:01 北京时间）

本目录只是归档，不参与 APP 部署；仓库根目录的 index.html / data.json / android/ 未改动。

| 目录 | 原始路径 | 用途 |
|---|---|---|
| creators/ | /workspace/x-creators（不含 state.bak_* 与 creators.json.bak_*） | 博主监控：creators.py 主脚本及历史备份版本、creators.json 博主名单与阈值、state/（posts.json 全部抓到的帖子、history.json 历史热帖、creators_meta.json）、raw/ 原始 X 接口返回、reports/ 报告与总览、各 .sh 辅助脚本、RUNBOOK |
| hot-posts/ | /workspace/x-zh-24h | 「热贴」：zh24h.py / add_link.py / save.py、state.json（已收 id 去重）、pool.json / pool_skip.json 账号池、blocklist.json 黑名单、简中24h十万热帖.md、runs/ 原始结果、RUNBOOK |
| app-data/ | /workspace/x-app | build.py（生成 data.json）、deploy.sh、build_apk.sh、ft_missing.py / ft_merge.py、fulltext.json 全文、gone.json 已删帖记录、ft_raw/ 全文原始返回 |
| backups/x-creators-snapshots.tar.xz | /workspace/x-creators/state.bak_* + creators.json.bak_* | 每次深挖前的状态与名单快照（18+ 份，xz 压缩；sha256 见 MANIFEST.sha256） |
| RULES.md | — | 用户长期业务规则汇总 |
| MANIFEST.sha256 | — | 全部文件校验 |

## 恢复运行
1. 依赖：Python 3（仅标准库）、X connector（namespace `x`）；APK 构建另需 Android SDK/Gradle 与签名 keystore（未归档）。
2. 复制回原路径：`cp -a creators/. /workspace/x-creators/`、`cp -a hot-posts/. /workspace/x-zh-24h/`、app-data 文件放回 /workspace/x-app/，仓库根作为 /workspace/x-app/site。
3. 快照：`tar xJf backups/x-creators-snapshots.tar.xz -C /workspace/x-creators`。
4. 校验：`sha256sum -c MANIFEST.sha256`。
5. 按 creators/RUNBOOK.md 执行 plan → 调 X → ingest → finalize，再 app-data/deploy.sh。

## 排除项
- 签名 release.keystore、local.properties、.env/凭据（安全原因，均未归档；扫描未发现脚本内有密钥）。
- APK（X热帖*.apk，已在 GitHub Release）、android build/.gradle 缓存、.git、Android SDK。
- 定时任务（routine）本体在 Grok Bot 中，未导出。
