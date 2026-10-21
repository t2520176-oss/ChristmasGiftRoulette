#!/usr/bin/env python3
"""Write beds/PLAN.md (Korean) from plan.json, bedN.json and slice_summary.json.

  python3 write_plan_md.py --beds ../beds --print-dir ../stl/print
"""
import argparse
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import family_palette as fp  # noqa: E402

KO = {"WHITE": "흰색", "BLUE": "아이스 블루", "GREEN": "초록", "BLACK": "검정", "ORANGE": "주황"}
PIECE_KO = {"ground": "눈 덮인 바닥판(티라이트 홈 + 소켓)", "body": "이글루 몸통(얼음 블록 4단 중 아래 3단)", "lid": "이글루 뚜껑(솔리드, 눈 결정)",
            "trim": "입구 아치 장식(블루)", "sign": "팻말", "tree": "소나무", "peng0": "펭귄(어른)", "peng1": "펭귄(아기)"}
ROLE_KO = {"snow": "눈", "ice": "얼음 블록", "belt": "블루 띠", "flake": "눈 결정", "ring": "아치 링", "wood": "나무 판", "text": "글자",
           "green": "잎", "body": "몸", "belly": "배", "patch": "얼굴 무늬", "eyes": "눈", "beak": "부리", "feet": "발", "scarf": "목도리"}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--beds", required=True)
    ap.add_argument("--print-dir", required=True)
    a = ap.parse_args()
    plan = json.load(open(os.path.join(a.beds, "plan.json")))
    fp.COLOR_HEX.update(plan.get("hex", {}))
    sl_path = os.path.join(a.beds, "slice_summary.json")
    sl = json.load(open(sl_path)) if os.path.exists(sl_path) else {}
    out = ["# 프린트 베드 계획 — PENGUIN HOME", "",
           f"베드 {plan['bed'][0]:.0f} × {plan['bed'][1]:.0f} mm, 베드당 최대 {plan['max_colors']}색. 조각 하나의 모든 색은 같은 베드에서 출력됩니다.", "",
           "| 베드 | 필라멘트 슬롯 | 조각 | 베드 점유율 | 예상 시간* | 필라멘트(g)* | 서포트 |", "|---|---|---|---|---|---|---|"]
    for b in plan["beds"]:
        s = sl.get(str(b["bed"]), {})
        slots = " · ".join(b["colors"])
        g = " / ".join(f"{x:.0f}" for x in s.get("filament_g", [])[:len(b["colors"])]) if s else "-"
        sup = s.get("auto_support_extrusion_mm") or {}
        out.append(f"| **{b['bed']}** | {slots} | {len(b['pieces'])} | {b['footprint_pct']} % | {s.get('time', '-')} | {g} | "
                   f"{'없음' if s and not sup else ('약 %d mm' % round(sum(sup.values()))) if s else '-'} |")
    out += ["", "\\* PrusaSlicer 2.7 CLI, 0.4 mm 노즐 · 0.2 mm 레이어 · 벽 3 · 인필 12 % · 프라임 타워 포함(`slicer/prusaslicer_4color_256.ini`). "
            "일반 프로파일 추정치라 실제 기계에서는 달라집니다. g 은 슬롯 순서이며 색 교환 퍼지 분량이 포함됩니다.", ""]
    for b in plan["beds"]:
        n = b["bed"]
        info = json.load(open(os.path.join(a.beds, f"bed{n}.json")))
        out += [f"## 베드 {n}", "", f"![bed {n}](../renders/bed{n}.png)", "", "| 슬롯 | 색 | HEX | 순수 모델 부피 |", "|---|---|---|---|"]
        for slot, c in b["slots"].items():
            out.append(f"| {slot} | {c} ({KO[c]}) | `{fp.COLOR_HEX[c]}` | {b['solid_volume_cm3'].get(c, 0)} cm³ |")
        out += ["", "| 조각 | 의미 | 색 구성 |", "|---|---|---|"]
        byp = {}
        for v in info["volumes"]:
            byp.setdefault(v["piece"], []).append(v)
        for p in sorted(byp):
            label = PIECE_KO.get(p) or PIECE_KO.get(p.rstrip("0123456789"), p)
            parts = ", ".join(f"{ROLE_KO.get(v['role'], v['role'])}={v['color']}" for v in byp[p])
            out.append(f"| `{p}` | {label} | {parts} |")
        if str(n) in sl:
            s = sl[str(n)]
            sup = s.get("auto_support_extrusion_mm") or {}
            out += ["", f"- 슬라이서 자동 서포트 점검(45°): {'**서포트 없음**' if not sup else '약 %d mm 필라멘트' % round(sum(sup.values()))}",
                    f"- 프라임 타워: x {s['tower_xy'][0]}–{s['tower_xy'][1]}, y {s['tower_xy'][2]}–{s['tower_xy'][3]} mm "
                    f"(모델과 {s['tower_gap_to_pieces_mm']} mm 이상 떨어짐, 베드 안: {'예' if s['tower_inside_bed'] else '아니오'})",
                    f"- 색 교환: 슬롯 {', '.join(s['tools'])} 사용"]
        out.append("")
    open(os.path.join(a.beds, "PLAN.md"), "w").write("\n".join(out) + "\n")
    print("wrote", os.path.join(a.beds, "PLAN.md"))


if __name__ == "__main__":
    main()
