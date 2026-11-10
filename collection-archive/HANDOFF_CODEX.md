# 交接文档 · 交给 Codex（快照 2026-11-10 16:16 北京时间）

本文根据仓库内真实文件与当时本地运行状态整理，不编造未核实的接口或额度。目标仓库：[`zh641153-source/x-hot-app`](https://github.com/zh641153-source/x-hot-app)，默认分支 `main`。

**读者**：Codex（在用户自己的环境/云端运行，不能访问原 Grok Bot 的 `/workspace`，只能通过本 GitHub 仓库取代码和数据）。

**用户已安装手机 APP 1.4。日常只需更新远端 `data.json`，不要重新打包或发布 APK。** 只有改 UI / 客户端逻辑时才需要新 APK。

---

## 1. 系统概览

### 1.1 仓库里有什么

Git 仓库根目录实际上就是原环境的 `/workspace/x-app/site/`（GitHub Pages 站点根）：

| 路径 | 作用 |
|---|---|
| `index.html` + `data.json` | 网页版；APP 也读这份 `data.json` |
| `android/` | APP 源码（`applicationId=com.xhot.app`，`versionName=1.4`，`versionCode=5`） |
| `build.py` / `deploy.sh` / `build_apk.sh` | 根目录有一份拷贝，路径仍写死 `/workspace/...`，**不能原样在 Codex 环境跑** |
| `collection-archive/` | 采集脚本、名单、状态、原始结果、规则与本文；**不参与 Pages 展示** |

可运行的采集与导出脚本在归档里（路径相对 `collection-archive/`）：

- `creators/` ← 原 `/workspace/x-creators`
- `hot-posts/` ← 原 `/workspace/x-zh-24h`
- `app-data/` ← 原 `/workspace/x-app` 中的 `build.py`、`deploy.sh`、`ft_*.py`、`fulltext.json`、`gone.json`、`ft_raw/`

### 1.2 数据流

```
X 数据读取（原：Grok Bot 内置 X connector）
    ↓ 原始响应写入 creators/raw/<时间>/ 或 hot-posts/runs/<时间>/
creators.py ingest / seed-history / finalize
    ↓ 更新 creators/state/posts.json、history.json、creators_meta.json
    （「单个」链接走 hot-posts/add_link.py，写入 简中24h十万热帖.md 与 hot-posts/state.json，全文写入 app-data/fulltext.json）
app-data/build.py（只读上述文件 + fulltext.json + gone.json，不访问 X）
    ↓ 生成仓库根 data.json（字段 built 为北京时间）
deploy.sh：commit + git push origin main
    ↓
网页与已安装的 APP 1.4 从 GitHub 拉取 data.json
```

`build.py` 还会写一个空的 `.nojekyll`，以便 GitHub Pages 正常提供站点文件。

### 1.3 APP / 网页读取 data.json 的确切地址

Android 1.4 源码 `android/app/src/main/java/com/xhot/app/MainActivity.kt` 中，下拉刷新按下面顺序尝试，成功即停止（请求带 `?t=<时间戳>`，并设 `Cache-Control: no-cache`）。第一条是 GitHub Contents API，请求头为 `Accept: application/vnd.github.raw`，直接拿到文件正文而不是 JSON 包装：

1. `https://api.github.com/repos/zh641153-source/x-hot-app/contents/data.json`
2. `https://raw.githubusercontent.com/zh641153-source/x-hot-app/main/data.json`
3. `https://zh641153-source.github.io/x-hot-app/data.json`

失败则使用本地缓存（`filesDir/data.json`），再没有则用 APK 内 `assets/data.json`（打包时的快照，仅离线兜底）。

网页 `index.html` 使用相对路径：`fetch('data.json?'+Date.now())`。在 GitHub Pages 上即：

- 页面：`https://zh641153-source.github.io/x-hot-app/`
- 数据：`https://zh641153-source.github.io/x-hot-app/data.json`

Pages 配置（已核实）：来源为 `main` 分支根目录 `/`，`https` 已强制，`status=built`。Pages 对该文件有约 600 秒缓存；APP 优先走 API/raw，并带 cache bust，一般不必等 Pages。

当前线上 `data.json` 的 `built` 字段：`2026-11-09 19:17`（北京时间）。其中博主 51 个、历史热帖 4615 条；「热贴」16 个日期、131 条。

### 1.4 APK

用户已安装 1.4，**更新数据不必重装**。Release：

- https://github.com/zh641153-source/x-hot-app/releases/tag/v1.4
- 安装包：https://github.com/zh641153-source/x-hot-app/releases/download/v1.4/XHot-1.4.apk
- 说明：搜索 + 收藏 + 热贴改名。发布时间 2026-11-05 20:09 北京时间（tag 记录为 2026-11-05T12:09:08Z）。

`app-data/build_apk.sh` 仅在改 APP 时使用，依赖本机 Android SDK / Gradle / 签名 keystore（keystore 与 `local.properties` **未入库**，也不应入库）。

收藏存在手机本地，不在 `data.json` 里，服务器无法恢复。

---

## 2. 业务规则（以 `RULES.md` 为准）

原始操作手册 `creators/RUNBOOK.md`、`creators/X2_ROUTINE_INTENT.md`、`hot-posts/RUNBOOK.md` 里有过时描述（例如每小时一轮、晚上 20:08、历史窗口 183 天）。**以 RULES.md 与 `creators.json` 的现行设置为准。**

### 2.1 博主

- 每个新博主都要整号深挖：先用时间线（`get_users_posts`，排除转推和回复）翻到头，不够再用 `search_posts_all` 按 `from:HANDLE -is:retweet -is:reply` 补全可获取历史，再挑出热门帖。
- **历史热帖门槛**：浏览 ≥ 10 万（`creators.json` → `settings.history_min_views=100000`）、非回复、非转推、发布已满 24 小时。写入 `creators/state/history.json`，**只追加、不改旧条目**。
- 用户发链接并标明「**博主**」：把该账号加入 `creators/creators.json` 的 `creators` 列表，并做全历史回填。
- 现行 `settings.history_days=9999`（等于尽量保留全部历史；RUNBOOK 里写的 183 已过时）。`include_replies=false`。`daily_min_views` 是遗留字段，例行入榜不再用它。

### 2.2 「单个」链接与「热贴」标签

- 用户发链接并标明「**单个**」：用 `hot-posts/add_link.py` 写入当天「热贴」（`简中24h十万热帖.md` 当日段落），**不受 10 万 / 24 小时门槛限制**。
- 自动 24 小时账号池扫描已于 2026-11-01 暂停，不要恢复，除非用户明确要求。相关脚本 `hot-posts/zh24h.py`、`save.py` 仍留在归档，但不能原样运行（见第 3 节）。

### 2.3 通用

- 收录**完整文案**（长帖取 `note_tweet.text`，否则 `text`），**不含图片**。
- **新增置顶**：`build.py` 以 `added_at`（写入历史的时间）为排序键 `k`，新加入的在前；若 `added_at` 比发布时间晚 7 天以上（深挖回填的旧帖），改按发布时间排，避免大批旧帖全部顶到最前。
- 永久排除（`hot-posts/blocklist.json`）：李老师 `@whyyoutouzhele`、立党 `@lidangzzz`、王局志安 `@wangzhian8848`。禁止加回 `pool.json`。
- 删帖 / 封号导致接口拿不到的 id 进入 `app-data/gone.json`，APP 保留已有原文并标注。

### 2.4 每日扫描与通知

- 每天 **北京时间 19:00 左右**扫描已监控博主；发现新的历史热帖（或有需要上线的数据）就生成并推送 `data.json`。
- **成功：只通知用户「今天的已更新」。不要再按旧 RUNBOOK 推送各博主的 .md 报告或「有 M 条进入历史热帖」配文。**
- **失败：只用一句话说明原因。**

### 2.5 仅 UI、无需采集侧恢复的功能

已在 APP 1.4 / `index.html` 实现：右上角放大镜搜索已采集推文；每条右上角收藏（数据只在手机本地）；顶栏「热贴」（由「24小时热贴」改名）与「收藏」标签。这些不是采集脚本的恢复项。

---

## 3. X 数据来源：Codex 必须改造的部分

### 3.1 原环境怎么取数（不能直接用）

原采集跑在 Grok Bot 里，使用其内置 X 读取工具（MCP namespace `x`），**不是**脚本自己发 HTTP。典型流程：

1. `python3 creators.py plan` 只**打印**一批调用描述（JSON 行），并建 `raw/<YYYYMMDD_HHMM>/`，写入 `plan.json`。
2. 由 Grok Bot 按行调用 `CallDynamicTool(namespace="x", toolName=..., arguments=...)`，把**完整原始返回**存到 `save_as`（文件 mtime 当作抓取时间）。
3. `python3 creators.py ingest RUN_DIR` 再 `finalize RUN_DIR`。

`plan` **不会**自己访问网络。没有 Grok Bot 的 X connector，这些步骤不能原样执行。

Codex 需要用户自备的 **X API v2 Bearer Token**，放入环境变量（例如 `X_BEARER_TOKEN`），**不要写入仓库、不要提交 .env**。本文不规定具体变量名，但禁止把密钥放进 Git。

Grok Bot 的额度（可在 raw 响应末行看到，仅作背景，**不是**官方 API 的额度）：每分钟 30 次、每天 1000 次，每天 08:00 北京时间重置。官方 X API v2 的限额取决于用户申请的开发者访问级别，需自行查阅，不要套用上述数字。浏览量字段 `public_metrics.impression_count` 与长帖 `note_tweet` 是否可用，也取决于该访问级别，接入前必须先用一条真实请求确认。

### 3.2 脚本中的 connector 调用 ↔ 官方 X API v2 对应关系

下列对应按公开的 API v2 惯例写出，供改造时对照。参数名在脚本里是 connector 风格（`post.fields`），官方接口一般是 `tweet.fields`。分页：请求里用 `pagination_token`（connector）/ 官方常用 `pagination_token` 或 `next_token` 字段，以官方文档为准；响应在 `meta.next_token`。每页 `max_results=25` 是现脚本的设定，官方上限通常可到 100，改页大小后要同步改 ingest 的翻页预期。

| 脚本位置 | connector `toolName` | 主要 arguments | 官方 API v2（对照） | 用途 |
|---|---|---|---|---|
| `creators.py plan` 日常 / `--history` | `get_users_by_usernames` | `usernames`（逗号分隔，每批最多 100）、`user.fields=id,name,username,public_metrics` | `GET /2/users/by?usernames=` | 补 id、粉丝数；缺 id 或距上次查询超过 12 小时才调用 |
| 同上 | `get_users_posts` | `id`（用户数字 id）、`max_results=25`、`exclude=retweets,replies`、`start_time`、`post.fields` 见下 | `GET /2/users/:id/tweets`（exclude 与 start_time 为查询参数） | 日常：近 24 小时；深挖：时间线翻页，默认最多 60 页 |
| 同上 | `get_posts_by_ids` | `ids`（逗号分隔，每批最多 100）、`post.fields` 见下 | `GET /2/tweets?ids=` | 刷新未毕业帖的浏览量，必须放在该轮最后，保证是最新数 |
| RUNBOOK 深挖补全（**plan 不会自动生成**，需人工组请求） | `search_posts_all` | `query=from:HANDLE -is:retweet -is:reply`，`start_time` / `end_time`，`max_results=25`，`sort_order=recency`，`post.fields=created_at,public_metrics,referenced_tweets,note_tweet` | 全历史搜索对应 `GET /2/tweets/search/all`（通常要付费/企业级访问）。`/2/tweets/search/recent` **只能查约 7 天**，不能代替整号深挖 | 时间线翻到头（约 3200 条上限）后补更早的帖 |
| `add_link.py` 之前 | `get_posts_by_id`（单条） | 含 `note_tweet` 的 post.fields | `GET /2/tweets/:id` | 取单帖 text/作者/时间/浏览量/全文 |
| `ft_missing.py` 之后 | `get_posts_by_ids` | `post.fields=text,note_tweet` | `GET /2/tweets?ids=` | 补 `fulltext.json` |

日常 `creators.py` 里的字段常量：

```text
POST_FIELDS = "created_at,public_metrics,referenced_tweets,in_reply_to_user_id,lang"
```

**注意：日常 plan 没有带 `note_tweet`。** 完整长文靠事后 `ft_missing.py` + `get_posts_by_ids(text,note_tweet)` + `ft_merge.py` 补进 `fulltext.json`。`build.py` 展示优先级为：`fulltext.json[id]` → 记录里的 `full_text` → `text`。

`ingest` 会把 connector 的 `public_metrics.repost_count` 映射为 `retweet_count`（官方 API 一般已是 `retweet_count`）。判定转发/回复/引用看 `referenced_tweets[].type`（`retweeted` / `replied_to` / `quoted`）以及 `in_reply_to_user_id`。浏览量用 `public_metrics.impression_count`。

### 3.3 原始响应格式：哪些能直接 ingest，哪些要改

`creators.py parse_file` / `ingest` 可以接受：

- 文件中**第一个 `{` 起**的 JSON 对象（前面若有工具包装或后面有 `X reads left ...` 一行，会被忽略）。标准 API 的纯 JSON 响应一般可以直接存下来再 ingest。
- 或一种内部 TSV 精简格式（见 `creators.py` 文件头注释）。

期望的 JSON 形态与 API v2 相近：`data` 为推文数组或用户对象，`includes` 不一定必需（用户查找时 `data` 为带 `username` 的对象/数组），`meta.next_token`、`errors[]`。没有 `data`、只有 `errors` 时记入 `meta.error`，例如账号被停或未授权。

下列**不能直接用**，必须改造或停用：

1. **整个「plan → 由外部 MCP 逐条调用 → 落盘」循环。** 需要自己写一个执行器：读 `plan()` 的 JSON，改成 HTTP 调用，把响应写入 `save_as`，按 `paginate=true` 且 `meta.result_count=25` 且存在 `next_token` 时续翻，文件名去 `.json` 加 `_p2.json`、`_p3.json`…，不超过 `max_pages`。429 时旧规则是等 60 秒重试一次，仍失败则停止本轮剩余调用，但仍做 ingest/finalize。
2. **`hot-posts/save.py`**：从 `/workspace/agent-tools/*.txt` 取「最新一份」MCP `write_to_file` 结果再拷贝。这是 Grok Bot 的临时文件约定，Codex 环境没有。
3. **`creators/` 下 `sv.sh`、`sv2.sh`、`sv3.sh`、`sv3u.sh`、`sv_pg.sh`、`s.sh`、`n.sh` 等**：同样是把 `agent-tools` 里的 MCP 原始结果拷进 `raw/` 的手工助手，且部分脚本写死了某次深挖的目录名，不能当通用流水线。
4. **`hot-posts/zh24h.py` 与 `save.py`** 依赖 `/workspace/x-monitor/connector/process.py` 的 `lang_check`、`read_response`、`extract`，该项目**不在本仓库**，本归档也未收录。自动池扫描已暂停，日常不需要跑它们；不要在缺依赖的情况下假装能恢复自动扫描。
5. **所有写死的 `/workspace/x-creators`、`/workspace/x-zh-24h`、`/workspace/x-app` 路径**（见 `app-data/build.py`、`deploy.sh`、`ft_missing.py`、`ft_merge.py`、`hot-posts/add_link.py`、`zh24h.py`）。必须改成相对路径或环境变量后才能在 Codex 环境运行。
6. **`app-data/ft_merge.py`** 按 MCP 原始文本解析（从第一个 `{` 解码 JSON，并从正文里抓 `N of 1000 today`）。若改为官方 API 纯 JSON，合并逻辑基本可用，但「剩余读取次数」那一行不会再出现，属正常。
7. **`search_posts_all` 全历史深挖**：若只有 recent search，做不到「把能采到的历史都采齐」。需在交接时确认用户 API 是否具备全归档搜索；不具备则应明确告诉用户深挖能力受限，而不是静默少采。

**可以直接（或只改路径后）运行、且不访问 X 的部分：**

- `app-data/build.py`：从 `history.json` + `creators.json` + `creators_meta.json` + `简中24h十万热帖.md` + `fulltext.json` + `gone.json` 生成 `data.json`。
- `hot-posts/add_link.py`：你先自行用 API 取到字段，再传入 JSON。
- `creators.py ingest / finalize / seed-history / report / dashboard / status`：只要 `RUN_DIR` 里已是可解析的响应文件。
- `ft_missing.py`：根据现有 `data.json` / `fulltext.json` / `gone.json` 列出缺全文的 id。

### 3.4 加博主与加单帖的推荐步骤（逻辑不变，取数方式要换）

**加「博主」：**

1. 在 `creators/creators.json` 的 `creators` 增加 `{"username":"xxx"}`（id 可稍后由 users 接口写入）。
2. `python3 creators.py plan --history HANDLE --pages 60`，用改造后的执行器翻时间线。
3. 时间线没有 `next_token` 但仍未覆盖到目标起点时，用全历史搜索补，结果存成 `NN_search_sK_<handle>.json`。
4. `ingest` → `seed-history` → 必要时 `report`。再跑全文补齐与 `build.py` / 部署。
5. 不要因为历史热帖为 0 就自动删号。

**加「单个」：**

1. 用 API 取该帖：`id`、`handle`、`name`、`created_at`、`views`（impression_count）、`text`（优先 `note_tweet.text`）。
2. `python3 add_link.py '<json>'`（已存在则退出 `already present`）。
3. 补全文（若已写入 `fulltext.json` 可跳过）后部署。

部署前建议：`python3 ft_missing.py`，对输出的每批 id（每行最多 100 个）拉 `text,note_tweet`，把响应放入 `app-data/ft_raw/`，再 `python3 ft_merge.py`。拿不到的 id 会进入 `gone.json`。当前快照下 `ft_missing.py` 输出为 **0 missing**（18765 条全文、112 条 gone）。

---

## 4. 定时

Grok Bot 上的 routine 在用户确认后会由原助手暂停，**不在本仓库、也无法从本归档恢复**。交接之后 Codex 必须自己安排定时，例如：

- GitHub Actions：`cron: "0 11 * * *"`（11:00 UTC = 19:00 北京时间，CST / UTC+8，无夏令时）；或
- 用户自己的服务器 cron（`CRON_TZ=Asia/Shanghai`，19:00）。

不要在仓库里提交 Token。成功通知保持一句「今天的已更新」；失败一句原因。旧的 `X2_ROUTINE_INTENT.md`（20:08、按 PUSH 报告）不要再当作现行通知格式。

本仓库目前**没有**现成的 GitHub Actions 工作流。

---

## 5. 部署

`app-data/deploy.sh`（与仓库根 `deploy.sh` 内容相同）当前是：

```bash
#!/bin/bash
set -e; cd /workspace/x-app && python3 build.py && cd site
git add -A && (git commit -qm "update $(date '+%F %H:%M')" || echo nochange) && git push -q origin main
```

它会：先跑 `build.py` 覆盖 `site/data.json` 并写入 `.nojekyll`，然后在 git 仓库（原环境即 `site/`，也就是本仓库根）里 `git add -A`、提交 `update YYYY-MM-DD HH:MM`、推送到 `origin main`。

对 Codex 的提醒：

- 必须改路径，使 `build.py` 读到归档/工作副本里的 `creators` 与 `hot-posts`，并把 `data.json` 写到本仓库根。
- **`git add -A` 会暂存仓库内所有变更。** 提交前检查 diff，只应包含本次 `data.json`（及有意更新的采集状态，若你决定把状态也提交进 `collection-archive/`）。不要把密钥、`.env`、无关文件推进去，也不要误改 `index.html` / `android/`。
- 不要 force push，不要跳过 hooks。
- 验证：
  1. 本地 `git log -1` 与 `git ls-remote origin main` 的 commit 一致；
  2. `https://raw.githubusercontent.com/zh641153-source/x-hot-app/main/data.json` 的 `built` 已更新；
  3. Contents API（与 APP 第一条 URL 相同，`Accept: application/vnd.github.raw`）返回同一份；
  4. 手机 APP 下拉刷新出现新的「已更新到 …」或 `built` 时间（若仍看到旧值，可稍等或再刷，优先以 API/raw 为准）。

---

## 6. 当前状态（本归档刷新时）

- **归档快照时间**：2026-11-10 16:16 北京时间。
- **最近一次成功的每日扫描与部署**：2026-11-09 19:12 开始拉数（`creators/raw/20261109_1912/`），`state/meta.json` 的 `last_ingest=2026-11-09T11:16:39+00:00`（即 19:16 北京时间），`data.json` 的 `built=2026-11-09 19:17`。当时仓库 commit 为 `9c8f62c`（`update 2026-11-09 19:17`）。本文提交后，`main` 会多一次只含归档/交接文档的 commit。
- **博主名单**：`collection-archive/creators/creators.json`，**51** 个，均无 `disabled`，且都至少有 1 条历史热帖，因此 51 个都出现在 APP「博主」列表中。
  `@wmtbxsh, @woaierjie, @Resfeber0, @ciiikick, @shaojiull, @fadai202202, @AirdropAlchemis, @r0x_ht, @_miuj_9, @HtO0ha, @Songzjiu, @lizisu2000, @luer0416, @SUZUdesu111, @Nameiccc, @nvjishi, @roten_rhonda, @diaoke, @mazziniunsaya, @icecake999, @Warmkiss33, @ChunQingXiaoQ, @mimimimibushuo, @gebaobao520, @qijiangbb_, @Mantou_naive, @ratshelprats, @MissMafiana, @Mikey00312, @Vox35112635, @tuitui0072, @odelia21360, @iam678, @Hy9788, @0xTheo520, @DearDoll_Love, @tart_egg25188, @Six_One61_, @0TwT012, @Kokittyi, @MomoYiyi2225, @xixikeai_, @Mcfly2140, @HnrBB2nai, @LIYUNLONG1111, @BerryTokkiny, @kittenpoems, @SnKRyan930, @SheryaSuma57400, @Guillermin45548, @LOvOe_me`
- **数量**：`history.json` / APP 历史热帖 **4615** 条；`posts.json` 中抓到过的帖子 **26616** 条；「热贴」**131** 条（16 天，去重在 `hot-posts/state.json` 的 `seen`）；`fulltext.json` **18765** 条；`gone.json` **112** 个 id。
- **已知问题（有原始响应为证，不是推测）**：
  - `@roten_rhonda`（张女士，id `1350225433388851200`）：2026-11-09 日常请求返回 `User has been suspended`（`title: Forbidden`）。名单与已入榜的 1 条历史热帖仍保留。
  - `@shaojiull`（mio，id `1501915925578420228`）：同日返回 `Sorry, you are not authorized to see the user`（`title: Authorization Error`）。已有 40 条历史热帖保留；很可能是锁推或对当前读取方不可见，后续日常拉新会持续失败，除非状态变化。
- **全文机制**：先 `build.py` 再 `ft_missing.py` 列出缺全文且不在 gone 中的 id，分批（≤100）拉取后 `ft_merge.py` 合并；错误里的 resource id 进入 `gone.json`，若后来取到全文会从 gone 里剔除。当前缺全文为 0。
- **备份**：`backups/x-creators-snapshots.tar.xz` 为深挖前的 `state.bak_*` / `creators.json.bak_*`（本轮刷新时这些备份没有新文件，沿用 2026-11-08 的压缩包）。

---

## 7. 给 Codex 的第一步与验收标准

### 7.1 建议的第一步

1. `git clone https://github.com/zh641153-source/x-hot-app.git`，阅读 `collection-archive/README.md`、`RULES.md` 和本文。
2. 在用户环境建工作目录，把 `collection-archive/creators`、`hot-posts`、`app-data` 拷出或改路径，使脚本不再依赖 `/workspace/...`。仓库根继续作为 GitHub Pages 站点（`index.html` / `data.json` / `android/`）。
3. 向用户要 X API v2 Bearer Token，只放环境变量或本地密钥管理，**不入库**。先用一条 `GET /2/tweets/:id`（要 `public_metrics` 与 `note_tweet`）确认浏览量和长帖全文都能拿到。
4. 写一个最小执行器，替换 MCP：至少支持 `get_users_by_usernames`、`get_users_posts`、`get_posts_by_ids` 及翻页落盘，输出能被现有 `ingest` 解析。不要改入榜规则（10 万 / 24 小时 / 排除回复与转推 / history 只追加），除非用户要求。
5. 用已归档的 `state/` 先只改路径跑一遍 `build.py`，确认生成的博主数、帖子数与当前线上 `data.json` 一致（51 / 4615 / 16 天），再谈上线推送。
6. 安排北京时间 19:00 的定时任务。通知文案按第 2.4 节。
7. 不要提交 keystore、`.env`、Token；不要无故改 UI；不要恢复已暂停的 24h 池扫描；不要 force push。

### 7.2 验收标准

- 日常：对 51 个博主拉近 24 小时原创/引用，刷新待毕业帖浏览量，满 24 小时且 ≥10 万的追加进 `history.json`，再补全文、`build.py`、推送到 `origin/main`。
- 线上 `data.json` 的 `built` 更新后，已安装的 1.4 APP 下拉刷新能读到新数据，**无需重装 APK**。
- 用户给「单个」链接能进「热贴」且不受 10 万/24h 限制；给「博主」链接会入名单并做可获取范围内的全历史深挖（若 API 没有全归档搜索，必须向用户说明限制）。
- 成功通知仅为「今天的已更新」；失败一句原因。
- 黑名单三人不会被采集。`@roten_rhonda` / `@shaojiull` 的既有热帖仍在，拉新失败有记录即可，不要无说明地删号或删历史。
- 仓库中无密钥。`index.html` 与 Android 功能代码无无关改动。

---

## 8. 关键文件索引（均为 collection-archive/ 下，除非注明仓库根）

| 文件 | 说明 |
|---|---|
| `creators/creators.json` | 名单与阈值 |
| `creators/creators.py` | plan / ingest / finalize 等 |
| `creators/state/history.json` | 已入榜历史热帖（只追加） |
| `creators/state/posts.json` | 抓到过的全部帖与毕业状态 |
| `creators/state/creators_meta.json` | 昵称、粉丝、回溯进度 |
| `creators/state/meta.json` | 最近 ingest 时间等 |
| `creators/raw/` | 各轮原始响应 |
| `creators/RUNBOOK.md` | 旧操作手册（通知格式与部分日程已过时） |
| `hot-posts/add_link.py` | 手动加「单个」 |
| `hot-posts/简中24h十万热帖.md` | 「热贴」正文 |
| `hot-posts/state.json` | 已收录 id |
| `hot-posts/blocklist.json` | 永久排除 |
| `app-data/build.py` | 生成 data.json |
| `app-data/deploy.sh` | 构建并 push |
| `app-data/ft_missing.py` / `ft_merge.py` | 全文补齐 |
| `app-data/fulltext.json` / `gone.json` | 全文与已失效 id |
| 仓库根 `data.json` | APP 与网页的数据 |
| 仓库根 `android/.../MainActivity.kt` | APP 拉数 URL 列表 |

校验：`collection-archive/MANIFEST.sha256`。恢复步骤见 `README.md`。
