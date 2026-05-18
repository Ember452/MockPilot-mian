---
name: xunzhi-business-dictionary
description: AI-Meeting 业务对象词典 Skill。用于统一跨域名词、真相源、对象层级、生命周期和不变量；当需求涉及 `sessionId`、`requestId`、`questionNumber`、`agentName`、`loadMode`、`confidence` 等高频名词，或者在改接口、配置、工作流、快照、归档前需要先搞清对象真实语义时使用。
---

# xunzhi-business-dictionary

这个 Skill 不负责直接改某个具体业务域，它负责先把“你到底在改什么对象”说清楚。

## 使用顺序

- 先看 `references/object-dictionary.md`，统一对象层级和真相源。
- 再判断当前对象属于面试域、Agent 域、媒体域、鉴权域还是运行时域。
- 如果对象语义已经清楚，再切到对应领域 Skill 做具体修改。

## 这层主要解决什么

- `sessionId` 在不同域里是不是同一个东西。
- `requestId` 是业务主键，还是幂等边界。
- `questionNumber` 是数据库主键，还是业务题号。
- `loadMode`、`confidence` 是展示字段，还是运行时写入能力的正式语义。
- 某个对象到底是主记录、运行态、快照、归档，还是接口契约。

## 必守约束

- 先判定真相源，再改实现。
- 不要把运行态缓存当成最终业务事实。
- 不要把 DTO、工作流字段或视图对象误当成主记录。
- 不要让同一个名词在不同域里被默认当成同一个对象。

## 不要用我做什么

- 不要用我直接改面试状态机；那属于 `xunzhi-interview-domain`。
- 不要用我直接改通用 Agent 聊天；那属于 `xunzhi-agent-domain`。
- 不要用我直接改媒体协议；那属于 `xunzhi-media-domain`。
- 不要用我直接调 Guard、限流或线程池；那属于 `xunzhi-ai-runtime`。

## 参考资料

- `references/object-dictionary.md`
