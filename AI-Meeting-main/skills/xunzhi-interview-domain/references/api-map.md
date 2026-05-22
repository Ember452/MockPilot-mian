# 面试 API 映射

## 会话与历史

- `POST /api/xunzhi/v1/interview/sessions`：创建面试会话。
- `GET /api/xunzhi/v1/interview/conversations`：分页查面试会话。
- `PUT /api/xunzhi/v1/interview/sessions/{sessionId}/finish`：正式收尾。
- `PUT /api/xunzhi/v1/interview/conversations/{sessionId}/end`：当前实现等价于 finish。

## 题目与答题

- `POST /api/xunzhi/v1/interview/sessions/{sessionId}/interview-questions`：上传简历并提题。
- `POST /api/xunzhi/v1/interview/sessions/{sessionId}/interview/answer`：表单方式答题。
- `POST /api/xunzhi/v1/interview/sessions/{sessionId}/interview/answer-json`：JSON 方式答题。
- `GET /api/xunzhi/v1/interview/sessions/{sessionId}/next-question`：取下一题。
- `GET /api/xunzhi/v1/interview/sessions/{sessionId}/current-question`：取当前题。

## 恢复与结果

- `GET /api/xunzhi/v1/interview/sessions/{sessionId}/restore`：恢复页总装接口。
- `GET /api/xunzhi/v1/interview/sessions/{sessionId}/interview/questions`：查题目集合。
- `GET /api/xunzhi/v1/interview/sessions/{sessionId}/interview/score`：查总分。
- `GET /api/xunzhi/v1/interview/sessions/{sessionId}/interview/suggestions`：查建议。
- `GET /api/xunzhi/v1/interview/sessions/{sessionId}/resume/score`：查简历分。
- `GET /api/xunzhi/v1/interview/sessions/{sessionId}/radar-chart`：查雷达图。
- `GET /api/xunzhi/v1/interview/sessions/{sessionId}/resume/preview`：预览简历 PDF。
- `POST /api/xunzhi/v1/interview/sessions/{sessionId}/demeanor-evaluation`：做神态分析。

## 入口后的第一站

- 控制器入口统一落到 `InterviewSessionFacade`。
- 真正的核心编排通常在 `InterviewAnswerPipeline`、状态机、恢复服务和 workflow 服务里。