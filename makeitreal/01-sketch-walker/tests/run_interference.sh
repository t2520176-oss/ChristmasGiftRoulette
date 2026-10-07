#!/bin/bash
# Prints the overlap volume (mm^3) of every part pair at several crank angles.  All must be ~0.
cd "$(dirname "$0")"
TMP=$(mktemp -d)
for th in 0 60 120 200 300; do
  for pair in $(seq 1 20); do
    openscad -D "PAIR=$pair" -D "THETA=$th" -D '$fn=24' -o "$TMP/o.stl" interference.scad > "$TMP/log" 2>&1
    if grep -q "empty" "$TMP/log" || [ ! -s "$TMP/o.stl" ]; then v=0; else
      v=$(python3 -c "
import re,numpy as np
t=open('$TMP/o.stl').read()
v=np.array([[float(x) for x in m.groups()] for m in re.finditer(r'vertex (\S+) (\S+) (\S+)',t)]).reshape(-1,3,3)
print(round(abs(np.sum(np.einsum('ij,ij->i',v[:,0],np.cross(v[:,1],v[:,2])))/6),2) if len(v) else 0)")
    fi
    echo "theta=$th pair=$pair overlap_mm3=$v"
  done
done
