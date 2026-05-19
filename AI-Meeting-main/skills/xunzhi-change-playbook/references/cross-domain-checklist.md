# 跨域改动检查清单

## 第一步：先判定你改的是哪类对象

| 对象类型 | 典型对象 | 改动时最容易漏什么 |
| --- | --- | --- |
| 主记录 | `InterviewSession`、`AgentConversation`、`UserDO` | 创建、结束、归属、历史查询、归档 |
| 运行态 | `InterviewFlowState`、幂等标记、锁、转写上下文 | 恢复、并发、失效、补偿 |
| 快照 | 热快照、冷快照、恢复视图 | 回填、只读恢复、和主记录对齐 |
| 契约 | DTO、workflow 字段、WebSocket 消息、配置键 | 前后端一致性、解析器、自动索引 |
| 配置 | stage、线程池、限流、agent-binding | 常量对齐、监控语义、回滚策略 |

## 第二步：再判断改动覆盖哪几层

- 接口层：controller、DTO、返回结构。
- 业务层：facade、service、状态机、缓存写入。
- 工作流层：`workflow/*.yml`。
- 运行时层：限流、guard、singleflight、线程池、配置。
- 鉴权层：`@CurrentUser`、会话归属、管理员权限。

## 改动类型 -> 必看清单

| 改动类型 | 先看什么 | 一定要联动什么 |
| --- | --- | --- |
| 新增接口 | controller、DTO、归属语义 | service、权限校验、索引文档 |
| 改面试对象字段 | `InterviewSession` / `InterviewQuestion` / `InterviewRecordDO` | 恢复页、答题链路、归档、对象词典 |
| 改通用会话字段 | `AgentConversation` / `AgentMessage` | 历史查询、归属校验、SSE 输出 |
| 改工作流字段 | workflow YAML | Java 解析器、DTO、自动抽取索引 |
| 改运行时配置 | `application.yaml` | stage 常量、日志、监控、线程池、对象词典 |
| 改鉴权逻辑 | token、`@CurrentUser`、WebSocket 鉴权 | 归属校验、管理员接口、历史查询 |

## 一套推荐顺序

1. 先定位主域。
2. 再确认对象类型和真相源。
3. 先改契约，再改实现。
4. 再改缓存/快照/恢复逻辑。
5. 最后补自动索引、对象词典和排障剧本。

## 每次都要问

- 改动会不会影响前端接口契约？
- 改动会不会影响缓存键、状态推进或恢复结果？
- 改动会不会影响工作流字段名？
- 改动会不会影响运行时保护层？
- 改动会不会让一个非真相源对象被误当成真相源？

## 一个简短原则

- 先改契约，再改实现，再改索引，最后改文档。
- 任何改动只要碰到对象语义，都要同步刷新对象词典。
