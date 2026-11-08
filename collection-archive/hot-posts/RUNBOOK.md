# 简中24h十万+热帖日报（routine 24h，每天 21:30 北京）
独立于 /workspace/x-creators（博主监控只读复用其当天 raw 数据，不写入）。
1. `python3 zh24h.py plan` → RUN_DIR + 每组 from: 查询（账号池 pool.json，已剔除 pool_skip.json 的高频低浏览号）。
2. 每组用 x/search_posts_all（write_to_file=true）按 arguments 调用，`python3 save.py RUN_DIR NN[-pK].json` 保存并打印 剩余额度/条数/最早时间/next_token；
   只要有 next_token 且最早时间未到 start 就继续翻页（result_count<25 也可能还有下一页）。2026-10-21 首跑：~36 次调用、867 条。
3. `python3 zh24h.py ingest RUN_DIR` → stdout `NO_RESULTS` 或 `NEW n` + Top3；MD 顶部插入当天段落，state.json 记已收 id。
局限：X 搜索无浏览量/点赞过滤，只能覆盖账号池内账号；池外的爆帖抓不到。
4. 永久黑名单 blocklist.json（用户 2026-11-01：李老师@whyyoutouzhele、立党@lidangzzz、王局志安@wangzhian8848）：plan 不查、ingest 丢弃，禁止加回 pool.json。

## 手动加链接（2026-11-01 起，自动池扫描已暂停）
用户发帖子链接 → x.get_posts_by_id 取 text/作者/created_at/impression_count →
`python3 /workspace/x-zh-24h/add_link.py '{"id":..,"handle":..,"name":..,"created_at":..,"views":..,"text":..}'`
（加入当天北京日期段，按浏览量排序，更新"共 N 条"与 state.json；不足10万也照实记录）→ 运行 `/workspace/x-app/deploy.sh`。

## 全文（2026-11-01 起）
单个链接：get_posts_by_id 时 post.fields 带 note_tweet，传给 add_link.py 的 text 用 note_tweet.text（没有则 text）。
add_link.py 会把全文写入 /workspace/x-app/fulltext.json，APP 显示全文。deploy 前可跑 x-app/ft_missing.py 补漏（见 x-creators/RUNBOOK.md）。
