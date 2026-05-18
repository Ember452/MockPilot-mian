# 业务对象词典

## 这份词典解决什么问题

同一个仓库里，最容易出错的不是代码本身，而是名词没有统一。

这份词典的目标只有一个：把仓库里的核心名词统一成可执行的业务语言。

## 对象层级

| 层级 | 作用 | 典型对象 |
| --- | --- | --- |
| 主记录 | 最终业务事实 | `UserDO`、`InterviewSession`、`AgentConversation`、`InterviewRecordDO` |
| 运行态 | 当前处理中的状态 | `InterviewFlowState`、`TranscriptionSessionContext`、锁、幂等标记 |
| 快照 | 恢复和回放所需的视图 | `InterviewSessionRuntimeHotSnapshot`、`InterviewSessionRuntimeColdSnapshot` |
| 归档 | 长期追溯和最终留痕 | `InterviewSessionTurnArchive`、最终记录表 |
| 契约 | 接口、工作流、配置的边界 | DTO、`workflow/*.yml`、`application.yaml`、Java 常量 |

## 真相源层级

| 层级 | 适合什么 | 不适合什么 |
| --- | --- | --- |
| MySQL | 最终记录、权限、配置、归档结果 | 高频游标、临时状态 |
| MongoDB | 会话材料、可增量演化的业务文档、快照 | 纯短期锁状态 |
| Redis | 高并发游标、幂等标记、锁、过渡态缓存 | 长期主记录 |
| YAML | 契约、规则、保护策略、运行参数 | 业务最终事实 |
| Java 常量/枚举 | 稳定语义边界、状态边界 | 动态配置值 |

## 同词不同义

| 名词 | 在不同域里的含义 |
| --- | --- |
| `sessionId` | 面试会话 ID、通用 Agent 会话 ID、WebSocket 连接上下文 ID，不能默认相等 |
| `requestId` | 幂等边界，不是用户身份，也不是业务主键 |
| `questionNumber` | 面试题号、追问题号、恢复回放定位号，不是数据库主键 |
| `record` | 可能是归档记录、消息记录、事件记录，必须结合域判断 |
| `snapshot` | 可能是热快照、冷快照、恢复视图，不能和主记录混写 |
| `agentName` / `agentId` / `sceneCode` | 场景、配置入口、持久化标识是三件事 |
| `loadMode` / `confidence` | 恢复读写模式和恢复置信度，属于运行态语义，不是展示字段 |

## 全局对象词典

### 1. 用户与身份

| 对象 | 含义 | 真相源 | 关键字段 | 能否做归属判断 |
| --- | --- | --- | --- | --- |
| `CurrentPrincipal` | 当前登录主体 | `Sa-Token` + `UserDO` | `userId`、`username` | 可以 |
| `UserContext` | 接口层用户上下文 | 请求上下文 | `userId`、`username` | 可以 |
| `UserDO` | 用户持久化实体 | MySQL `t_user` | `id`、`username`、`password`、`realName`、`phone`、`mail` | 可以 |

### 2. 面试会话

| 对象 | 含义 | 真相源 | 关键字段 | 写入时机 |
| --- | --- | --- | --- | --- |
| `InterviewSession` | 面试会话主记录 | MongoDB `interview_session` | `sessionId`、`userId`、`status`、`resumeFileUrl`、`interviewType`、`interviewerAgentId` | 创建会话、状态变更、会话结束 |
| `InterviewQuestion` | 面试材料与题目结果 | MongoDB `interview_question` | `sessionId`、`questionsJson`、`suggestionsJson`、`resumeScore`、`interviewType` | 提题成功、恢复回填 |
| `InterviewRecordDO` | 面试正式记录 | MySQL `interview_record` | `sessionId`、`interviewScore`、`resumeScore`、`interviewStatus`、`questionCount`、`sessionSnapshotJson` | finalize 收尾 |
| `InterviewFlowState` | 题目流转游标 | Redis | `status`、`currentIndex`、`currentQuestionNumber`、`followUpCount`、`maxFollowUp`、`version` | 每次答题推进 |
| `InterviewTurnLog` | 单轮答题日志 | Redis / 快照 / 归档 | `requestId`、`questionNumber`、`answerContent`、`score`、`totalScore`、`followUpCount` | 每轮答题成功后 |
| `InterviewSessionRuntimeSnapshot` | 运行态统一快照 | MongoDB | 热态 + 冷态的组合视图 | 恢复、回放、排障 |
| `InterviewSessionRuntimeHotSnapshot` | 热快照 | MongoDB | `flow`、`scoreAggregate`、`recentTurns`、`archiveWatermark`、`lastAppliedRequestId` | 高频恢复、幂等回放 |
| `InterviewSessionRuntimeColdSnapshot` | 冷快照 | MongoDB | `questions`、`suggestions`、`resumeContext`、`resumeScore`、`demeanorScore` | 低频材料回补 |
| `InterviewSessionTurnArchive` | 轮次归档 | MongoDB | `sessionId`、`requestId`、`seq`、`turnPayload` | 稳定留痕和追溯 |
| `InterviewSessionRuntimeView` | 恢复后的统一视图 | Java 视图对象 | `confidence`、`loadMode`、`restoreSource`、`hotSnapshot`、`coldSnapshot` | 恢复后读写判断 |

### 3. 通用 Agent

| 对象 | 含义 | 真相源 | 关键字段 | 写入时机 |
| --- | --- | --- | --- | --- |
| `AgentConversation` | 通用 Agent 会话主记录 | MongoDB `agent_conversation` | `sessionId`、`userId`、`agentId`、`conversationTitle`、`messageCount`、`status` | 创建会话、结束会话、标题回填 |
| `AgentMessage` | 通用 Agent 消息 | MongoDB `agent_message` | `sessionId`、`messageType`、`messageSeq`、`messageContent`、`tokenCount` | 每次消息入历史 |
| `AgentPropertiesDO` | Agent 配置记录 | MySQL `agent_properties` | `agentName`、`apiKey`、`apiSecret`、`apiFlowId` | 配置更新、绑定变更 |
| `BusinessAgentScene` | 业务场景枚举 | Java 常量 | `code`、`defaultAgentName`、`candidateAgentNames` | 代码发布时固定 |

### 4. 媒体与实时通信

| 对象 | 含义 | 真相源 | 关键字段 | 写入时机 |
| --- | --- | --- | --- | --- |
| `WebSocketMessage` | 客户端控制命令 | WebSocket 入参 | `type` | 每次客户端控制消息 |
| `WebSocketResponse` | 统一推送响应 | WebSocket 出参 | `type`、`message`、`data`、`fullText`、`updateAction`、`timestamp` | 服务端推送 |
| `TranscriptionSessionContext` | 单个转写会话上下文 | 内存态 | `audioInputStream`、`audioOutputStream`、`active`、`stopRequested` | 建连、start、stop |
| `RealtimeTranscriptionUpdate` | 转写增量结果 | 转写引擎回调 | `fullText`、`committedText`、`liveText`、`revision`、`segmentId`、`finalPacket` | 引擎回调 |
| `LongTextTtsReqDTO` | TTS 创建请求 | HTTP 请求体 | `text`、`vcn`、`language`、`speed`、`volume`、`pitch`、`rhy`、`audioEncoding`、`sampleRate` | 提交合成 |
| `LongTextTtsTaskRespDTO` | TTS 任务响应 | HTTP 响应体 | `taskId`、`taskStatus`、`code`、`audioBase64`、`pybufContent`、`completed`、`success` | 查询任务 |

### 5. 运行时与保护层

| 对象 | 含义 | 真相源 | 关键字段 | 写入时机 |
| --- | --- | --- | --- | --- |
| `InterviewAiGuardStage` | AI 保护阶段名 | Java 常量 | `stage` | stage 发布时 |
| `InterviewRuntimeLoadMode` | 恢复读写模式 | Java 枚举 | `READ_ONLY`、`READ_WRITE_REQUIRED` | 恢复决策时 |
| `InterviewRuntimeConfidence` | 恢复置信度 | Java 枚举 | `EXACT`、`DERIVED`、`READ_ONLY`、`TERMINAL` | 恢复后判定时 |
| `BusinessAgentScene` | 场景目录 | Java 枚举 | `sceneCode` | 场景解析时 |

## 最重要的判断

> 先问：这个对象的真相源在哪里？
>
> 再问：它是主记录、运行态、快照、归档，还是契约对象？
>
> 最后问：它能不能作为别人依赖的唯一真相？

只要这三个问题没答清，就不要直接改代码。
