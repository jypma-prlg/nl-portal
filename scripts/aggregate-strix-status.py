#!/usr/bin/env python3
"""Aggregate Strix scan severity counts into a public-facing status page.

Reads every ``vulnerabilities.json`` under --runs-dir, counts findings per
severity, and writes a Markdown summary. Deliberately omits finding titles,
descriptions and locations (see ADR 0008): only counts are ever published.
"""

import argparse
import json
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

SEVERITY_ORDER = ["critical", "high", "medium", "low", "info"]


def collect_counts(runs_dir: Path) -> Counter:
    counts: Counter = Counter()
    for vuln_file in runs_dir.glob("*/vulnerabilities.json"):
        findings = json.loads(vuln_file.read_text(encoding="utf-8"))
        for finding in findings:
            severity = str(finding.get("severity", "unknown")).lower()
            counts[severity] += 1
    return counts


def render(counts: Counter, generated_at: str) -> str:
    known = [(s, counts.get(s, 0)) for s in SEVERITY_ORDER]
    other = [(s, n) for s, n in counts.items() if s not in SEVERITY_ORDER]
    rows = known + sorted(other)
    total = sum(counts.values())

    lines = [
        "# Security scan status",
        "",
        f"Laatst bijgewerkt: {generated_at} (automatisch gegenereerd uit de nieuwste Strix-scan"
        " op `main`, zie [ADR 0008](https://github.com/pinkroccade/nl-portal/blob/main/site/development/adr/0008-security-transparantie-en-registry-strategie.md)).",
        "",
        "Dit overzicht toont alleen aantallen per ernst-niveau, nooit titels, CVE's of locaties"
        " van individuele bevindingen.",
        "",
        "| Severity | Aantal open bevindingen |",
        "|---|---|",
    ]
    for severity, n in rows:
        lines.append(f"| {severity} | {n} |")
    lines.append(f"| **Totaal** | **{total}** |")
    lines.append("")
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--runs-dir", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    counts = collect_counts(args.runs_dir)
    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(render(counts, generated_at), encoding="utf-8")


if __name__ == "__main__":
    main()
