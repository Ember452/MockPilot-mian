# 面试状态机

面试域里至少有两套状态：

- 会话状态：`DRAFT / RESUME_UPLOADING / READY / IN_PROGRESS / FINISHED / ABANDONED`
- 题目流转状态：`INIT / ASKING / EVALUATING / FOLLOW_UP / COMPLETED`

## Flow 合法流转

- `INIT -> ASKING` 或 `INIT -> COMPLETED`
- `ASKING -> EVALUATING / FOLLOW_UP / COMPLETED`
- `EVALUATING -> ASKING / FOLLOW_UP / COMPLETED`
- `FOLLOW_UP -> EVALUATING / ASKING / COMPLETED`
- `COMPLETED` 不再向外流转

## 关键方法

- `moveToEvaluating`：进入评分阶段。
- `moveToFollowUp`：进入追问阶段。
- `startFollowUpQuestion`：在 flow 合法的前提下创建追问题号。
- `advanceMainQuestion`：推进到下一主问题；如果越界则改为完成。
- `markCompleted`：显式收口。

## 题号规则

- `currentQuestionNumber` 优先取缓存中的显式题号。
- 如果显式题号为空，会用 `currentIndex + 1` 推导。
- `currentIndex >= totalQuestions` 会被视为越界，再由状态机收口到完成。

## 实战提醒

- 改题号推进时，必须同时考虑主问题和追问题号。
- 会话状态变成 `FINISHED` 并不自动等于 flow 已完成，收尾逻辑需要统一打通。