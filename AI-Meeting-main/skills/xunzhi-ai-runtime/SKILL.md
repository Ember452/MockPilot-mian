---
name: xunzhi-ai-runtime
description: AI-Meeting 运行时与配置知识 Skill。用于处理面试与 Agent 链路中的限流、AI Guard、singleflight、线程池、场景绑定、长会话恢复配置和关键 `application.yaml` 语义；当需求涉及超时、重试、并发、回放、接管、缓存 TTL 或运行参数时使用。
---

# xunzhi-ai-runtime

这层不直接定义业务结果，但决定业务链路的稳定性和并发表现。

## 使用顺序

- 再看 `references/object-dictionary.md`，确认运行时语义对象和配置对象的真相源。
- 先看 `references/generated-config-index.md`，确认真实配置值。
- 再看 `references/ai-guard.md` 和 `references/ai-singleflight.md`，判断链路受哪层保护。
- 涉及限流时看 `references/flow-limit.md`。
- 涉及线程池与异步任务时看 `references/thread-pool.md`。
- 涉及场景与 Agent 名称绑定时看 `references/agent-binding.md`。

## 这层管什么

- 面试评分、追问、提题、神态分析的超时、重试、并发上限。
- 同一输入的重复 AI 调用去重与结果复用。
- 热点请求的跟随者等待、接管和结果缓存策略。
- 线程池资源划分和调度资源。
- 业务场景到 Agent 配置的绑定。

## 必守约束

- stage 名称必须在配置、调用点和监控语义上保持一致。
- `loadMode`、`confidence` 不是展示字段，而是恢复是否可写的正式业务语义。
- singleflight key 的组成不能随意改，否则会直接影响重复调用复用率。
- 不要只改配置不改说明文档；改完重新生成配置索引。
- 不要在业务层绕过 Guard 或 SingleFlight，否则等于把保护层拆掉。

## 参考资料

- `references/object-dictionary.md`
- `references/generated-config-index.md`
- `references/agent-binding.md`
- `references/ai-guard.md`
- `references/ai-singleflight.md`
- `references/flow-limit.md`
- `references/thread-pool.md`
- `references/gotchas.md`
- `scripts/extract_config_index.py`
