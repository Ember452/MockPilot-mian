# 面试卡住排障剧本

## 常见现象

- 一直提示“当前请求处理中”。
- 一直提示“当前题处理中”。
- 一直提示 stale question。
- 当前题拿不到，或恢复后提示只读。
- 题目能看见，但答题后题号没有推进。

## 先看哪些对象

| 现象 | 先看对象 | 真相源 |
| --- | --- | --- |
| 当前请求处理中 | `InterviewAnswerIdempotencyService` / `requestId` | Redis / 运行时幂等标记 |
| 当前题处理中 | `InterviewQuestionLockService` / `InterviewFlowState` | 同题锁、流转游标 |
| stale question | `currentQuestionNumber` vs 请求题号 | Redis flow + 请求体 |
| 恢复后只读 | `InterviewSessionRuntimeView` / `loadMode` / `confidence` | 恢复视图 |
| 题目不推进 | `InterviewFlowStateMachine` / `InterviewTurnLog` | 状态机 + 回放记录 |

## 排障顺序

1. 先确认是不是同一个 `requestId` 在重放。
2. 再确认当前题号是否和 `InterviewFlowStateMachine` 中的游标一致。
3. 再确认是否是同题并发，锁没拿到。
4. 再确认运行态是否只恢复到了 `READ_ONLY`。
5. 最后确认分数提交是否失败后回滚了 flow。

## 常见根因

- 重复请求没有稳定 `requestId`。
- 同题并发提交打满了同题锁。
- 前端持有旧题号继续提交。
- 运行态恢复不完整，导致只能只读回放。
- 分数提交失败后回滚了 flow，但前端没有刷新当前题。

## 处理建议

- 先让前端刷新当前题，再重提一次。
- 检查同一个 session 是否在多个标签页并发答题。
- 检查缓存里的当前题号和数据库里的题号是否一致。
- 如果是恢复态问题，先补运行态，再看答题逻辑。
