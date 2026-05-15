# AI 运行时易错点

## 最常见的五类误判

- 新增 stage 只改代码不改配置，会直接丢失 Guard 和 SingleFlight 保护。
- 把“超时”当成单纯网络慢，忽略了重复请求、恢复重建、文件下载这些前置耗时。
- 改 `singleflight key` 时只验证功能正确，没有验证复用边界是否被放大或缩小。
- 只调大并发，不看线程池、Redis、模型配额和下游限速，容易把局部优化变成全链路抖动。
- 改 `agent-binding` 时只看显示名称，不检查真实配置名、默认名、别名和实际 Agent 记录是否一致。

## 现象到检查点

| 现象 | 第一检查点 | 常见根因 |
| --- | --- | --- |
| 同一请求重复调用模型 | `InterviewAiInvoker.buildSingleFlightKey(...)` | key 过细、sessionId 不稳定、业务 key 变了 |
| 请求一直慢但不一定报错 | `ai-guard` stage 超时值、下游文件/恢复耗时 | 真正慢点不在模型，而在前置准备 |
| 某个 stage 偶发性全挂 | stage 名称、`ai-guard.stages.*`、`ai-singleflight.stage-policies.*` | 新 stage 没配置，或配置名和代码常量不一致 |
| 限流突然大量命中 | `flow-limit` 各窗口阈值 | 前端轮询放大、重试风暴、批量任务混入在线链路 |
| 跟随者等待体验很差 | `follower-max-wait-millis`、结果 TTL、L1 缓存 | 主请求太慢，或 singleflight 命中低 |

## 改动时不要只改一处

- 改 Guard，要同时看 stage 超时、重试次数、并发上限和线程池容量。
- 改 SingleFlight，要同时看 key 组成、结果 TTL、失败 TTL 和接管阈值。
- 改限流，要同时看全局阈值、答题阈值、heavy 阈值、read 阈值和 ai-call 阈值。
- 改 Agent 绑定，要同时看 `BusinessAgentScene`、`application.yaml` 和实际 Agent 配置记录。

## 一个实用判断

- 如果现象是“偶发重复”，优先查 singleflight key 和 requestId 稳定性。
- 如果现象是“稳定超时”，优先查 Guard stage 和前置重材料步骤。
- 如果现象是“整体卡顿”，优先查线程池与限流，而不是先怀疑单个业务方法。
