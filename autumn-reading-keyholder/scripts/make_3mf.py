#!/usr/bin/env python3
"""
색상별 STL 4개 → 멀티파트 3MF 1개 (파트마다 필라멘트 슬롯 1~4 지정)

  python3 scripts/make_3mf.py out/bed out/autumn_keyholder_4color.3mf

· 표준 3MF(코어 스펙) + PrusaSlicer 호환 Metadata/Slic3r_PE_model.config 로
  "한 오브젝트 = 색상 파트 4개, 파트별 extruder 1~4" 를 기록합니다.
  (PrusaSlicer 2.7 CLI 로 열어서 4개 extruder 로 슬라이싱되는 것까지 확인함.
   Bambu Studio / OrcaSlicer 는 이 구조를 멀티파트 오브젝트로 읽도록 되어 있으나,
   안 읽히면 STL 4개를 한꺼번에 열고 "멀티파트 오브젝트로 불러오기" 를 선택하세요.)
· 모든 STL 은 같은 원점(베드 좌표)이라 그대로 겹쳐서 정확히 맞물립니다.
"""
import re
import sys
import zipfile
from xml.sax.saxutils import escape

PARTS = [  # (파일 접미사, 파트 이름, 필라멘트 슬롯)
    ("brown", "brown_walnut", 1),
    ("cream", "cream_ivory", 2),
    ("orange", "autumn_orange", 3),
    ("green", "forest_green", 4),
]
NAME = "Autumn Reading Key Holder"


def read_ascii_stl(path):
    """ASCII STL → (vertices[list of (x,y,z)], triangles[list of (i,j,k)]), 같은 좌표는 하나로 합침"""
    vidx, verts, tris, cur = {}, [], [], []
    num = re.compile(r"vertex\s+(\S+)\s+(\S+)\s+(\S+)")
    with open(path, "r") as f:
        for line in f:
            m = num.search(line)
            if not m:
                continue
            key = tuple(round(float(v), 5) for v in m.groups())
            if key not in vidx:
                vidx[key] = len(verts)
                verts.append(key)
            cur.append(vidx[key])
            if len(cur) == 3:
                if len(set(cur)) == 3:          # 퇴화 삼각형 제외
                    tris.append(tuple(cur))
                cur = []
    return verts, tris


def main(prefix, out):
    vertices, triangles, volumes = [], [], []
    for suffix, name, slot in PARTS:
        v, t = read_ascii_stl(f"{prefix}_{suffix}.stl")
        base = len(vertices)
        first = len(triangles)
        vertices.extend(v)
        triangles.extend((a + base, b + base, c + base) for a, b, c in t)
        volumes.append((name, slot, first, len(triangles) - 1))
        print(f"  {suffix:7s} slot {slot}: {len(v):6d} vertices {len(t):6d} triangles")

    model = ['<?xml version="1.0" encoding="UTF-8"?>',
             '<model unit="millimeter" xml:lang="en-US" xmlns="http://schemas.microsoft.com/3dmanufacturing/core/2015/02" '
             'xmlns:slic3rpe="http://schemas.slic3r.org/3mf/2017/06">',
             ' <metadata name="slic3rpe:Version3mf">1</metadata>',
             f' <metadata name="Title">{escape(NAME)}</metadata>',
             ' <resources>',
             '  <object id="1" type="model">',
             '   <mesh>',
             '    <vertices>']
    model += [f'     <vertex x="{x:.5f}" y="{y:.5f}" z="{z:.5f}"/>' for x, y, z in vertices]
    model += ['    </vertices>', '    <triangles>']
    model += [f'     <triangle v1="{a}" v2="{b}" v3="{c}"/>' for a, b, c in triangles]
    model += ['    </triangles>', '   </mesh>', '  </object>', ' </resources>',
              ' <build>', '  <item objectid="1" transform="1 0 0 0 1 0 0 0 1 0 0 0" printable="1"/>', ' </build>', '</model>']

    cfg = ['<?xml version="1.0" encoding="UTF-8"?>', '<config>',
           ' <object id="1" instances_count="1">',
           f'  <metadata type="object" key="name" value="{escape(NAME)}"/>']
    for name, slot, first, last in volumes:
        cfg += [f'  <volume firstid="{first}" lastid="{last}">',
                f'   <metadata type="volume" key="name" value="{name}"/>',
                '   <metadata type="volume" key="volume_type" value="ModelPart"/>',
                f'   <metadata type="volume" key="extruder" value="{slot}"/>',
                '   <metadata type="volume" key="matrix" value="1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1"/>',
                '   <mesh edges_fixed="0" degenerate_facets="0" facets_removed="0" facets_reversed="0" backwards_edges="0"/>',
                '  </volume>']
    cfg += [' </object>', '</config>']

    content_types = ('<?xml version="1.0" encoding="UTF-8"?>'
                     '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
                     '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
                     '<Default Extension="model" ContentType="application/vnd.ms-package.3dmanufacturing-3dmodel+xml"/>'
                     '<Default Extension="config" ContentType="text/xml"/></Types>')
    rels = ('<?xml version="1.0" encoding="UTF-8"?>'
            '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
            '<Relationship Target="/3D/3dmodel.model" Id="rel0" '
            'Type="http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel"/></Relationships>')

    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", content_types)
        z.writestr("_rels/.rels", rels)
        z.writestr("3D/3dmodel.model", "\n".join(model))
        z.writestr("Metadata/Slic3r_PE_model.config", "\n".join(cfg))
    print(f"wrote {out}  ({len(vertices)} vertices, {len(triangles)} triangles)")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
