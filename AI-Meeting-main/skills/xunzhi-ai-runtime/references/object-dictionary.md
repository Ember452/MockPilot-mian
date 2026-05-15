# 运行时对象词典

## 1. 业务场景对象

| 对象 | 含义 | 真相源 | 说明 |
| --- | --- | --- | --- |
| `InterviewAiGuardStage` | AI 保护阶段名 | Java 常量 | 代码、配置、监控必须同名 |
| `BusinessAgentScene` | 场景目录 | Java 枚举 | 决定场景命中哪个 Agent |
| `xunzhi-agent.agent-binding` | 场景到 Agent 的映射 | `application.yaml` | 运行时绑定真相源 |
| `xunzhi-agent.flow-limit` | 流量限制规则 | `application.yaml` | 限流边界真相源 |
| `xunzhi-agent.ai-guard` | 守护策略 | `application.yaml` | 超时、重试、并发保护真相源 |
| `xunzhi-agent.ai-singleflight` | 去重与接管策略 | `application.yaml` | 单次调用复用和等待策略真相源 |
| `xunzhi-agent.thread-pool` | 线程池配置 | `application.yaml` | 并发执行资源边界 |

## 2. 运行时语义对象

| 对象 | 含义 | 关键点 | 不能替代什么 |
| --- | --- | --- | --- |
| `InterviewRuntimeLoadMode` | 恢复读写模式 | `READ_ONLY` / `READ_WRITE_REQUIRED` | 普通展示字段 |
| `InterviewRuntimeRehydrateScope` | 恢复范围 | `FLOW_ONLY` / `SCORE_ONLY` / `PLAYBACK_ONLY` / `MATERIAL_ONLY` / `HOT_RUNTIME` / `FULL_RUNTIME` | 任意硬编码的“全量恢复” |
| `InterviewRuntimeConfidence` | 恢复置信度 | `EXACT` / `DERIVED` / `READ_ONLY` / `TERMINAL` | 真实数据本身 |
| `InterviewSessionRuntimeView` | 统一恢复视图 | `canWrite()`、`isTerminal()` | 主记录本身 |
| `InterviewRuntimeScoreAggregate` | 运行态分数聚合 | `scoreSum`、`scoreCount`、`totalScore` | 最终归档记录 |

## 3. 幂等与去重对象

| 对象 | 作用 | 真相源 | 说明 |
| --- | --- | --- | --- |
| `InterviewAnswerIdempotencyService` | 答题幂等门禁 | 运行时服务 | `processing` / `replay` 双通道 |
| `SingleFlight key` | 去重键 | 代码拼接结果 | 决定什么算同一次业务调用 |
| `processing` 标记 | 处理中门禁 | Redis / 运行时缓存 | 防止重复并发写 |
| `replay` 响应 | 成功回放 | 缓存结果 | 防止重复出分 |

## 4. 读写矩阵

| 对象 | 谁写 | 谁读 | 不能替代什么 |
| --- | --- | --- | --- |
| `application.yaml` 中的运行时配置 | 运维、发布、代码生成 | 启动流程、运行时决策 | 业务记录 |
| `stage` 常量 | 代码 | guard、singleflight、监控 | 随意拼接字符串 |
| `singleflight key` | 业务代码 | singleflight 保护层 | 用户身份本身 |
| `loadMode` / `confidence` | 恢复服务 | 答题链路、恢复页 | 主记录 |

## 5. 关键不变量

- stage 名必须在代码、配置、监控里一致。
- key 过粗会误复用，key 过细会失去去重价值。
- Guard、SingleFlight、限流、线程池是一个整体，不能只改一层。
- 运行时配置不是“性能参数”，而是业务稳定性的正式契约。
- `loadMode`、`confidence` 不是展示字段，而是恢复是否可写的正式业务语义。

## 6. 常见误判

- 新增 stage 只改代码不改配置，会直接丢失保护。
- 把“超时”当成单纯网络慢，忽略了恢复、下载、重试和排队。
- `singleflight key` 改动后只看功能正确，不看复用边界。
- `loadMode = READ_ONLY` 时，不要误判成恢复失败，它可能只是只读恢复。
