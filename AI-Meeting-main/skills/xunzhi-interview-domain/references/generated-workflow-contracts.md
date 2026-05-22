# Workflow 契约索引（自动生成）

该文档从 `admin/src/main/resources/workflow/*.yml` 自动提取。改工作流后请重新运行 `scripts/extract_workflow_contracts.py`。

## 用户答案评分官.yml

- 流程名称：`用户答案评分官`
- 描述：`1`
- 分类：`14`
- DSL 版本：`v1`

| 字段名 | 类型 | 必填 | 默认值 |
| --- | --- | --- | --- |
| `AGENT_USER_INPUT` | `string` | 是 | `用户本轮对话输入内容` |
| `question` | `string / xfyun-file` | 否 | `问题` |
| `resume_context` | `string / xfyun-file` | 否 | `` |

## 简历评分面试官.yml

- 流程名称：`简历评分面试官`
- 描述：`对用户的简历进行评分`
- 分类：`10`
- DSL 版本：`v1`

| 字段名 | 类型 | 必填 | 默认值 |
| --- | --- | --- | --- |
| `AGENT_USER_INPUT` | `string` | 是 | `用户本轮对话输入内容` |
| `USER_RESUME` | `string / file / 允许:pdf` | 是 | `用户的简历` |

## 表情分析面试官.yml

- 流程名称：`表情分析面试官`
- 描述：`对用户的行为进行表情分析`
- 分类：`10`
- DSL 版本：`v1`

| 字段名 | 类型 | 必填 | 默认值 |
| --- | --- | --- | --- |
| `AGENT_USER_INPUT` | `string` | 是 | `用户本轮对话输入内容` |
| `USER_FILE` | `string / file / 允许:image` | 否 | `用户的表情信息` |

## 面试提问官.yml

- 流程名称：`面试提问官`
- 描述：`1`
- 分类：`14`
- DSL 版本：`v1`

| 字段名 | 类型 | 必填 | 默认值 |
| --- | --- | --- | --- |
| `AGENT_USER_INPUT` | `string` | 是 | `用户本轮对话输入内容` |
| `mode` | `string / xfyun-file` | 否 | `是否追问` |
| `follow_up_count` | `number / xfyun-file` | 否 | `` |
| `resume_context` | `string / xfyun-file` | 否 | `` |
| `max_follow_up` | `number / xfyun-file` | 否 | `` |
| `question` | `string / xfyun-file` | 否 | `` |

## 面试题出题官.yml

- 流程名称：`面试题出题官`
- 描述：`专门出面试题`
- 分类：`10`
- DSL 版本：`v1`

| 字段名 | 类型 | 必填 | 默认值 |
| --- | --- | --- | --- |
| `AGENT_USER_INPUT` | `string` | 是 | `用户本轮对话输入内容` |
| `USER_FILE` | `string / file / 允许:pdf` | 否 | `用户简历` |

