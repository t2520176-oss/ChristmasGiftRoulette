#!/usr/bin/env python3
"""Create the standalone browser setup helper; it never deploys or embeds credentials."""
import argparse
import base64
from pathlib import Path
from urllib.parse import urlencode

ROOT = Path(__file__).resolve().parent.parent


def transaction_body(text: str) -> str:
    before, marker, rest = text.partition("\nbegin;\n")
    if not marker:
        raise ValueError("Expected a top-level transaction")
    body, marker, after = rest.rpartition("\ncommit;")
    if not marker:
        raise ValueError("Expected a top-level commit")
    return before + "\n" + body


def build(output: Path, source_ref: str) -> None:
    if len(source_ref) != 40 or any(c not in "0123456789abcdef" for c in source_ref):
        raise ValueError("Use the immutable 40-character source commit")
    migration = transaction_body((ROOT / "supabase/migrations/202610180001_ourchat.sql").read_text())
    seed = transaction_body((ROOT / "supabase/seed-members-dashboard.sql").read_text())
    guard = (ROOT / "supabase/new-project-only.guard.sql").read_text()
    sql = "-- NEW OurChat project only. One atomic transaction; do not use an existing project.\nbegin;\n" + guard + "\n" + migration + "\n" + seed + "\ncommit;\nselect user_id, display_name from public.members order by member_slot;\n"
    source_url = f"https://github.com/t2520176-oss/ChristmasGiftRoulette/tree/{source_ref}/ourchat-private"
    deploy_url = "https://vercel.com/new/clone?" + urlencode({
        "repository-url": source_url,
        "project-name": "ourchat",
        "repository-name": "ourchat-private",
        "env": "NEXT_PUBLIC_SUPABASE_URL,NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY",
        "envDescription": "Use only the URL and publishable key of your NEW OurChat Supabase project.",
    })
    html = (ROOT / "tools/setup-template.html").read_text()
    for placeholder, value in {
        "__SETUP_SQL_BASE64__": base64.b64encode(sql.encode()).decode(),
        "__DEPLOY_URL__": deploy_url.replace("&", "&amp;"),
        "__SOURCE_REF__": source_ref,
    }.items():
        if placeholder not in html:
            raise ValueError(f"Missing template placeholder {placeholder}")
        html = html.replace(placeholder, value)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(html, encoding="utf-8")
    print(f"Created {output} ({output.stat().st_size:,} bytes), pinned source {source_ref}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-ref", required=True)
    parser.add_argument("--output", type=Path, default=ROOT.parent / "downloads/OurChat-Start.html")
    args = parser.parse_args()
    build(args.output.resolve(), args.source_ref)
