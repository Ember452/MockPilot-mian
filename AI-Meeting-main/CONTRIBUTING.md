# 贡献指南

## 提交前建议

- 先阅读 [README.md](README.md) 和 `docs/` 下的相关专题文档
- 本地优先执行 `./mvnw -B -ntp clean verify`
- 新增功能时补充最小可验证测试
- 不要提交真实密钥、临时音频、构建产物或 IDE 私有文件

## 分支与提交

- 分支命名建议使用 `feature/<topic>`、`fix/<topic>`、`docs/<topic>`
- 提交信息应直接说明修改意图，例如 `fix: normalize interview validation errors`

## Pull Request 要求

PR 描述建议至少包含以下内容：

- 变更背景
- 主要实现点
- 兼容性或风险说明
- 测试方式
- 如果涉及接口或 UI，附上截图、响应示例或调用说明

## 文档要求

- README 和 `docs/` 中的命令必须与当前仓库结构一致
- 新增接口时同步更新接口文档
- 演示素材统一放到 `docs/assets/`
