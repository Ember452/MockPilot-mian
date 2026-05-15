# AI SingleFlight

`xunzhi-agent.ai-singleflight` 负责把“同一次业务请求的重复 AI 调用”折叠成一次。

## 当前全局配置

- `enable: true`
- `mode: hybrid`
- `distributed-enabled: true`
- `ttl-millis: 65000`
- `wait-timeout-millis: 65000`
- `stream-block-timeout-millis: 3000`
- `poll-fallback-interval-millis: 2000`
- `follower-max-wait-millis: 20000`
- `l1-cache-max-size: 1000`
- `cleanup-threshold: 256`
- `heavy-lock-expire-seconds: 90`

## key 语义

`InterviewAiInvoker` 目前有两类 key：

- `stage|sessionId|questionNumber|answerHash`
- `stage|sessionId|businessKey`

这意味着：

- 同 session、同 stage、同题号、同答案内容会尽量复用结果。
- 文件类或材料类调用通常用 `businessKey`，例如 `fileUrl`。
- 改 key 组成会直接改变“什么算同一次业务调用”。

## stage 策略

- `interview-evaluation`：结果 TTL `600000ms`，失败 TTL `60000ms`，L1 缓存开启。
- `interview-followup`：结果 TTL `180000ms`，失败 TTL `30000ms`，L1 缓存开启。
- `interview-extraction`：结果 TTL `1800000ms`，L1 缓存开启，适合重材料场景。
- `interview-demeanor`：结果 TTL `900000ms`，L1 缓存关闭，避免过度复用图片分析结果。

## 你要关注的不是“去重”本身，而是这些副作用

- 跟随者等待过久会被用户感知成卡顿。
- 结果 TTL 过长可能复用过旧结果。
- key 过粗会误复用，key 过细会完全打不到。
- 接管检测时间过短会放大抖动，过长会拖慢故障恢复。