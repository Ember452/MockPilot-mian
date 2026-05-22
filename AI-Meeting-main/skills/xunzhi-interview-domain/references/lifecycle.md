# 面试生命周期

## 会话状态

- `DRAFT`：刚创建，尚未上传或提取简历。
- `RESUME_UPLOADING`：正在上传并提取题目，避免并发接口误判状态。
- `READY`：题目、方向、简历地址已准备好，可以进入面试。
- `IN_PROGRESS`：已经开始取题或答题。
- `FINISHED`：已完成面试并收尾。
- `ABANDONED`：已废弃或不可继续。

## 核心推进链路

- `createSession`：创建会话，起点通常是 `DRAFT`。
- `extractInterviewQuestions`：先把会话标成 `RESUME_UPLOADING`，成功后标成 `READY`，失败回落 `DRAFT`。
- `getCurrentQuestion`、`getNextQuestion`、`answerInterviewQuestion`：都会先校验会话归属和是否可继续；首次进入会把 `READY` 升到 `IN_PROGRESS`。
- `finishSession` / `endConversation`：统一走 finalize 收口，落记录并补齐结束态。

## `canResume` 语义

- 只有 `READY` 和 `IN_PROGRESS` 可以恢复。
- `DRAFT` 说明题目材料还不完整。
- `FINISHED` 和 `ABANDONED` 不应该再继续答题。

## 生命周期与材料回填

- 提题成功后，`InterviewSessionFacade` 会把 `resumeFileUrl` 和 `interviewType` 回填到会话主记录。
- 恢复接口会优先读会话主字段，再用 `InterviewQuestion` 补齐方向、简历、简历分。
- 建议前端把恢复接口看成“恢复页渲染的总装接口”，而不只是“查状态”。