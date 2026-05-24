# 面试域对象词典

## 目标

把面试域里所有“会被误解的名词”统一成同一种业务语言，并明确每个对象的真相源、写入路径、读取路径和不能替代什么。

## 1. 主记录层

| 对象 | 真相源 | 写入时机 | 读取时机 | 不能替代什么 |
| --- | --- | --- | --- | --- |
| `InterviewSession` | MongoDB | 创建会话、状态变更、结束会话 | 创建页、恢复页、答题前校验 | 题目流转状态 |
| `InterviewQuestion` | MongoDB | 提题成功、恢复回填 | 提题、恢复、当前题回填 | 主会话状态 |
| `InterviewRecordDO` | MySQL | finalize 收尾 | 结果页、历史页、恢复兜底 | 实时运行态 |

## 2. 运行态层

| 对象 | 真相源 | 作用 | 典型写入者 | 典型读取者 |
| --- | --- | --- | --- | --- |
| `InterviewFlowState` | Redis | 当前题号、追问次数、状态游标 | 答题链路、状态机 | 答题链路、恢复链路 |
| `InterviewTurnLog` | Redis / 快照 / 归档 | 单轮答题回放、补偿、软回放 | 答题流水线 | 恢复、排障、回放 |
| `InterviewSessionRuntimeHotSnapshot` | MongoDB | 高频恢复、幂等回放、最近轮次 | 恢复服务、答题后刷新 | 恢复页、排障页 |
| `InterviewSessionRuntimeColdSnapshot` | MongoDB | 题目、建议、简历上下文、神态结果 | 提题链路、恢复链路 | 恢复页、结果页 |
| `InterviewSessionRuntimeSnapshot` | 组合视图 | 热态与冷态统一快照 | 恢复服务 | 恢复服务、调试工具 |
| `InterviewSessionRuntimeView` | Java 视图 | 恢复后的读写能力判断 | 恢复服务 | 答题链路、恢复页 |
| `InterviewSessionTurnArchive` | MongoDB | 轮次归档和长期追溯 | 答题成功后的收口 | 追溯、报表、修复 |
| `InterviewRuntimeScoreAggregate` | 运行态对象 | 总分、题数、累计分聚合 | 答题后聚合、恢复回放 | 恢复页、归档同步 |

## 3. 契约层

| 对象 | 角色 | 核心字段 | 说明 |
| --- | --- | --- | --- |
| `InterviewAnswerReqDTO` | 输入契约 | `questionNumber`、`answerContent`、`requestId` | 幂等、提问、作答入口 |
| `InterviewAnswerRespDTO` | 输出契约 | `score`、`totalScore`、`nextQuestion`、`finished` | 一次答题同时承载评分和下一步 |
| `InterviewQuestionRespDTO` | 输出契约 | `questions`、`suggestions`、`resumeScore` | 提题结果和简历评分合并输出 |
| `InterviewSessionRestoreRespDTO` | 输出契约 | `canResume`、`resumeFileUrl`、`suggestions` | 恢复页最小可渲染对象 |
| `RadarChartDTO` | 输出契约 | 五维画像 | 恢复后的综合展示 |
| `DemeanorScoreDTO` | 输入/输出契约 | 慌乱度、严肃度、表情处理、综合分 | 神态分析细粒度结果 |

## 4. 题号语义

| 名词 | 语义 | 规则 |
| --- | --- | --- |
| `questionNumber` | 业务题号 | 可以是主问题，也可以是追问题 |
| `nextQuestionNumber` | 下一步题号 | 由状态机和追问规则共同决定 |
| `followUpCount` | 当前追问序号 | 只属于当前主问题分支 |
| `maxFollowUp` | 最大追问次数 | 约束追问是否还能继续 |

## 5. 真相源判定规则

- 要判断“能不能继续答题”，先看 `InterviewSession.status`，再看 `InterviewFlowState`。
- 要判断“当前题是什么”，先看 `InterviewFlowState.currentQuestionNumber`，再看 `InterviewQuestion` / `InterviewTurnLog`。
- 要判断“总分是多少”，先看缓存聚合，再看运行态快照，再看 `InterviewRecordDO` 兜底。
- 要判断“题目/建议/简历评分是什么”，先看 `InterviewQuestion`，再看冷快照。
- 要判断“当前是否能写”，先看 `InterviewSessionRuntimeView.loadMode` 和 `confidence`。

## 6. 生命周期

1. 创建 `InterviewSession`，初始化会话主记录。
2. 提题后写入 `InterviewQuestion`，同时落下运行态快照。
3. 答题链路不断更新 `InterviewFlowState` 和 `InterviewTurnLog`。
4. 恢复时先拿主记录，再回补快照和流转状态。
5. 结束时把最终结果写入 `InterviewRecordDO`，并归档轮次。

## 7. 常见误解

- `InterviewSession.status` 不是题目流转状态。
- `questionNumber` 不是数据库主键，它是业务题号语义。
- `totalScore` 不是任何一层都能随便改的数字，只有答题完成路径能稳定写它。
- `resumeScore` 不是最终报告的全部，只是面试材料的一部分。
- 追问是主问题的附属分支，不是独立会话。
