from __future__ import annotations

import re
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[3]
SCENE_FILE = ROOT / "admin" / "src" / "main" / "java" / "com" / "hewei" / "hzyjy" / "xunzhi" / "agent" / "application" / "BusinessAgentScene.java"
APP_YAML = ROOT / "admin" / "src" / "main" / "resources" / "application.yaml"
OUT = Path(__file__).resolve().parents[1] / "references" / "generated-agent-scene-map.md"
PATTERN = re.compile(r'\s*([A-Z_]+)\("([^"]+)",\s*"([^"]+)"(.*)\),?')


def load_bindings() -> dict[str, str]:
    with APP_YAML.open("r", encoding="utf-8") as fp:
        data = yaml.safe_load(fp) or {}
    return (((data.get("xunzhi-agent") or {}).get("agent-binding")) or {})


def parse_aliases(raw_tail: str) -> list[str]:
    return re.findall(r'"([^"]+)"', raw_tail or "")


def main() -> None:
    bindings = load_bindings()
    lines = [
        "# Agent 场景索引（自动生成）",
        "",
        "该文档从 `BusinessAgentScene.java` 和 `application.yaml` 的 `xunzhi-agent.agent-binding` 自动提取。",
        "",
        "| 枚举名 | 场景 code | 默认名 | 别名 | 当前配置绑定 |",
        "| --- | --- | --- | --- | --- |",
    ]
    for line in SCENE_FILE.read_text(encoding="utf-8").splitlines():
        match = PATTERN.match(line)
        if not match:
            continue
        enum_name, scene_code, default_name, tail = match.groups()
        aliases = parse_aliases(tail)
        configured = bindings.get(scene_code, "")
        alias_text = "、".join(aliases) if aliases else "-"
        lines.append(f"| `{enum_name}` | `{scene_code}` | `{default_name}` | `{alias_text}` | `{configured}` |")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()