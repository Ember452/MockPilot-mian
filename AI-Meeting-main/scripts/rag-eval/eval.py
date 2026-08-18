#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RAG 检索质量评测脚本
=====================
零依赖 Python 3，调后端 POST /api/xunzhi/v1/knowledge-bases/{kbId}/search-debug
逐条跑查询，取 top3 的 doc_id 判断是否命中 expected_doc_id，输出 recall@3 + P50/P95。

用法:
  python eval.py --fake --queries queries.json --label es-round1
  python eval.py --base-url http://localhost:8002 --token <token> --kb-id 1 --queries queries.json --label es-round1
"""

import argparse
import json
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

# ── 伪造数据常量 ──────────────────────────────────────────────

_DOC_FILES = {
    "doc_redis_001":  "Redis持久化机制详解.pdf",
    "doc_java_002":   "Java并发编程：synchronized与Lock.pdf",
    "doc_spring_003": "Spring Boot自动配置原理.pdf",
    "doc_mysql_004":  "MySQL索引优化实战.pdf",
    "doc_docker_005": "Docker容器化部署最佳实践.pdf",
    "doc_kafka_006":  "Kafka消息队列原理.pdf",
    "doc_es_007":     "Elasticsearch搜索引擎.pdf",
    "doc_nginx_008":  "Nginx反向代理与负载均衡.pdf",
    "doc_micro_009":  "微服务架构设计模式.pdf",
    "doc_pattern_010": "Java设计模式实战.pdf",
}

_DISTRACTORS = {
    "doc_redis_001":  ["doc_mysql_004", "doc_docker_005", "doc_java_002"],
    "doc_java_002":   ["doc_spring_003", "doc_redis_001", "doc_mysql_004"],
    "doc_spring_003": ["doc_java_002", "doc_docker_005", "doc_redis_001"],
    "doc_mysql_004":  ["doc_redis_001", "doc_spring_003", "doc_java_002"],
    "doc_docker_005": ["doc_spring_003", "doc_mysql_004", "doc_redis_001"],
    "doc_kafka_006":  ["doc_es_007", "doc_micro_009", "doc_docker_005"],
    "doc_es_007":     ["doc_kafka_006", "doc_nginx_008", "doc_mysql_004"],
    "doc_nginx_008":  ["doc_micro_009", "doc_es_007", "doc_spring_003"],
    "doc_micro_009":  ["doc_nginx_008", "doc_kafka_006", "doc_docker_005"],
    "doc_pattern_010": ["doc_java_002", "doc_spring_003", "doc_micro_009"],
}

_BOUNDARY_QUERIES = {
    # 20 条评测集边界查询
    "数据断电后怎么恢复",
    "线程安全有哪些实现手段",
    "应用容器化怎么做",
    "Spring 框架怎么简化开发的",
    "数据库查询太慢怎么办",
    "容器镜像怎么构建",
    # 100 条评测集边界查询（增量）
    "缓存怎么持久化到磁盘",
    "并发编程中怎么避免死锁",
    "框架怎么做到零配置启动",
    "查询慢了怎么优化",
    "容器怎么部署到生产环境",
    "消息队列选型对比",
    "全文检索技术方案",
    "高并发架构设计",
    "分布式系统怎么拆分",
    "代码重构方法",
}


def _make_chunk(doc_id, chunk_idx, rerank_score):
    return {
        "chunk_id":    "chunk_{}_{}".format(doc_id, chunk_idx),
        "doc_id":      doc_id,
        "file_name":   _DOC_FILES.get(doc_id, "unknown.pdf"),
        "chunk_index": chunk_idx,
        "rrf_score":   round(0.028 + hash(doc_id + str(chunk_idx)) % 8 * 0.001, 4),
        "rerank_score": round(rerank_score, 4),
        "content":     "[{}] 这是第{}个分块的内容摘要...".format(doc_id, chunk_idx),
    }


def build_fake_response(query, expected_doc_id, seed):
    import random
    rng = random.Random(hash(query + str(seed)) & 0x7FFFFFFF)

    took_ms = rng.randint(120, 165)
    distractors = _DISTRACTORS.get(expected_doc_id,
                                   ["doc_redis_001", "doc_java_002", "doc_spring_003"])

    is_boundary = query in _BOUNDARY_QUERIES

    chunks = []
    if is_boundary:
        # 边界查询故意不返回 expected_doc_id，模拟检索失败
        chunks.append(_make_chunk(distractors[0], rng.randint(0, 15), 0.78 + rng.random() * 0.10))
        chunks.append(_make_chunk(distractors[1], rng.randint(0, 15), 0.72 + rng.random() * 0.06))
        chunks.append(_make_chunk(distractors[2], rng.randint(0, 15), 0.65 + rng.random() * 0.10))
    else:
        chunks.append(_make_chunk(expected_doc_id, rng.randint(0, 15), 0.82 + rng.random() * 0.12))
        chunks.append(_make_chunk(expected_doc_id, rng.randint(0, 15), 0.70 + rng.random() * 0.10))
        chunks.append(_make_chunk(distractors[rng.randint(0, 2)], rng.randint(0, 15),
                                  0.60 + rng.random() * 0.15))

    return took_ms, chunks


# ── 真实模式：调后端 API ──────────────────────────────────────

def call_search_debug(base_url, token, kb_id, query, top_k=5):
    url = "{}/api/xunzhi/v1/knowledge-bases/{}/search-debug".format(
        base_url.rstrip("/"), kb_id)
    payload = json.dumps({"query": query, "topK": top_k}).encode("utf-8")
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
    except urllib.error.HTTPError as e:
        print("  [ERROR] HTTP {}: {}".format(e.code, e.read().decode()[:300]), file=sys.stderr)
        return None, None
    except Exception as e:
        print("  [ERROR] {}".format(e), file=sys.stderr)
        return None, None

    if body.get("code") != "0":
        print("  [ERROR] API error: {}".format(body.get("message", "unknown")), file=sys.stderr)
        return None, None

    data = body.get("data", {})
    return data.get("tookMs"), data.get("chunks", [])


# ── 评测核心 ──────────────────────────────────────────────────

def percentile(sorted_values, p):
    if not sorted_values:
        return 0
    k = (len(sorted_values) - 1) * p / 100.0
    f = int(k)
    c = k - f
    if f + 1 < len(sorted_values):
        return sorted_values[f] + c * (sorted_values[f + 1] - sorted_values[f])
    return sorted_values[f]


def evaluate(queries, base_url, token, kb_id, fake):
    hit = 0
    total = 0
    server_lats = []
    e2e_lats = []
    details = []

    for i, item in enumerate(queries, 1):
        query = item["query"]
        expected = item["expected_doc_id"]
        total += 1

        if fake:
            t0 = time.time()
            took_ms, chunks = build_fake_response(query, expected, i)
            e2e_ms = int((time.time() - t0) * 1000)
        else:
            t0 = time.time()
            took_ms, chunks = call_search_debug(base_url, token, kb_id, query)
            e2e_ms = int((time.time() - t0) * 1000)

        if took_ms is None:
            details.append({
                "query": query, "expected": expected, "hit": False,
                "top3_docs": [], "server_ms": 0, "e2e_ms": e2e_ms, "error": True,
            })
            continue

        top3_docs = [c["doc_id"] for c in (chunks or [])[:3]]
        is_hit = expected in top3_docs
        if is_hit:
            hit += 1

        server_lats.append(took_ms)
        e2e_lats.append(e2e_ms)
        details.append({
            "query": query, "expected": expected, "hit": is_hit,
            "top3_docs": top3_docs, "server_ms": took_ms, "e2e_ms": e2e_ms,
            "error": False,
        })

        status = "OK" if is_hit else "MISS"
        print("  [{:02d}/{:02d}] {} | {:>4d}ms | {}".format(
            i, len(queries), status, took_ms, query[:50]))

    recall = hit / total if total > 0 else 0.0
    return recall, hit, total, server_lats, e2e_lats, details


# ── 主入口 ────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="RAG 检索质量评测：recall@3 + P50/P95",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="示例:\n"
               "  python eval.py --fake --queries queries.json --label es-round1\n"
               "  python eval.py --base-url http://localhost:8002 --token abc --kb-id 1 --queries queries.json --label es-round1",
    )
    parser.add_argument("--base-url", default="http://localhost:8002")
    parser.add_argument("--token", default="")
    parser.add_argument("--kb-id", type=int, default=1)
    parser.add_argument("--queries", default="queries.json")
    parser.add_argument("--label", default="default")
    parser.add_argument("--fake", action="store_true")
    args = parser.parse_args()

    queries_path = Path(args.queries)
    if not queries_path.exists():
        print("查询文件不存在: {}".format(queries_path), file=sys.stderr)
        sys.exit(1)
    with open(queries_path, "r", encoding="utf-8") as f:
        queries = json.load(f)

    if not queries:
        print("查询集为空", file=sys.stderr)
        sys.exit(1)

    print("")
    print("=" * 60)
    print("  RAG 检索质量评测")
    print("  Label  : {}".format(args.label))
    if args.fake:
        print("  Mode   : FAKE (本地模拟数据)")
    else:
        print("  Mode   : REAL ({})".format(args.base_url))
    print("  Queries: {} 条".format(len(queries)))
    print("=" * 60)
    print("")

    recall, hit, total, server_lats, e2e_lats, details = evaluate(
        queries, args.base_url, args.token, args.kb_id, args.fake,
    )

    server_lats.sort()
    e2e_lats.sort()

    server_p50 = percentile(server_lats, 50)
    server_p95 = percentile(server_lats, 95)
    e2e_p50 = percentile(e2e_lats, 50)
    e2e_p95 = percentile(e2e_lats, 95)

    print("")
    print("=" * 60)
    print("  评测结果 [{}]".format(args.label))
    print("=" * 60)
    print("  recall@3       : {:.1f}% ({}/{})".format(recall * 100, hit, total))
    print("  服务端耗时 P50  : {:.0f}ms".format(server_p50))
    print("  服务端耗时 P95  : {:.0f}ms".format(server_p95))
    print("  端到端耗时 P50  : {:.0f}ms".format(e2e_p50))
    print("  端到端耗时 P95  : {:.0f}ms".format(e2e_p95))
    print("=" * 60)

    print("")
    print("  逐条明细:")
    print("  " + "-" * 56)
    for i, d in enumerate(details, 1):
        flag = "HIT" if d["hit"] else "MISS"
        docs = ", ".join(d["top3_docs"][:3]) if d["top3_docs"] else "(empty)"
        print("  [{:02d}] {:4s} | {:>4d}ms | {}".format(i, flag, d["server_ms"], d["query"][:40]))
        if not d["hit"]:
            print("         expected={}, got={}".format(d["expected"], docs))

    misses = [d for d in details if not d["hit"]]
    if misses:
        print("")
        print("  失败案例 ({} 条):".format(len(misses)))
        for d in misses:
            print("    - [{}] expected={}".format(d["query"], d["expected"]))

    result_file = Path("result_{}.json".format(args.label))
    result = {
        "label": args.label,
        "mode": "fake" if args.fake else "real",
        "total": total,
        "hit": hit,
        "recall_at_3": round(recall, 4),
        "server_p50_ms": round(server_p50, 0),
        "server_p95_ms": round(server_p95, 0),
        "e2e_p50_ms": round(e2e_p50, 0),
        "e2e_p95_ms": round(e2e_p95, 0),
        "details": details,
    }
    with open(str(result_file), "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    print("")
    print("  结果已保存至: {}".format(result_file))


if __name__ == "__main__":
    main()