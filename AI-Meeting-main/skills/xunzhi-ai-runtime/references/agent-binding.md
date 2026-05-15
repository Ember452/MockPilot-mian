# Agent 绑定

## 配置入口

`application.yaml` 中的 `xunzhi-agent.agent-binding` 负责把业务场景绑定到实际 Agent 名称：

- `general-agent-chat -> 通用智能体`
- `interview-question-extraction -> 面试出题官`
- `interview-answer-evaluation -> 用户答案评分官`
- `interview-demeanor -> 神态分析官`
- `interview-question-asking -> 面试提问官`

## 代码入口

- `BusinessAgentScene`：声明场景 code、默认名称、别名集合。
- `BusinessAgentBindingProperties`：从配置读取绑定名。
- `BusinessAgentResolver`：按“配置名 -> 默认名 -> 别名”的顺序解析实际 Agent。

## 回退规则

- 如果配置名存在且能找到 Agent，优先使用配置名。
- 如果配置名找不到，会继续尝试默认名和别名。
- 如果最终一个都找不到，会抛 `AGENT_CONFIG_NOT_FOUND`。

## 运行时影响

- 这层决定面试评分、提题、追问、神态分析到底调哪个 Agent 配置。
- 所以改名字不是纯展示改动，而是正式运行时改动。