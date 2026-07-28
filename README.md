# （ MockPilot Agent）

> 基于 **Spring AI + 多 Agent 协作** 的 AI 技术面试模拟平台，支持智能出题、实时评分、追问引导、神态分析、RAG 知识库对话等完整面试场景。

---

## 项目简介

**MockPilot**是一个面向技术面试场景的 AI 智能助手系统。候选人上传简历后，系统通过多个 AI Agent 协作完成面试出题、答案评估、智能追问、神态分析等全流程；同时支持基于 RAG（检索增强生成）的知识库对话，让 AI 在面试中基于候选人提供的技术文档进行精准提问。

系统后端基于 **Spring Boot 3.4 + Spring AI 1.0.0** 构建，采用多 Agent 协作架构，结合 **LiteFlow 规则引擎** 编排面试流程，通过 **SSE 流式输出** 实现逐 token 级的实时回复，支持 OpenAI / DeepSeek / 讯飞星火 / 通义千问等多种大模型接入。

---

## 核心功能

| 功能模块 | 说明 |
|---------|------|
| **AI 智能面试** | 候选人上传简历 → AI 自动解析简历 → 生成面试题 → 逐题作答 → 实时评分 → 智能追问 → 面试复盘 |
| **多 Agent 协作** | 面试出题官、答案评分官、追问引导官、神态分析官等多个 Agent 分工协作，各司其职 |
| **RAG 知识库对话** | 用户上传技术文档构建私有知识库，面试时 AI 基于知识库内容进行精准提问，支持 BM25 + 向量语义混合检索 |
| **通用 AI 对话** | 支持多模型（DeepSeek / 星火 / 通义千问等）统一接入，SSE 流式输出 + 思维链展示 |
| **实时语音交互** | 集成讯飞语音 SDK，支持实时 ASR 语音识别、TTS 文本转语音、WebSocket 实时推送 |
| **面试全流程管理** | 面试状态机、会话恢复、中断续面、面试结果雷达图分析、面试建议生成 |
| **AI 运行防护** | 流控限流、熔断降级（Resilience4j）、SingleFlight 请求合并、多级线程池隔离 |

---

## 技术架构

### 技术栈

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 语言 | Java | 17 | 项目基础语言 |
| 框架 | Spring Boot | 3.4.4 | 核心应用框架 |
| AI 框架 | Spring AI | 1.0.0 | 大模型统一接入（OpenAI 兼容协议） |
| 规则引擎 | LiteFlow | 2.15.3.2 | 面试流程编排 & RAG 链条编排 |
| ORM | MyBatis-Plus | 3.5.9 | 关系型数据持久化 |
| 文档数据库 | MongoDB | 7.0 | 会话消息、文档元数据持久化 |
| 缓存 | Redis + Redisson | 7.2 | 分布式缓存、分布式锁、会话管理 |
| 向量数据库 | ElasticSearch | 9.2.1 | 向量存储 + BM25 混合检索（RAG 专用） |
| 关系型数据库 | MySQL | 8.4 | 用户、面试记录、知识库等结构化数据 |
| 认证 | Sa-Token | 1.39.0 | 登录认证、权限控制 |
| 语音能力 | 讯飞 WebSDK | - | ASR 语音识别、TTS 语音合成、人脸检测 |
| 容错 | Resilience4j | 2.2.0 | 熔断、重试、限流、超时控制 |
| 实时通信 | WebSocket + SSE | - | 实时消息推送、流式 AI 回复 |
| 爬虫 | WebMagic + Jsoup | 1.0.3 | 网页解析、联网搜索 |
| 容器化 | Docker + Docker Compose | - | 多阶段构建 & 一键部署 |

### 模块结构

```
AI-Meeting-main/
├── admin/                          # 后端服务（单体应用）
│   └── src/main/java/com/hewei/hzyjy/xunzhi/
│       ├── agent/                  # Agent 管理模块（智能体配置、会话、消息、文件上传）
│       ├── ai/                     # AI 对话模块（多模型接入、通用聊天、SSE 流式）
│       ├── auth/                   # 认证授权模块（Sa-Token 集成、WebSocket 鉴权）
│       ├── common/                 # 公共模块（配置、工具类、防重复提交、流控）
│       ├── interview/              # 面试核心模块（出题、评分、追问、神态、状态机、复盘）
│       ├── knowledge/              # RAG 知识库模块（文档 ETL、混合检索、智能对话）
│       ├── media/                  # 媒体交互模块（WebSocket、TTS、实时 ASR）
│       └── user/                   # 用户管理模块（注册、登录、个人信息）
├── skills/                         # Agent Skill 定义（开发辅助）
├── docs/assets/                    # 项目文档截图与演示素材
├── Dockerfile                      # 多阶段 Docker 构建文件
└── pom.xml                         # Maven 父 POM（统一依赖管理）
```

> 仓库根目录另含全栈部署编排：`docker-compose.yml`、`.env.example`、`start.sh` / `start.bat`、`stop.sh` / `stop.bat`、`milvus/`。

### 数据流架构

```
┌─────────────────────────────────────────────────────────────┐
│                      前端 (Vite + React)                      │
│    简历上传 / 面试对话 / 知识库管理 / 实时语音                    │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP / SSE / WebSocket
┌──────────────────────────────▼──────────────────────────────┐
│                     Controller 层                             │
│  InterviewSessionController │ AgentController │ AiController  │
│  KnowledgeBaseController   │ UserController  │ MediaController│
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                     Service 层                                │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  面试流程：LiteFlow 状态机 + Agent 协作 + 评分追问策略    │  │
│  │  AI 对话：AiChatHandlerFactory + SSE 流式输出           │  │
│  │  RAG 检索：LiteFlow 链编排 + ES 混合检索 + Prompt 增强  │  │
│  │  防护体系：限流 + 熔断 + SingleFlight + 线程池隔离      │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                      数据存储层                                │
│  MySQL (结构化数据)  │  MongoDB (会话/消息)  │  Redis (缓存/锁) │
│  ElasticSearch (向量检索/知识库)                                │
└─────────────────────────────────────────────────────────────┘
```

---

## 快速开始

### 环境要求

- **JDK** 17+
- **Maven** 3.6.3+
- **MySQL** 8.x
- **MongoDB** 7.x
- **Redis** 7.x
- **ElasticSearch** 9.x（RAG 知识库功能需要）

### 本地开发

#### 1. 克隆项目

```bash
(https://github.com/Ember452/MockPilot-mian.git)
cd MockPilot-main
```

#### 2. 初始化数据库

在 MySQL 中创建数据库并执行初始化脚本：

```sql
CREATE DATABASE mainshi_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

依次执行 `admin/src/main/resources/sql/` 目录下的 SQL 脚本：

- `table.sql` — 建表
- `admin_permission.sql` — 管理员权限初始数据
- `agent_properties.sql` — Agent 属性初始数据
- `ai_properties.sql` — AI 模型配置初始数据
- `t_user.sql` — 用户初始数据
- `knowledge_base.sql` — 知识库表（RAG 功能）

#### 3. 配置环境变量

复制 `.env.example` 为 `.env`，根据实际环境填写配置：

```bash
cp .env.example .env
```

主要配置项：

| 配置项 | 说明 | 示例 |
|--------|------|------|
| `MYSQL_HOST` | MySQL 地址 | `localhost` |
| `MYSQL_PORT` | MySQL 端口 | `3306` |
| `MYSQL_DATABASE` | 数据库名 | `mainshi_agent` |
| `MYSQL_USERNAME` | 数据库用户名 | `root` |
| `MYSQL_PASSWORD` | 数据库密码 | `123456` |
| `MONGODB_HOST` | MongoDB 地址 | `localhost` |
| `MONGODB_PORT` | MongoDB 端口 | `27017` |
| `MONGODB_DATABASE` | MongoDB 数据库名 | `xunzhi_agent` |
| `REDIS_HOST` | Redis 地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `SPRING_AI_OPENAI_API_KEY` | 大模型 API Key | `sk-xxx` |
| `SPRING_AI_OPENAI_BASE_URL` | 大模型 API 地址 | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| `SPRING_AI_OPENAI_MODEL` | 默认模型 | `qwen-plus` |
| `SPRING_AI_OPENAI_EMBEDDING_MODEL` | Embedding 模型 | `text-embedding-v4` |
| `ES_HOST` | ElasticSearch 地址 | `localhost` |
| `ES_PORT` | ElasticSearch 端口 | `9200` |
| `XUNFEI_APP_ID` | 讯飞 AppID | （语音功能需要） |
| `XUNFEI_API_KEY` | 讯飞 API Key | （语音功能需要） |
| `XUNFEI_API_SECRET` | 讯飞 API Secret | （语音功能需要） |

#### 4. 启动服务

```bash
# 编译打包（跳过测试）
./mvnw clean package -DskipTests

# 启动应用
java -jar admin/target/xunzhi-admin-*.jar
```

或直接通过 Maven 启动：

```bash
./mvnw spring-boot:run -pl admin
```

服务默认运行在 `http://localhost:8002`。

### Docker 一键部署（推荐，本仓库即部署入口）

只需克隆本仓库即可在任意装有 Docker 的机器（Linux / macOS / Windows）上启动**前端 + 后端 + 全部中间件**：

```bash
git clone https://github.com/Ember452/MockPilot-mian.git
cd MockPilot-mian

# 生成环境变量文件并填入自己的密钥（DashScope / 讯飞）
cp .env.example .env

# 一键启动（Linux/macOS；Windows 双击 start.bat）
./start.sh
```

启动完成后访问 `http://localhost`（前端），后端健康检查 `http://localhost:8002/actuator/health`。

常用命令：

```bash
docker compose ps              # 查看服务状态
docker compose logs -f backend # 查看后端日志
./stop.sh                      # 停止（数据卷保留）
```

Docker 部署的服务架构包括：

- **MySQL 8.0** — 关系型数据库，首次启动自动建库建表并灌入初始数据
- **MongoDB 7.0** — 文档数据库
- **Redis 7.2** — 缓存与分布式锁
- **ElasticSearch 9.2.1** — 向量检索引擎（RAG），可选切换 Milvus（`--profile milvus`）
- **后端服务** — Maven 多阶段构建，运行在 JRE 17 上
- **前端服务** — 默认直接以 GitHub 前端仓库（[MockPilot-Frontend](https://github.com/Ember452/MockPilot-Frontend)）为 Docker 构建上下文远程构建，Nginx 托管并反代 `/api`；本地开发前端时在 `.env` 中将 `FRONTEND_BUILD_CONTEXT` 改为本地前端目录即可

注意：Linux 下 Elasticsearch 需要 `vm.max_map_count >= 262144`（`start.sh` 会自动检测并提示）。

---

## 核心 API 概览

### 用户管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/xunzhi/v1/user/register` | 用户注册 |
| POST | `/api/xunzhi/v1/user/login` | 用户登录 |

### 面试管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/xunzhi/v1/interview/sessions` | 创建面试会话 |
| POST | `/api/xunzhi/v1/interview/sessions/{id}/answer` | 提交回答 |
| GET | `/api/xunzhi/v1/interview/records` | 获取面试记录列表 |
| POST | `/api/xunzhi/v1/interview/resume/upload` | 上传简历 |

### Agent 管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/xunzhi/v1/agents` | 获取 Agent 列表 |
| POST | `/api/xunzhi/v1/agents/sessions` | 创建 Agent 会话 |
| POST | `/api/xunzhi/v1/agents/sessions/{id}/chat` | Agent 对话（SSE） |

### AI 对话

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/xunzhi/v1/ai/conversations` | 获取 AI 会话列表 |
| POST | `/api/xunzhi/v1/ai/sessions` | 创建 AI 会话 |
| POST | `/api/xunzhi/v1/ai/sessions/{id}/messages` | 发送消息（SSE 流式） |
| GET | `/api/xunzhi/v1/ai/properties` | 获取 AI 模型配置 |

### RAG 知识库

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/xunzhi/v1/knowledge-bases` | 创建知识库 |
| GET | `/api/xunzhi/v1/knowledge-bases` | 获取知识库列表 |
| POST | `/api/xunzhi/v1/knowledge-bases/{id}/documents` | 上传文档（自动 ETL） |
| POST | `/api/xunzhi/v1/knowledge-chat/sessions/{id}/chat` | 知识库对话（SSE） |

### 媒体服务

| 方法 | 路径 | 说明 |
|------|------|------|
| WebSocket | `/ws/tts` | 实时 TTS 语音合成 |
| POST | `/api/xunzhi/v1/tts/convert` | 文本转语音 |

---

## RAG 知识库特性

RAG（检索增强生成）知识库模块是本项目的核心亮点，提供端到端的智能知识问答能力：

- **文档 ETL 管道**：支持 txt/md/pdf/doc/docx 格式文档上传，自动进行智能分段（段落边界 + 句末检测 + 重叠滑动窗口），异步处理不阻塞主流程
- **混合检索策略**：BM25 关键词检索 + kNN 向量语义检索，通过 RRF 融合排序 + Cosine 余弦重排序，提升检索精度
- **LiteFlow 规则链编排**：知识库检索 → 联网搜索降级（DuckDuckGo） → 上下文压缩，三个节点顺序执行，支持热更新
- **源引用追溯**：回答中标注引用来源段落，可追溯到具体文档
- **多知识库隔离**：每个知识库对应独立 ES 索引，数据完全隔离

---

## AI 运行防护体系

为保障系统在高并发场景下的稳定性，项目内置了完善的 AI 调用防护机制：

- **流控限流**：基于 Redis 的滑动窗口计数器，针对不同面试阶段（出题/评分/追问/神态分析）配置独立限流策略
- **熔断降级**：基于 Resilience4j 的熔断器，当 AI 调用失败率超过阈值时自动熔断，保护下游服务
- **SingleFlight 请求合并**：分布式请求去重，避免相同面试阶段的重复 AI 调用，支持主从协调模式
- **多级线程池隔离**：通用线程池、AI IO 线程池、CPU 密集型线程池、查询线程池分离，避免相互影响
- **会话恢复机制**：面试中断后可从 Redis 热缓存或 MongoDB 冷存储恢复面试状态

---

## 项目亮点

- **Spring AI 1.0.0 深度集成**：基于 OpenAI 兼容协议统一接入多种大模型（通义千问 / DeepSeek / 星火等），支持 SSE 流式输出与思维链展示
- **LiteFlow 多场景编排**：面试追问规则链 + RAG 检索链，业务规则可视化编排，支持热更新无需重启
- **端到端 RAG Pipeline**：文档解析 → 语义分块 → Embedding → ES 向量存储 → 混合检索 → Prompt 增强 → 流式输出
- **分布式 SingleFlight**：基于 Redis 的分布式请求合并，主从协调 + 心跳检测 + 结果共享，大幅降低 AI 调用成本
- **完整面试状态机**：INIT → IN_PROGRESS → FINISHED → EVALUATED，支持中断恢复、会话续面、结果复盘
- **多数据源协同**：MySQL（结构化数据） + MongoDB（会话消息） + Redis（缓存/锁） + ES（向量检索） 四库协同

---

## 开发指南

### 代码规范

- 项目使用 **Spotless** 进行代码格式化，提交前执行：

```bash
./mvnw spotless:apply
```

### 测试

```bash
# 运行所有测试
./mvnw test

# 运行验证（包含测试 + 格式检查）
./mvnw -B -ntp clean verify
```

### 分支策略

- `feature/<topic>` — 新功能开发
- `fix/<topic>` — Bug 修复
- `docs/<topic>` — 文档更新

### 提交规范

提交信息应直接说明修改意图，例如：
- `fix: normalize interview validation errors`
- `feat: add RAG hybrid search with RRF reranking`
- `docs: update API documentation for knowledge base`

---

## 贡献指南

欢迎参与贡献！提交 PR 前请注意：

1. 先阅读项目文档和 `skills/` 目录下的模块说明
2. 本地执行 `./mvnw -B -ntp clean verify` 确保通过
3. 新增功能需补充最小可验证测试
4. 不要提交真实密钥、临时音频、构建产物或 IDE 私有文件
5. PR 描述应包含：变更背景、主要实现点、兼容性说明、测试方式



---

## 许可证

本项目基于 [MIT License](LICENSE) 开源，欢迎使用和贡献。
