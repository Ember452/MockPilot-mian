# 答题流水线详解

`InterviewAnswerPipeline` 是面试域最核心的业务编排器。

## 执行顺序

1. 校验基础参数。
2. 归一化 `requestId`，保证幂等键稳定。
3. 调 `InterviewAnswerIdempotencyService.tryStart` 做幂等门禁。
4. 加载当前题和 flow，拒绝过期题号。
5. 以“当前题号”为粒度加锁，串行化同题并发提交。
6. 加锁后再次校验题号，防止锁前后题号漂移。
7. 调评分链路，解析出结构化评分结果。
8. 决定是否追问；若不追问则推进主问题或结束。
9. 成功前才提交主问题分数；提交失败要回滚 flow。
10. 成功后写幂等回放结果，并刷新运行时快照。

## 幂等门禁

- 处理中直接返回：`current request is processing, please retry later`。
- 命中成功回放时，不再重新评分，直接返回历史响应。
- 回放数据不仅来自幂等缓存，也会从运行时快照中找可复用响应。

## 题号保护

- 过期题号返回：`stale question number, please refresh current question`。
- 同题并发抢锁失败返回：`current question is processing, please retry later`。
- 如果缓存里当前题不存在，会返回“题目不存在或已过期”。

## 评分结构化结果

当前代码约定的主字段：

- `score`：`0~100` 的整数。
- `feedback`：简洁、可执行的反馈。
- `missing_points`：字符串数组。
- `follow_up_needed`：布尔值。
- `follow_up_question`：当需要追问时应非空。

## 追问分支

- 先由评分链路给出 AI 建议的 `follow_up_needed`。
- 再进入 `InterviewFollowUpRuleService`，由规则引擎最终决定是否追问。
- 即使 AI 建议追问，只要次数达到上限，也会回落到主问题推进。
- 追问题成功生成后，会缓存题目并把 flow 切到 `FOLLOW_UP`。

## 分数提交与补偿

- 只有主问题计入总分，追问本身不累计总分。
- 分数是在“返回成功前”提交，避免失败重试时重复加分。
- 如果 flow 已推进但分数提交失败，会按快照回滚 flow，保证客户端重试时仍命中当前题。

## 与恢复链路的关系

- 载入当前题时会调用 `ensureRuntime(..., READ_WRITE_REQUIRED, HOT_RUNTIME)`。
- 如果恢复结果是只读且非终态，会直接失败：`interview runtime restored as read-only`。
- 成功提交后会刷新运行时快照，供后续恢复和幂等回放使用。