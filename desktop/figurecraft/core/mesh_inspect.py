"""Mesh inspection. Results are heuristics - the final check always belongs to the slicer."""
from __future__ import annotations

from dataclasses import asdict, dataclass, field

import numpy as np
import trimesh
from scipy import ndimage

from ..logging_setup import get_logger
from . import mesh_io
from .models import PrintOptions

log = get_logger("inspect")

GOOD, CHECK, FAILED = "GOOD", "CHECK", "FAILED"


@dataclass
class Issue:
    code: str                    # i18n key suffix: issue.<code>
    severity: str                # info | warn | error
    params: dict = field(default_factory=dict)


@dataclass
class InspectionReport:
    status: str = GOOD
    vertices: int = 0
    triangles: int = 0
    extents_mm: tuple = (0.0, 0.0, 0.0)
    volume_mm3: float | None = None
    watertight: bool = False
    components: int = 0
    degenerate_faces: int = 0
    duplicate_faces: int = 0
    non_manifold_edges: int = 0
    boundary_edges: int = 0
    tiny_components: int = 0
    floating_components: int = 0
    thin_ratio: float | None = None
    thin_spots: int = 0
    flat_contact_area_mm2: float = 0.0
    flat_bottom_ok: bool = False
    overhang_ratio: float = 0.0
    issues: list[Issue] = field(default_factory=list)

    def to_dict(self) -> dict:
        return asdict(self)

    @classmethod
    def from_dict(cls, d: dict) -> "InspectionReport":
        d = dict(d)
        d["issues"] = [Issue(**i) for i in d.get("issues", [])]
        d["extents_mm"] = tuple(d.get("extents_mm", (0, 0, 0)))
        return cls(**d)


# ------------------------------------------------------------ primitives
def degenerate_mask(mesh: trimesh.Trimesh, eps: float = 1e-10) -> np.ndarray:
    ext = float(np.max(mesh.extents)) if len(mesh.vertices) else 1.0
    return mesh.area_faces <= eps * max(ext, 1.0) ** 2


def duplicate_face_mask(mesh: trimesh.Trimesh) -> np.ndarray:
    key = np.sort(mesh.faces, axis=1)
    _, first = np.unique(key, axis=0, return_index=True)
    mask = np.ones(len(key), bool)
    mask[first] = False
    return mask


def edge_stats(mesh: trimesh.Trimesh) -> tuple[int, int]:
    """(boundary edges, non-manifold edges) based on welded vertex indices."""
    e = np.sort(mesh.edges, axis=1)
    _, counts = np.unique(e, axis=0, return_counts=True)
    return int((counts == 1).sum()), int((counts > 2).sum())


def component_info(mesh: trimesh.Trimesh):
    labels = mesh_io.sort_components_by_size(mesh_io.face_components(mesh), mesh)
    n = int(labels.max()) + 1 if len(labels) else 0
    return labels, n


def component_boxes(mesh: trimesh.Trimesh, labels: np.ndarray, n: int):
    tri = mesh.triangles
    lo = np.full((n, 3), np.inf)
    hi = np.full((n, 3), -np.inf)
    mins, maxs = tri.min(axis=1), tri.max(axis=1)
    for k in range(3):
        np.minimum.at(lo[:, k], labels, mins[:, k])
        np.maximum.at(hi[:, k], labels, maxs[:, k])
    return lo, hi


def classify_components(mesh: trimesh.Trimesh, labels: np.ndarray, n: int, min_feature: float):
    """Split secondary components into 'tiny fragments' and 'floating bodies' (not touching the main body)."""
    if n <= 1:
        return [], []
    lo, hi = component_boxes(mesh, labels, n)
    diag = np.linalg.norm(hi - lo, axis=1)
    main_diag = diag[0]
    pad = max(0.3, min_feature * 0.25)
    tiny, floating = [], []
    for c in range(1, n):
        overlaps = bool(np.all(lo[c] <= hi[0] + pad) and np.all(hi[c] >= lo[0] - pad))
        if diag[c] < max(min_feature * 1.5, 0.01 * main_diag):
            tiny.append(c)
        elif not overlaps:
            floating.append(c)
    return tiny, floating


# --------------------------------------------------------------- thinness
@dataclass
class ThinAnalysis:
    ratio: float
    spots: int
    pitch: float
    origin: np.ndarray
    thin: np.ndarray        # bool grid
    reliable: bool


def voxelize_solid(mesh: trimesh.Trimesh, pitch: float, pad: int = 4, closing: int = 1):
    """Fast solid voxelization: dense surface sampling -> closing -> flood-fill. Returns (grid, origin)."""
    origin = np.asarray(mesh.bounds[0]) - pad * pitch
    shape = np.ceil((np.asarray(mesh.bounds[1]) - origin) / pitch).astype(int) + pad
    n = int(min(8_000_000, max(20_000, mesh.area / (0.22 * pitch) ** 2)))
    pts, _ = trimesh.sample.sample_surface(mesh, n, seed=3)
    pts = np.vstack([pts, mesh.vertices])
    idx = np.floor((pts - origin) / pitch).astype(int)
    idx = np.clip(idx, 0, shape - 1)
    surf = np.zeros(tuple(shape), bool)
    surf[idx[:, 0], idx[:, 1], idx[:, 2]] = True
    surf = ndimage.binary_closing(surf, structure=ndimage.generate_binary_structure(3, 3), iterations=closing)
    solid = ndimage.binary_fill_holes(surf)
    return solid, origin


def analyze_thin(mesh: trimesh.Trimesh, min_feature: float, max_cells: int = 4_000_000) -> ThinAnalysis | None:
    """Approximate local thickness with a morphological opening on a voxel grid (ball r = min_feature/2)."""
    try:
        ext = np.maximum(mesh.extents, 1e-6)
        pitch = max(min_feature / 4.0, float(np.cbrt(np.prod(ext) / max_cells)))
        if pitch > min_feature / 2.0:          # grid too coarse to say anything about thin parts
            return None
        work = mesh
        if len(mesh.faces) > 300_000:
            try:
                import fast_simplification as fs
                v, f = fs.simplify(np.asarray(mesh.vertices, np.float32), np.asarray(mesh.faces, np.int32),
                                   target_count=250_000)
                work = trimesh.Trimesh(v, f, process=False)
            except Exception:
                pass
        try:
            vol_mesh = abs(float(work.volume))
        except Exception:
            vol_mesh = 0.0
        grid = origin = None
        for closing in (1, 2):                  # a leaky shell fails to fill; close harder once, then give up
            g, o = voxelize_solid(work, pitch, closing=closing)
            vol_vox = float(g.sum() * pitch ** 3)
            if vol_mesh <= 0 or 0.8 < vol_vox / vol_mesh < 1.35:
                grid, origin = g, o
                break
        if grid is None:
            log.info("thin analysis skipped: voxelisation did not match mesh volume")
            return None
        r = max(1.0, min_feature / 2.0 / pitch)
        d = ndimage.distance_transform_edt(grid)
        core = d > r
        opened = ndimage.distance_transform_edt(~core) <= r
        thin = grid & ~opened
        lab, nspots = ndimage.label(thin)
        sizes = np.bincount(lab.ravel())[1:] if nspots else np.array([])
        significant = int((sizes * pitch ** 3 > (min_feature ** 3) * 0.5).sum())
        ratio = float(thin.sum() / max(grid.sum(), 1))
        return ThinAnalysis(ratio, significant, pitch, origin, thin, True)
    except Exception as exc:
        log.warning("thin analysis skipped: %s", exc)
        return None


# ------------------------------------------------------------- main entry
def inspect_mesh(mesh: trimesh.Trimesh, opts: PrintOptions | None = None, with_thin: bool = True) -> InspectionReport:
    opts = opts or PrintOptions()
    r = InspectionReport()
    issues = r.issues
    if mesh is None or len(mesh.faces) == 0:
        r.status = FAILED
        issues.append(Issue("empty", "error"))
        return r

    r.vertices, r.triangles = int(len(mesh.vertices)), int(len(mesh.faces))
    r.extents_mm = tuple(float(x) for x in mesh.extents)
    r.degenerate_faces = int(degenerate_mask(mesh).sum())
    r.duplicate_faces = int(duplicate_face_mask(mesh).sum())
    r.boundary_edges, r.non_manifold_edges = edge_stats(mesh)
    r.watertight = bool(r.boundary_edges == 0 and r.non_manifold_edges == 0)
    if r.watertight:
        try:
            r.volume_mm3 = abs(float(mesh.volume))
        except Exception:
            r.volume_mm3 = None

    labels, n = component_info(mesh)
    r.components = n
    tiny, floating = classify_components(mesh, labels, n, opts.min_feature_mm)
    r.tiny_components, r.floating_components = len(tiny), len(floating)

    # flat-bottom contact patch
    zmin = float(mesh.bounds[0][2])
    nrm = mesh.face_normals
    low = (mesh.triangles[:, :, 2].max(axis=1) <= zmin + 0.25) & (nrm[:, 2] < -0.9)
    r.flat_contact_area_mm2 = float(mesh.area_faces[low].sum())
    footprint = float(mesh.extents[0] * mesh.extents[1])
    r.flat_bottom_ok = r.flat_contact_area_mm2 >= max(20.0, 0.04 * footprint)

    # overhang estimate (faces > 45 deg past vertical, above the bed)
    above = mesh.triangles[:, :, 2].min(axis=1) > zmin + 0.4
    over = (nrm[:, 2] < -0.7071) & above
    r.overhang_ratio = float(mesh.area_faces[over].sum() / max(mesh.area, 1e-9))

    # ---- issues
    if min(r.extents_mm) < 1e-6 or r.triangles < 12:
        issues.append(Issue("degenerate_model", "error"))
    if r.degenerate_faces:
        sev = "warn" if r.degenerate_faces > 0.002 * r.triangles else "info"
        issues.append(Issue("degenerate_faces", sev, {"n": r.degenerate_faces}))
    if r.duplicate_faces:
        issues.append(Issue("duplicate_faces", "warn", {"n": r.duplicate_faces}))
    if r.non_manifold_edges:
        sev = "error" if r.non_manifold_edges > 0.02 * len(mesh.edges) else "warn"
        issues.append(Issue("non_manifold", sev, {"n": r.non_manifold_edges}))
    if r.boundary_edges:
        issues.append(Issue("open_edges", "warn", {"n": r.boundary_edges}))
    if not r.watertight and not r.boundary_edges and not r.non_manifold_edges:
        issues.append(Issue("not_watertight", "warn"))
    if r.tiny_components:
        issues.append(Issue("tiny_components", "warn", {"n": r.tiny_components}))
    if r.floating_components:
        issues.append(Issue("floating", "warn", {"n": r.floating_components}))
    if r.components > 1 and not (r.tiny_components or r.floating_components):
        issues.append(Issue("multi_component", "info", {"n": r.components}))
    if not r.flat_bottom_ok:
        issues.append(Issue("flat_bottom_missing", "warn", {"area": round(r.flat_contact_area_mm2, 1)}))
    if r.overhang_ratio > 0.18:
        issues.append(Issue("overhangs", "info", {"pct": round(100 * r.overhang_ratio)}))
    if with_thin and r.watertight:
        t = analyze_thin(mesh, opts.min_feature_mm)
        if t is not None:
            r.thin_ratio, r.thin_spots = t.ratio, t.spots
            if t.spots >= 1 and t.ratio > 0.02:
                issues.append(Issue("thin_features", "warn", {"n": t.spots, "pct": round(100 * t.ratio, 1),
                                                              "mm": opts.min_feature_mm}))
    elif with_thin and not r.watertight:
        issues.append(Issue("thin_skipped", "info"))

    sev = {i.severity for i in issues}
    r.status = FAILED if "error" in sev else (CHECK if "warn" in sev else GOOD)
    return r
