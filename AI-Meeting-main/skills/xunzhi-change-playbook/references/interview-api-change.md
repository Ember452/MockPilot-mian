# 面试接口改动剧本

## 常见受影响位置

- `InterviewSessionController`
- `InterviewSessionFacade`
- `InterviewAnswerPipeline`
- `InterviewFlowStateMachine`
- `workflow/*.yml`
- `interview-followup-rule.yaml`

## 改动步骤

1. 明确是新增字段、改语义还是改流程。
2. 先改请求/响应 DTO 和接口入口。
3. 再改 facade 和真正持有约束的业务编排。
4. 如果涉及评分或追问，检查 workflow 和规则引擎。
5. 如果涉及恢复或总分，检查运行态快照与回填路径。

## 必查项

- 题号是否仍然受 stale check 保护。
- 新字段是否要落到缓存、快照或正式记录。
- `READY -> IN_PROGRESS` 这类状态推进是否还成立。