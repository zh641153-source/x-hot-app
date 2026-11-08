# X 博主历史爆款采集 · Routine 操作步骤（官方 X connector，namespace `x`）

每小时一轮（北京时间 11:00–23:00）。工作目录：/workspace/x-creators。所有时间按北京时间显示。

## 规则（2026-10-13 起：只推历史热帖）
- 只看原创和引用帖（exclude=retweets,replies）。
- **推送目标只有历史热帖（爆款）**：一条推文满 24 小时后的第一轮会再刷新浏览量；≥10 万就追加进 `state/history.json`（记下当时数值），否则丢弃。
  `history.json` **只追加**，旧条目不改不删；报告里显示近 183 天的，按发布时间由近到远，全部列出。
- **不再推送「最新推文 / 今日热帖」**：例行扫描仍会拉近 24h 推文并刷新待毕业帖（供入榜判定），但普通新帖**不**出报告、**不**发「有新推文」配文。
- **推送条件**：某博主本轮有帖**新进入历史热帖** → `PUSH` 其报告；否则 → `NO_UPDATES`。整轮无人入榜时最终回复只写 `NO_UPDATES`。
- 报告文件只含历史热帖表：列 = # / 发布时间 / 浏览量 / 内容摘要 / 链接（**不显示点赞、转发**）；新入榜摘要前标 🆕。
- 报告路径：`reports/<昵称>_YYYYMMDD_HHMM.md`（昵称去掉 emoji 和非法字符；为空时用 handle）。同一博主只在 `reports/` 留最新一份，旧的自动移到 `reports/archive/`。
- **总览页**：`reports/历史热帖总览.html`（单页，标签=博主，点开看该博主全部历史热帖）。`finalize` / `report` / `seed-history` 会覆盖更新；也可手动 `python3 creators.py dashboard`。
- 阈值在 `creators.json` 的 settings：`daily_window_hours=24`、`history_min_views=100000`、`history_days=183`、
  `daily_max_pages_per_creator=3`、`history_max_rows=0`（0=全列）。`daily_min_views` 仅遗留字段，例行推送不再使用。

## 每轮步骤（严格按顺序）
1. `cd /workspace/x-creators && python3 creators.py plan`
   - 第一行 `RUN_DIR=...`，第二行 `CALLS=N`，后面每行一个 JSON：{save_as, namespace, toolName, paginate, max_pages, arguments}。
   - 一般是：0–1 次 get_users_by_usernames（超过 12 小时才查一次粉丝数，每 100 个博主 1 次）
     + 每个博主 1 次 get_users_posts（最近 24 小时，供后续毕业判定）
     + 每 100 条待刷新推文 1 次 get_posts_by_ids（24 小时内的 + 刚满 24 小时待毕业的）。
2. 按顺序逐行调用：`CallDynamicTool(namespace="x", toolName=<toolName>, arguments=<arguments 原样>)`。
   - arguments 里 id 是 "<从 00_users 结果取 id>" 时，换成 get_users_by_usernames 返回的该博主 id。
   - 每次调用完**立刻**把完整返回原样写入 save_as（用 heredoc，含末尾 "X reads left ..." 那行也可以）。
     文件修改时间就当作抓取时间，所以不要事后批量补写。get_posts_by_ids 那条必须最后调。
   - paginate=true 且 meta.result_count=25 且有 meta.next_token：加 "pagination_token": next_token 再调，
     存到 save_as 去掉 .json 后加 _p2.json、_p3.json…，不超过 max_pages。
   - 429：等 60 秒重试同一次调用 1 次；还是 429 就停止本轮剩余调用，直接做第 3、4 步，并在回复里注明「blocked by 429」。
   - 其他错误：记下来，继续下一条。
3. `python3 creators.py ingest RUN_DIR`
4. `python3 creators.py finalize RUN_DIR`
   - 每个博主输出一行：`PUSH<Tab>报告路径<Tab>配文` 或 `NO_UPDATES<Tab>@handle`。
   - 配文仅形如：「{昵称} 有 M 条推文进入历史热帖（HH:MM 采集）」——**禁止**「更新了 N 条新推文」类配文。
   - 同时覆盖更新 `reports/历史热帖总览.html`（无论本轮有无 PUSH）。

## 最终回复格式（routine 的最后一条消息）
- 有 PUSH 行时：每个文件两行，不加别的内容：
  ```
  /workspace/x-creators/reports/我明天不想说话_20261013_1500.md
  我明天不想说话 有 2 条推文进入历史热帖（15:00 采集）
  ```
  （配文直接用 finalize 给的第三列。）
- 全部是 NO_UPDATES 时：回复只写 `NO_UPDATES`。
- 遇到 429 时在最后另起一行写 `blocked by 429`。

## 用量（每天 1000 次，北京时间 08:00 重置；每分钟 30 次）
每轮 ≈ 每个博主 1 次 + 每 100 条待刷新推文 1 次 + 每 12 小时每 100 个博主 1 次。
1 个博主：每轮 2–3 次，一天 13 轮约 30 次。50 个博主：每轮约 51–55 次，一天约 700 次，接近上限，届时可改成每 2 小时一轮。

## 加新博主（一次性，不在 routine 里做）
1. creators.json 的 creators 加 {"username": "xxx"}（id 可留空）。
2. 历史回溯：
   A. `python3 creators.py plan --history HANDLE --pages 60`，按上面方法翻页（文件名不要带 daily）。
   B. 时间线翻到头（没有 next_token）还没到 183 天前时，改用 search_posts_all 补：
      query="from:HANDLE -is:retweet -is:reply"，start_time=183 天前，end_time=时间线最老一条的 created_at，
      max_results=25，sort_order=recency，post.fields=created_at,public_metrics,referenced_tweets,note_tweet（长帖全文在 note_tweet.text，TSV 的 text 列写全文），用 next_token 翻页，
      存成 NN_search_sK_<handle>.json；某页不满 25 条就翻完了。
   C. `python3 creators.py ingest RUN_DIR`，然后 `python3 creators.py seed-history`（把已满 24 小时、≥10 万的写进 history.json），
      再 `python3 creators.py report HANDLE` 看结果。
3. 首次加入可推送「已加入监控，历史热帖 N 条」；之后例行轮次仍只按「新入历史热帖」推送。零历史账号不要自动移除。

## 状态文件（state/）
posts.json（所有抓到的帖子、最新数据、first_run、graduated=history/discarded/gone）·
history.json（历史热帖，只追加）· creators_meta.json（昵称、粉丝数、回溯进度）· meta.json（剩余额度）。
seen_daily.json 是旧版用的，已不再使用。
脚本备份：creators.py.bak_20261009、creators.py.bak_20261009_1016、creators.py.bak_20261013_history_only。

## x-2 routine 意图
见同目录 `X2_ROUTINE_INTENT.md`。x-2 若处于 paused，改意图时保持 paused，除非需要跑代码验证。

## 全文补齐（2026-11-01 起，每次 deploy.sh 之前必做）
APP 要显示/复制完整文案。运行 `python3 /workspace/x-app/build.py && python3 /workspace/x-app/ft_missing.py`，
对输出的每一行（≤100 个 id）调用 x.get_posts_by_ids(ids=该行, post.fields="text,note_tweet", write_to_file=true)，
把结果文件 mv 到 /workspace/x-app/ft_raw/，再跑 `python3 /workspace/x-app/ft_merge.py`，然后 deploy.sh。
拿不到的（删帖/封号）进 gone.json，保留原文并在 APP 标注。
