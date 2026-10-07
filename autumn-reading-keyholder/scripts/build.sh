#!/usr/bin/env bash
# 색상별 STL 생성 (베드 배치용 bed_*.stl + 조립 미리보기용 asm_*.stl)
#   ./scripts/build.sh            # 전부
#   ./scripts/build.sh bed        # 프린트용(베드 배치)만 → print/ 에 STL 4개 + 3MF 까지 생성
set -euo pipefail
cd "$(dirname "$0")/.."
SCAD=autumn_keyholder.scad
MODE=${1:-all}
mkdir -p out

jobs=()
for c in brown cream orange green; do
  if [[ $MODE == all || $MODE == bed ]]; then jobs+=("bed $c"); fi
  if [[ $MODE == all || $MODE == asm ]]; then jobs+=("asm $c"); fi
done

run_one() {
  kind=$1; color=$2
  asm=false; [[ $kind == asm ]] && asm=true
  start=$(date +%s)
  openscad -D "PART=\"$color\"" -D "ASSEMBLED=$asm" -o "out/${kind}_${color}.stl" autumn_keyholder.scad > "out/.${kind}_${color}.log" 2>&1
  echo "  ${kind}_${color}.stl  ($(( $(date +%s) - start ))s)"
}
export -f run_one

printf '%s\n' "${jobs[@]}" | xargs -P "$(nproc)" -L1 bash -c 'run_one $0 $1'
# CGAL 가 "mesh is not closed" 로 일부 층을 조용히 버리는 경우를 잡아냄
if grep -l "ERROR" out/.*.log 2>/dev/null; then
  echo "!! 위 로그에 ERROR 가 있습니다 — 일부 형상이 누락됐을 수 있으니 확인하세요." >&2
  exit 1
fi
if [[ $MODE == all || $MODE == bed ]]; then
  mkdir -p print
  cp out/bed_*.stl print/
  python3 scripts/make_3mf.py out/bed print/autumn_keyholder_4color.3mf
fi
echo "done -> out/ , print/"
