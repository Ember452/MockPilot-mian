from __future__ import annotations

from collections import defaultdict
from pathlib import Path
from typing import Any

import yaml

ROOT = Path(__file__).resolve().parents[3]
APP_YAML = ROOT / "admin" / "src" / "main" / "resources" / "application.yaml"
FOLLOWUP_YAML = ROOT / "admin" / "src" / "main" / "resources" / "interview-followup-rule.yaml"
OUT = Path(__file__).resolve().parents[1] / "references" / "generated-config-index.md"
TARGET_PREFIXES = [
    "spring.ai.openai",
    "xunfei.lat-key",
    "xunzhi-agent.agent-binding",
    "xunzhi-agent.flow-limit",
    "xunzhi-agent.ai-guard",
    "xunzhi-agent.ai-singleflight",
    "xunzhi-agent.thread-pool",
    "xunzhi-agent.interview.answer-guard",
    "xunzhi-agent.interview.turn-repair",
    "xunzhi-agent.redis-session",
    "xunzhi-agent.interview.rule-engine",
    "collection.vector",
    "liteflow.rule-source",
]


def flatten(data: Any, prefix: str = "") -> dict[str, Any]:
    result: dict[str, Any] = {}
    if isinstance(data, dict):
        for key, value in data.items():
            next_prefix = f"{prefix}.{key}" if prefix else str(key)
            result.update(flatten(value, next_prefix))
    elif isinstance(data, list):
        items = []
        for value in data:
            if isinstance(value, (dict, list)):
                items.append(yaml.safe_dump(value, allow_unicode=True, sort_keys=False).strip())
            else:
                items.append(str(value))
        result[prefix] = " | ".join(items)
    else:
        result[prefix] = data
    return result


def load_yaml(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {}
    with path.open("r", encoding="utf-8") as fp:
        loaded = yaml.safe_load(fp)
    return loaded or {}


def group_by_prefix(flat: dict[str, Any]) -> dict[str, list[tuple[str, Any]]]:
    grouped: dict[str, list[tuple[str, Any]]] = defaultdict(list)
    for key, value in flat.items():
        for prefix in TARGET_PREFIXES:
            if key == prefix or key.startswith(prefix + "."):
                grouped[prefix].append((key, value))
                break
    for prefix in grouped:
        grouped[prefix].sort(key=lambda item: item[0])
    return grouped


def main() -> None:
    app_flat = flatten(load_yaml(APP_YAML))
    followup_flat = flatten(load_yaml(FOLLOWUP_YAML))
    merged = {**app_flat, **followup_flat}
    grouped = group_by_prefix(merged)

    lines = [
        "# 运行时配置索引（自动生成）",
        "",
        "该文档从 `application.yaml` 和 `interview-followup-rule.yaml` 自动提取。改配置后请重新运行 `scripts/extract_config_index.py`。",
        "",
    ]
    for prefix in TARGET_PREFIXES:
        items = grouped.get(prefix)
        if not items:
            continue
        lines.extend([f"## {prefix}", "", "| 键 | 值 |", "| --- | --- |"])
        for key, value in items:
            display = str(value).replace("\n", "<br>")
            lines.append(f"| `{key}` | `{display}` |")
        lines.append("")

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()