# MockPilot Agent 项目代码阅读指南

本文档提供项目代码的阅读路径，按照从入口到核心业务、从基础设施到领域逻辑的顺序组织，帮助快速理解项目架构和核心实现。

---

## 一、项目整体架构

```
MockPilot Agent - AI 技术面试模拟平台
├── 技术栈：Spring Boot 3.4 + Spring AI 1.0.0 + LiteFlow + 多数据源协同
├── 核心能力：多 Agent 协作面试、RAG 知识库对话、实时语音交互、流式 AI 输出
└── 架构特点：DDD 分层 + 状态机驱动 + 规则引擎编排 + 分布式防护体系
```

**建议阅读顺序：**
1. 启动入口 → 2. 基础设施层 → 3. 认证授权 → 4. 用户管理 → 5. AI 对话核心 → 6. 面试核心流程 → 7. RAG 知识库 → 8. 媒体交互 → 9. 工具类

---

## 二、启动入口与配置

### 2.1 应用启动类

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `com.hewei.hzyjy.xunzhi` | `XunZhiAdminApplication.java` | Spring Boot 应用主入口，启动整个后端服务 | 标准 Spring Boot 启动类，配合 `@SpringBootApplication` 自动扫描 |

### 2.2 配置文件

| 文件路径 | 作用 | 亮点 |
|---------|------|------|
| `application.yaml` | 主配置文件，包含数据源、Redis、MongoDB、AI 模型、讯飞语音等核心配置 | 环境变量注入 + 多 Profile 支持（satoken、sse） |
| `application-satoken.yml` | Sa-Token 权限认证配置 | 基于 Sa-Token 的轻量级权限框架 |
| `interview-followup-rule.yaml` | 面试追问规则配置（LiteFlow 规则链） | 业务规则外置，支持热更新无需重启 |

---

## 三、公共基础设施层（common）

### 3.1 约定与规范（convention）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `common.convention.result` | `Result.java` | 统一 API 响应封装 | 泛型设计，支持成功/失败状态码 + 数据体 |
| `common.convention.result` | `PageInfo.java` | 分页结果封装 | 配合 MyBatis-Plus 分页插件 |
| `common.convention.result` | `Results.java` | Result 工厂类 | 提供 `success()` / `failure()` 静态方法 |
| `common.convention.exception` | `AbstractException.java` | 异常基类 | 统一异常体系，包含错误码 + 消息 |
| `common.convention.exception` | `ClientException.java` | 客户端异常（4xx） | 参数校验失败、业务规则违反 |
| `common.convention.exception` | `ServiceException.java` | 服务端异常（5xx） | 内部服务错误 |
| `common.convention.exception` | `RemoteException.java` | 远程调用异常 | 第三方服务调用失败 |
| `common.convention.errorcode` | `IErrorCode.java` | 错误码接口 | 统一错误码规范 |
| `common.convention.errorcode` | `BaseErrorCode.java` | 基础错误码枚举 | 通用错误码定义 |
| `common.convention.context` | `UserContext.java` | 用户上下文（ThreadLocal） | 请求级别用户信息传递 |
| `common.convention.annotation` | `CurrentUser.java` | 当前用户注解 | 配合参数解析器自动注入用户信息 |

### 3.2 枚举定义（enums）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `common.enums` | `UserErrorCodeEnum.java` | 用户模块错误码 | 用户注册、登录、权限相关错误 |
| `common.enums` | `InterviewErrorCodeEnum.java` | 面试模块错误码 | 面试状态、题目、评分相关错误 |
| `common.enums` | `AgentErrorCodeEnum.java` | Agent 模块错误码 | Agent 会话、消息相关错误 |
| `common.enums` | `AgentTagType.java` | Agent 标签类型枚举 | 区分不同职能的 Agent |
| `common.enums` | `ExpressionType.java` | 表情类型枚举 | 神态分析相关 |

### 3.3 数据库基础（database）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `common.database` | `BaseDO.java` | 数据对象基类 | 统一 id、createTime、updateTime、deleted 字段 |
| `common.database` | `BaseMessage.java` | 消息基类 | MongoDB 消息文档基类 |

### 3.4 配置类（config）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `common.config.database` | `DataBaseConfiguration.java` | 数据库配置总入口 | MyBatis-Plus 配置 |
| `common.config.database` | `MyMetaObjectHandler.java` | MyBatis-Plus 自动填充处理器 | 自动填充 createTime、updateTime |
| `common.config.database` | `MongoConfig.java` | MongoDB 配置 | 文档数据库连接配置 |
| `common.config.redis` | `RedissonConfig.java` | Redisson 分布式锁配置 | 基于 Redisson 的分布式锁、缓存 |
| `common.config.redis` | `RBloomFilterConfiguration.java` | Redis 布隆过滤器配置 | 防止缓存穿透 |
| `common.config.thread` | `ThreadPoolConfig.java` | 线程池配置 | 多级线程池隔离（通用、AI IO、CPU、查询） |
| `common.config.thread` | `ApplicationThreadPoolProperties.java` | 线程池属性配置 | 可配置的线程池参数 |
| `common.config.storage` | `ApplicationStorageProperties.java` | 存储路径配置 | 日志、临时文件、音频存储路径 |
| `common.config.storage` | `ApplicationStorageInitializer.java` | 存储目录初始化 | 启动时自动创建必要目录 |
| `common.config.sse` | `SseConfig.java` | SSE 流式输出配置 | Server-Sent Events 配置 |
| `common.config.web` | `WebConfig.java` | Web 配置 | CORS、拦截器等 |
| `common.config` | `WebSocketConfig.java` | WebSocket 配置 | 实时通信端点注册 |
| `common.config.xunfei` | `XunfeiLatProperties.java` | 讯飞语音 SDK 配置 | AppID、API Key、Secret |
| `common.config.xunfei` | `XunfeiSecretStartupValidator.java` | 讯飞密钥启动校验 | 启动时校验配置完整性 |
| `common.config.user` | `UserConfiguration.java` | 用户模块配置 | 用户相关 Bean 配置 |
| `common.config.user` | `UserFlowRiskControlConfiguration.java` | 用户流控配置 | 基于 Redis 的滑动窗口限流 |
| `common.config.satoken` | `StpInterfaceImpl.java` | Sa-Token 权限数据加载接口 | 实现用户权限动态加载 |

### 3.5 工具类（util & toolkit）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `common.util` | `FileUploadUtil.java` | 文件上传工具 | 统一文件上传处理 |
| `toolkit` | `SnowflakeIdGenerator.java` | 雪花算法 ID 生成器 | 分布式唯一 ID 生成 |
| `toolkit` | `RandomGenerator.java` | 随机数生成器 | 验证码、随机字符串 |
| `toolkit` | `EasyExcelWebUtil.java` | EasyExcel Web 工具 | Excel 导入导出 |
| `toolkit` | `AiManager.java` | AI 调用管理器 | 统一 AI 调用入口 |
| `toolkit` | `Threads.java` | 线程工具类 | 线程池辅助方法 |
| `toolkit.xunfei` | `XingChenAIClient.java` | 讯飞星火大模型客户端 | 对接讯飞星火 API |
| `toolkit.xunfei` | `SparkIatUtil.java` | 讯飞语音识别工具 | ASR 语音转文字 |
| `toolkit.xunfei` | `SparkIatService.java` | 讯飞语音识别服务 | 语音识别封装 |
| `toolkit.xunfei` | `AgentPropertiesLoader.java` | Agent 属性加载器 | 从数据库加载 Agent 配置 |
| `toolkit.xunfei` | `AIContentAccumulator.java` | AI 内容累加器 | 流式输出内容拼接 |
| `toolkit.xunfei` | `RoleContent.java` | 角色内容封装 | 对话角色 + 内容 |
| `toolkit.mediatools` | `MicrophoneRecorderUtil.java` | 麦克风录音工具 | 本地录音功能 |
| `toolkit.mediatools` | `AudioPlayer.java` | 音频播放器 | 本地音频播放 |

### 3.6 业务组件（biz）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `common.biz.message` | `MessageSequenceAllocator.java` | 消息序列号分配器 | 保证消息顺序性 |
| `common.biz.user` | `UserFlowRiskControlFilter.java` | 用户流控过滤器 | 基于 Redis 滑动窗口的请求限流 |

### 3.7 切面与注解（aspect & annotation）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `common.annotation` | `PreventDuplicateSubmit.java` | 防重复提交注解 | 标记需要防重的接口 |
| `common.aspect` | `PreventDuplicateSubmitAspect.java` | 防重复提交切面 | 基于 Redis 分布式锁实现 |

### 3.8 限流组件（ratelimit）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `common.ratelimit` | `RequestRateLimitService.java` | 请求限流服务接口 | 定义限流策略 |
| `common.ratelimit` | `RedissonRequestRateLimitService.java` | 基于 Redisson 的限流实现 | 分布式限流 |
| `common.ratelimit` | `RequestRateLimitPolicy.java` | 限流策略 | 限流参数配置 |
| `common.ratelimit` | `RequestRateLimitKeyResolver.java` | 限流 Key 解析器 | 根据用户/IP 生成限流 Key |

### 3.9 序列化（serialize）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `common.serialize` | `PhoneDesensitizationSerializer.java` | 手机号脱敏序列化器 | Jackson 自定义序列化，中间 4 位替换为 * |

### 3.10 全局异常处理（web）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `common.web` | `GlobalExceptionHandler.java` | 全局异常处理器 | `@RestControllerAdvice` 统一异常响应 |

### 3.11 常量定义（constant）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `common.constant` | `RedisCacheConstant.java` | Redis 缓存常量 | 统一缓存 Key 前缀定义 |

---

## 四、认证授权模块（auth）

### 4.1 领域层（domain）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `auth.domain` | `CurrentPrincipal.java` | 当前主体（用户） | 封装登录用户信息 |

### 4.2 应用层（application）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `auth.application` | `CurrentUserService.java` | 当前用户服务接口 | 获取当前登录用户 |
| `auth.application` | `LoginSessionService.java` | 登录会话服务接口 | 登录/登出管理 |
| `auth.application` | `PermissionService.java` | 权限服务接口 | 权限校验 |
| `auth.application` | `WebSocketAuthService.java` | WebSocket 认证服务 | WebSocket 连接鉴权 |

### 4.3 基础设施层（infrastructure）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `auth.infrastructure.satoken` | `SaTokenLoginSessionService.java` | 基于 Sa-Token 的登录会话实现 | 集成 Sa-Token 会话管理 |
| `auth.infrastructure.satoken` | `SaTokenCurrentUserService.java` | 基于 Sa-Token 的当前用户实现 | 从 Token 解析用户信息 |
| `auth.infrastructure.satoken` | `SaTokenPermissionService.java` | 基于 Sa-Token 的权限实现 | 权限校验逻辑 |
| `auth.infrastructure.web` | `AuthWebConfig.java` | 认证 Web 配置 | 拦截器注册 |
| `auth.infrastructure.web` | `SaTokenAuthInterceptorConfig.java` | Sa-Token 拦截器配置 | 路由拦截规则 |
| `auth.infrastructure.web` | `CurrentUserArgumentResolver.java` | 当前用户参数解析器 | 自动注入 `@CurrentUser` 参数 |
| `auth.infrastructure.websocket` | `SaTokenWebSocketAuthService.java` | WebSocket 认证实现 | WebSocket 握手鉴权 |

---

## 五、用户管理模块（user）

### 5.1 API 层（api）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `user.api` | `UserController.java` | 用户控制器 | 注册、登录、个人信息接口 |
| `user.api.io.req` | `UserRegisterReqDTO.java` | 用户注册请求 DTO | 注册参数封装 |
| `user.api.io.req` | `UserLoginReqDTO.java` | 用户登录请求 DTO | 登录参数封装 |
| `user.api.io.req` | `UserUpdateReqDTO.java` | 用户更新请求 DTO | 修改个人信息 |
| `user.api.io.req` | `UserPageReqDTO.java` | 用户分页查询请求 | 分页参数 |
| `user.api.io.req` | `UserMessageReqDTO.java` | 用户消息请求 DTO | 消息相关 |
| `user.api.io.req` | `AdminUserReqDTO.java` | 管理员用户请求 DTO | 管理端接口 |
| `user.api.io.resp` | `UserLoginRespDTO.java` | 登录响应 DTO | 返回 Token + 用户信息 |
| `user.api.io.resp` | `UserRespDTO.java` | 用户响应 DTO | 用户基本信息 |
| `user.api.io.resp` | `UserPageRespDTO.java` | 用户分页响应 DTO | 分页结果 |
| `user.api.io.resp` | `UserActualRespDTO.java` | 用户实际响应 DTO | 完整用户信息 |

### 5.2 服务层（service）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `user.service` | `UserService.java` | 用户服务接口 | 用户业务逻辑定义 |
| `user.service.impl` | `UserServiceImpl.java` | 用户服务实现 | 注册、登录、查询实现 |
| `user.service` | `AdminPermissionService.java` | 管理员权限服务接口 | 权限管理 |
| `user.service.impl` | `AdminPermissionServiceImpl.java` | 管理员权限服务实现 | 权限 CRUD |

### 5.3 数据层（dao）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `user.dao.entity` | `UserDO.java` | 用户数据对象 | MySQL 用户表映射 |
| `user.dao.entity` | `AdminPermission.java` | 管理员权限实体 | 权限表映射 |
| `user.dao.mapper` | `UserMapper.java` | 用户 Mapper | MyBatis-Plus Mapper 接口 |
| `user.dao.mapper` | `AdminPermissionMapper.java` | 权限 Mapper | 权限表操作 |

---

## 六、AI 对话模块（ai）

### 6.1 API 层（api）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `ai.api` | `AiController.java` | AI 对话控制器 | 创建会话、发送消息、流式输出 |
| `ai.api.io.resp` | `AiSessionCreateRespDTO.java` | AI 会话创建响应 | 返回会话 ID |

### 6.2 服务层（service）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `ai.service` | `AiConversationService.java` | AI 会话服务接口 | 会话生命周期管理 |
| `ai.service.impl` | `AiConversationServiceImpl.java` | AI 会话服务实现 | 会话创建、消息处理 |
| `ai.service` | `AiMessageService.java` | AI 消息服务接口 | 消息持久化 |
| `ai.service.impl` | `AiMessageServiceImpl.java` | AI 消息服务实现 | MongoDB 消息存储 |
| `ai.service` | `AiPropertiesService.java` | AI 属性服务接口 | 模型配置管理 |
| `ai.service.impl` | `AiPropertiesServiceImpl.java` | AI 属性服务实现 | 多模型配置加载 |
| `ai.service.chat` | `AiChatHandler.java` | AI 聊天处理器接口 | 定义聊天处理规范 |
| `ai.service.chat` | `AiChatHandlerFactory.java` | AI 聊天处理器工厂 | 根据模型类型路由到不同处理器 |
| `ai.service.chat` | `UniversalAiChatHandler.java` | 通用 AI 聊天处理器 | 统一处理 OpenAI 兼容协议 |

### 6.3 枚举（enums）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `ai.enums` | `AiPropritiesType.java` | AI 属性类型枚举 | 区分不同 AI 模型提供商 |

### 6.4 数据层（dao）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `ai.dao.entity` | `AiConversation.java` | AI 会话实体 | MongoDB 会话文档 |
| `ai.dao.entity` | `AiMessage.java` | AI 消息实体 | MongoDB 消息文档 |
| `ai.dao.entity` | `AiPropertiesDO.java` | AI 属性数据对象 | MySQL 模型配置表 |
| `ai.dao.mapper` | `AiPropertiesMapper.java` | AI 属性 Mapper | 模型配置表操作 |
| `ai.dao.repository` | `AiConversationRepository.java` | AI 会话仓库 | MongoDB 会话操作封装 |
| `ai.dao.repository` | `AiMessageRepository.java` | AI 消息仓库 | MongoDB 消息操作封装 |

### 6.5 基础设施层（infrastructure）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `ai.infrastructure.persistence` | `AiMessageMongoPersistencePort.java` | AI 消息 MongoDB 持久化端口 | 消息存储适配 |

---

## 七、会话管理模块（conversation）

### 7.1 应用层（application）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `conversation.application` | `ConversationStreamingSupport.java` | 会话流式输出支持 | SSE 流式推送封装 |
| `conversation.application` | `ConversationMessageHistoryService.java` | 会话消息历史服务 | 历史消息查询 |
| `conversation.application` | `ConversationMessagePersistenceService.java` | 会话消息持久化服务 | 消息异步落库 |
| `conversation.application` | `ConversationOwnershipService.java` | 会话归属权服务 | 校验会话归属 |
| `conversation.application` | `MessageSequenceService.java` | 消息序列号服务 | 保证消息顺序 |
| `conversation.application.port` | `AiMessagePersistencePort.java` | AI 消息持久化端口 | 消息存储抽象 |
| `conversation.application.port` | `AgentMessagePersistencePort.java` | Agent 消息持久化端口 | Agent 消息存储抽象 |

---

## 八、面试核心模块（interview）⭐️

这是项目的核心业务模块，实现了完整的 AI 面试流程。

### 8.1 API 层（api）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `interview.api` | `InterviewSessionController.java` | 面试会话控制器 | 创建会话、答题、查询记录 |
| `interview.api.io.req` | `InterviewAnswerReqDTO.java` | 面试答题请求 DTO | 答案提交参数 |
| `interview.api.io.req` | `InterviewQuestionReqDTO.java` | 面试出题请求 DTO | 出题参数 |
| `interview.api.io.req` | `DemeanorEvaluationReqDTO.java` | 神态评估请求 DTO | 照片评估参数 |
| `interview.api.io.req` | `InterviewConversationPageReqDTO.java` | 面试会话分页请求 | 分页参数 |
| `interview.api.io.resp` | `InterviewSessionCreateRespDTO.java` | 面试会话创建响应 | 返回会话 ID |
| `interview.api.io.resp` | `InterviewAnswerRespDTO.java` | 面试答题响应 | 评分 + 下一题 |
| `interview.api.io.resp` | `InterviewQuestionRespDTO.java` | 面试题目响应 | 题目内容 |
| `interview.api.io.resp` | `InterviewConversationRespDTO.java` | 面试会话响应 | 会话信息 |
| `interview.api.io.resp` | `InterviewRecordRespDTO.java` | 面试记录响应 | 历史记录 |
| `interview.api.io.resp` | `InterviewSessionRestoreRespDTO.java` | 面试会话恢复响应 | 断点续面数据 |
| `interview.api.io.resp` | `RadarChartDTO.java` | 雷达图数据 DTO | 多维度评分可视化 |

### 8.2 应用层（application）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `interview.application` | `InterviewWorkflowService.java` | 面试工作流服务 | 编排整个面试流程 |
| `interview.application.runtime` | `InterviewSessionRuntimeSnapshotService.java` | 面试运行时快照服务 | 状态持久化 |
| `interview.application.runtime` | `InterviewSessionRuntimeRehydrateService.java` | 面试运行时恢复服务 | 断点续面 |
| `interview.application.runtime` | `InterviewRuntimeRehydrateScope.java` | 恢复范围枚举 | 控制恢复粒度 |

### 8.3 流程编排层（flow）⭐️⭐️⭐️

#### 8.3.1 会话管理（flow.session）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `interview.flow.session` | `InterviewSessionFacade.java` | 面试会话门面 | 统一对外接口，协调多个服务 |
| `interview.flow.session` | `InterviewAgentOrchestrationService.java` | Agent 编排服务 | 多 Agent 协作调度 |

**亮点：**
- **门面模式**：`InterviewSessionFacade` 作为统一入口，屏蔽内部复杂性
- **状态机驱动**：通过状态转换控制面试流程（DRAFT → READY → IN_PROGRESS → FINISHED）
- **断点续面**：支持从 Redis 热缓存或 MongoDB 冷存储恢复面试状态

#### 8.3.2 答题流水线（flow.answer）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `interview.flow.answer` | `InterviewAnswerPipeline.java` | 答题流水线 | 编排答题全流程 |
| `interview.flow.answer` | `InterviewAnswerIdempotencyService.java` | 答题幂等性服务 | 防止重复提交 |
| `interview.flow.answer` | `InterviewQuestionLockService.java` | 题目锁定服务 | 分布式锁防止并发答题 |
| `interview.flow.answer` | `InterviewEvaluationService.java` | 评分服务 | AI 答案评分 |
| `interview.flow.answer` | `InterviewFollowUpService.java` | 追问服务 | 智能追问逻辑 |
| `interview.flow.answer` | `InterviewTurnRepairService.java` | 轮次修复服务 | 异常轮次自动修复 |

**亮点：**
- **流水线模式**：`InterviewAnswerPipeline` 编排 幂等检查 → 加锁 → 评分 → 追问 → 推进
- **分布式锁**：基于 Redisson 的题目锁定，防止并发答题
- **幂等性保证**：基于答案哈希的去重，避免重复评分
- **自动修复**：异步定时任务修复异常的面试轮次

#### 8.3.3 出题流程（flow.extraction）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `interview.flow.extraction` | `InterviewQuestionExtractionService.java` | 面试题提取服务 | 简历解析 + AI 出题 |

**亮点：**
- **简历解析**：上传 PDF 简历，AI 自动解析提取关键信息
- **智能出题**：根据简历内容 + 面试方向生成定制化题目

#### 8.3.4 神态分析（flow.demeanor）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `interview.flow.demeanor` | `InterviewDemeanorService.java` | 神态分析服务 | 照片表情分析 |

**亮点：**
- **多模态 AI**：结合视觉模型分析候选人神态
- **实时反馈**：面试过程中实时评估专注度、自信度

#### 8.3.5 报告生成（flow.report）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `interview.flow.report` | `InterviewRecordServiceImpl.java` | 面试记录服务实现 | 面试结果落库 |
| `interview.flow.report` | `InterviewResumePreviewService.java` | 简历预览服务 | 简历文件访问 |

**亮点：**
- **雷达图生成**：多维度评分可视化（简历分、面试表现、神态评估、专业技能、潜力指数）
- **面试建议**：AI 生成个性化改进建议

### 8.4 服务层（service）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `interview.service` | `InterviewSessionService.java` | 面试会话服务接口 | 会话生命周期 |
| `interview.service.impl` | `InterviewSessionServiceImpl.java` | 面试会话服务实现 | 状态机转换 |
| `interview.service` | `InterviewQuestionService.java` | 面试题目服务接口 | 题目管理 |
| `interview.service.impl` | `InterviewQuestionServiceImpl.java` | 面试题目服务实现 | 题目 CRUD |
| `interview.service` | `InterviewQuestionCacheService.java` | 题目缓存服务接口 | 缓存加速 |
| `interview.service.impl` | `InterviewQuestionCacheServiceImpl.java` | 题目缓存服务实现 | Redis 缓存 |
| `interview.service` | `InterviewScoreService.java` | 评分服务接口 | 评分逻辑 |
| `interview.service.impl` | `InterviewScoreServiceImpl.java` | 评分服务实现 | 分数计算 |
| `interview.service` | `InterviewRadarService.java` | 雷达图服务接口 | 雷达图数据 |
| `interview.service.impl` | `InterviewRadarServiceImpl.java` | 雷达图服务实现 | 多维度评分 |
| `interview.service` | `InterviewRecordService.java` | 面试记录服务接口 | 记录管理 |

### 8.5 服务模型（service.model）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `interview.service.model` | `InterviewSessionStatus.java` | 面试会话状态枚举 | 状态机定义 |
| `interview.service.model` | `InterviewFlowState.java` | 面试流程状态 | 流程节点 |
| `interview.service.model` | `InterviewTurnLog.java` | 面试轮次日志 | 每轮答题记录 |
| `interview.service.model` | `InterviewRuntimeLoadMode.java` | 运行时加载模式 | READ_ONLY / READ_WRITE |
| `interview.service.model` | `InterviewRuntimeConfidence.java` | 运行时置信度 | 数据完整性评估 |
| `interview.service.model` | `InterviewRuntimeScoreAggregate.java` | 运行时分数聚合 | 多维度分数汇总 |

**亮点：**
- **状态机模式**：`InterviewSessionStatus` 定义完整的状态转换规则
- **运行时快照**：通过 `InterviewRuntimeLoadMode` 控制恢复粒度

### 8.6 缓存层（service.cache）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `interview.service.cache` | `InterviewCacheStore.java` | 面试缓存存储接口 | 缓存抽象 |
| `interview.service.cache` | `RedisInterviewCacheStore.java` | Redis 缓存实现 | 热数据存储 |
| `interview.service.cache` | `InterviewCacheKeys.java` | 缓存 Key 定义 | 统一 Key 规范 |

**亮点：**
- **多级存储**：Redis（热数据） + MongoDB（冷数据） + MySQL（持久化）
- **缓存 Key 规范**：统一前缀 + 会话 ID + 数据类型

### 8.7 共享组件（shared）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `interview.shared` | `InterviewAiInvoker.java` | 面试 AI 调用器 | 统一 AI 调用入口 |
| `interview.shared` | `InterviewResponseParser.java` | 面试响应解析器 | AI 输出结构化 |
| `interview.shared` | `InterviewJsonValueNormalizer.java` | JSON 值规范化 | 数据清洗 |

### 8.8 数据层（dao）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `interview.dao.entity` | `InterviewSession.java` | 面试会话实体 | MySQL 会话表 |
| `interview.dao.entity` | `InterviewQuestion.java` | 面试题目实体 | MySQL 题目表 |
| `interview.dao.entity` | `InterviewRecordDO.java` | 面试记录数据对象 | MySQL 记录表 |
| `interview.dao.entity` | `InterviewSessionRuntimeSnapshot.java` | 运行时快照 | MongoDB 快照文档 |
| `interview.dao.entity` | `InterviewSessionRuntimeHotSnapshot.java` | 热快照 | Redis 热数据 |
| `interview.dao.entity` | `InterviewSessionRuntimeColdSnapshot.java` | 冷快照 | MongoDB 冷数据 |
| `interview.dao.entity` | `InterviewSessionTurnArchive.java` | 轮次归档 | 历史轮次存档 |
| `interview.dao.mapper` | `InterviewRecordMapper.java` | 面试记录 Mapper | MyBatis-Plus |
| `interview.dao.repository` | `InterviewSessionRepository.java` | 会话仓库 | 会话 CRUD |
| `interview.dao.repository` | `InterviewQuestionRepository.java` | 题目仓库 | 题目 CRUD |
| `interview.dao.repository` | `InterviewSessionRuntimeSnapshotRepository.java` | 快照仓库接口 | 快照操作抽象 |
| `interview.dao.repository` | `InterviewSessionRuntimeHotSnapshotRepository.java` | 热快照仓库 | Redis 操作 |
| `interview.dao.repository` | `InterviewSessionRuntimeColdSnapshotRepository.java` | 冷快照仓库 | MongoDB 操作 |
| `interview.dao.repository` | `InterviewSessionTurnArchiveRepository.java` | 轮次归档仓库 | 历史数据 |

**亮点：**
- **冷热分离**：热快照（Redis） + 冷快照（MongoDB），平衡性能与成本
- **Repository 模式**：封装数据访问细节，提供统一操作接口

---

## 九、RAG 知识库模块（knowledge）⭐️⭐️

这是项目的另一个核心亮点，实现了完整的 RAG（检索增强生成）流程。

### 9.1 API 层（api）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `knowledge.api` | `KnowledgeBaseController.java` | 知识库控制器 | 创建知识库、上传文档 |
| `knowledge.api` | `KnowledgeChatController.java` | 知识库对话控制器 | RAG 对话接口 |
| `knowledge.api.io.req` | `KnowledgeBaseCreateReqDTO.java` | 知识库创建请求 | 知识库参数 |
| `knowledge.api.io.req` | `KnowledgeChatReqDTO.java` | 知识库对话请求 | 对话参数 |
| `knowledge.api.io.resp` | `KnowledgeBaseRespDTO.java` | 知识库响应 | 知识库信息 |
| `knowledge.api.io.resp` | `KnowledgeDocumentRespDTO.java` | 文档响应 | 文档信息 |

### 9.2 流程编排层（flow）⭐️⭐️⭐️

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `knowledge.flow` | `RagContext.java` | RAG 上下文 | LiteFlow 流程上下文 |
| `knowledge.flow` | `RagRetrievalNode.java` | RAG 检索节点 | 混合检索 |
| `knowledge.flow` | `WebSearchNode.java` | 联网搜索节点 | DuckDuckGo 降级搜索 |
| `knowledge.flow` | `ContextCompressionNode.java` | 上下文压缩节点 | Prompt 压缩 |

**亮点：**
- **LiteFlow 规则链编排**：检索 → 联网搜索降级 → 上下文压缩，三个节点顺序执行
- **热更新**：规则链配置外置，支持热更新无需重启
- **降级策略**：知识库检索不到时自动降级到联网搜索

### 9.3 服务层（service）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `knowledge.service` | `KnowledgeBaseService.java` | 知识库服务接口 | 知识库管理 |
| `knowledge.service.impl` | `KnowledgeBaseServiceImpl.java` | 知识库服务实现 | 知识库 CRUD |
| `knowledge.service` | `DocumentEtlPipeline.java` | 文档 ETL 管道 | 文档解析 + 分段 + Embedding |
| `knowledge.service` | `EmbeddingService.java` | Embedding 服务 | 文本向量化 |
| `knowledge.service` | `HybridSearchService.java` | 混合检索服务 | BM25 + 向量语义检索 |
| `knowledge.service` | `ElasticsearchVectorStore.java` | ES 向量存储 | 向量数据库操作 |
| `knowledge.service` | `RagChatService.java` | RAG 对话服务 | 检索增强生成 |

**亮点：**
- **端到端 RAG Pipeline**：文档解析 → 智能分段 → Embedding → ES 存储 → 混合检索 → Prompt 增强 → 流式输出
- **混合检索策略**：BM25 关键词检索 + kNN 向量语义检索
- **RRF 融合排序**：Reciprocal Rank Fusion 融合两路检索结果
- **Cosine 重排序**：基于余弦相似度的二次排序
- **智能分段**：段落边界 + 句末检测 + 重叠滑动窗口
- **异步处理**：文档 ETL 异步执行，不阻塞主流程
- **源引用追溯**：回答中标注引用来源，可追溯到具体文档段落

### 9.4 配置层（config）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `knowledge.config` | `ElasticsearchConfig.java` | Elasticsearch 配置 | ES 连接配置 |

### 9.5 数据层（dao）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `knowledge.dao.entity` | `KnowledgeBaseDO.java` | 知识库数据对象 | MySQL 知识库表 |
| `knowledge.dao.entity` | `KnowledgeDocument.java` | 知识库文档实体 | MySQL 文档表 |
| `knowledge.dao.mapper` | `KnowledgeBaseMapper.java` | 知识库 Mapper | MyBatis-Plus |
| `knowledge.dao.repository` | `KnowledgeDocumentRepository.java` | 文档仓库 | 文档操作 |

---

## 十、媒体交互模块（media）

### 10.1 API 层（api）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `media.api` | `WebSocketController.java` | WebSocket 控制器 | 实时语音交互 |
| `media.api` | `XunfeiTtsController.java` | 讯飞 TTS 控制器 | 文本转语音 |
| `media.api.io.req` | `LongTextTtsReqDTO.java` | 长文本 TTS 请求 | 长文本参数 |
| `media.api.io.resp` | `LongTextTtsTaskRespDTO.java` | 长文本 TTS 响应 | 任务状态 |

### 10.2 应用层（application）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `media.application` | `WebSocketMessageService.java` | WebSocket 消息服务 | 实时消息推送 |

### 10.3 基础设施层（infrastructure）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `media.infrastructure.websocket` | `AudioTranscriptionWebSocketHandler.java` | 语音转写 WebSocket 处理器 | 实时 ASR |
| `media.infrastructure.integration` | `AudioTranscriptionService.java` | 语音转写服务 | 讯飞 ASR 集成 |
| `media.infrastructure.integration` | `XunfeiAudioService.java` | 讯飞音频服务 | 音频处理 |
| `media.infrastructure.integration` | `XunfeiLongTextTtsService.java` | 讯飞长文本 TTS 服务 | 长文本语音合成 |

**亮点：**
- **实时语音交互**：WebSocket 双向通信，支持实时 ASR + TTS
- **讯飞 SDK 集成**：语音识别、语音合成、人脸检测
- **长文本 TTS**：支持长文本分片合成，异步任务管理

---

## 十一、Agent 管理模块（agent）

### 11.1 API 层（api）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `agent.api` | `AgentController.java` | Agent 控制器 | Agent 列表、会话管理 |

### 11.2 应用层（application）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `agent.application` | `AgentSessionService.java` | Agent 会话服务 | 会话生命周期 |

### 11.3 数据层（dao）

| 包名 | 文件名 | 作用 | 亮点 |
|------|--------|------|------|
| `agent.dao.entity` | `AgentPropertiesDO.java` | Agent 属性数据对象 | Agent 配置表 |

---

## 十二、项目核心亮点总结

### 12.1 架构亮点

1. **DDD 分层架构**：api → application → service → dao，职责清晰
2. **多数据源协同**：MySQL（结构化数据） + MongoDB（会话/消息） + Redis（缓存/锁） + ES（向量检索）
3. **状态机驱动**：面试流程通过状态机控制，支持断点续面
4. **规则引擎编排**：LiteFlow 实现面试追问链 + RAG 检索链，业务规则可视化

### 12.2 业务亮点

1. **多 Agent 协作**：出题官、评分官、追问官、神态分析官分工协作
2. **端到端 RAG**：文档 ETL → 混合检索 → Prompt 增强 → 流式输出
3. **实时语音交互**：WebSocket + 讯飞 SDK，支持实时 ASR/TTS
4. **面试全流程管理**：简历解析 → 智能出题 → 实时评分 → 智能追问 → 雷达图分析

### 12.3 技术亮点

1. **Spring AI 1.0.0 深度集成**：统一接入多种大模型（通义千问 / DeepSeek / 星火）
2. **分布式 SingleFlight**：基于 Redis 的请求合并，降低 AI 调用成本
3. **多级线程池隔离**：通用、AI IO、CPU、查询线程池分离
4. **AI 运行防护**：流控限流 + 熔断降级（Resilience4j） + SingleFlight
5. **混合检索策略**：BM25 + kNN 向量检索 + RRF 融合排序 + Cosine 重排序
6. **冷热数据分离**：Redis 热缓存 + MongoDB 冷存储 + MySQL 持久化

### 12.4 工程亮点

1. **统一异常体系**：`AbstractException` + 错误码枚举
2. **统一响应封装**：`Result<T>` 泛型设计
3. **防重复提交**：基于 Redis 分布式锁的幂等性保证
4. **全局流控**：基于 Redis 滑动窗口的请求限流
5. **代码规范**：Spotless 代码格式化 + 统一命名规范

---

## 十三、推荐阅读路径

### 路径一：快速了解项目（30 分钟）

1. `README.md` - 项目概览
2. `XunZhiAdminApplication.java` - 启动入口
3. `application.yaml` - 核心配置
4. `InterviewSessionFacade.java` - 面试门面（理解核心流程）
5. `HybridSearchService.java` - RAG 混合检索（理解技术亮点）

### 路径二：深入面试模块（2 小时）

1. `InterviewSessionController.java` - API 入口
2. `InterviewSessionFacade.java` - 门面协调
3. `InterviewWorkflowService.java` - 工作流编排
4. `InterviewAnswerPipeline.java` - 答题流水线
5. `InterviewSessionStatus.java` - 状态机
6. `InterviewEvaluationService.java` - 评分逻辑
7. `InterviewFollowUpService.java` - 追问逻辑

### 路径三：深入 RAG 模块（1.5 小时）

1. `KnowledgeBaseController.java` - API 入口
2. `DocumentEtlPipeline.java` - 文档 ETL
3. `HybridSearchService.java` - 混合检索
4. `ElasticsearchVectorStore.java` - 向量存储
5. `RagRetrievalNode.java` - LiteFlow 检索节点
6. `RagChatService.java` - RAG 对话

### 路径四：理解基础设施（1 小时）

1. `common.convention` - 约定规范
2. `common.config` - 配置类
3. `auth.infrastructure` - 认证授权
4. `common.ratelimit` - 限流组件
5. `common.aspect` - 切面组件

---

## 十四、关键设计模式

| 设计模式 | 应用场景 | 示例文件 |
|---------|---------|---------|
| **门面模式** | 面试会话统一入口 | `InterviewSessionFacade.java` |
| **工厂模式** | AI 聊天处理器路由 | `AiChatHandlerFactory.java` |
| **策略模式** | 限流策略、检索策略 | `RequestRateLimitPolicy.java` |
| **状态机模式** | 面试流程控制 | `InterviewSessionStatus.java` |
| **流水线模式** | 答题流程编排 | `InterviewAnswerPipeline.java` |
| **观察者模式** | WebSocket 消息推送 | `WebSocketMessageService.java` |
| **Repository 模式** | 数据访问封装 | `InterviewSessionRepository.java` |
| **端口-适配器模式** | 持久化抽象 | `AiMessagePersistencePort.java` |
| **规则引擎模式** | 业务流程编排 | `RagRetrievalNode.java` (LiteFlow) |

---

## 十五、技术栈速查

| 技术 | 版本 | 用途 | 关键文件 |
|------|------|------|---------|
| Spring Boot | 3.4.4 | 核心框架 | `pom.xml` |
| Spring AI | 1.0.0 | AI 模型接入 | `application.yaml` |
| LiteFlow | 2.15.3.2 | 规则引擎 | `interview-followup-rule.yaml` |
| MyBatis-Plus | 3.5.9 | ORM 框架 | `DataBaseConfiguration.java` |
| Redisson | 3.27.2 | 分布式锁/缓存 | `RedissonConfig.java` |
| Sa-Token | 1.39.0 | 认证授权 | `StpInterfaceImpl.java` |
| Elasticsearch | 9.2.1 | 向量检索 | `ElasticsearchConfig.java` |
| Resilience4j | 2.2.0 | 熔断降级 | `application.yaml` |
| 讯飞 WebSDK | - | 语音能力 | `XunfeiLatProperties.java` |

---

**文档生成时间**：2026-07-04  
**项目版本**：1.0-SNAPSHOT  
**建议阅读顺序**：启动入口 → 基础设施 → 认证授权 → 用户管理 → AI 对话 → 面试核心 → RAG 知识库 → 媒体交互
