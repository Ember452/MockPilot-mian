# 简历量化数据测试指南（基于真实代码）

本文档给出三组量化测试的完整操作步骤，产出的数字直接回填简历对应条目。
所有指标名、接口路径、配置项均来自当前代码，无需新增业务代码。

| # | 测试 | 产出指标 | 回填简历条目 |
|---|------|---------|-------------|
| 1 | RAG 检索质量评测 | recall@3、检索延迟 P50/P95、ES vs Milvus 对比 | RAG 知识库 |
| 2 | 分布式 Single-flight 去重 | 去重率 = hit/(hit+miss)、LLM 调用次数收敛 | 分布式 Single-flight |
| 3 | 答题主链路并发一致性 | N 并发重复提交 → 1 次评分、幂等回放命中数、锁竞争数 | 答题主链路 |

---

## 0. 前置准备（三组测试共用）

### 0.1 启动系统

```powershell
cd d:\DEVELOP\java\MockPilot-project\MockPilot-mian
docker compose up -d --build
```

等待 `mockpilot-backend` 健康（`docker compose ps` 显示 healthy），后端端口 `8002`。

### 0.2 临时开放 metrics 端点

代码中 actuator 默认只暴露 `health,info`（见 `admin/src/main/resources/application.yaml` 的
`management.endpoints.web.exposure.include`），测试期需追加 `metrics`。
**不改源码**，用环境变量覆盖：在 `docker-compose.yml` 的 `backend.environment` 临时加一行
（或写入 0.4 节的 bench override 文件）：

```yaml
      MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: health,info,metrics
```

重启后端后验证：

```powershell
curl http://localhost:8002/actuator/metrics
# 读单个指标（示例）
curl http://localhost:8002/actuator/metrics/idempotency_replay_hit_total
```

> 注意：这些计数器是**实例内存级**的，多实例场景要分别读取每个实例再求和；重启后清零。
> 测试结束后移除该环境变量。

### 0.3 获取登录 token

```powershell
curl -X POST http://localhost:8002/api/xunzhi/v1/users/login `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"<用户名>\",\"password\":\"<密码>\"}'
```

从响应中取 token，后续请求以 `Authorization: Bearer <token>` 携带（与 `scripts/rag-eval/eval.py` 的用法一致）。

### 0.4 多实例部署（仅测试 2 需要）

Single-flight 的核心卖点是**跨节点**去重，必须起第二个后端实例，共用同一套
Redis/MySQL/Mongo。新建 `docker-compose.bench.yml`（与 `docker-compose.yml` 同目录）：

```yaml
# 压测专用：第二个后端实例 + 开放 metrics 端点
services:
  backend:
    environment:
      MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: health,info,metrics

  backend-2:
    extends:
      file: docker-compose.yml
      service: backend
    container_name: mockpilot-backend-2
    ports: !override
      - "8003:8002"
    environment:
      MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: health,info,metrics
```

启动：

```powershell
docker compose -f docker-compose.yml -f docker-compose.bench.yml up -d
curl http://localhost:8003/actuator/health   # 确认实例 2 就绪
```

> `ports: !override` 需要 Docker Compose v2.24+；版本较低就把 backend-2 的整段 environment
> 从 docker-compose.yml 复制过来，不用 extends。
> 两实例共享 `backend-data` 卷仅测试期可接受，测试完 `docker compose -f ... -f ... down` 移除实例 2。

### 0.5 准备一场进行中的面试会话

测试 2、3 需要真实的 `sessionId` 和 `questionNumber`：

1. 打开前端（http://localhost），登录后开始一场面试；
2. F12 → Network，找到任一 `/api/xunzhi/v1/interview/sessions/{sessionId}/...` 请求，
   记下 `sessionId`、当前题目的 `questionNumber`，以及请求头里的 token。

---

## 1. RAG 检索质量评测（recall@3 / P50 / P95 / 双引擎）

**用什么**：现成脚本 `scripts/rag-eval/eval.py`（零依赖 Python 3），调用后端
`search-debug` 接口，自动输出 recall@3 与服务端/端到端 P50/P95。

**怎么做**（详细说明见 `scripts/rag-eval/README.md`）：

1. **建标注集**：前端建知识库并上传 5～10 篇你熟悉的技术文档，等 ETL 完成（文档状态=2）。
   调 `GET /api/xunzhi/v1/knowledge-bases/{kbId}/documents` 拿各文档 `docId`；
   复制 `queries.example.json` 为 `queries.json`，写约 20 条查询，每条填 `expected_doc_id`。
   查询要覆盖两类：与文档标题字面接近的（考关键词路）、换了说法的语义改写（考向量路）。
2. **ES 基线**：
   ```powershell
   cd d:\DEVELOP\java\MockPilot-project\scripts\rag-eval
   python eval.py --base-url http://localhost:8002 --token <token> --kb-id <kbId> --queries queries.json --label elasticsearch
   ```
3. **Milvus 对比**（可选，双引擎对比数据）：`.env` 设 `RAG_VECTOR_STORE=milvus`，
   `docker compose --profile milvus up -d`，`docker compose up -d --force-recreate backend`，
   新建知识库重传同一批文档后再跑一轮，`--label milvus`。
4. **记录**：两轮输出的 `recall@3`、`服务端耗时 P50/P95` 直接抄进下方模板。

**简历写法示例**（用真实数字替换）：

> 自建 20 条中文技术问答评测集验证：混合检索 recall@3 达 __%，检索延迟 P95 __ms（ES/Milvus 双引擎对比验证）。

> 进阶（可选）：若想量化"混合检索 vs 纯向量"的提升，需在 `HybridSearchService` 临时注释
> BM25 一路再跑一轮对比。属于改代码实验，做完记得还原，简历中表述为离线对比实验即可。

---

## 2. 分布式 Single-flight 去重率（多实例）

**原理**：答题请求触发评分（stage `interview-evaluation`），Single-flight 以
"业务键（会话+题目+阶段）"为粒度在 Redis 协调 owner/follower——同一时刻只有 owner 真正调
LLM，follower 等待并回放结果。代码位置：
`interview/application/guard/singleflight/`，指标埋点在 `InterviewAiSingleFlightService`：

- `ai_singleflight_hit_total`：命中去重（未发起新 LLM 调用）
- `ai_singleflight_miss_total`：未命中（真实发起 LLM 调用）

**方法**：把**同一题的答案**同时打到两个实例（8002/8003）。注意每个请求用**不同的
`requestId`**——否则会先被幂等门禁拦下（那是测试 3 的内容），到不了 Single-flight 层。

### 2.1 压测脚本

保存为 `scripts/bench/answer_bench.py`（零依赖）：

```python
import argparse, json, threading, time, urllib.request, uuid

def post(url, token, payload, results, idx):
    req = urllib.request.Request(
        url, data=json.dumps(payload).encode(), method="POST",
        headers={"Content-Type": "application/json",
                 "Authorization": "Bearer " + token})
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            results[idx] = (resp.status, int((time.time()-t0)*1000), resp.read().decode()[:300])
    except Exception as e:
        results[idx] = ("ERR", int((time.time()-t0)*1000), str(e)[:300])

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--targets", required=True, help="逗号分隔，如 http://localhost:8002,http://localhost:8003")
    p.add_argument("--token", required=True)
    p.add_argument("--session-id", required=True)
    p.add_argument("--question-number", required=True)
    p.add_argument("--answer", default="这是压测答案：HashMap 基于数组+链表/红黑树，扩容阈值为容量*负载因子……")
    p.add_argument("--concurrency", type=int, default=10)
    p.add_argument("--shared-request-id", action="store_true",
                   help="所有请求共用一个 requestId（测幂等）；默认各自随机（测 single-flight）")
    args = p.parse_args()

    targets = args.targets.split(",")
    shared_rid = uuid.uuid4().hex if args.shared_request_id else None
    results = [None] * args.concurrency
    threads = []
    for i in range(args.concurrency):
        url = targets[i % len(targets)].rstrip("/") + \
              f"/api/xunzhi/v1/interview/sessions/{args.session_id}/interview/answer-json"
        payload = {"questionNumber": args.question_number,
                   "answerContent": args.answer,
                   "sessionId": args.session_id,
                   "requestId": shared_rid or uuid.uuid4().hex}
        threads.append(threading.Thread(target=post, args=(url, args.token, payload, results, i)))
    t0 = time.time()
    for t in threads: t.start()
    for t in threads: t.join()

    print(f"\n总耗时 {int((time.time()-t0)*1000)}ms，并发 {args.concurrency}，实例数 {len(targets)}")
    for i, (status, ms, body) in enumerate(results):
        print(f"[{i:02d}] status={status} {ms}ms {body[:120]}")
    bodies = {}
    for status, _, body in results:
        if status == 200:
            bodies.setdefault(body, 0)
            bodies[body] += 1
    print(f"\n200 响应 {sum(bodies.values())} 个，去重后不同响应体 {len(bodies)} 种（期望 1 种 = 结果回放一致）")

if __name__ == "__main__":
    main()
```

### 2.2 执行步骤

```powershell
# 1) 记录基线（两个实例都要读）
curl http://localhost:8002/actuator/metrics/ai_singleflight_hit_total
curl http://localhost:8002/actuator/metrics/ai_singleflight_miss_total
curl http://localhost:8003/actuator/metrics/ai_singleflight_hit_total
curl http://localhost:8003/actuator/metrics/ai_singleflight_miss_total

# 2) 双实例并发打同一题（不同 requestId）
python scripts\bench\answer_bench.py `
  --targets http://localhost:8002,http://localhost:8003 `
  --token <token> --session-id <sessionId> --question-number <questionNumber> `
  --concurrency 10

# 3) 复读四个指标，计算增量
```

**辅助验证**（面试时能讲的证据链）：

```powershell
# Redis 中的 flight 协调键（meta/result/owner-seq）
docker exec mockpilot-redis redis-cli --scan --pattern "ai:flight:*"
# 两实例日志中 owner 执行 / follower 等待回放的痕迹
docker logs mockpilot-backend --since 5m | Select-String -Pattern "flight"
docker logs mockpilot-backend-2 --since 5m | Select-String -Pattern "flight"
```

### 2.3 计算与记录

- ΔLLM 真实调用数 = 两实例 `miss_total` 增量之和（期望 **1**）
- Δ命中回放数 = 两实例 `hit_total` 增量之和（期望 **并发数 - 1**）
- **去重率 = Δhit / (Δhit + Δmiss)**（10 并发期望 90%）
- 脚本输出"不同响应体 1 种"即证明结果回放一致

用不同并发（5/10/20）跑 3 轮取稳定值。**简历写法示例**：

> 双实例 10 并发同请求压测下，LLM 真实调用从 10 次收敛为 1 次（去重率 90%），follower 结果回放一致。

---

## 3. 答题主链路并发一致性（幂等 + 题级锁）

**原理**：`InterviewAnswerPipeline` 入口先过幂等门禁（`sessionId + requestId`，命中已成功
请求直接回放、处理中请求快速失败），再抢题级分布式锁（`InterviewQuestionLockService`）。
相关指标（均在 `InterviewAnswerPipeline` 中埋点）：

- `idempotency_replay_hit_total`：幂等命中回放次数
- `question_lock_contention_total`：题级锁竞争次数
- `stale_question_reject_total`：过期题目拒绝次数
- `answer_pipeline_fail_total`（带 reason 标签）：链路失败数

**方法**：复用 2.1 的脚本，加 `--shared-request-id`（模拟前端重试/网络重发导致的同一请求重复提交）：

```powershell
# 1) 换一道新题（换 questionNumber，避免被上一轮的结果缓存干扰），记录指标基线
curl http://localhost:8002/actuator/metrics/idempotency_replay_hit_total
curl http://localhost:8002/actuator/metrics/question_lock_contention_total

# 2) 单实例 20 并发、同一 requestId
python scripts\bench\answer_bench.py `
  --targets http://localhost:8002 `
  --token <token> --session-id <sessionId> --question-number <questionNumber> `
  --concurrency 20 --shared-request-id

# 3) 复读指标，计算增量
```

**判定标准**：

1. 有且仅有 1 个请求真正走完评分（响应正常返回评分结果）；
2. 其余请求：幂等回放同一结果（`idempotency_replay_hit_total` 增加），或"处理中"快速失败
   （业务错误码，非 500）；
3. 前端会话详情中该题只出现 **1 条**评分记录（无重复评分、无重复追问）；
4. `answer_flow_rollback_total`、`answer_pipeline_fail_total` 无异常增量。

**简历写法示例**：

> 20 并发重复提交压测下，同题仅产生 1 次评分与状态推进，其余请求经幂等回放/快速失败拦截，答题数据零重复、零丢失。

---

## 4. 数据记录模板

| 测试 | 参数 | 指标 | 第1轮 | 第2轮 | 第3轮 | 取值 |
|------|------|------|-------|-------|-------|------|
| RAG-ES | 20 queries, top5 | recall@3 | | | | |
| RAG-ES | | 服务端 P50/P95 (ms) | | | | |
| RAG-Milvus | 同上 | recall@3 / P95 | | | | |
| Single-flight | 2实例×10并发 | Δmiss（LLM 真实调用） | | | | |
| Single-flight | | Δhit（回放命中） | | | | |
| Single-flight | | 去重率 | | | | |
| 答题链路 | 1实例×20并发同 requestId | 评分次数（期望1） | | | | |
| 答题链路 | | idempotency_replay_hit 增量 | | | | |

## 5. 注意事项

- **成本**：测试 2/3 会真实调用 LLM。Single-flight 生效时每轮只扣 1 次调用的钱，但建议
  用便宜模型的 API Key 跑测试。
- **环境隔离**：用专门的测试账号 + 测试面试会话，不要混入正式数据。
- **指标清零**：actuator 计数器随实例重启清零；每轮测试前先记基线、测后算增量，不要读绝对值。
- **测试后还原**：移除 `docker-compose.bench.yml` 启动的 backend-2，去掉
  `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` 环境变量；若做过 Milvus 切换，把
  `RAG_VECTOR_STORE` 还原为 `elasticsearch`。
- **诚实原则**：简历里标注测试口径（如"双实例 10 并发压测"），面试被追问时直接讲本文档的
  证据链：指标增量 + Redis flight 键 + 双实例日志。

---

## 6. 附录：JMeter 持续负载测试（QPS / P95）

测试 1~3 产出的是**正确性/去重计数**，用零依赖脚本即可；本节是可选补充，用 JMeter 产出
**吞吐/延迟型**指标（"X QPS 持续负载下 P95 < Y ms"），给简历再加一类数字。

### 6.1 压测对象选择（关键：控制成本）

| 接口 | 路径 | 触发的外部调用 | 适合产出 |
|------|------|---------------|---------|
| RAG 检索调试 | `POST /api/xunzhi/v1/knowledge-bases/{kbId}/search-debug` | embedding + rerank API（单价低，但有限流） | RAG 检索链路持续负载 P95 |
| 会话恢复 | `GET /api/xunzhi/v1/interview/sessions/{sessionId}/restore` | 无 LLM，纯 Redis/Mongo（懒加载恢复链路） | 会话恢复 P95，佐证"会话状态治理"条目 |
| 答题提交 | `POST .../interview/answer-json` | LLM 评分 | **不要持续压**（烧钱），只用测试 2/3 的瞬时突发 |

> 压 restore 接口还能顺带观察 `flow_restore_source_total`（tag: flow_cache / turn_finished /
> turn_recovered / flow_reinit）各来源的分布，即热冷分层命中情况——面试讲"会话状态治理"时是现成证据。

### 6.2 测试计划结构（.jmx 配置要点）

GUI 里按如下结构搭好后保存为 `scripts/bench/load-test.jmx`：

```
Test Plan
├── User Defined Variables        BASE_URL=http://localhost:8002, TOKEN=<token>
├── HTTP Request Defaults          协议 http，从 ${BASE_URL} 取主机端口
├── HTTP Header Manager            Authorization: Bearer ${TOKEN}
│                                  Content-Type: application/json
├── Thread Group（阶梯：10 → 20 → 50 线程各跑一轮）
│   ├── Loop Count: Infinite + Duration: 120s（Scheduler 勾选）
│   ├── Ramp-up: 10s（避免瞬时全量涌入把冷启动算进分位数）
│   ├── HTTP Request: search-debug
│   │     POST /api/xunzhi/v1/knowledge-bases/${KB_ID}/search-debug
│   │     Body: {"query":"${__CSVRead(queries.csv,0)}","topK":5}
│   ├── CSV Data Set Config        queries.csv：复用测试 1 的 20 条查询，循环取用
│   ├── Constant Throughput Timer  可选，按目标 QPS 限速（如 600/分钟 = 10 QPS）
│   └── JSON Assertion             $.code 等于成功码（防止 4xx/5xx 也被计入延迟统计）
├── Aggregate Report               读 P90/P95/P99、Throughput
└── Summary Report
```

restore 接口同理：换成 GET，无 Body，`${SESSION_ID}` 用 0.5 节拿到的会话。

注意事项：

- **先预热**：正式统计前先跑 30s 丢弃（JVM JIT + 连接池 + ES 缓存预热），否则 P95 偏大；
- **同一 requestId 的坑不存在**：这两个接口是只读的，无幂等/single-flight 干扰；
- **限流红线**：search-debug 每请求调一次 embedding API（DashScope 有 QPS 配额），建议
  `Constant Throughput Timer` 限在 10 QPS 以内，观察无 429/限流报错再逐步上调。

### 6.3 命令行执行与报告

GUI 只用来调试计划，正式压测用无界面模式（官方推荐，GUI 本身影响测试结果）：

```powershell
# -n 无界面  -t 计划  -l 结果  -e -o 生成 HTML 报告（目录必须为空）
jmeter -n -t scripts\bench\load-test.jmx -l scripts\bench\result.jtl -e -o scripts\bench\report
```

打开 `scripts\bench\report\index.html`：

- **Statistics 表**：每接口的 P90/P95/P99、Throughput（即 QPS）、错误率；
- **Response Times Over Time 图**：确认延迟稳定无爬升（有爬升说明有泄漏或积压，数字不可用）。

每档并发（10/20/50 线程）各跑一轮，记录"吞吐量 + P95 + 错误率"，取**错误率为 0 的最高档**
作为简历数字。

### 6.4 简历写法示例

> RAG 检索链路在 __ QPS 持续负载下 P95 __ms、错误率 0%；会话恢复接口 P95 __ms（JMeter 阶梯压测验证）。

> 口径提醒：这是本机 Docker 单实例数据，简历不用写环境，但面试被问到要如实说明
> "本地压测环境，未做生产容量规划"，并能讲出 6.2 的计划结构即可。
