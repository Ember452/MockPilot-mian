# 面试域易错点

## 最容易混淆的几件事

- 不要把会话状态和 flow 状态混为一谈。
- 不要只改 workflow，不改 Java 解析字段。
- 不要在答题链路绕过幂等和同题加锁。
- 不要让追问直接累计到总分，当前实现只累计主问题分数。
- 不要假设恢复接口只读数据库，它会主动触发运行态回填。
- 不要在可写场景使用只读恢复结果，否则会遇到 `read-only` 失败。

## 现象到检查点

| 现象 | 第一检查点 | 常见原因 |
| --- | --- | --- |
| 一直无法答题推进 | `InterviewAnswerPipeline` 前半段 | requestId 不稳定、同题锁冲突、旧题号 |
| 评分结果字段缺失 | `InterviewEvaluationService` 输出 | workflow 字段和 Java 解析不一致 |
| 追问次数不对 | `InterviewFollowUpRuleService` + flow 中追问计数 | 规则引擎、默认上限或当前题号状态错了 |
| 恢复页内容不完整 | `restoreInterviewSession` | 会话主记录、题目表、缓存三层回填不一致 |
| 总分回退或不一致 | `InterviewSessionRuntimeSnapshotService` | 缓存刷新时机、回滚补偿、记录同步问题 |

## 一个实用判断

- 如果问题描述里有“题号”“追问”“评分”“恢复”，优先看面试域，不要先去怀疑通用 Agent。
- 如果问题描述里有“只读”“回放”“回滚”，优先看运行态恢复和快照，不要只盯 controller。
- 如果问题描述里有“结果缺字段”，优先看 workflow 契约，不要只看业务代码。
