# Agent 场景绑定

## 固定场景目录

`BusinessAgentScene` 当前定义了这些场景：

- `general-agent-chat` -> 默认名 `通用智能体`
- `interview-question-extraction` -> 默认名 `面试出题官`，别名 `面试题出题官`
- `interview-answer-evaluation` -> 默认名 `用户答案评分官`，别名 `面试答案评分官`
- `interview-demeanor` -> 默认名 `神态分析官`，别名 `神态评分面试官`、`表情分析面试官`
- `interview-question-asking` -> 默认名 `面试提问官`

## 解析顺序

- 先读 `xunzhi-agent.agent-binding` 中的配置名。
- 如果配置名存在，先尝试配置名。
- 再按枚举里的默认名和别名顺序尝试。
- 找到第一个能命中的 Agent 配置就返回。

## 为什么重要

- 这层决定代码里的“业务场景”最终调用哪个 Agent 配置。
- 配置名改了但 Agent 表里没同步，运行时就会 fallback，严重时直接报错。
- 面试域和通用 Agent 都依赖这个解析过程，但不要把它们的业务边界混在一起。