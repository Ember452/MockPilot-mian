#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RAG 检索压测脚本
================
零依赖 Python 3，并发调 POST /api/xunzhi/v1/knowledge-bases/{kbId}/search-debug，
输出 P50 / P95 / P99 / avg / max / QPS / 成功率。

用法:
  python stress.py --base-url http://localhost:8002 --token <token> --kb-id 1 --concurrency 10 --rounds 50
  python stress.py --base-url http://localhost:8002 --token <token> --kb-id 1 -c 5 -r 100 --queries queries_100.json
"""

import argparse
import json
import sys
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path


def percentile(sorted_values, p):
    if not sorted_values:
        return 0
    k = (len(sorted_values) - 1) * p / 100.0
    f = int(k)
    c = k - f
    if f + 1 < len(sorted_values):
        return sorted_values[f] + c * (sorted_values[f + 1] - sorted_values[f])
    return sorted_values[f]


def call_search_debug(base_url, token, kb_id, query):
    url = "{}/api/xunzhi/v1/knowledge-bases/{}/search-debug".format(
        base_url.rstrip("/"), kb_id)
    payload = json.dumps({"query": query, "topK": 5}).encode("utf-8")
    req = urllib.request.Request(
        url, data=payload, method="POST",
        headers={
            "Content-Type": "application/json",
            "Authorization": "Bearer {}".format(token),
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            body = json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        return None, str(e)

    if body.get("code") != "0":
        return None, body.get("message", "unknown")
    data = body.get("data", {})
    return data.get("tookMs"), None


def run_one(query, base_url, token, kb_id):
    t0 = time.perf_counter()
    took_ms, err = call_search_debug(base_url, token, kb_id, query)
    e2e_ms = (time.perf_counter() - t0) * 1000
    return {
        "query": query,
        "server_ms": took_ms,
        "e2e_ms": e2e_ms,
        "error": err,
    }


def main():
    parser = argparse.ArgumentParser(description="RAG 检索压测：P50/P95/P99 + QPS")
    parser.add_argument("--base-url", default="http://localhost:8002")
    parser.add_argument("--token", default="")
    parser.add_argument("--kb-id", type=int, default=1)
    parser.add_argument("--queries", default="queries_100.json",
                        help="评测查询集，轮询复用")
    parser.add_argument("-c", "--concurrency", type=int, default=10,
                        help="并发线程数")
    parser.add_argument("-r", "--rounds", type=int, default=50,
                        help="总请求轮数")
    parser.add_argument("--output", default="",
                        help="结果输出文件（可选）")
    args = parser.parse_args()

    queries_path = Path(args.queries)
    if not queries_path.exists():
        print("查询文件不存在: {}".format(queries_path), file=sys.stderr)
        sys.exit(1)
    with open(queries_path, "r", encoding="utf-8") as f:
        queries = [item["query"] for item in json.load(f)]

    if not queries:
        print("查询集为空", file=sys.stderr)
        sys.exit(1)

    print("")
    print("=" * 60)
    print("  RAG 检索压测")
    print("  URL       : {}/api/xunzhi/v1/knowledge-bases/{}/search-debug".format(
        args.base_url.rstrip("/"), args.kb_id))
    print("  Queries   : {} 条 (轮询)".format(len(queries)))
    print("  Concurrency : {}".format(args.concurrency))
    print("  Rounds      : {}".format(args.rounds))
    print("=" * 60)
    print("")

    results = []
    errors = []
    t_start = time.perf_counter()

    with ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        futures = []
        for i in range(args.rounds):
            query = queries[i % len(queries)]
            futures.append(pool.submit(run_one, query, args.base_url, args.token, args.kb_id))

        for i, f in enumerate(as_completed(futures), 1):
            r = f.result()
            if r["error"]:
                errors.append(r)
                print("  [{:03d}/{:03d}] ERROR | {:.0f}ms | {}".format(
                    i, args.rounds, r["e2e_ms"], r["error"]))
            else:
                results.append(r)
                print("  [{:03d}/{:03d}] OK   | {:>5.0f}ms | {}".format(
                    i, args.rounds, r["server_ms"], r["query"][:40]))

    total_sec = time.perf_counter() - t_start
    qps = args.rounds / total_sec if total_sec > 0 else 0

    server_lats = sorted([r["server_ms"] for r in results if r["server_ms"] is not None])
    e2e_lats = sorted([r["e2e_ms"] for r in results])

    print("")
    print("=" * 60)
    print("  压测结果")
    print("=" * 60)
    print("  总请求      : {}".format(args.rounds))
    print("  成功        : {}".format(len(results)))
    print("  失败        : {}".format(len(errors)))
    print("  成功率      : {:.1f}%".format(
        len(results) / args.rounds * 100 if args.rounds else 0))
    print("  总耗时      : {:.1f}s".format(total_sec))
    print("  QPS         : {:.1f}".format(qps))
    print("  " + "-" * 40)
    if server_lats:
        avg = sum(server_lats) / len(server_lats)
        print("  服务端耗时")
        print("    avg        : {:.0f}ms".format(avg))
        print("    P50        : {:.0f}ms".format(percentile(server_lats, 50)))
        print("    P95        : {:.0f}ms".format(percentile(server_lats, 95)))
        print("    P99        : {:.0f}ms".format(percentile(server_lats, 99)))
        print("    max        : {:.0f}ms".format(server_lats[-1]))
    if e2e_lats:
        print("  端到端耗时")
        print("    avg        : {:.0f}ms".format(sum(e2e_lats) / len(e2e_lats)))
        print("    P50        : {:.0f}ms".format(percentile(e2e_lats, 50)))
        print("    P95        : {:.0f}ms".format(percentile(e2e_lats, 95)))
        print("    P99        : {:.0f}ms".format(percentile(e2e_lats, 99)))
        print("    max        : {:.0f}ms".format(e2e_lats[-1]))
    print("=" * 60)

    if errors:
        print("")
        print("  失败明细:")
        for e in errors:
            print("    - {} | {}".format(e["query"][:40], e["error"]))

    if args.output:
        output = {
            "total": args.rounds,
            "success": len(results),
            "fail": len(errors),
            "success_rate": round(len(results) / args.rounds, 4) if args.rounds else 0,
            "total_sec": round(total_sec, 2),
            "qps": round(qps, 1),
            "server_avg_ms": round(sum(server_lats) / len(server_lats), 0) if server_lats else 0,
            "server_p50_ms": round(percentile(server_lats, 50), 0),
            "server_p95_ms": round(percentile(server_lats, 95), 0),
            "server_p99_ms": round(percentile(server_lats, 99), 0),
            "server_max_ms": server_lats[-1] if server_lats else 0,
        }
        with open(args.output, "w", encoding="utf-8") as f:
            json.dump(output, f, ensure_ascii=False, indent=2)
        print("")
        print("  结果已保存至: {}".format(args.output))


if __name__ == "__main__":
    main()