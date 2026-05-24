---
name: xunzhi-interview-domain
description: AI-Meeting 面试业务知识 Skill。用于处理面试会话创建、简历提题、答题评分、追问、恢复、简历预览、神态分析、收尾归档、工作流契约和面试状态机相关需求；当需求命中 `/api/xunzhi/v1/interview/**`、`workflow/*.yml` 或面试运行态恢复逻辑时使用。
---

# xunzhi-interview-domain

只要需求落到面试主链路，就使用这个 Skill。

## 使用顺序

- 再看 `references/object-dictionary.md` 和 `references/invariants.md`，确认对象模型和不可破坏的业务约束。
- 先看 `references/lifecycle.md`，确认会话处于哪个业务阶段。
- 再看 `references/answer-pipeline.md`，确认答题是如何做幂等、加锁、评分和推进的。
- 改状态推进时看 `references/state-machine.md`。
- 改评分、追问、规则引擎时看 `references/scoring-followup-rules.md`。
- 改工作流字段、结构化输出时看 `references/workflow-contracts.md` 和 `references/generated-workflow-contracts.md`。
- 改恢复、收尾、快照、会话重建时看 `references/restore-and-finalize.md`。

## 关键入口

- `admin/src/main/java/com/hewei/hzyjy/xunzhi/interview/api/InterviewSessionController.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/interview/api/InterviewResumeController.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/interview/flow/session/InterviewSessionFacade.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/interview/flow/answer/InterviewAnswerPipeline.java`
- `admin/src/main/java/com/hewei/hzyjy/xunzhi/interview/application/flow/InterviewFlowStateMachine.java`
- `admin/src/main/resources/workflow/`
- `admin/src/main/resources/interview-followup-rule.yaml`

## 必守约束

- 面试会话状态和题目流转状态是两套状态，不要混写。
- `questionNumber` 既可能是主问题，也可能是追问题，不能把它当数据库主键看。
- `requestId` 是答题幂等边界，必须稳定，不能让前端每次重试都换值。
- 工作流输出字段必须和 Java 侧解析字段对齐。
- 答题链路不能跳过幂等、题号校验、同题加锁和分数提交补偿。
- 追问是否发生，不只看 AI 输出，还要看规则引擎和最大追问次数。
- 恢复与收尾不是附属能力，而是正式业务契约。

## 参考资料

- `references/object-dictionary.md`
- `references/invariants.md`
- `references/lifecycle.md`
- `references/answer-pipeline.md`
- `references/state-machine.md`
- `references/scoring-followup-rules.md`
- `references/workflow-contracts.md`
- `references/generated-workflow-contracts.md`
- `references/restore-and-finalize.md`
- `references/gotchas.md`
- `scripts/extract_workflow_contracts.py`
