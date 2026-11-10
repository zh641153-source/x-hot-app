# collection-archive · X 采集项目归档（快照 2026-11-10 16:16 北京时间）

本目录只是归档，不参与 APP 部署；仓库根目录的 index.html / data.json / android/ 未改动。

交接说明见同目录 [HANDOFF_CODEX.md](HANDOFF_CODEX.md)（给 Codex 的完整交接文档）。

| 目录 | 原始路径 | 用途 |
|---|---|---|
| creators/ | /workspace/x-creators（不含 state.bak_* 与 creators.json.bak_*） | 博主监控：creators.py 主脚本及历史备份版本、creators.json 博主名单与阈值、state/（posts.json 全部抓到的帖子、history.json 历史热帖、creators_meta.json）、raw/ 原始 X 接口返回、reports/ 报告与总览、各 .sh 辅助脚本、RUNBOOK |
| hot-posts/ | /workspace/x-zh-24h | 「热贴」：zh24h.py / add_link.py / save.py、state.json（已收 id 去重）、pool.json / pool_skip.json 账号池、blocklist.json 黑名单、简中24h十万热帖.md、runs/ 原始结果、RUNBOOK |
| app-data/ | /workspace/x-app | build.py（生成 data.json）、deploy.sh、build_apk.sh、ft_missing.py / ft_merge.py、fulltext.json 全文、gone.json 已删帖记录、ft_raw/ 全文原始返回 |
| backups/x-creators-snapshots.tar.xz | /workspace/x-creators/state.bak_* + creators.json.bak_* | 每次深挖前的状态与名单快照（18+ 份，xz 压缩；sha256 见 MANIFEST.sha256）。本轮刷新时这些备份文件无新增，沿用 2026-11-08 打包的压缩包。 |
| RULES.md | — | 用户长期业务规则汇总 |
| HANDOFF_CODEX.md | — | 交给 Codex 的交接文档 |
| MANIFEST.sha256 | — | 全部文件校验 |

数据本身最近一次采集/部署：2026-11-09 19:17 北京时间（daily 扫描）。本归档刷新时间见上方快照。

## 恢复运行
1. 依赖：Python 3（仅标准库）、X 数据源（原为 Grok Bot 的 X connector，Codex 需改为自备的 X API v2，见 HANDOFF_CODEX.md）；APK 构建另需 Android SDK/Gradle 与签名 keystore（未归档，日常更新数据不需要重打 APK）。
2. 复制回原路径：`cp -a creators/. /workspace/x-creators/`、`cp -a hot-posts/. /workspace/x-zh-24h/`、app-data 文件放回 /workspace/x-app/，仓库根作为 /workspace/x-app/site。在其他环境需改脚本里写死的 `/workspace/...` 路径。
3. 快照：`tar xJf backups/x-creators-snapshots.tar.xz -C <x-creators 目录>`。
4. 校验：`sha256sum -c MANIFEST.sha256`。
5. 按 creators/RUNBOOK.md 执行 plan → 拉取 X 数据 → ingest → finalize，补全文后运行 app-data/deploy.sh。注意：plan 输出的是 Grok Bot MCP 调用描述，不能直接在 Codex 环境执行，见 HANDOFF_CODEX.md。

## 排除项
- 签名 release.keystore、local.properties、.env/凭据（安全原因，均未归档；刷新时再次扫描，脚本内无密钥）。
- APK（X热帖*.apk，已在 GitHub Release v1.4）、android build/.gradle 缓存、.git、Android SDK。
- 定时任务（routine）本体在 Grok Bot 中，未导出。
- `/workspace/x-monitor/connector`（zh24h.py / save.py 依赖其中的 process.py）。该目录不属于本 APP 仓库，自动 24h 池扫描已暂停，故未并入本归档。若要恢复自动池扫描，需另行获取 `lang_check` / `read_response` / `extract`。
