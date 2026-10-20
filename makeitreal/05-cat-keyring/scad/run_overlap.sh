#!/bin/bash
# prints the overlap volume (mm^3) of every colour pair of the cat keyring (pass -D use_white=...)
cd "$(dirname "$0")"
names="${NAMES:-lavender black yellow pink}"
for a in $names; do for b in $names; do
  [[ "$a" < "$b" ]] || continue
  openscad -D "A=\"$a\"" -D "B=\"$b\"" -D "use_white=${USE_WHITE:-false}" -o /tmp/ov_$$.stl overlap_test.scad > /tmp/ov_$$.log 2>&1
  if [ -s /tmp/ov_$$.stl ] && ! grep -q "empty" /tmp/ov_$$.log; then
    v=$(python3 -c "
import re,numpy as np
t=open('/tmp/ov_$$.stl').read()
v=np.array([[float(x) for x in m.groups()] for m in re.finditer(r'vertex (\S+) (\S+) (\S+)',t)]).reshape(-1,3,3)
print(round(abs(np.sum(np.einsum('ij,ij->i',v[:,0],np.cross(v[:,1],v[:,2])))/6),3) if len(v) else 0)")
  else v=0; fi
  echo "$a x $b : $v"
done; done
rm -f /tmp/ov_$$.stl /tmp/ov_$$.log
