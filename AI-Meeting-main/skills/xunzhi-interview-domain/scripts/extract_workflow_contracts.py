from __future__ import annotations

from pathlib import Path
from typing import Any

import yaml

ROOT = Path(__file__).resolve().parents[3]
WORKFLOW_DIR = ROOT / "admin" / "src" / "main" / "resources" / "workflow"
OUT = Path(__file__).resolve().parents[1] / "references" / "generated-workflow-contracts.md"


def load_yaml(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as fp:
        loaded = yaml.safe_load(fp)
    return loaded or {}


def find_start_node(nodes: list[dict[str, Any]]) -> dict[str, Any] | None:
    for node in nodes:
        if str(node.get("type", "")).startswith("开始"):
            return node
    return nodes[0] if nodes else None


def format_types(output_item: dict[str, Any]) -> str:
    schema = output_item.get("schema") or {}
    data_type = schema.get("type") or "unknown"
    file_type = output_item.get("fileType") or output_item.get("customParameterType")
    allowed = output_item.get("allowedFileType") or []
    parts = [str(data_type)]
    if file_type:
        parts.append(str(file_type))
    if allowed:
        parts.append("允许:" + ",".join(map(str, allowed)))
    return " / ".join(parts)


def main() -> None:
    lines = [
        "# Workflow 契约索引（自动生成）",
        "",
        "该文档从 `admin/src/main/resources/workflow/*.yml` 自动提取。改工作流后请重新运行 `scripts/extract_workflow_contracts.py`。",
        "",
    ]
    for path in sorted(WORKFLOW_DIR.glob("*.yml")):
        data = load_yaml(path)
        flow_meta = data.get("flowMeta") or {}
        flow_data = data.get("flowData") or {}
        start_node = find_start_node(flow_data.get("nodes") or []) or {}
        outputs = ((start_node.get("data") or {}).get("outputs") or [])

        lines.extend([
            f"## {path.name}",
            "",
            f"- 流程名称：`{flow_meta.get('name', '')}`",
            f"- 描述：`{flow_meta.get('description', '')}`",
            f"- 分类：`{flow_meta.get('category', '')}`",
            f"- DSL 版本：`{flow_meta.get('dslVersion', '')}`",
            "",
            "| 字段名 | 类型 | 必填 | 默认值 |",
            "| --- | --- | --- | --- |",
        ])
        for output_item in outputs:
            name = output_item.get("name", "")
            required = "是" if output_item.get("required") else "否"
            schema = output_item.get("schema") or {}
            default = schema.get("default", "")
            lines.append(
                f"| `{name}` | `{format_types(output_item)}` | {required} | `{default}` |"
            )
        if not outputs:
            lines.append("| `-` | `-` | 否 | `-` |")
        lines.append("")

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()