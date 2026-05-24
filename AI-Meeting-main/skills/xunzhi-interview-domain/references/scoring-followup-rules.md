# 评分与追问规则

## 评分结果先于规则决策

- `InterviewEvaluationService` 先返回结构化评分结果。
- 代码会统一归一化这些字段：`score`、`feedback`、`missing_points`、`follow_up_needed`、`follow_up_question`。
- 如果工作流解析失败，会回落到统一 schema 的兜底输出要求。

## 规则引擎入口

- `InterviewFollowUpRuleService.decide(context)` 是最终裁决点。
- 它会先把 `chainId`、`ruleVersion`、`resolvedMaxFollowUp`、`lowScoreThreshold` 回填到上下文。
- 默认链路来自 `interview-followup-rule.yaml` 中的 `default_followup_chain`。

## 当前默认规则参数

- `fail-open: true`
- `default-max-follow-up: 2`
- `default-low-score-threshold: 60`
- `rule-version: v1.0.0`

## 决策原则

- 如果规则引擎正常执行，以 LiteFlow 决策为准。
- 如果规则引擎失败且 `fail-open=true`，会回退到旧策略。
- 旧策略的本质是：AI 建议追问并且未超过次数上限时，允许追问。
- 如果已达到追问上限，原因码会变成 `FOLLOW_UP_LIMIT_REACHED`。

## 你改规则时要同步看什么

- `interview-followup-rule.yaml`
- `liteflow/interview-followup-chain.xml`
- `InterviewFollowUpRuleContext` / `InterviewFollowUpRuleDecision`
- `InterviewAnswerPipeline.stepAdvanceFlowAndAssemble`
- `workflow/面试提问官.yml`