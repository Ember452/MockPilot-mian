# 恢复与收尾

## 恢复不是单点接口，而是一整套运行态模型

`InterviewSessionRuntimeRehydrateService` 会按范围恢复运行态：

- `FLOW_ONLY`：只恢复题目材料和 flow。
- `SCORE_ONLY`：只恢复总分。
- `PLAYBACK_ONLY`：只恢复轮次回放。
- `MATERIAL_ONLY`：恢复题目、建议、简历材料。
- `HOT_RUNTIME`：恢复答题时所需的热态。
- `FULL_RUNTIME`：恢复完整运行态。

## 读写模式

- `READ_ONLY`：用于只读查询，例如恢复页、总分、建议。
- `READ_WRITE_REQUIRED`：用于答题推进等必须可写的场景。

## 恢复来源

- 优先复用缓存中的运行态。
- 缓存不完整时，尝试用运行时快照重建。
- 再不够时，回退到会话主记录、题目表、记录表和轮次归档。

## 快照服务的职责

`InterviewSessionRuntimeSnapshotService` 负责：

- 初始化草稿快照。
- 在提题、答题成功后刷新热快照与冷快照。
- 维护最近轮次、归档水位、分数聚合和恢复材料。
- 为幂等回放和长会话恢复提供软回放数据。

## 收尾语义

- `finishSession` 最终会委托记录服务从 Redis/运行态落正式记录。
- `endConversation` 在面试域里等价于 `finishSession`，不是简单的“会话关闭”。
- 查询总分、建议、雷达图时，代码会优先读缓存，再回退数据库记录，避免恢复后结果倒退。