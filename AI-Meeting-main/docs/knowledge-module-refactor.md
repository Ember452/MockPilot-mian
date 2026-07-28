# 知识库（RAG）模块改造方案

> 版本：v1.3 ｜ 范围：`admin/src/main/java/com/hewei/hzyjy/xunzhi/knowledge` 及配套前端 ｜ 状态：待实施
>
> v1.2 修订：4.1 放弃 ES 原生 RRF retriever（Basic license 下 403，付费订阅功能），改为客户端 rank-based 标准 RRF；4.3 Rerank 明确接入阿里云百炼 DashScope；4.2 改写超时放宽；4.5 评分语义与降级路径闭环；补充存量 `embedding_model` 兼容与 `_source` 瘦身。
>
> v1.3 修订：新增 §6.1 全链路首 token 延迟预算；4.2 增加查询改写启发式跳过；4.3 cosine 降级补查向量明确 `mget` 批量；4.4 定义 references 与 webSearch 降级的关系；4.6 会话列表过滤兼容存量无 `chatMode` 字段；Phase 2 拆分为后端先行 / 前端独立交付两批。

---

## 1. 背景与现状

知识库模块已具备端到端 RAG 能力，当前链路：

```
文档上传 → DocumentEtlPipeline（多格式解析 → 智能分段 800/100 重叠 → Embedding 1536维）
        → ElasticsearchVectorStore（每库独立索引，Cosine dense vector + ik 分词）

对话请求 → KnowledgeChatController（SSE）
        → RagChatService → LiteFlow default_rag_chain
              THEN(ragRetrieval, webSearch, contextCompression)
        → buildAugmentedPrompt（硬编码模板）→ AiChatHandlerFactory 流式输出
```

核心文件：

| 层 | 文件 | 职责 |
|---|---|---|
| API | `knowledge/api/KnowledgeBaseController.java` | 知识库/文档 CRUD |
| API | `knowledge/api/KnowledgeChatController.java` | 知识库对话 SSE 端点 |
| Flow | `knowledge/flow/RagRetrievalNode.java` | LiteFlow 检索节点 |
| Flow | `knowledge/flow/WebSearchNode.java` | 联网搜索降级节点 |
| Flow | `knowledge/flow/ContextCompressionNode.java` | 上下文压缩节点 |
| Service | `knowledge/service/HybridSearchService.java` | 混合检索 + 精排 |
| Service | `knowledge/service/ElasticsearchVectorStore.java` | ES 索引/检索/删除 |
| Service | `knowledge/service/DocumentEtlPipeline.java` | 异步文档 ETL |
| Service | `knowledge/service/RagChatService.java` | RAG 对话编排 + Prompt 增强 |
| 配置 | `resources/liteflow/rag-chat-chain.xml` | RAG 规则链定义 |
| 配置 | `resources/interview-followup-rule.yaml` | top-k / rerank-top-n 等参数 |

## 2. 现存问题清单

| # | 问题 | 位置 | 严重度 |
|---|---|---|---|
| P1 | **伪 RRF**：`rrfScore = 1/(RRF_K + esScore + 1)` 用分数代入了本应使用排名（rank）的 RRF 公式，实际是对 ES 高分文档的轻微惩罚，非标准排名融合 | `HybridSearchService` L64 | 高 |
| P2 | 精排仅用 Bi-Encoder 余弦相似度（复用召回向量），无 Cross-Encoder 精排，长尾查询排序质量有限 | `HybridSearchService#rerankBySimilarity` | 高 |
| P3 | 多轮对话未做查询改写，指代性提问（"那它的缺点呢"）直接进检索，召回质量差 | `RagChatService` | 高 |
| P4 | 无引用溯源：仅在 Prompt 中要求模型"附来源编号"，SSE 无结构化 references 事件，前端无来源 UI | `RagChatService#buildAugmentedPrompt` | 中 |
| P5 | RAG 增强 Prompt 与系统提示词硬编码在 Java 中，不可配置 | `RagChatService` L77-L94 | 中 |
| P6 | `kbId` / 对话模式不随会话持久化，切会话或刷新后知识库绑定丢失 | 前端 + 会话元数据 | 中 |
| P7 | `ElasticsearchVectorStore` 为具体类直接注入，无向量存储抽象，无法切换/对比其他引擎 | `knowledge/service` | 中 |
| P8 | 知识库无 embedding 模型元数据记录，更换 embedding 模型后维度不一致会静默检索失败 | `knowledge_base` 表 | 中 |
| P9 | webSearch 降级无"检索质量评估"前置判断，触发条件粗糙（非完整 CRAG） | `rag-chat-chain.xml` | 低 |
| P10 | 单一 800 字分块，检索精度与上下文完整性互相牵制 | `DocumentEtlPipeline` | 低 |
| P11 | 知识库功能藏于普通对话页的模式切换器中，无独立板块入口；RAG 会话与普通会话在列表中混杂不可区分；文档管理被压缩在抽屉面板内，ETL 状态不可见 | 前端信息架构（`ChatModeSelector` / `KnowledgeBasePanel`） | 中 |

## 3. 改造目标

改造完成后 RAG 链形态：

```
查询改写(queryRewrite) → 混合召回(客户端标准 RRF) → Cross-Encoder 精排(rerank)
   → 检索质量评估(retrievalGrader，CRAG 分支：合格→直用 / 不合格→联网降级)
   → 上下文压缩(contextCompression) → Prompt 模板渲染 → 流式生成 + 结构化引用溯源
```

配套：知识库升级为独立一级板块（`/knowledge` 路由 + 库内专属会话）、向量存储抽象层（ES 默认 / Milvus 可选）、会话级知识库绑定持久化、父子分块。

## 4. 分期实施计划

### Phase 1（P0，约 3~4 天）：检索质量与正确性

#### 4.1 客户端标准 RRF 替换伪 RRF

> 不采用 ES 原生 `retriever.rrf`：该 DSL 为付费订阅功能（Platinum+），当前部署为官方镜像默认 Basic license，调用直接返回 403。客户端 rank-based 融合即标准 RRF，无 license 依赖。

- `ElasticsearchVectorStore#hybridSearch` 改为 `msearch` 并行双路召回：
  - BM25 路：bool match（content boost 0.3 / file_name boost 0.1），size = topK*2；
  - kNN 路：knn（k = topK*2，num_candidates = topK*4），size = topK*2；
  - 两路均 `_source.excludes: ["embedding"]`（精排改走外部 Rerank 后不再需要回传 1536 维向量，显著减小响应体）。
- Java 侧按 chunk_id 合并两路结果，以各自**排名**计算标准 RRF 融合分：`score = Σ 1/(60 + rank_i)`，按融合分降序取 topK*2 送精排。
- 删除 `HybridSearchService` 中手写的 `rrfScore` 计算（分数代入排名公式的伪 RRF）。

#### 4.2 查询改写节点（queryRewrite）

- 新增 `knowledge/flow/QueryRewriteNode.java`（LiteFlow 普通组件）：
  - 输入：`RagContext.query` + 最近 N 轮历史（`RagContext` 新增 `historyMessages` 字段，由 `RagChatService` 注入）；
  - 无历史或首轮 → 跳过（原 query 直通）；
  - **启发式跳过**：query 不含常见指代词（它/这/那/上面/刚才/前面 等固定词表）且长度 ≥ 10 字 → 判定为独立完整问题，跳过改写——多数自包含提问零成本直通，避免每轮都吃一次 LLM 调用的延迟与 token；
  - 有历史 → 调用轻量小模型（复用 `AiChatHandlerFactory`，走非流式，指定低延迟模型）把问题改写为独立完整问题，写入 `RagContext.rewrittenQuery`；
  - 超时（2500ms，非流式 LLM 调用普遍 1~3s，过短会导致 fail-open 常态化、改写形同虚设）或失败 → fail-open，回退原 query。
- `RagRetrievalNode` 检索时优先取 `rewrittenQuery`。
- `rag-chat-chain.xml` 更新为 `THEN(queryRewrite, ragRetrieval, webSearch, contextCompression)`。
- `interview-followup-rule.yaml` 新增 `xunzhi-agent.rag.rule-engine.enable-query-rewrite: true` 开关。

#### 4.3 Cross-Encoder Rerank 节点

- 新增 `knowledge/service/RerankService.java`：
  - 接口化：`List<Chunk> rerank(String query, List<Chunk> candidates, int topN)`；
  - 默认实现 `DashScopeRerankService`：接入**阿里云百炼 DashScope Rerank API**（`POST https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank`，模型 `gte-rerank-v2`，请求体 `{model, input:{query, documents}, parameters:{top_n, return_documents:false}}`，Header `Authorization: Bearer ${DASHSCOPE_API_KEY}`），按返回的 `relevance_score` 排序取 topN；项目 embedding 已走百炼，可复用同一 API Key；
  - 保留 `CosineRerankFallback` 实现（现有 cosine 逻辑迁移；因 4.1 已从 `_source` 排除 embedding，降级时按 chunk_id 用 **`mget` 一次批量补查**向量，禁止逐条查询——降级路径只允许多一次 ES 往返），API 失败或未配置时降级使用（fail-open），降级时在 `RagContext` 标记 `rerankDegraded = true`（供 4.5 grader 判断）。
- `HybridSearchService#search`：召回 topK*2 → `RerankService.rerank(...)` → 取 rerankTopN。
- `rerankBySimilarity` 中的加权融合逻辑删除（其职责由客户端标准 RRF + DashScope Rerank 接管）。

### Phase 2（P1，约 8~10 天）：产品完成度与板块化

> 内部分两批交付：**2a 后端先行**（4.4 后端 + 4.5 + 4.6 + 4.8，约 4 天，接口就绪即可联调）→ **2b 前端独立交付**（4.4 前端 + 4.7，约 4~6 天）。前端信息架构重构体量最大、最易超期，独立成批避免拖累后端上线。

#### 4.4 结构化引用溯源

- 后端：
  - `RagContext` 新增 `List<RetrievedReference>`（fileName、docId、chunkIndex、score、snippet 前 200 字）；
  - `KnowledgeChatController` 在流式输出开始前，先推送一条 `{"type":"references","data":[...]}` SSE 事件；
  - references **仅包含知识库检索命中**；若本轮触发 webSearch 降级，references 事件 `data` 为空数组，前端不渲染来源卡片（网页来源溯源不在本次范围）；
  - `buildAugmentedPrompt` 中给每条参考资料标注 `[1]`、`[2]` 编号，要求模型行内引用。
- 前端：
  - `knowledgeService.streamChat` 的 `onmessage` 增加 `references` 分支，回调 `onReferences`；
  - 新增 `ReferenceCard` 组件：回答下方渲染可展开的来源列表（文件名 + 片段 + 相关度）；
  - `ChatMessage` 类型扩展 `references?: RetrievedReference[]`，随消息持久化到 MongoDB 消息体（后端 `saveAiAssistantMessage` 扩展可选字段）。

#### 4.5 CRAG 检索质量评估节点

- 新增 `knowledge/flow/RetrievalGraderNode.java`：
  - **评分语义绑定分数来源**：阈值仅对 DashScope `relevance_score`（0~1）有效；若 `rerankDegraded == true`（cosine 降级，分布普遍 0.7+）或仅有 RRF 融合分（量级 ~0.02），跳过评估直接放行（fail-open），避免阈值语义错乱；
  - 规则先行：Rerank 后 top1 `relevance_score` ≥ `grade-pass-threshold`（默认 0.5，配置化）→ 判定合格，跳过 webSearch；
  - 分数不足且开启 LLM 评估（`enable-llm-grader`，默认关）→ 轻量 LLM 判断分块与问题相关性；
  - 判定不合格 → 置 `RagContext.needWebSearch = true`。
- `WebSearchNode` 改为仅在 `needWebSearch == true` 时执行。
- 链条更新：`THEN(queryRewrite, ragRetrieval, retrievalGrader, webSearch, contextCompression)`。

#### 4.6 会话级知识库绑定持久化

- 会话元数据（MongoDB 会话文档或 `AiConversationService` 对应实体）新增 `chatMode`（normal/rag）、`kbId` 字段；
- 知识库会话统一在 `/knowledge/:kbId` 工作区内创建（见 §4.7），创建会话请求 DTO 增加可选 `kbId`，创建时即写入绑定——配合独立板块后，不再存在"通用页切模式后绑定丢失"的场景；
- 会话列表接口新增 `chatMode` / `kbId` 过滤参数：普通对话页只列 normal 会话，知识库工作区按 kbId 过滤各库专属会话；
- **存量兼容**：过滤 normal 时 Mongo 查询条件必须是 `chatMode == 'normal' OR chatMode 字段不存在`——存量会话文档没有该字段，否则上线当天历史会话在普通对话页全部消失；
- 会话详情响应返回 `chatMode`、`kbId`，前端从历史列表进入会话时据此自动路由到对应知识库工作区。

#### 4.7 知识库独立板块（前端信息架构重构）

- 新增一级路由与侧边栏导航入口，"知识库"与"AI 对话""模拟面试"并列为三大板块：
  - `/knowledge`：知识库列表页（卡片展示名称/描述/文档数/分块数/更新时间，支持新建/删除）；
  - `/knowledge/:kbId`：知识库工作区，左右布局——左栏文档管理（文档列表 + 上传 + ETL 状态 + 删除，复用既有 `listDocuments` / `uploadDocument` / `deleteDocument` API），主区该库专属会话列表 + 对话窗口（内置 §4.4 引用溯源卡片）；
- 文档列表展示异步 ETL 处理状态（解析中/完成/失败），文档元数据已有 `status` 字段，前端轮询或手动刷新；
- 普通对话页下线 `ChatModeSelector` 的 rag 模式与 `KnowledgeBasePanel` 抽屉，替换为跳转知识库板块的轻量入口，普通对话回归纯粹；
- 后端边界不动：`knowledge` 包继续复用 `conversation` 会话体系，不做服务/模块拆分。

#### 4.8 RAG Prompt 模板配置化

- `buildAugmentedPrompt` 与系统提示词模板下沉为配置：
  - 优先级：`knowledge_base.prompt_template`（新增列，可空）→ `application.yaml` 默认模板；
  - 模板占位符：`{context}`、`{question}`；
- `knowledge_base` 表 DDL 变更见 §5。

### Phase 3（P2，约 4~5 天，可选）：架构与扩展

#### 4.9 向量存储抽象层 + Milvus 双引擎

- 抽取接口 `knowledge/service/VectorStore.java`：
  `ensureCollection(kbId)` / `indexChunks(kbId, chunks)` / `hybridSearch(kbId, query, embedding, topK)` / `deleteByDocId(kbId, docId)` / `dropCollection(kbId)`；
- `ElasticsearchVectorStore` 实现该接口，注入点全部改为接口类型；
- 新增 `MilvusVectorStore`（Milvus 2.5 standalone：dense + BM25 稀疏向量 + RRFRanker），`@ConditionalOnProperty(xunzhi-agent.rag.vector-store=milvus)`，默认 `elasticsearch`；
- docker-compose 为 Milvus 增加独立 profile（默认不启动）；
- 附带评测脚本：同一文档集 + 20 条标注查询，输出两引擎 recall@3 与 P95 延迟对比表。

#### 4.10 父子分块（Small-to-Big）

- `DocumentEtlPipeline`：先切父块（约 1600 字），父块内切子块（约 400 字，重叠 50）；
- 索引 mapping 新增 `parent_id`、`parent_content` 不参与检索仅存储（或父块单独存 MongoDB 按 id 取）；
- 检索命中子块 → 上下文注入取父块内容并按 `parent_id` 去重。

#### 4.11 Embedding 元数据防护

- `knowledge_base` 表新增 `embedding_model`、`embedding_dim` 列，建库时落库；
- ETL 与检索前校验当前配置模型与库记录一致，不一致抛出明确错误（禁止静默混用维度）；
- **存量兼容**：`embedding_model = ''`（legacy 存量库）跳过校验直接放行，首次新增文档时回填当前模型标识；否则改造上线当天存量库全部报错。

## 5. 数据结构变更

```sql
-- knowledge_base 表新增列（Phase 2 / 3）
ALTER TABLE knowledge_base
    ADD COLUMN prompt_template TEXT NULL COMMENT 'RAG增强Prompt模板，空则用全局默认',
    ADD COLUMN embedding_model VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'embedding模型标识，空串表示改造前存量库(legacy)',
    ADD COLUMN embedding_dim   INT NOT NULL DEFAULT 1536 COMMENT '向量维度';
```

- MongoDB 会话文档：新增 `chatMode`（normal/rag）、`kbId`（可空）；助手消息新增可选 `references` 数组。
- ES mapping（父子分块，新库生效）：新增 `parent_id`(keyword)。存量索引不迁移，旧库继续按旧 mapping 检索。

## 6. 配置项汇总（application.yaml / interview-followup-rule.yaml）

```yaml
xunzhi-agent:
  rag:
    rule-engine:
      enable-query-rewrite: true
      query-rewrite-timeout-ms: 2500
      grade-pass-threshold: 0.5
      enable-llm-grader: false
    rerank:
      provider: dashscope           # dashscope（阿里云百炼） | cosine（降级实现）
      base-url: https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank
      api-key: ${DASHSCOPE_API_KEY:}   # 与 embedding 复用同一百炼 Key
      model: gte-rerank-v2
      timeout-ms: 1500
    vector-store: elasticsearch    # elasticsearch | milvus
```

所有新增能力均遵循现有 **fail-open** 原则：任一增强节点失败，链路退化为改造前行为，不阻断对话。

### 6.1 全链路首 token 延迟预算（多轮 RAG）

改造后链路为串行，新增节点的耗时会直接叠加到首 token 延迟上，各环节预算如下：

| 环节 | 预算上限 | 控制手段 |
|---|---|---|
| queryRewrite | 2500ms | 启发式跳过 + 超时 fail-open |
| 双路召回（msearch） | ~300ms | 并行双路，`_source` 已剔除 embedding |
| DashScope Rerank | 1500ms | 超时降级 cosine |
| retrievalGrader | ~50ms（规则） | `enable-llm-grader` 默认关 |
| webSearch（仅不合格时） | 现状不变 | CRAG 判定合格即跳过 |
| contextCompression | 现状不变 | — |

- 常规路径（启发式跳过改写 + 不触发联网）目标首 token 增量 ≤ 2s；
- 最坏路径（改写超时 + rerank 超时 + 联网降级）约 7s，为各超时上限之和而非常态，且每一段均 fail-open 不阻断。

## 7. 验收标准

| 项 | 验收方式 |
|---|---|
| 标准 RRF | ES 请求为 msearch 双路召回（`_source` 不含 embedding）；融合分基于**排名**而非分数；`HybridSearchService` 无手写 rrfScore；既有检索单测通过 |
| 查询改写 | 多轮会话中发送指代性提问，日志可见改写后 query，召回命中目标文档 |
| Rerank | 配置百炼 Key 后日志走 dashscope provider，命中 `gte-rerank-v2`；关闭/失败时自动降级 cosine 且对话不报错 |
| 引用溯源 | SSE 首帧为 references 事件；前端回答下方展示来源卡片；刷新后历史消息仍带来源 |
| CRAG | top1 `relevance_score` 达标时 webSearch 不执行（日志验证）；不达标触发联网降级；cosine 降级态下 grader 跳过评估直接放行 |
| 会话绑定 | 知识库会话在工作区内创建即绑定 kbId；从历史列表进入自动路由到对应工作区；刷新不丢绑定 |
| 独立板块 | `/knowledge` 列表页与工作区可用；普通对话页无 rag 模式残留；普通/知识库会话列表互不混杂；文档 ETL 状态可见 |
| 双引擎 | `vector-store=milvus` 时全链路可用；评测脚本输出对比表 |
| 延迟 | 多轮 RAG 对话首 token P95 ≤ 4s（本地环境实测 20 轮）；自包含提问日志中无改写 LLM 调用（启发式跳过生效） |
| 回归 | 普通对话（非 RAG）链路零改动零回归；面试主链路不受影响 |

## 8. 风险与注意事项

1. **ES RRF license 限制**：原生 `retriever.rrf` 为 Platinum+ 订阅功能，Basic license 下 403，故采用客户端 rank-based RRF（见 4.1）；当前 `co.elastic.clients:elasticsearch-java` 已为 9.2.1，msearch/knn 均原生支持，无需升级依赖。
2. **查询改写引入额外 LLM 调用**：每轮多一次非流式调用（1~3s + token 成本），必须保留开关、启发式跳过与超时 fail-open（延迟预算见 §6.1）；建议接入现有 SingleFlight/线程池防护体系。
3. **DashScope Rerank 配额与计费**：gte-rerank-v2 按调用量计费且有 QPS 限制，需确认百炼控制台已开通该模型；单次请求 documents 数量有上限（当前 topK*2 远未触及），超时/限流均走 cosine 降级不阻断对话。
4. **SSE 协议变更**：新增 `references` 事件类型，前端旧版本对未知 type 需静默忽略（现有 `try/catch` 已兜底，需回归验证）。
5. **存量索引兼容**：父子分块只对新建知识库生效，旧库不做在线迁移；如需迁移提供"重建索引"管理接口（后续单独排期）。
6. **Milvus 部署内存**：standalone 约需 2G+，与 ES 同机需评估总内存；profile 默认关闭，避免影响一键启动体验。
7. **改造顺序不可颠倒**：4.1（标准 RRF）必须先于 4.3（Rerank）落地，否则精排输入的融合分语义混乱；4.6（会话绑定）应与 4.7（独立板块）同批实施，避免出现“有板块但历史会话无法归位”的中间态。
8. **存量 RAG 会话归位**：改造前产生的知识库会话无 `kbId` 元数据，历史列表中按 normal 会话展示（可接受）；不做存量数据回填。
9. **存量库 embedding 元数据兼容**：4.11 校验必须把 `embedding_model = ''` 视为 legacy 跳过，否则上线当天存量知识库检索全部报错（见 §4.11）。

## 9. 明确不做（本次范围外）

- 后端服务/模块拆分（`knowledge` 包边界已清晰，继续复用 `conversation` 会话体系，拆分无收益）；
- GraphRAG / 知识图谱构建（链路重、token 成本高，收益与当前规模不匹配）；
- Self-RAG（依赖模型特调）；
- 面试出题链路与本地知识库打通（跨 interview 域，单独立项）；
- 存量索引在线迁移工具。
