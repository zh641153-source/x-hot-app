#!/usr/bin/env python3
"""简中 24h 十万+热帖日报。
plan  -> 打印 RUN_DIR 和每条 search_posts_all 查询（from: 账号池分组）
ingest RUN_DIR -> 合并结果+可选 x-creators 当日数据，筛 ≥阈值、简中、24h 内、未收录，更新 MD 和 state。
stdout: 'NO_RESULTS' 或 'NEW <n>' + top3 行。
"""
import json, os, sys, glob, re
from datetime import datetime, timedelta, timezone
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, "/workspace/x-monitor/connector")
from process import lang_check, read_response, extract  # noqa
BJ = timezone(timedelta(hours=8))
MD = os.path.join(HERE, "简中24h十万热帖.md")
STATE = os.path.join(HERE, "state.json")
POOL = os.path.join(HERE, "pool.json")
MIN_VIEWS = 100000
EXCLUDE = {"justinsuntron"}  # 主要英文
BLOCK = {h.lower() for h in json.load(open(os.path.join(HERE, "blocklist.json"))).get("blocked", [])} if os.path.exists(os.path.join(HERE, "blocklist.json")) else set()

def load(p, d):
    try: return json.load(open(p, encoding="utf-8"))
    except Exception: return d

def save(p, o):
    t = p + ".tmp"; json.dump(o, open(t, "w", encoding="utf-8"), ensure_ascii=False, indent=1); os.replace(t, p)

def pool():
    pl = load(POOL, None)
    if pl: return [h for h in pl if h.lower() not in BLOCK]
    acc = load("/workspace/x-monitor/connector/state/watchlist.json", {}).get("accounts", {})
    sel = [a["username"] for a in acc.values() if not a.get("disabled") and a["username"] not in EXCLUDE
           and ((a.get("followers") or 0) >= 100000 or (a.get("peak30") or 0) >= 1500)]
    save(POOL, sel); return sel

def cmd_plan():
    now = datetime.now(timezone.utc)
    start = (now - timedelta(hours=24)).strftime("%Y-%m-%dT%H:%M:%SZ")
    run = os.path.join(HERE, "runs", now.astimezone(BJ).strftime("%Y%m%d_%H%M"))
    os.makedirs(run, exist_ok=True)
    groups, cur = [], []
    suffix = ") -is:retweet -is:reply"
    for h in pool():
        cand = cur + [h]
        q = "(" + " OR ".join("from:" + x for x in cand) + suffix
        if len(q) > 1000 and cur:
            groups.append(cur); cur = [h]
        else:
            cur = cand
    if cur: groups.append(cur)
    print("RUN_DIR=" + run)
    for i, g in enumerate(groups, 1):
        args = {"query": "(" + " OR ".join("from:" + x for x in g) + suffix, "start_time": start,
                "max_results": 25, "sort_order": "recency",
                "post.fields": "created_at,public_metrics,author_id,lang,note_tweet", "expansions": "author_id",
                "user.fields": "name,username"}
        print(json.dumps({"save_as": os.path.join(run, "%02d.json" % i), "arguments": args}, ensure_ascii=False))
    save(os.path.join(run, "plan.json"), {"start": start, "now": now.isoformat()})

FT = {}

def trim(t, n=60):
    t = re.sub(r"https?://\S+", "", t or "")
    t = re.sub(r"\s+", " ", t).strip().replace("|", "｜")
    return (t[:n] + "…") if len(t) > n else (t or "（无文字）")

def cmd_ingest(run):
    plan = load(os.path.join(run, "plan.json"), {})
    start = datetime.strptime(plan["start"], "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)
    st = load(STATE, {"seen": {}})
    posts, users = {}, {}
    files = sorted(glob.glob(os.path.join(run, "*.json")))
    # 复用博主监控当天数据（只读）
    xc = sorted(glob.glob("/workspace/x-creators/raw/*/"))
    if xc:
        files += sorted(glob.glob(os.path.join(xc[-1], "*_daily_*.json"))) + sorted(glob.glob(os.path.join(xc[-1], "90_ids_*.json")))
    for f in files:
        if f.endswith("plan.json"): continue
        obj, _ = read_response(f)
        ps, us = extract(obj)
        users.update(us)
        for p in ps:
            old = posts.get(p["id"])
            v = (p.get("public_metrics") or {}).get("impression_count") or 0
            if not old or v >= ((old.get("public_metrics") or {}).get("impression_count") or 0):
                posts[p["id"]] = dict(p, author_id=p.get("author_id") or (old or {}).get("author_id"))
    rows, stats = [], {"total": len(posts)}
    for pid, p in posts.items():
        ca = datetime.strptime(p["created_at"][:19], "%Y-%m-%dT%H:%M:%S").replace(tzinfo=timezone.utc)
        v = (p.get("public_metrics") or {}).get("impression_count") or 0
        if ca < start or v < MIN_VIEWS or pid in st["seen"]: continue
        if (p.get("text") or "").startswith("RT @"): continue
        ok, why = lang_check(p.get("text") or "", p.get("lang"))
        if not ok: stats[why[:2]] = stats.get(why[:2], 0) + 1; continue
        u = users.get(str(p.get("author_id")), {})
        if (u.get("username") or "").lower() in BLOCK: continue
        full = (p.get("note_tweet") or {}).get("text") or p.get("text") or ""
        FT[pid] = full
        rows.append((v, pid, u.get("name") or "", u.get("username") or "", ca, full))
    print("STATS", json.dumps(stats, ensure_ascii=False), file=sys.stderr)
    if not rows:
        print("NO_RESULTS"); return
    rows.sort(key=lambda r: -r[0])
    today = datetime.now(BJ).strftime("%Y-%m-%d")
    sec = ["## %s（北京时间）" % today, "", "共 %d 条。" % len(rows), "",
           "| 浏览量 | 作者 | 发布时间（北京） | 内容摘要 | 链接 |", "| ---: | --- | --- | --- | --- |"]
    for v, pid, name, un, ca, text in rows:
        link = "https://x.com/%s/status/%s" % (un or "i", pid)
        sec.append("| {:,} | {} @{} | {} | {} | [打开]({}) |".format(v, name.replace("|", "｜"), un,
                   ca.astimezone(BJ).strftime("%m-%d %H:%M"), trim(text), link))
        st["seen"][pid] = today
    old = open(MD, encoding="utf-8").read() if os.path.exists(MD) else ""
    head = "# 简中 24h 十万+热帖\n\n每天一段，新日期在最上方。浏览量为采集时数值。\n\n"
    body = old[len(head):] if old.startswith(head) else old
    open(MD + ".tmp", "w", encoding="utf-8").write(head + "\n".join(sec) + "\n\n" + body)
    os.replace(MD + ".tmp", MD)
    ftp = "/workspace/x-app/fulltext.json"; ft = load(ftp, {}); ft.update({k: v for k, v in FT.items() if v}); json.dump(ft, open(ftp, "w"), ensure_ascii=False)
    save(STATE, st)
    print("NEW %d" % len(rows))
    for v, pid, name, un, ca, text in rows[:3]:
        print("%s(@%s) %.1f万 %s" % (name, un, v / 10000, trim(text, 30)))

if __name__ == "__main__":
    {"plan": lambda: cmd_plan(), "ingest": lambda: cmd_ingest(sys.argv[2])}[sys.argv[1]]()
