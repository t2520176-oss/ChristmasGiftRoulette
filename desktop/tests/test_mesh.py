import numpy as np
import pytest
import trimesh

from figurecraft.core import mesh_inspect as mi
from figurecraft.core import mesh_io, mesh_print, mesh_repair
from figurecraft.core.models import PrintOptions
from figurecraft.errors import MeshError


def test_glb_roundtrip_keeps_colors_and_zup(tmp_path, demo_mesh):
    p = mesh_io.save_glb(demo_mesh, tmp_path / "a.glb")
    m = mesh_io.load_mesh(p)
    assert abs(len(m.faces) - len(demo_mesh.faces)) < 30                    # welding drops collapsed slivers
    assert np.allclose(m.extents, demo_mesh.extents, atol=1e-3)            # Y-up/Z-up conversion is symmetric
    cols = lambda mm: set(map(tuple, mesh_io.get_face_colors(mm).tolist()))
    assert cols(m) == cols(demo_mesh)


def test_texture_is_converted_to_face_colors(tmp_path):
    from PIL import Image
    box = trimesh.creation.box(extents=[1, 1, 1])
    uv = np.zeros((len(box.vertices), 2)); uv[:, 0] = 0.25
    img = Image.new("RGB", (4, 4), (200, 30, 30))
    box.visual = trimesh.visual.TextureVisuals(uv=uv, material=trimesh.visual.material.SimpleMaterial(image=img, diffuse=(255, 255, 255, 255)))
    path = tmp_path / "t.glb"
    path.write_bytes(trimesh.Scene(box).export(file_type="glb"))
    m = mesh_io.load_mesh(path)
    assert (mesh_io.get_face_colors(m) == (200, 30, 30)).all()


def test_corrupt_glb_is_rejected(tmp_path):
    bad = tmp_path / "bad.glb"
    bad.write_bytes(b"glTF" + b"\x00" * 40)
    with pytest.raises(MeshError) as e:
        mesh_io.load_mesh(bad)
    assert e.value.code == "corrupt_glb"
    notglb = tmp_path / "x.glb"
    notglb.write_bytes(b"hello world, definitely not glb")
    with pytest.raises(MeshError):
        mesh_io.load_mesh(notglb)
    empty = tmp_path / "e.glb"
    empty.write_bytes(b"")
    with pytest.raises(MeshError):
        mesh_io.load_mesh(empty)
    with pytest.raises(MeshError) as e:
        mesh_io.load_mesh(tmp_path / "missing.glb")
    assert e.value.code == "file_missing"


def test_inspection_of_clean_demo(demo_mesh):
    r = mi.inspect_mesh(demo_mesh)
    assert r.watertight and r.components == 1 and r.flat_bottom_ok
    assert r.status in (mi.GOOD, mi.CHECK)
    assert abs(r.extents_mm[2] - 120) < 0.1


def test_repair_handles_holes_fragments_degenerates(demo_mesh):
    f = demo_mesh.faces[:-6]
    m = mesh_io.make_mesh(demo_mesh.vertices, f, mesh_io.get_face_colors(demo_mesh)[:-6])
    junk = trimesh.creation.icosphere(subdivisions=1, radius=0.3); junk.apply_translation([30, 30, 50])
    V = np.vstack([m.vertices, junk.vertices]); F = np.vstack([m.faces, junk.faces + len(m.vertices)])
    bad = mesh_io.make_mesh(V, F, np.vstack([mesh_io.get_face_colors(m), np.full((len(junk.faces), 3), 99, np.uint8)]))
    before = mi.inspect_mesh(bad, with_thin=False)
    assert before.boundary_edges > 0 and before.tiny_components == 1
    fixed, acts = mesh_repair.safe_repair(bad, PrintOptions())
    codes = {a.code for a in acts}
    assert {"fragments", "holes_filled"} <= codes
    after = mi.inspect_mesh(fixed, with_thin=False)
    assert after.watertight and after.components == 1
    assert len(mesh_io.get_face_colors(fixed)) == len(fixed.faces)


def test_repair_never_mutates_input(demo_mesh):
    snapshot = demo_mesh.vertices.copy(), demo_mesh.faces.copy()
    mesh_repair.safe_repair(demo_mesh, PrintOptions())
    assert (demo_mesh.vertices == snapshot[0]).all() and (demo_mesh.faces == snapshot[1]).all()


def test_large_hole_is_left_alone():
    s = trimesh.creation.icosphere(subdivisions=3, radius=10)
    keep = s.vertices[s.faces].mean(axis=1)[:, 2] < 3          # open the whole top
    m = mesh_io.make_mesh(s.vertices, s.faces[keep])
    fixed, acts = mesh_repair.safe_repair(m, PrintOptions())
    assert "holes_left" in {a.code for a in acts}
    assert not mi.inspect_mesh(fixed, with_thin=False).watertight


def test_scale_and_ground(demo_mesh):
    m = mesh_io.copy_mesh(demo_mesh)
    mesh_print.fit_to_height(m, 80.0)
    assert abs(m.extents[2] - 80.0) < 1e-6 and abs(m.bounds[0][2]) < 1e-9


def test_flat_bottom_plan_apply_and_preview_only_plans():
    s = trimesh.creation.icosphere(subdivisions=4, radius=40)
    mesh_io.set_face_colors(s, np.full((len(s.faces), 3), 180, np.uint8)); mesh_print.ground(s)
    z0 = s.bounds.copy()
    plan = mesh_print.plan_flat_bottom(s)
    assert plan.needed and plan.cut_z > 0
    assert np.allclose(s.bounds, z0)                         # planning never modifies the mesh
    out, ok = mesh_print.apply_flat_bottom(s, plan)
    assert ok and out.is_watertight and mi.inspect_mesh(out, with_thin=False).flat_bottom_ok
    assert out.extents[2] > 0.9 * s.extents[2]               # did not cut large parts away


def test_base_variants(demo_mesh):
    for shape in ("round", "oval", "square", "custom"):
        o = PrintOptions(base_shape=shape)
        out, how = mesh_print.add_base(demo_mesh, o)
        assert how == "union" and out.is_watertight
        assert out.extents[2] > demo_mesh.extents[2] + 2.0   # thickness 3mm minus 0.3 overlap
        assert abs(out.bounds[0][2]) < 1e-6
    assert mesh_print.make_base(demo_mesh, PrintOptions(base_shape="none")) is None


def test_thin_analysis_flags_thin_plate():
    plate = trimesh.creation.box(extents=[40, 40, 0.5])
    t = mi.analyze_thin(plate, 1.2)
    assert t is not None and t.ratio > 0.5
    block = trimesh.creation.box(extents=[20, 20, 20])
    assert mi.analyze_thin(block, 1.2).ratio < 0.05
