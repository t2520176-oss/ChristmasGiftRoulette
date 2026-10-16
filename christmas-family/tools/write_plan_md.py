#!/usr/bin/env python3
"""Write beds/PLAN.md (Korean) from plan.json, bedN.json, slice_summary.json and the family config.

  python3 write_plan_md.py --beds ../beds --print-dir ../stl/print
"""
import argparse
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import family_palette as fp  # noqa: E402

KO = {"BLUE": "파랑", "YELLOW": "노랑", "WHITE": "흰색", "BROWN": "갈색(진저브레드)", "GREEN": "초록", "RED": "빨강", "BLACK": "검정", "PINK": "핑크"}
ROLE_KO = {"panel": "하늘 판", "stars": "달·별", "green": "잎", "snow": "쌓인 눈", "star": "꼭대기 별", "wall": "벽", "window": "창문",
           "wood": "나무 몸통", "plate": "판", "text": "글자", "leaf": "잎", "berry": "열매", "frame": "틀", "glass": "유리",
           "jacket": "재킷", "scarf": "목도리", "hat": "모자", "pom": "방울", "face": "얼굴", "eyes": "눈동자·표정", "dress": "원피스",
           "collar": "칼라", "hair": "머리", "bow": "리본", "body": "몸", "band": "모자 띠", "nose": "코", "fur": "털", "ears": "귀",
           "white": "흰 무늬", "red": "빨강 무늬"}


PIECE_KO = {"sky": "밤하늘 아치 판", "tree": "나무", "house": "집", "base": "받침(나무 몸통 + 쌓인 눈)", "plaque": "명판",
            "holly": "호랑가시", "lantern": "랜턴", "tag": "이름표"}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--beds", required=True)
    ap.add_argument("--print-dir", required=True)
    a = ap.parse_args()
    plan = json.load(open(os.path.join(a.beds, "plan.json")))
    fp.COLOR_HEX.update(plan.get("hex", {}))
    cfg = json.load(open(os.path.join(a.print_dir, "config.json")))
    sl_path = os.path.join(a.beds, "slice_summary.json")
    sl = json.load(open(sl_path)) if os.path.exists(sl_path) else {}
    who = {f"fig{i}": f"{cfg['names'][i]} ({m})" for i, m in enumerate(cfg["members"])}
    out = [f"# 프린트 베드 계획 — THE {cfg['family']} FAMILY · Christmas {cfg['year']}", "",
           f"베드 {plan['bed'][0]:.0f} × {plan['bed'][1]:.0f} mm, 베드당 최대 {plan['max_colors']}색. 필라멘트 색이 {plan['max_colors']}개를 넘으면 "
           "그 색이 필요한 조각은 다음 베드로 넘어갑니다(조각 하나의 모든 색은 항상 같은 베드에서 출력).", "",
           "| 베드 | 필라멘트 슬롯 1 · 2 · 3 · 4 | 조각 수 | 베드 점유율 | 예상 시간* | 필라멘트(g)* |", "|---|---|---|---|---|---|"]
    for b in plan["beds"]:
        s = sl.get(str(b["bed"]), {})
        slots = " · ".join(f"{c}" for c in b["colors"])
        g = " / ".join(f"{x:.0f}" for x in s.get("filament_g", [])) if s else "-"
        out.append(f"| **{b['bed']}** | {slots} | {len(b['pieces'])} | {b['footprint_pct']} % | {s.get('time', '-')} | {g} |")
    out += ["", "\\* PrusaSlicer 2.7 CLI, 0.4 mm 노즐 · 0.2 mm 레이어 · 벽 3 · 인필 12 % · 프라임 타워 포함(`slicer/prusaslicer_4color_256.ini`). "
            "일반적인 느린 프로파일 기준이라 실제 기계(Bambu 등)에서는 달라집니다. 필라멘트 g 은 슬롯 순서이며 색 교환 퍼지(프라임 타워) 분량이 포함됩니다.", ""]
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
            label = who.get(p) or PIECE_KO.get(p.rstrip("0123456789"), p)
            if p.startswith("tag"):
                label = f"이름표 {cfg['names'][int(p[3:])]}"
            elif p.startswith(("tree", "holly", "lantern")):
                label = f"{PIECE_KO[p.rstrip('0123456789')]} {int(p[-1]) + 1}"
            elif p == "plaque":
                label = f"명판 THE {cfg['family']} FAMILY / Christmas {cfg['year']}"
            parts = ", ".join(f"{ROLE_KO.get(v['role'], v['role'])}={v['color']}" for v in byp[p])
            out.append(f"| `{p}` | {label} | {parts} |")
        if str(n) in sl:
            s = sl[str(n)]
            sup = s.get("auto_support_extrusion_mm") or {}
            out += ["", f"- 슬라이서 자동 서포트 점검(45°): {'서포트 없음' if not sup else '약 ' + str(round(sum(sup.values()))) + ' mm 필라멘트(≈ %.2f g) — 사실상 없음' % (sum(sup.values()) * 2.405 * 1.24 / 1000)}",
                    f"- 프라임 타워: x {s['tower_xy'][0]}–{s['tower_xy'][1]}, y {s['tower_xy'][2]}–{s['tower_xy'][3]} mm (모델과 {s['tower_gap_to_pieces_mm']} mm 이상 떨어짐, 베드 안: {'예' if s['tower_inside_bed'] else '아니오'})",
                    f"- 색 교환: 슬롯 {', '.join(t for t in s['tools'])} 사용"]
        out.append("")
    open(os.path.join(a.beds, "PLAN.md"), "w").write("\n".join(out) + "\n")
    print("wrote", os.path.join(a.beds, "PLAN.md"))


if __name__ == "__main__":
    main()
