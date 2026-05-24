# 面试工作流契约

面试域的工作流不是“补充资料”，而是正式契约的一部分。

## 当前工作流清单

- `面试题出题官.yml`
- `用户答案评分官.yml`
- `面试提问官.yml`
- `简历评分面试官.yml`
- `表情分析面试官.yml`

## 重点关注的输入输出

- 出题工作流通常依赖简历文件或简历上下文。
- 评分工作流必须稳定输出 `score / feedback / missing_points / follow_up_needed / follow_up_question`。
- 追问工作流要能区分主问题与追问模式，并接收 `follow_up_count`、`max_follow_up` 一类上下文。
- 神态分析工作流通常携带图片或文件 URL。

## 契约维护方式

- 结构化字段变更时，必须同步修改工作流 YAML、Java 解析代码和下游 DTO。
- 改动后重新运行 `scripts/extract_workflow_contracts.py`，刷新自动生成文档。
- 自动文档是“从 YAML 提取的事实表”，手写文档负责解释语义和边界。