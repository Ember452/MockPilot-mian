# 面试域不变量

## 会话不变量

- `InterviewSession.sessionId` 全局唯一。
- `InterviewSession.userId` 是归属判断唯一依据。
- `READY` 和 `IN_PROGRESS` 才能恢复继续答题。
- `FINISHED` 和 `ABANDONED` 不应再进入答题编排。

## 流转不变量

- 题目流转状态和会话状态不是一个东西。
- `ASKING` / `EVALUATING` / `FOLLOW_UP` / `COMPLETED` 仅描述 flow。
- `currentQuestionNumber` 必须和请求题号一致，否则直接拒绝。
- 同题并发必须串行化。

## 评分不变量

- 评分结果必须包含 `score`。
- `follow_up_needed` 只能作为建议，最终是否追问要看规则引擎和追问上限。
- 追问不直接累计总分。
- 主问题分数只在成功返回前提交。

## 恢复不变量

- 恢复页必须优先读取主记录，再回补快照。
- 只读恢复不能冒充可写恢复。
- 快照和记录不一致时，应优先遵循更新更近、语义更强的运行态数据。
- 软回放和补偿必须保持题号连续性。

## 契约不变量

- workflow 输出字段必须和 Java 解析字段一致。
- `requestId` 是幂等边界，不是业务主键。
- `questionNumber`、`nextQuestionNumber`、`followUpCount`、`finished` 一起构成答题响应语义。