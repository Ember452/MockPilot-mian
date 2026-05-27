from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parents[3]
SRC_ROOT = ROOT / "admin" / "src" / "main" / "java"
OUT = Path(__file__).resolve().parents[1] / "references" / "generated-api-index.md"

METHOD_RE = re.compile(r"\bpublic\b.*?\b([A-Za-z0-9_]+)\s*\(")
STRING_RE = re.compile(r'"([^"]+)"')
PACKAGE_RE = re.compile(r"xunzhi/([^/]+)/")


def parse_path(annotation: str | None) -> str:
    if not annotation:
        return ""
    parts = STRING_RE.findall(annotation)
    return parts[0] if parts else ""


@dataclass
class ApiRow:
    module: str
    file: str
    controller: str
    mapping: str
    method: str
    auth: str
    note: str


def find_java_files() -> Iterable[Path]:
    for path in SRC_ROOT.rglob("*.java"):
        text = path.as_posix()
        if "/api/" in text or text.endswith("AudioTranscriptionWebSocketHandler.java"):
            yield path


def parse_file(path: Path) -> list[ApiRow]:
    lines = path.read_text(encoding="utf-8").splitlines()
    relative = path.relative_to(ROOT).as_posix()
    module_match = PACKAGE_RE.search(relative)
    module = module_match.group(1) if module_match else "unknown"
    controller = path.stem
    base_path = ""
    websocket_path = ""
    rows: list[ApiRow] = []
    pending_annotations: list[str] = []

    for index, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith("@RequestMapping") and not base_path:
            base_path = parse_path(stripped)
        if stripped.startswith("@ServerEndpoint"):
            websocket_path = parse_path(stripped)
        if stripped.startswith("@"):
            pending_annotations.append(stripped)
            continue
        method_match = METHOD_RE.search(line)
        if method_match:
            method_name = method_match.group(1)
            joined = " ".join(pending_annotations)
            mapping_name = ""
            mapping_path = ""
            for token in ["GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping", "RequestMapping"]:
                if f"@{token}" in joined:
                    mapping_name = token.replace("Mapping", "").upper()
                    for anno in reversed(pending_annotations):
                        if anno.startswith(f"@{token}"):
                            mapping_path = parse_path(anno)
                            break
                    break
            if mapping_name:
                if base_path and mapping_path:
                    full_path = base_path.rstrip("/") + "/" + mapping_path.lstrip("/")
                else:
                    full_path = mapping_path or base_path or "-"
                auth = "需要" if "@CurrentUser" in line or any("@CurrentUser" in anno for anno in pending_annotations) else "未显式声明"
                local_window = " ".join(lines[max(0, index - 6): index + 6])
                note = "SSE" if "SseEmitter" in local_window else ""
                rows.append(ApiRow(module, relative, controller, f"{mapping_name} {full_path}", method_name, auth, note))
            pending_annotations = []
        elif stripped and not stripped.startswith("//") and not stripped.startswith("*"):
            pending_annotations = []

    if websocket_path:
        rows.insert(0, ApiRow(module, relative, controller, f"WEBSOCKET {websocket_path}", "onOpen/onMessage/onClose", "需要", "实时转写"))
    return rows


def main() -> None:
    rows: list[ApiRow] = []
    for path in find_java_files():
        rows.extend(parse_file(path))
    rows.sort(key=lambda item: (item.module, item.file, item.mapping, item.method))

    lines = [
        "# 接口总览（自动生成）",
        "",
        "该文档用于从代码快速定位控制器、映射前缀和显式鉴权点。若代码已变更，请重新运行 `scripts/extract_api_index.py`。",
        "",
        "| 模块 | 文件 | 控制器 | 映射 | 方法 | 显式鉴权 | 备注 |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ]
    for row in rows:
        lines.append(
            f"| `{row.module}` | `{row.file}` | `{row.controller}` | `{row.mapping}` | `{row.method}` | {row.auth} | {row.note} |"
        )
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()