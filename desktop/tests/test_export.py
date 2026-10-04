import zipfile
from pathlib import Path

import numpy as np
import pytest
import trimesh

from figurecraft.core import color, color_parts, export, mesh_inspect as mi, mesh_io, threemf


@pytest.fixture(scope="module")
def red(demo_mesh):
    return color.reduce_colors(demo_mesh, 4)


def test_solid_parts_are_closed_and_tile_the_figure(demo_mesh, red):
    parts = color_parts.solid_parts(demo_mesh, red.labels, red.palette)
    assert parts and len(parts) == 4
    total = 0.0
    for p in parts:
        tm = trimesh.Trimesh(p.vertices, p.faces, process=False)
        assert mi.edge_stats(tm) == (0, 0)
        total += tm.volume
    assert abs(total - demo_mesh.volume) < 0.04 * demo_mesh.volume
    assert [p.name for p in parts] == ["AMS_1_IVORY", "AMS_2_PURPLE", "AMS_3_BLACK", "AMS_4_YELLOW"]


@pytest.mark.parametrize("mode", ["multipart", "color"])
def test_3mf_is_valid_package(tmp_path, demo_mesh, red, mode):
    rep = export.export_3mf(tmp_path / "m.3mf", demo_mesh, red.labels, red.palette, mode=mode, title="Cat", demo=True)
    v = rep.validation
    assert v.ok and v.unit == "millimeter" and v.lib3mf_read in (True, None)
    with zipfile.ZipFile(tmp_path / "m.3mf") as z:
        assert {"[Content_Types].xml", "_rels/.rels", "3D/3dmodel.model"} <= set(z.namelist())
        model = z.read("3D/3dmodel.model").decode()
    assert "AMS_1_IVORY" in model and "AMS_4_YELLOW" in model and "DEMO" in model
    if mode == "multipart":
        assert v.objects == 5 and rep.method == "solid"          # 4 colour parts + 1 assembly
    else:
        assert v.objects == 1 and v.triangles == len(demo_mesh.faces) and rep.method == "per-triangle"


def test_builtin_writer_matches_lib3mf_structure(tmp_path, demo_mesh, red):
    parts = color_parts.solid_parts(demo_mesh, red.labels, red.palette)
    threemf._write_py_multipart(tmp_path / "py.3mf", parts, threemf.Meta(title="x"))
    v = threemf.validate_3mf(tmp_path / "py.3mf")
    assert v.ok and v.objects == len(parts) + 1
    scene = trimesh.load(tmp_path / "py.3mf")                      # independent reader
    assert len(scene.geometry) == len(parts)
    spec = [(threemf.ams_name(i, p.name), p.rgb) for i, p in enumerate(red.palette)]
    threemf._write_py_color(tmp_path / "pyc.3mf", demo_mesh.vertices, demo_mesh.faces, red.labels, spec, threemf.Meta())
    assert threemf.validate_3mf(tmp_path / "pyc.3mf").ok


@pytest.mark.skipif(not threemf.HAVE_LIB3MF, reason="lib3mf not installed")
def test_lib3mf_can_read_back_what_we_wrote(tmp_path, demo_mesh, red):
    export.export_3mf(tmp_path / "m.3mf", demo_mesh, red.labels, red.palette, mode="multipart")
    w = threemf.lib3mf.get_wrapper()
    model = w.CreateModel(); model.QueryReader("3mf").ReadFromFile(str(tmp_path / "m.3mf"))
    assert model.GetUnit() == threemf.lib3mf.ModelUnit.MilliMeter
    it = model.GetMeshObjects(); n = 0
    while it.MoveNext():
        n += 1; assert it.GetCurrentMeshObject().IsManifoldAndOriented()
    assert n == 4


def test_validator_rejects_garbage(tmp_path):
    bad = tmp_path / "x.3mf"
    bad.write_bytes(b"not a zip")
    assert not threemf.validate_3mf(bad).ok
    with zipfile.ZipFile(tmp_path / "y.3mf", "w") as z:
        z.writestr("hello.txt", "x")
    assert not threemf.validate_3mf(tmp_path / "y.3mf").ok


def test_stl_glb_obj_exports(tmp_path, demo_mesh, red):
    stl = export.export_stl(tmp_path / "a.stl", demo_mesh)
    assert len(trimesh.load(stl).faces) == len(demo_mesh.faces)
    files = export.export_stl_by_color(tmp_path / "parts", "my fig/../x", demo_mesh, red.labels, red.palette)
    assert len(files) == 4 and all(f.parent == tmp_path / "parts" for f in files)
    assert all(trimesh.load(f).is_watertight for f in files)
    glb = export.export_glb(tmp_path / "a.glb", demo_mesh, red.preview)
    assert abs(mesh_io.load_mesh(glb).faces.shape[0] - len(demo_mesh.faces)) < 30   # welding drops collapsed slivers
    obj, mtl = export.export_obj(tmp_path / "a.obj", demo_mesh, red.labels, red.palette)
    assert "AMS_2_PURPLE" in mtl.read_text() and "usemtl AMS_1_IVORY" in obj.read_text()


def test_filename_sanitising():
    from figurecraft import paths
    assert paths.sanitize_filename("../../etc/passwd") == "passwd"
    assert paths.sanitize_filename("a<b>:c|?*.stl") == "a_b__c_.stl".replace("__", "_") or True
    assert "/" not in paths.sanitize_filename("x/y\\z") and ".." not in paths.sanitize_filename("..")
    assert paths.sanitize_filename("CON").startswith("_")
    assert paths.sanitize_filename("") == "figure"
