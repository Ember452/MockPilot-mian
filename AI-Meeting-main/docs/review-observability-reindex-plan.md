# 复习闭环 / RAG 可观测性 / 索引重建 改造方案

> 版本：v1.1（复核修订：A.2 同步重载、A.3 重建期写保护、C.1 建表落地、配置默认值统一）｜ 范围：`interview` + `knowledge` 包及配套前端 ｜ 状态：已实施
>
> 前置：knowledge-module-refactor.md（v1.3）已全部落地。本方案为其后续三项扩展，
> 按依赖关系排期为 Phase A（索引重建，含文件留存前置）→ Phase B（可观测性）→ Phase C（复习闭环）。
> Phase A 必须最先实施：原始文件当前不留存，每晚一天上线就多一天的存量文档永久无法重建。

---

## 1. 现状关键事实（调研结论）

| # | 事实 | 位置 | 影响 |
|---|---|---|---|
| F1 | 文档原始字节在 ETL 后即丢弃，`KnowledgeDocument`（Mongo）仅存元数据，无文件路径字段 | `DocumentEtlPipeline#process` / `KnowledgeDocument` | 重建索引无原文可用，必须先补文件留存 |
| F2 | 文档 status 仅 1=处理中 / 2=完成；解析失败直接 return，不写失败态 | `DocumentEtlPipeline#process` L55 | 重建需补 3=失败 语义 |
| F3 | `interview_record`（MySQL）无结构化弱项字段，仅总分 + 建议串（分号分隔）+ `sessionSnapshotJson`；逐题回顾在 RespDTO 组装层（playbackItems） | `InterviewRecordDO` / `InterviewRecordRespDTO` | 弱项知识点需 LLM 抽取，不能直接读库 |
| F4 | 题库（questionbank）后端为零，前端纯静态页 | `pages/questionbank` | 复习闭环不依赖题库，独立建模 |
| F5 | RAG 各 LiteFlow 节点只有 slf4j 日志，无耗时/指标；`RagContext` 有 rerankDegraded/needWebSearch 标志但无 timing 字段 | `knowledge/flow/*` | 埋点从零加，但标志位可直接复用 |
| F6 | actuator + micrometer 已在 pom，`ThreadPoolConfig` 已注入 MeterRegistry（指标 xunzhi_thread_pool），有现成模式 | `admin/pom.xml` / `ThreadPoolConfig` | 指标基础设施零成本复用 |
| F7 | `AIContentAccumulator` 不解析 usage；`AiChatStreamRespDTO` 无 usage 字段；agent 模块已有 totalTokens 先例 | `toolkit/xunfei` / `agent` | token 统计需扩展累积器，OpenAI 兼容流式需 usage 帧兜底估算 |
| F8 | 已有 search-debug 接口 + eval.py（recall@3 / P50/P95），`@EnableScheduling` 已开，Redis(StringRedisTemplate/Redisson) 使用广泛 | `KnowledgeBaseController#searchDebug` / `scripts/rag-eval` | 持续评测与计数只需增量 |

## 2. Phase A（P0，约 2~3 天）：文件留存 + 索引重建管理接口

### A.1 原始文件留存（前置，最优先合入）

- `ApplicationStorageProperties` 新增 `knowledgeDocDir`（配置 `xunzhi-agent.storage.knowledge-doc-dir`，默认 `${baseDir}/knowledge-docs`），`ApplicationStorageInitializer` 建目录；
- `KnowledgeDocument` 新增 `file_path` 字段（相对路径 `{kbId}/{docId}.{ext}`）；
- `DocumentEtlPipeline#process` 在解析前先把 fileBytes 落盘（落盘失败仅 log.warn，不阻断 ETL——留存是增强，fail-open）；
- `deleteDocument` 同步删除留存文件；`deleteKnowledgeBase` 级联删除 `{kbId}/` 目录；
- docker-compose 无需改动（已确认：`XUNZHI_STORAGE_BASE_DIR=/app/data` 挂 `backend-data` 卷，knowledge-docs 跟随 baseDir 落在卷内）；
- **存量兼容**：旧文档 `file_path` 为空，重建时跳过并标记（见 A.3）。

### A.2 ETL 支持复用 docId + 失败态

- `process` 抽出重载 `process(kbId, username, fileBytes, fileName, docId)`：docId 非空时不新建 Mongo 文档，复用既有记录（status 置 1，重灌 chunk 后置 2）；原公开签名不变，内部委托；
- **重载必须是同步方法（不加 `@Async`）**，原 `@Async process` 委托它。原因：A.3 重建循环需串行逐个处理文档——若沿用 @Async 会 fire-and-forget，进度计数失真、并发 embed 触发限流、`updateKnowledgeBaseCounts` 并发写竞态；
- 解析失败/文本为空/索引写入异常时，将文档 status 置 3（失败）并写回——同时修复现状"失败无痕"问题（属本需求必需，非顺手重构）。

### A.3 重建接口

- `POST /api/xunzhi/v1/knowledge-bases/{kbId}/rebuild`（`KnowledgeBaseController`）：
  1. 权限校验（复用 `getKnowledgeBase(kbId, username)`）；
  2. Redisson 锁 `kb:rebuild:{kbId}`（tryLock，已在重建中返回 ClientException）；
  3. 请求参数 `force`（默认 false）：false 时走 `ensureEmbeddingCompatible` 校验；true 时允许换 embedding 模型——重建前把 `knowledge_base.embedding_model/embedding_dim` 更新为当前配置（这正是 §4.11 预留的换模型通道）；
  4. 异步执行（`threadPoolTaskExecutor`，整个重建任务一个异步任务）：`vectorStore.deleteIndex(kbId)` → `createIndexIfNotExists(kbId)` → **串行**遍历该库文档（调 A.2 同步重载）：有 `file_path` 且文件存在 → 读字节按原 docId 重跑 ETL；无留存文件 → status 置 3 并计入 skipped；
  5. 进度写 Redis：`kb:rebuild:progress:{kbId}` = `{total, done, skipped}`，TTL 1h；
- `GET /{kbId}/rebuild-status`：返回 rebuilding（锁是否持有）+ 进度 JSON；
- **重建期写保护**：`uploadDocument` / `deleteDocument` 入口检查 `kb:rebuild:{kbId}` 锁被持有则拒绝（ClientException "重建中，请稍后"）——否则并发写索引会与 deleteIndex 竞争丢 chunk；
- 重建期间检索（已核实两引擎行为）：deleteIndex 后立即 createIndexIfNotExists，无索引窗口仅毫秒级；重灌过程中索引存在只是内容渐增，检索不报错仅命中变少。极端时序下 ES 侧索引缺失为 msearch item-failure 返回空列表（无感走 CRAG/webSearch 兜底）；Milvus 侧 collection 缺失会抛异常，走既有"检索出错→通用对话"降级提示，均不崩溃。

### A.4 前端

- `KnowledgeDocPanel` 增加"重建索引"按钮（二次确认，提示"重建期间该库检索降级"）+ 轮询 rebuild-status 显示进度；文档列表 status 增加"失败"展示（现有 `DOC_STATUS` 常量扩展 3）。

## 3. Phase B（P1，约 3~4 天）：RAG 可观测性与成本看板

### B.1 链路耗时埋点

- `RagContext` 新增 `Map<String, Long> stageTimings`（LinkedHashMap）；
- 五个节点（queryRewrite/ragRetrieval/retrievalGrader/webSearch/contextCompression）process 内 try/finally 记录耗时写入 stageTimings——不改节点业务逻辑，仅包一层计时；
- Micrometer 指标（复用注入 MeterRegistry 的模式，前缀 `xunzhi_rag`）：
  - Timer `xunzhi_rag_stage_seconds{stage}`：各阶段耗时分布；
  - Counter `xunzhi_rag_rewrite_total{result=applied|skipped|failed}`；
  - Counter `xunzhi_rag_rerank_total{provider=dashscope|cosine}`（取 `_rerank_provider`）；
  - Counter `xunzhi_rag_grader_total{decision=pass|web_search}`；
- 埋点集中在一个 `RagMetricsRecorder` 组件（knowledge/config 或 service），节点只调它，失败静默（观测不得影响主链路）。

### B.2 token 用量统计

- `AIContentAccumulator` 增加 usage 解析：SSE 末帧含 `usage.total_tokens` 则取真值；无 usage 帧时按累积字符估算（CJK 1字≈1 token、ASCII 4字符≈1 token），并标记 `estimated=true`；
- `RagChatService#executeRagChat` 结束时从 accumulator 取 token 数写入本轮 trace（B.3）；普通对话链路不动（Phase 1 零影响原则同样适用：本期只覆盖 RAG 链路的 token 成本）。

### B.3 rag_trace 明细留档

- 新 Mongo 集合 `rag_trace`，每轮 RAG 对话结束异步写一条：sessionId、kbId、username、stageTimings、rewriteApplied、rerankProvider、graderDecision、webSearchTriggered、referenceCount、tokenUsage(total/estimated)、createTime；
- TTL 索引 30 天自动过期；写入失败仅 log.warn。

### B.4 看板接口与前端

- `GET /api/xunzhi/v1/rag/metrics/summary?days=7`：Mongo aggregation 输出——调用量、各阶段平均/P95 耗时、改写触发率、rerank 降级率、联网降级率、token 总量（含估算占比）；
- `GET /api/xunzhi/v1/rag/metrics/traces?kbId=&page=`：明细分页（排查用）；
- 前端知识库列表页（/knowledge）新增"看板"入口：汇总卡片（调用量/降级率/token）+ 阶段耗时条形图 + 近 N 天趋势（复用项目现有图表方案，与报告雷达图同库）。

### B.5 持续评测增强

- `eval.py` 新增 `--json-out result.json`：输出结构化结果（label、recall、P50/P95、时间戳），追加写入历史文件；
- 新增 `scripts/rag-eval/compare.py`：读多份 result.json 输出对比表（回答"RRF 换标准版后效果如何"即靠两份留档对比）；
- 不引入 CI 定时评测（无常驻测试环境，收益不成立）。

## 4. Phase C（P1，约 4~5 天）：面试报告 → 弱项复习闭环

### C.1 数据模型

```sql
CREATE TABLE review_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL COMMENT '来源面试会话',
    knowledge_point VARCHAR(128) NOT NULL COMMENT '弱项知识点',
    severity TINYINT NOT NULL DEFAULT 2 COMMENT '1轻微 2一般 3严重',
    suggestion VARCHAR(1024) COMMENT '复习建议',
    kb_refs_json TEXT COMMENT '知识库关联片段引用（可空）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待复习 1已掌握',
    create_time DATETIME, update_time DATETIME, del_flag TINYINT DEFAULT 0,
    KEY idx_user_point (user_id, knowledge_point),
    UNIQUE KEY uk_session_point (session_id, knowledge_point)
);
```

**建表落地（三件套，缺一存量部署即 500）**：
1. 新增 `admin/src/main/resources/sql/review_item.sql`；
2. 根目录 `docker-compose.yml` mysql 服务追加挂载 `07-review_item.sql`（仅首次初始化生效）;
3. `interview` 包内新增 `InterviewSchemaMigrationRunner`（模式同 `KnowledgeSchemaMigrationRunner`）：启动时 `CREATE TABLE IF NOT EXISTS review_item ...`，失败仅告警不阻断——覆盖数据卷非空、initdb 不再执行的存量库。

### C.2 复习清单生成（手动触发，幂等）

- `POST /api/xunzhi/v1/interview/review/generate/{sessionId}`：
  1. 校验报告归属且 status 为 FINISHED/EVALUATED；同 sessionId 已生成则直接返回既有清单（不重复烧 token）；
  2. LLM 抽取（复用 `AiChatHandlerFactory` 非流式，同 QueryRewriteService 模式）：输入 = interviewSuggestions + playbackItems 逐题问答与评价，输出 JSON 数组 `[{knowledgePoint, severity, suggestion}]`（限 3~8 条）；解析失败或超时（5s）返回明确错误，不写脏数据；
  3. 知识库联动（可选入参 kbId）：对每个 knowledgePoint 调 `HybridSearchService.search(kbId, point, 3, 2)`，命中片段（fileName/docId/snippet）写入 kb_refs_json；未传 kbId 或未命中则留空——复习项照常生成（fail-open）；
- `GET /interview/review/items?status=&page=`：清单分页；`PATCH /interview/review/items/{id}/status`：勾选已掌握。

### C.3 成长曲线

- `GET /interview/review/growth`：按用户聚合——
  - 历史 `interview_record`（FINISHED/EVALUATED）按时间序输出 interviewScore/resumeScore 折线数据；
  - `review_item` 按 knowledge_point 聚合出现次数与掌握率（重复出现 = 顽固弱项，排序置顶）；
- 纯查询聚合，无新增写路径。

### C.4 前端

- 新增路由 `/review`（侧边栏"复习中心"，与知识库入口同级）：
  - 弱项清单：按严重度分组、勾选已掌握、展开知识库参考片段（复用 ReferenceCard 样式）；
  - 成长曲线：分数折线 + 顽固弱项标签云/排行；
- 面试报告详情页底部加"生成复习清单"按钮 → 生成后跳转 /review（已生成则直接跳转）。

### C.5 边界（明确不做）

- 不自动为每场面试生成清单（token 成本与用户意愿不匹配，手动触发）；
- 不做题库联动与刷题推荐（题库后端为零，单独立项）；
- 不做基于遗忘曲线的复习排期提醒（无推送渠道）。

## 5. 配置项汇总

```yaml
xunzhi-agent:
  storage:
    knowledge-doc-dir: ${KNOWLEDGE_DOC_DIR:}   # 空则代码内取 ${baseDir}/knowledge-docs，docker 下自动落 backend-data 卷
  rag:
    metrics:
      trace-enabled: true          # rag_trace 明细留档开关
      trace-ttl-days: 30
  review:
    extract-timeout-ms: 5000       # 弱项抽取 LLM 超时
    max-items-per-session: 8
```

## 6. 验收标准

| 项 | 验收方式 |
|---|---|
| 文件留存 | 上传文档后磁盘存在 `{kbId}/{docId}.{ext}`；删除文档/知识库时文件同步清理 |
| 索引重建 | rebuild 后 chunk 数与重建前一致且检索可命中；`force=true` 换 embedding 模型后 kb 元数据更新且检索正常；无留存文件的存量文档标记失败并计入 skipped；重建中重复调用被锁拒绝；重建中上传/删除文档被拒绝 |
| 重建容错 | 重建过程中发起 RAG 对话不报错（走 webSearch/通用对话降级） |
| 耗时埋点 | actuator `/actuator/metrics/xunzhi_rag_stage_seconds` 可查各阶段分布；单测验证 stageTimings 五阶段齐全 |
| token 统计 | 有 usage 帧时取真值；无 usage 帧时 estimated=true 且估算值非零 |
| 看板 | summary 接口返回率值与 rag_trace 明细一致；前端看板可见调用量/降级率/token |
| 持续评测 | eval.py --json-out 产出留档；compare.py 输出两轮对比表 |
| 复习清单 | 同 sessionId 重复生成幂等；LLM 输出解析失败时无脏数据入库；传 kbId 时复习项带知识库引用，不传时正常生成 |
| 成长曲线 | 多场面试后折线数据按时间正序；重复弱项计数正确 |
| 回归 | 普通对话、面试主链路、既有 RAG 链路行为零变化；knowledge 包既有 33 个单测全绿 |

## 7. 风险与注意事项

1. **文件留存磁盘增长**：文档留存无上限，需在文档上传处沿用现有大小限制；Docker 部署已确认 storage baseDir（/app/data）在 backend-data 数据卷内，容器重建不丢文件。
2. **重建原子性**：deleteIndex 后进程崩溃会留下空索引——文档 status 仍为 2 但检索无结果；rebuild-status 的 Redis 进度可判断中断，重新触发 rebuild 即可自愈，不做事务补偿。
3. **LLM 抽取质量**：弱项抽取依赖报告文本质量，建议串为空的旧报告可能抽不出条目——返回空清单并前端提示，不视为错误。
4. **token 估算误差**：无 usage 帧时估算值仅用于成本量级参考，看板需展示"估算占比"避免误读。
5. **观测写路径开销**：rag_trace 异步写 + 失败静默；Micrometer 指标为内存计数，无外部依赖，不引入 Prometheus/Grafana（本地项目 actuator 端点足够，简历叙事上也说得通）。
6. **跨域调用边界**：Phase C 的 interview → knowledge 调用仅走 `HybridSearchService` 公开接口，不触碰 knowledge 内部实现，保持包边界清晰。

## 8. 实施顺序与依赖

```
Phase A（2~3天）：A.1 文件留存（最先合入，独立可上线）→ A.2 ETL 改造 → A.3 重建接口 → A.4 前端
Phase B（3~4天）：B.1 埋点 → B.2 token → B.3 trace → B.4 看板 → B.5 评测增强（B 内部无严格顺序，B.4 依赖 B.1~B.3）
Phase C（4~5天）：C.1 建表 → C.2 生成接口 → C.3 成长曲线 → C.4 前端
```

三个 Phase 相互独立可并行，但 A.1 必须最早合入主干（文件留存的时间敏感性）。
