---
name: xunzhi-repo-map
description: AI-Meeting 仓库导航 Skill。用于快速判断需求应该落到哪个业务域、哪个控制器、哪个工作流或哪个配置文件；当需求入口不清晰、需要先找模块边界、接口总览、运行入口或改动影响面时使用。
---

# xunzhi-repo-map

先用这层做路由，再切到对应业务 Skill。

## 使用顺序

- 先看 `references/request-to-module-map.md`，把需求归到一个主域。
- 再看 `references/common-entrypoints.md`，找到最先该打开的控制器、配置或工作流文件。
- 再看 `references/generated-api-index.md`，按接口前缀和认证方式快速定位实现。
- 如果需求涉及改接口、改配置、改工作流或跨多个域，立刻切到对应领域 Skill，不要长期停留在总路由层。

## 路由原则

- 先判断对象是什么，再判断它属于哪个域。
- 面试会话、出题、评分、追问、恢复、简历预览、神态分析，都归 `xunzhi-interview-domain`。
- 通用 Agent 聊天、会话历史、文件上传、Agent 属性管理，都归 `xunzhi-agent-domain`。
- 实时转写、WebSocket 推送、长文本 TTS，都归 `xunzhi-media-domain`。
- 运行时限流、守护、singleflight、线程池、配置语义，都归 `xunzhi-ai-runtime`。
- 登录、当前用户、权限、WebSocket 鉴权，都归 `xunzhi-auth-user`。
- 需求同时改接口、DTO、工作流、配置、缓存时，先用 `xunzhi-change-playbook`。
- 现象不清楚、怀疑卡死、重试、重复、恢复失败时，先用 `xunzhi-debug-playbook`。

## 什么时候继续下钻

- 需求已经明确落到某个 controller、service 或 workflow 时，不要继续停在 repo-map。
- 需要确认入参、出参、缓存键、状态机、限流策略时，优先打开对应域的 references。
- 如果你已经知道要改哪一层，就直接切换 Skill，不要再做二次路由。

## 参考资料

- `references/module-routing.md`
- `references/common-entrypoints.md`
- `references/request-to-module-map.md`
- `references/generated-api-index.md`
- `scripts/extract_api_index.py`
