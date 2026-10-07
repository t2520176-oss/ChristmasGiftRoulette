#!/bin/bash
# Parallel interference sweep.  Fixed-part pairs (1-11, 18) do not depend on the crank angle -> checked once at theta=0.
# Pairs that depend on the crank angle (legs vs shafts, gears vs shafts) are checked at several angles.   Output: "theta pair overlap_mm3"
cd "$(dirname "$0")"
job() {
  th=$1; pair=$2; tmp=$(mktemp -d)
  openscad -D "PAIR=$pair" -D "THETA=$th" -D '$fn=24' -o "$tmp/o.stl" interference.scad > "$tmp/log" 2>&1
  if [ ! -s "$tmp/o.stl" ] || grep -q "empty" "$tmp/log"; then v=0; else
    v=$(python3 -c "
import re,numpy as np
t=open('$tmp/o.stl').read()
v=np.array([[float(x) for x in m.groups()] for m in re.finditer(r'vertex (\S+) (\S+) (\S+)',t)]).reshape(-1,3,3)
print(round(abs(np.sum(np.einsum('ij,ij->i',v[:,0],np.cross(v[:,1],v[:,2])))/6),2) if len(v) else 0)")
  fi
  echo "theta=$th pair=$pair overlap_mm3=$v"; rm -rf "$tmp"
}
export -f job
( for th in 0 60 120 200 300; do for pair in 14 15 18; do echo "$th $pair"; done; done ) | xargs -P 4 -L 1 bash -c 'job $0 $1'
