import numpy as np

from figurecraft.core import color, mesh_io
from figurecraft.core.models import PaletteEntry


def _noisy(mesh, seed=0):
    rng = np.random.default_rng(seed)
    fc = mesh_io.get_face_colors(mesh)
    out = np.clip(fc * rng.normal(1.0, 0.12, (len(fc), 1)) + rng.normal(0, 8, fc.shape), 0, 255).astype(np.uint8)
    m = mesh_io.copy_mesh(mesh)
    return mesh_io.set_face_colors(m, out)


def test_lab_roundtrip():
    rgb = np.array([[0, 0, 0], [255, 255, 255], [122, 63, 196], [255, 210, 31]], np.uint8)
    assert np.abs(color.lab_to_srgb(color.srgb_to_lab(rgb)).astype(int) - rgb.astype(int)).max() <= 1


def test_auto_palette_recovers_expected_colors_from_noisy_texture(demo_mesh):
    r = color.reduce_colors(_noisy(demo_mesh), 4)
    names = [p.name for p in r.palette]
    assert names == ["IVORY", "PURPLE", "BLACK", "YELLOW"]
    truth = color.assign_labels(mesh_io.get_face_colors(demo_mesh), r.palette)
    assert (truth == r.labels).mean() > 0.99


def test_prompt_hints_snap_to_exact_colors(demo_mesh):
    hints = [("IVORY", "#F2EDE0"), ("PURPLE", "#7A3FC4"), ("BLACK", "#111111"), ("YELLOW", "#FFD21F")]
    r = color.reduce_colors(_noisy(demo_mesh), 4, hints=hints)
    assert [p.hex for p in r.palette] == ["#F2EDE0", "#7A3FC4", "#111111", "#FFD21F"]


def test_k_limits_and_monotonic(demo_mesh):
    m = _noisy(demo_mesh)
    sizes = [len(color.reduce_colors(m, k).palette) for k in (1, 2, 3, 4)]
    assert sizes == [1, 2, 3, 4]
    assert len(color.reduce_colors(m, 9).palette) <= 4


def test_fewer_colors_than_requested_when_model_has_fewer(demo_mesh):
    one = mesh_io.copy_mesh(demo_mesh)
    mesh_io.set_face_colors(one, np.tile(np.array([10, 200, 90], np.uint8), (len(one.faces), 1)))
    assert len(color.reduce_colors(one, 4).palette) == 1


def test_custom_palette_assignment(demo_mesh):
    r = color.reduce_colors(demo_mesh, 2, "custom", ["#FFFFFF", "#000000"])
    assert [p.hex for p in r.palette] == ["#FFFFFF", "#000000"]
    assert set(np.unique(r.labels)) <= {0, 1}


def test_regions_edit_delete_merge(demo_mesh):
    r = color.reduce_colors(demo_mesh, 4)
    reg = color.label_regions(demo_mesh, r.labels)
    assert reg.max() + 1 >= 4
    labels, pal = color.merge_colors(r.labels, r.palette, 3, 0)
    assert len(pal) == 3 and labels.max() == 2
    labels2, pal2 = color.delete_color(r.labels, r.palette, 2, mesh_io.get_face_colors(demo_mesh))
    assert len(pal2) == 3 and labels2.max() == 2 and len(labels2) == len(r.labels)


def test_island_removal_merges_tiny_speckles(demo_mesh):
    r = color.reduce_colors(demo_mesh, 2)
    lab = r.labels.copy(); lab[100] = 1 - lab[100]               # single-triangle speckle
    cleaned = color.remove_islands(demo_mesh, lab, 3 * float(demo_mesh.area_faces[100]))
    assert cleaned[100] == r.labels[100]


def test_names_are_unique():
    pal = [PaletteEntry("#FFFFFF", "WHITE"), PaletteEntry("#FEFEFE", "WHITE")]
    color._unique_names(pal)
    assert pal[0].name != pal[1].name


def test_noisy_three_colour_texture_does_not_waste_a_fourth_slot():
    rng = np.random.default_rng(0)
    n = 20000
    base = np.vstack([np.tile([122, 63, 196], (n, 1)), np.tile([242, 237, 224], (n, 1)), np.tile([17, 17, 17], (n, 1))]).astype(float)
    cols = np.clip(base + rng.normal(0, 18, base.shape), 0, 255).astype(np.uint8)
    pal = color.extract_palette(cols, np.ones(len(cols)), 4)
    assert len(pal) == 3
    assert {p.name for p in pal} & {"PURPLE", "LAVENDER"} and {p.name for p in pal} & {"BLACK", "GRAY", "NAVY"}
