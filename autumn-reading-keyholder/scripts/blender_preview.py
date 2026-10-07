"""
Blender 헤드리스 미리보기/조립 스크립트
---------------------------------------
OpenSCAD 로 뽑은 색상별 STL 4장을 불러와 색 재질을 입히고, 레퍼런스 이미지와 같은 시점
(정면 직교 · 1 레퍼런스px = 1 렌더px) / 3/4 시점 / 프린트 베드 배치도를 렌더링합니다.
같은 장면을 .blend 로도 저장하므로 Blender 에서 열어 바로 손볼 수 있습니다.

사용:
  blender -b --factory-startup -P scripts/blender_preview.py -- <입력 STL 접두사> <출력 폴더> [view ...]
  예) blender -b --factory-startup -P scripts/blender_preview.py -- out/asm out/preview front iso
      blender -b --factory-startup -P scripts/blender_preview.py -- out/bed out/preview bed
view : front(정면 직교, 레퍼런스 대조용) | iso(3/4 시점) | side(옆모습) | back(뒷면·키홀) | bed(베드 배치 위에서)
"""
import bpy, sys, os, math
from mathutils import Vector, Matrix

# ---- 모델 상수 (autumn_keyholder.scad 와 동일해야 함) ----
S, X0, Y0 = 0.205, 288, 665
REF_W, REF_H = 1448, 1086                     # 레퍼런스 이미지 크기(px)
COLORS = {"brown": "#5a3825", "cream": "#f3e2bd", "orange": "#e8731d", "green": "#2f6b3c"}
BED = (256, 256)

argv = sys.argv[sys.argv.index("--") + 1:]
prefix, outdir = argv[0], argv[1]
views = argv[2:] or ["front", "iso"]
os.makedirs(outdir, exist_ok=True)


def srgb2lin(c):
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def hex2lin(h):
    h = h.lstrip("#")
    return tuple(srgb2lin(int(h[i:i + 2], 16) / 255) for i in (0, 2, 4)) + (1.0,)


# ---------- 장면 초기화 ----------
bpy.ops.wm.read_factory_settings(use_empty=True)
scene = bpy.context.scene
objs = {}
for name, hexcol in COLORS.items():
    path = f"{prefix}_{name}.stl"
    if not os.path.exists(path):
        print("missing", path)
        continue
    bpy.ops.wm.stl_import(filepath=path)
    ob = bpy.context.selected_objects[0]
    ob.name = name
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes["Principled BSDF"]
    bsdf.inputs["Base Color"].default_value = hex2lin(hexcol)
    bsdf.inputs["Roughness"].default_value = 0.42
    bsdf.inputs["Specular IOR Level"].default_value = 0.35
    mat.diffuse_color = hex2lin(hexcol)
    ob.data.materials.append(mat)
    bpy.ops.object.shade_smooth()
    try:                                   # 곡면(고양이·컵)은 부드럽게, 각진 모서리는 유지
        ob.data.use_auto_smooth = True
        ob.data.auto_smooth_angle = math.radians(32)
    except Exception:
        pass
    objs[name] = ob

# 전체 경계 상자
mins = Vector((1e9,) * 3)
maxs = Vector((-1e9,) * 3)
for ob in objs.values():
    for v in ob.bound_box:
        w = ob.matrix_world @ Vector(v)
        mins = Vector(map(min, mins, w))
        maxs = Vector(map(max, maxs, w))
center = (mins + maxs) / 2
size = maxs - mins
print("BBOX", [round(x, 2) for x in mins], [round(x, 2) for x in maxs], "size", [round(x, 2) for x in size])

# ---------- 렌더 설정 ----------
scene.render.engine = "CYCLES"
scene.cycles.device = "CPU"
scene.cycles.samples = 72          # 배포 빌드에는 OIDN 디노이저가 없어 샘플 수로 노이즈를 줄임
scene.cycles.use_denoising = False
scene.cycles.use_adaptive_sampling = True
scene.render.film_transparent = False
scene.view_settings.view_transform = "Standard"
world = bpy.data.worlds.new("w")
scene.world = world
world.use_nodes = True
bg = world.node_tree.nodes["Background"]
bg.inputs["Color"].default_value = hex2lin("#e9dccb")
bg.inputs["Strength"].default_value = 0.9


def add_light(kind, loc, energy, rot=None, size=None):
    data = bpy.data.lights.new(kind, kind)
    data.energy = energy
    if size and kind == "AREA":
        data.size = size
    ob = bpy.data.objects.new(kind, data)
    ob.location = loc
    if rot:
        ob.rotation_euler = [math.radians(a) for a in rot]
    scene.collection.objects.link(ob)
    return ob


def make_camera(name, loc, target, ortho=None, lens=60):
    cam = bpy.data.cameras.new(name)
    if ortho:
        cam.type = "ORTHO"
        cam.ortho_scale = ortho
    else:
        cam.lens = lens
    ob = bpy.data.objects.new(name, cam)
    ob.location = loc
    scene.collection.objects.link(ob)
    # 모델의 위쪽(+Y)을 화면 위로 고정 (to_track_quat 은 월드 Z 를 위로 잡아서 회전됨)
    fwd = (Vector(target) - Vector(loc)).normalized()
    right = fwd.cross(Vector((0, 1, 0))).normalized()
    cup = right.cross(fwd).normalized()
    ob.rotation_euler = Matrix(((right.x, cup.x, -fwd.x), (right.y, cup.y, -fwd.y), (right.z, cup.z, -fwd.z))).to_euler()
    return ob


def render(cam, fname, w, h):
    scene.camera = cam
    scene.render.resolution_x, scene.render.resolution_y = w, h
    scene.render.resolution_percentage = 100
    scene.render.filepath = os.path.join(outdir, fname)
    bpy.ops.render.render(write_still=True)
    print("WROTE", scene.render.filepath)


def clear_lights():
    for ob in [o for o in scene.objects if o.type == "LIGHT"]:
        bpy.data.objects.remove(ob, do_unlink=True)


for view in views:
    clear_lights()
    if view == "front":
        # 레퍼런스 이미지의 픽셀 격자와 정확히 겹치는 정면 직교 뷰
        add_light("SUN", (0, 0, 300), 3.2, rot=(18, -22, 0))
        add_light("AREA", (-60, 140, 260), 22000, size=160)
        cx, cy = (724 - X0) * S, (Y0 - 543) * S
        cam = make_camera("front", (cx, cy, 400), (cx, cy, 0), ortho=REF_W * S)
        render(cam, "front_ortho.png", REF_W, REF_H)
    elif view == "iso":
        add_light("SUN", (0, 0, 300), 3.0, rot=(30, -35, 0))
        add_light("AREA", (-120, 200, 220), 26000, size=200)
        tgt = (center.x + 2, center.y + 4, 8)
        # 구면 좌표: 방위 az(왼쪽으로), 고각 el(위에서), 거리 D
        az, el, D = math.radians(32), math.radians(17), 405
        pos = (tgt[0] - D * math.sin(az) * math.cos(el), tgt[1] + D * math.sin(el), tgt[2] + D * math.cos(az) * math.cos(el))
        cam = make_camera("iso", pos, tgt, lens=54)
        render(cam, "iso_view.png", 1600, 1200)
    elif view == "side":
        add_light("SUN", (0, 0, 300), 3.0, rot=(30, -70, 0))
        add_light("AREA", (150, 100, 200), 20000, size=200)
        cam = make_camera("side", (center.x + 260, center.y - 10, 110), (center.x, center.y, 8), lens=85)
        render(cam, "side_view.png", 1400, 900)
    elif view == "back":
        # 뒷면(벽에 닿는 면): 키홀 확인용
        add_light("SUN", (0, 0, -300), 3.2, rot=(180 - 20, 0, 0))
        add_light("AREA", (center.x, center.y, -300), 30000, size=250)
        cam = make_camera("back", (center.x, center.y, -500), (center.x, center.y, 0), ortho=max(size.x, size.y) * 1.08)
        render(cam, "back_view.png", 1400, 1200)
    elif view == "bed":
        # 베드 배치: 256x256 판 위에 올려 위에서 내려다봄
        bpy.ops.mesh.primitive_plane_add(size=1, location=(BED[0] / 2, BED[1] / 2, -0.05))
        plate = bpy.context.active_object
        plate.scale = (BED[0], BED[1], 1)
        pm = bpy.data.materials.new("plate")
        pm.use_nodes = True
        pm.node_tree.nodes["Principled BSDF"].inputs["Base Color"].default_value = hex2lin("#3b3f45")
        plate.data.materials.append(pm)
        add_light("SUN", (0, 0, 300), 3.4, rot=(15, -15, 0))
        add_light("AREA", (BED[0] / 2, BED[1] / 2, 400), 40000, size=300)
        cam = make_camera("bed", (BED[0] / 2, BED[1] / 2, 600), (BED[0] / 2, BED[1] / 2, 0), ortho=BED[0] * 1.06)
        render(cam, "bed_layout.png", 1400, 1400)
        bpy.data.objects.remove(plate, do_unlink=True)

bpy.ops.wm.save_as_mainfile(filepath=os.path.join(outdir, "autumn_keyholder_preview.blend"))
