# 长文本 TTS

## 三个接口

- `POST /api/xunzhi/v1/xunfei/tts/tasks`：创建异步任务。
- `GET /api/xunzhi/v1/xunfei/tts/tasks/{taskId}`：查询任务状态。
- `POST /api/xunzhi/v1/xunfei/tts/synthesize`：创建任务并等待完成。

## 生命周期

1. `createTask` 只负责提交任务，写出 `taskId`。
2. `queryTask` 只负责查看进度和结果，不创建新任务。
3. `synthesizeAndWait` 会把“提交 + 等待完成”打包在一次请求里。
4. 任务真正完成后，`audioBase64` / `audioUrl` 才可能可用。

## 关键对象

- `taskId`：任务主键，用于轮询。
- `taskStatus`：平台任务状态，不是本地随意定义的状态。
- `audioBase64` / `audioUrl`：最终语音结果。
- `pybufContent` / `pybufUrl`：可选拼音结果。

## 适用场景

- 纯异步就用 `tasks`。
- 前端需要轮询就用 `queryTask`。
- 调试或同步预览就用 `synthesize`。

## 一个实用判断

- 创建成功但没有音频，不一定是失败，先看 `taskStatus`。
- 查询不到任务，优先检查 `taskId` 是否来自同一环境和同一路径。
