"""3MF writing + validation.

Primary backend: lib3mf (the reference 3MF implementation). Fallback: a small standards-compliant
pure-Python writer, used automatically if lib3mf is missing or fails (e.g. a packaging problem).
Both write real 3MF packages (OPC zip + 3D/3dmodel.model), units = millimetre.
"""
from __future__ import annotations

import ctypes
import xml.etree.ElementTree as ET
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from xml.sax.saxutils import escape, quoteattr

import numpy as np

from ..errors import ExportError
from ..logging_setup import get_logger

log = get_logger("3mf")

try:  # optional native dependency
    import lib3mf  # type: ignore
    HAVE_LIB3MF = True
except Exception:  # pragma: no cover
    lib3mf = None
    HAVE_LIB3MF = False

NS_CORE = "http://schemas.microsoft.com/3dmanufacturing/core/2015/02"


@dataclass
class Part:
    name: str
    rgb: tuple[int, int, int]
    vertices: np.ndarray            # (N,3) float
    faces: np.ndarray               # (M,3) int


@dataclass
class Meta:
    title: str = "FigureCraft model"
    description: str = ""
    designer: str = "FigureCraft"


def ams_name(index0: int, name: str) -> str:
    return f"AMS_{index0 + 1}_{name.upper().replace(' ', '_')}"


def _hex(rgb) -> str:
    return "#{:02X}{:02X}{:02X}".format(*[int(c) for c in rgb])


# --------------------------------------------------------------- lib3mf
def _set_geometry(wrapper, obj, V: np.ndarray, F: np.ndarray) -> None:
    V = np.ascontiguousarray(V, np.float32)
    T = np.ascontiguousarray(F, np.uint32)
    vp = V.ctypes.data_as(ctypes.POINTER(lib3mf.Position))
    tp = T.ctypes.data_as(ctypes.POINTER(lib3mf.Triangle))
    wrapper.checkError(obj, wrapper.lib.lib3mf_meshobject_setgeometry(
        obj._handle, ctypes.c_uint64(len(V)), vp, ctypes.c_uint64(len(T)), tp))


def _set_triangle_props(wrapper, obj, group_id: int, prop_idx: np.ndarray) -> None:
    arr = np.empty((len(prop_idx), 4), np.uint32)
    arr[:, 0] = group_id
    arr[:, 1:] = prop_idx[:, None]
    arr = np.ascontiguousarray(arr)
    ptr = arr.ctypes.data_as(ctypes.POINTER(lib3mf.TriangleProperties))
    wrapper.checkError(obj, wrapper.lib.lib3mf_meshobject_setalltriangleproperties(
        obj._handle, ctypes.c_uint64(len(arr)), ptr))


def _lib3mf_meta(model, meta: Meta) -> None:
    g = model.GetMetaDataGroup()
    for k, v in (("Title", meta.title), ("Designer", meta.designer), ("Description", meta.description),
                 ("Application", "FigureCraft")):
        if v:
            g.AddMetaData("", k, v, "string", True)


def _write_lib3mf_multipart(path: Path, parts: list[Part], meta: Meta) -> None:
    w = lib3mf.get_wrapper()
    model = w.CreateModel()
    model.SetUnit(lib3mf.ModelUnit.MilliMeter)
    _lib3mf_meta(model, meta)
    mats = model.AddBaseMaterialGroup()
    objs = []
    for i, p in enumerate(parts):
        idx = mats.AddMaterial(p.name, w.RGBAToColor(*[int(c) for c in p.rgb], 255))
        o = model.AddMeshObject()
        o.SetName(p.name)
        _set_geometry(w, o, p.vertices, p.faces)
        o.SetObjectLevelProperty(mats.GetResourceID(), idx)
        objs.append(o)
    asm = model.AddComponentsObject()
    asm.SetName(meta.title)
    for o in objs:
        asm.AddComponent(o, w.GetIdentityTransform())
    model.AddBuildItem(asm, w.GetIdentityTransform())
    model.QueryWriter("3mf").WriteToFile(str(path))


def _write_lib3mf_color(path: Path, V, F, labels, mats_spec, meta: Meta) -> None:
    w = lib3mf.get_wrapper()
    model = w.CreateModel()
    model.SetUnit(lib3mf.ModelUnit.MilliMeter)
    _lib3mf_meta(model, meta)
    mats = model.AddBaseMaterialGroup()
    pids = [mats.AddMaterial(name, w.RGBAToColor(*[int(c) for c in rgb], 255)) for name, rgb in mats_spec]
    o = model.AddMeshObject()
    o.SetName(meta.title)
    _set_geometry(w, o, V, F)
    _set_triangle_props(w, o, mats.GetResourceID(), np.asarray(pids, np.uint32)[np.asarray(labels, int)])
    model.AddBuildItem(o, w.GetIdentityTransform())
    model.QueryWriter("3mf").WriteToFile(str(path))


# --------------------------------------------------------- pure python
_CT = ('<?xml version="1.0" encoding="UTF-8"?>\n'
       '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
       '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
       '<Default Extension="model" ContentType="application/vnd.ms-package.3dmanufacturing-3dmodel+xml"/>'
       '</Types>')
_RELS = ('<?xml version="1.0" encoding="UTF-8"?>\n'
         '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
         '<Relationship Target="/3D/3dmodel.model" Id="rel0" '
         'Type="http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel"/></Relationships>')


def _mesh_xml(V: np.ndarray, F: np.ndarray, tri_p: np.ndarray | None, pid: int) -> str:
    vs = "\n".join(f'<vertex x="{x:.5f}" y="{y:.5f}" z="{z:.5f}"/>' for x, y, z in np.asarray(V, float))
    if tri_p is None:
        ts = "\n".join(f'<triangle v1="{a}" v2="{b}" v3="{c}"/>' for a, b, c in np.asarray(F, int))
    else:
        ts = "\n".join(f'<triangle v1="{a}" v2="{b}" v3="{c}" pid="{pid}" p1="{p}" p2="{p}" p3="{p}"/>'
                       for (a, b, c), p in zip(np.asarray(F, int), tri_p))
    return f"<mesh><vertices>\n{vs}\n</vertices><triangles>\n{ts}\n</triangles></mesh>"


def _model_header(meta: Meta) -> str:
    md = "".join(f'<metadata name="{k}">{escape(v)}</metadata>' for k, v in
                 (("Title", meta.title), ("Designer", meta.designer), ("Description", meta.description),
                  ("Application", "FigureCraft")) if v)
    return (f'<?xml version="1.0" encoding="UTF-8"?>\n<model unit="millimeter" xml:lang="en-US" xmlns="{NS_CORE}">'
            f'{md}')


def _pack(path: Path, model_xml: str) -> None:
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", _CT)
        z.writestr("_rels/.rels", _RELS)
        z.writestr("3D/3dmodel.model", model_xml)


def _write_py_multipart(path: Path, parts: list[Part], meta: Meta) -> None:
    out = [_model_header(meta), "<resources>"]
    out.append('<basematerials id="1">')
    for p in parts:
        out.append(f'<base name={quoteattr(p.name)} displaycolor="{_hex(p.rgb)}FF"/>')
    out.append("</basematerials>")
    ids = []
    for i, p in enumerate(parts):
        oid = 2 + i
        ids.append(oid)
        out.append(f'<object id="{oid}" type="model" name={quoteattr(p.name)} pid="1" pindex="{i}">')
        out.append(_mesh_xml(p.vertices, p.faces, None, 1))
        out.append("</object>")
    asm = 2 + len(parts)
    out.append(f'<object id="{asm}" type="model" name={quoteattr(meta.title)}><components>')
    out += [f'<component objectid="{i}"/>' for i in ids]
    out.append("</components></object></resources>")
    out.append(f'<build><item objectid="{asm}"/></build></model>')
    _pack(path, "\n".join(out))


def _write_py_color(path: Path, V, F, labels, mats_spec, meta: Meta) -> None:
    out = [_model_header(meta), '<resources><basematerials id="1">']
    for name, rgb in mats_spec:
        out.append(f'<base name={quoteattr(name)} displaycolor="{_hex(rgb)}FF"/>')
    out.append("</basematerials>")
    out.append(f'<object id="2" type="model" name={quoteattr(meta.title)}>')
    out.append(_mesh_xml(V, F, np.asarray(labels, int), 1))
    out.append('</object></resources><build><item objectid="2"/></build></model>')
    _pack(path, "\n".join(out))


# ------------------------------------------------------------ public API
def write_multipart_3mf(path, parts: list[Part], meta: Meta | None = None) -> str:
    """One assembly object whose components are the colour parts. Returns the backend used."""
    path, meta = Path(path), meta or Meta()
    if not parts:
        raise ExportError("export_3mf_failed", "no parts")
    path.parent.mkdir(parents=True, exist_ok=True)
    if HAVE_LIB3MF:
        try:
            _write_lib3mf_multipart(path, parts, meta)
            return "lib3mf"
        except Exception as exc:
            log.warning("lib3mf multipart failed (%s); using built-in writer", exc)
    try:
        _write_py_multipart(path, parts, meta)
    except Exception as exc:
        raise ExportError("export_3mf_failed", str(exc)) from exc
    return "builtin"


def write_color_3mf(path, vertices, faces, labels, palette_spec, meta: Meta | None = None) -> str:
    """One mesh object, per-triangle base-material colours. `palette_spec` = [(name, rgb)]."""
    path, meta = Path(path), meta or Meta()
    path.parent.mkdir(parents=True, exist_ok=True)
    if HAVE_LIB3MF:
        try:
            _write_lib3mf_color(path, vertices, faces, labels, palette_spec, meta)
            return "lib3mf"
        except Exception as exc:
            log.warning("lib3mf colour export failed (%s); using built-in writer", exc)
    try:
        _write_py_color(path, vertices, faces, labels, palette_spec, meta)
    except Exception as exc:
        raise ExportError("export_3mf_failed", str(exc)) from exc
    return "builtin"


@dataclass
class ValidationResult:
    ok: bool
    objects: int = 0
    triangles: int = 0
    colors: list[str] = field(default_factory=list)
    unit: str = ""
    problems: list[str] = field(default_factory=list)
    lib3mf_read: bool | None = None


def validate_3mf(path) -> ValidationResult:
    """Independent re-read of a written file: OPC structure, XML, index ranges, then lib3mf round-trip."""
    path = Path(path)
    res = ValidationResult(ok=False)
    try:
        with zipfile.ZipFile(path) as z:
            names = set(z.namelist())
            for req in ("[Content_Types].xml", "_rels/.rels", "3D/3dmodel.model"):
                if req not in names:
                    res.problems.append(f"missing {req}")
            if res.problems:
                return res
            root = ET.fromstring(z.read("3D/3dmodel.model"))
    except Exception as exc:
        res.problems.append(f"unreadable package: {exc}")
        return res
    ns = {"m": NS_CORE}
    res.unit = root.get("unit", "")
    if res.unit != "millimeter":
        res.problems.append(f"unit is {res.unit!r}, expected millimeter")
    for bm in root.findall(".//m:basematerials/m:base", ns):
        res.colors.append(f'{bm.get("name")}={bm.get("displaycolor")}')
    objs = root.findall(".//m:resources/m:object", ns)
    res.objects = len(objs)
    ids = {o.get("id") for o in objs}
    for o in objs:
        mesh = o.find("m:mesh", ns)
        if mesh is None:
            for c in o.findall(".//m:component", ns):
                if c.get("objectid") not in ids:
                    res.problems.append(f"component refers to missing object {c.get('objectid')}")
            continue
        nv = len(mesh.findall("m:vertices/m:vertex", ns))
        tris = mesh.findall("m:triangles/m:triangle", ns)
        res.triangles += len(tris)
        for t in tris[:: max(1, len(tris) // 5000)]:
            for k in ("v1", "v2", "v3"):
                if not (0 <= int(t.get(k)) < nv):
                    res.problems.append("triangle index out of range")
                    break
    for it in root.findall(".//m:build/m:item", ns):
        if it.get("objectid") not in ids:
            res.problems.append("build item refers to missing object")
    if HAVE_LIB3MF:
        try:
            w = lib3mf.get_wrapper()
            model = w.CreateModel()
            model.QueryReader("3mf").ReadFromFile(str(path))
            it = model.GetBuildItems()
            res.lib3mf_read = bool(it.MoveNext())
        except Exception as exc:
            res.lib3mf_read = False
            res.problems.append(f"lib3mf could not read the file: {exc}")
    res.ok = not res.problems and res.triangles > 0
    return res
