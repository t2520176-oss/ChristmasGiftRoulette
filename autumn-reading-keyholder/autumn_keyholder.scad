// ============================================================================
//  가을 독서 벽걸이 키홀더  /  Autumn Reading Key Holder
//  - 4색(갈색·크림·주황·초록) 한 베드 멀티컬러 프린트용 파라메트릭 모델
//  - OpenSCAD 2021.01 이상 (CGAL 기준으로 작성, Manifold 최신 빌드면 훨씬 빠름)
//
//  색상별 STL 출력 (모두 같은 원점이라 슬라이서에서 그대로 겹쳐 올리면 됨)
//    openscad -D 'PART="brown"'  -o out/bed_brown.stl  autumn_keyholder.scad
//    openscad -D 'PART="cream"'  -o out/bed_cream.stl  autumn_keyholder.scad
//    openscad -D 'PART="orange"' -o out/bed_orange.stl autumn_keyholder.scad
//    openscad -D 'PART="green"'  -o out/bed_green.stl  autumn_keyholder.scad
//
//  PART      : "all"(F5 미리보기, 색 표시) | "brown" | "cream" | "orange" | "green"
//  ASSEMBLED : false = 프린트 베드 배치(본체 + 후크 4개 눕힘), true = 후크를 끼운 완성품
//
//  좌표계 메모
//   · 형상은 레퍼런스 이미지의 픽셀 좌표(px, y 아래로 증가)로 그리고 px() 에서 mm 로 변환
//   · 원점(0,0) = 선반 앞판 왼쪽 아래 모서리, +x 오른쪽, +y 위, +z 벽에서 앞쪽
//   · 뒷면(z=0)이 베드에 닿도록 엎어 놓고 출력 → 벽에 닿는 면이 완전 평면
// ============================================================================

PART      = "all";
ASSEMBLED = false;
FLAT      = false;     // 디버그용: z 없이 px 좌표 2D 로 출력 (이미지 대조용)

// ------------------------------- 크기/스케일 --------------------------------
S   = 0.205;           // mm / 레퍼런스 px  (전체 폭 ≈ 196mm)
X0  = 288;             // 선반 왼쪽 끝 (px)
Y0  = 665;             // 선반 아래 끝 (px)
function X(p) = (p - X0) * S;
function Y(p) = (Y0 - p) * S;

// ------------------------------- 높이(Z) 설정 -------------------------------
ZB       = 2.4;        // 갈색 뒷판 두께
Z_WALL   = 5.0;
Z_WAIN   = 4.6;
Z_FBACK  = 4.0;
Z_NOOK   = 3.6;
Z_LEAN   = 6.4;
Z_PIER   = 5.6;
Z_CAP    = 7.8;
Z_ROOF   = 7.4;
Z_GLASS  = 3.8;
Z_MUN    = 6.0;
Z_FRAME  = 7.2;
Z_LANT   = 8.4;
Z_TWIG   = 8.6;
Z_STEM   = 9.0;
Z_FENCE  = 8.0;
Z_BOOK   = 12.0;
Z_PAGES  = 10.8;
Z_CUP    = 13.0;
Z_LIP    = 14.0;       // 선반 윗턱
Z_BORDER = 13.0;       // 선반 앞판 테두리
Z_PLANK  = 11.5;       // 선반 앞판 속널(후크가 꽂히는 면)
TIER_Z   = [8.6, 9.4, 10.2, 11.0, 11.8, 12.8, 13.6, 14.4];   // 잎사귀 높이 단계

// ------------------------------- 후크(분리 부품) ----------------------------
HK_X       = [412, 618, 828, 1045];   // 후크 위치 (px)
HK_YC      = 616;                      // 후크 중심 높이 (px)
HK_W       = 8;                        // 후크 폭 (프린트 시 두께)
HK_TENON_L = 8.0;                      // 장부 길이
HK_TENON_H = 9.0;                      // 장부 높이
HK_CLR     = 0.2;                      // 장부 한쪽 여유 (끼움 공차)
HK_ARM     = 6.5;                      // 팔 굵기
HK_REACH   = 22;                       // 팔이 앞으로 나오는 길이

// ------------------------------- 키홀(벽걸이) -------------------------------
KH_X     = [520, 930];   // 키홀 위치 (px)
KH_Y     = 628;          // 입구 원 중심 (px)
KH_HEAD  = 8.5;          // 나사머리 통과 구멍 지름
KH_SLOT  = 4.2;          // 나사목 슬롯 폭 (뒷판 부분)
KH_POCK  = 7.6;          // 머리가 들어가는 안쪽 포켓 폭
KH_LEN   = 9;            // 슬롯 길이 (위쪽으로)
KH_Z     = 5.8;          // 포켓 상단 높이

// ------------------------------- 위쪽 못걸이 (균형점) ------------------------
// 무게중심 바로 위에 두면 못 1개만으로도 수평으로 걸립니다. 지붕 띠는 얇아서 포켓을 못 넣으므로
// 지붕 아래 박공 벽에 둥근 다락창(두꺼운 부분)을 올리고 그 뒷면에 못걸이를 팝니다.
NAIL_X    = 750;      // px — 무게중심 x (본체+후크 STL 부피중심 x=94.8mm 를 px 로 환산)
NAIL_HEAD = 6.4;      // 못머리가 들어가는 입구 원 지름 (mm)
NAIL_NECK = 3.4;      // 못 몸통이 지나는 슬롯 폭 (뒷판 부분)
NAIL_POCK = 5.6;      // 못머리가 앉는 안쪽 포켓 폭
NAIL_LEN  = 3.0;      // 슬롯 길이(위로)
NAIL_Z    = 4.6;      // 포켓 천장 높이 (못머리 두께 ≤ 2.0mm)

// ------------------------------- 베드 배치 ----------------------------------
BED      = [256, 256];
BED_M    = 6;                        // 베드 가장자리 여유
PL_K     = S / 0.205;                // S 를 바꿔도 배치 치수가 같이 스케일되도록
PL_MIN   = PL_K * [-5.63, -34.3];    // 본체 외곽 최소 x,y (mm)  ← STL 측정값 (S=0.205 기준)
PL_SIZE  = PL_K * [204.3, 165.9];    // 본체 외곽 크기 (mm)
PL_OFF   = [BED_M - PL_MIN[0], BED_M - PL_MIN[1]];
HK_GAP   = 6;

// ------------------------------- 색상 ---------------------------------------
C_BROWN  = "#5a3825";
C_CREAM  = "#f3e2bd";
C_ORANGE = "#e8731d";
C_GREEN  = "#2f6b3c";

// ============================================================================
//  기본 변환 / 압출
// ============================================================================
module px() { multmatrix([[S,0,0,-X0*S],[0,-S,0,Y0*S],[0,0,1,0],[0,0,0,1]]) children(); }

// z=a..b 로 압출 (자식은 px 좌표 2D)
module ext(a, b) {
  if (FLAT) children();
  else translate([0,0,a]) linear_extrude(height = b - a) px() children();
}
// 3D 자르기용 기둥 (z=a 부터 h 높이)
module prismZ(a, h) { translate([0,0,a]) linear_extrude(height = h) px() children(); }

// ============================================================================
//  2D 기본 도형 (px)
// ============================================================================
module rrect(x0, y0, x1, y1, r = 0) {
  if (r > 0) hull() for (p = [[x0+r,y0+r],[x1-r,y0+r],[x0+r,y1-r],[x1-r,y1-r]]) translate(p) circle(r = r, $fn = 24);
  else translate([x0,y0]) square([x1-x0, y1-y0]);
}
module ell2(c, rx, ry, rot = 0) { translate(c) rotate(rot) scale([rx, ry]) circle(r = 1, $fn = 48); }
module line2(p0, p1, w) { hull() { translate(p0) circle(d = w, $fn = 14); translate(p1) circle(d = w, $fn = 14); } }
module polyline(p, w) { for (i = [0 : len(p) - 2]) line2(p[i], p[i+1], w); }
module arch(xc, ys, r, yb) { translate([xc,ys]) circle(r = r, $fn = 96); translate([xc-r, ys]) square([2*r, yb - ys]); }
module above_shelf() { translate([-100,-100]) square([2000, 648]); }    // 선반 윗면(y=548) 위쪽만

// ============================================================================
//  단풍/담쟁이 잎 (극좌표 생성)
// ============================================================================
// 연(kite) 조각을 겹쳐 만든 단풍잎/담쟁이잎 (단위 크기, y 위쪽 기준)
module kite(L, w, a = 0, p = [0, 0]) { translate(p) rotate(a) polygon([[0,0],[w*L,0.42*L],[0,L],[-w*L,0.42*L]]); }
function lobe_pt(a, d) = [-sin(a) * d, cos(a) * d];            // 엽축 위의 점
module maple_unit() {
  circle(r = 0.30, $fn = 24);
  kite(1.00, 0.27, 0);
  for (s = [-1, 1]) {
    kite(0.42, 0.30, s*36, [0, 0.40]);                          // 윗 잎 톱니
    kite(0.88, 0.28, s*52);                                     // 옆 잎
    kite(0.40, 0.30, s*(52 - 40), lobe_pt(s*52, 0.50));         // 옆 잎 안쪽 톱니
    kite(0.36, 0.30, s*(52 + 42), lobe_pt(s*52, 0.42));         // 옆 잎 바깥 톱니
    kite(0.62, 0.30, s*104);                                    // 아래 잎
    kite(0.30, 0.30, s*(104 - 38), lobe_pt(s*104, 0.34));
  }
}
module ivy_unit() {
  circle(r = 0.30, $fn = 24);
  kite(1.00, 0.30, 0);
  for (s = [-1, 1]) { kite(0.74, 0.30, s*58); translate([s*0.20, -0.10]) circle(r = 0.28, $fn = 20); }
}
MAPLE_AX = [[0, 1.00], [52, 0.88], [-52, 0.88], [104, 0.62], [-104, 0.62]];
IVY_AX   = [[0, 1.00], [58, 0.74], [-58, 0.74]];

module leaf_shape(R, kind) {
  scale([R, -R]) { if (kind == 0) maple_unit(); else ivy_unit(); }
  hull() { translate([0, R*0.20]) circle(d = max(6, R*0.16), $fn = 12); translate([0, R*0.52]) circle(d = max(6, R*0.16), $fn = 12); }
}
module leaf_veins(R, kind) {
  w = max(3.0, R * 0.055);
  ax = (kind == 0) ? MAPLE_AX : IVY_AX;
  for (A = ax) line2([0, R*0.05], [R*A[1]*0.80*(-sin(A[0])), -R*A[1]*0.80*cos(A[0])], w);
}

// 책 위 화분에서 자라는 담쟁이 : [cx, cy, R, rot]
PLANT = [[447,333,24,-48],[478,322,26,0],[509,333,24,48],[430,356,20,-80],[526,356,20,80]];

// [cx, cy, R, rot, kind(0단풍/1담쟁이), color(O/C/G), tier, group(0=다른 아이템 뒤/1=맨 앞)]
LEAVES = concat([
  // ---- 오른쪽 담쟁이 군락 (낮은 단계)
  [1062, 428, 34, -40, 1, "G", 1, 1],
  [1192, 430, 38,  42, 1, "G", 1, 1],
  [1210, 492, 34,  85, 1, "G", 1, 1],
  [1160, 532, 34, 170, 1, "G", 1, 1],
  [1036, 452, 28, -65, 1, "G", 1, 1],
  // ---- 늘어진 담쟁이
  [1196, 578, 30, 150, 1, "G", 1, 1],
  [1212, 618, 30,  62, 1, "G", 1, 1],
  [1190, 652, 30,-125, 1, "G", 1, 1],
  [1210, 700, 28,  70, 1, "G", 1, 1],
  [1188, 726, 28,-110, 1, "G", 1, 1],
  [1205, 760, 26,  75, 1, "G", 1, 1],
  [1203, 800, 26, 175, 1, "G", 1, 1],
  // ---- 단풍
  [872,   68, 30,  40, 0, "C", 2, 1],   // 노랑(크림)
  [905,  100, 62,   8, 0, "O", 3, 1],
  [952,  178, 64, -30, 0, "O", 4, 1],
  [1026, 384, 48, -18, 0, "O", 3, 1],
  [1134, 480, 52,  14, 0, "C", 4, 1],   // 노랑
  [985,  553, 42, -15, 0, "O", 3, 1],
  [725,  520, 50,  12, 0, "O", 5, 1],
  // ---- 왼쪽 군락 (책 앞)
  [372,  340, 50, -22, 0, "O", 5, 1],
  [350,  398, 32,  30, 0, "C", 5, 1],
  [322,  456, 58, -30, 0, "O", 6, 1],
  [364,  522, 42,  10, 0, "O", 6, 1]
], [for (p = PLANT) [p[0], p[1], p[2], p[3], 1, "G", 5, 1]]);
LEAF_K = 1.18;          // 잎 전체 크기 배율
NL = len(LEAVES);
// j 가 i 의 앞에서 i 를 가리는가? (같은 색끼리는 겹쳐도 한 덩어리이므로 자르지 않음 → 맞닿은 경계 폴리곤이 꼬이는 것을 방지)
function leaf_front(j, i) = (j != i) && (LEAVES[j][5] != LEAVES[i][5])
                          && (LEAVES[j][6] > LEAVES[i][6] || (LEAVES[j][6] == LEAVES[i][6] && j > i));

module leaf_raw(i) {
  L = LEAVES[i];
  translate([L[0], L[1]]) rotate(L[3])
    leaf_shape(L[2] * LEAF_K, L[4]);
}
module leaf_vein_raw(i) {
  L = LEAVES[i];
  translate([L[0], L[1]]) rotate(L[3]) leaf_veins(L[2] * LEAF_K, L[4]);
}
module leaf_carved(i) {
  difference() {
    children();
    for (j = [0 : NL - 1]) if (leaf_front(j, i)) leaf_raw(j);
    if (LEAVES[i][7] == 0) above(I_LEAF0); else above(I_LEAF1);   // 선반은 잎보다 앞
  }
}
module leaves_group(g) { for (i = [0 : NL - 1]) if (LEAVES[i][7] == g) leaf_raw(i); }

// 한 색, 한 tier 의 잎사귀 3단(몸통 → 모서리 경사 → 잎맥 음각)
module leaves_color(col) {
  for (t = [0 : len(TIER_Z) - 1]) {
    h = TIER_Z[t];
    ext(ZB, h - 1.2)    for (i = [0 : NL - 1]) if (LEAVES[i][5] == col && LEAVES[i][6] == t) leaf_carved(i) leaf_raw(i);
    ext(h - 1.2, h - 0.6) for (i = [0 : NL - 1]) if (LEAVES[i][5] == col && LEAVES[i][6] == t) leaf_carved(i) offset(delta = -1.6) leaf_raw(i);
    ext(h - 0.6, h)     for (i = [0 : NL - 1]) if (LEAVES[i][5] == col && LEAVES[i][6] == t)
      leaf_carved(i) difference() { offset(delta = -3.4) leaf_raw(i); leaf_vein_raw(i); }
  }
}

// ============================================================================
//  아이템 원형(raw) 2D — 뒤에서 앞 순서 (번호가 클수록 앞)
// ============================================================================
I_NOOK=0; I_WALL=1; I_LEAN=2; I_WAIN=3; I_FBACK=4; I_PIER=5; I_CHIM=6; I_ROOF=7;
I_GLASS=8; I_MUN=9; I_FRAME=10; I_VENT=11; I_LANT=12; I_TWIG=13; I_STEM=14; I_LEAF0=15; I_FENCE=16;
I_PUMP=17; I_BOOKS=18; I_CUP=19; I_DECO=20; I_LEAF1=21; I_SHELF=22; N_ITEMS=23;   // 번호가 클수록 앞

// ---- 선반 ----
module S_lip()   { translate([288,548]) square([1185-288, 566-548]); }
module S_panel() { rrect(308, 556, 1172, 665, 14); }
module S_plank() { rrect(330, 584, 1150, 648, 5); }      // 속널(오목)

// ---- 집 ----
module S_nook()  { intersection() { polygon([[318,402],[497,316],[497,560],[318,560]]); above_shelf(); } }
module S_wall()  { intersection() { polygon([[497,560],[497,276],[803,70],[1092,266],[1092,560]]); above_shelf(); } }
module S_lean()  { polygon([[318,372],[497,286],[522,274],[522,304],[497,316],[318,402]]); }
module S_wain()  { intersection() { translate([662,480]) square([338, 100]); above_shelf(); } }
module S_fback() { intersection() { translate([1000,452]) square([170, 120]); above_shelf(); } }
module S_pier()  { translate([1000,252]) square([90, 200]); }
module S_chim()  { translate([995,98]) square([90, 160]); rrect(985, 68, 1093, 100, 6); }
module S_roof()  { polygon([[488,237],[803,24],[1118,237],[1103,274.5],[803,72],[503,274.5]]); }

// ---- 창 ----
WIN_X = 811;  WIN_YS = 308;
module S_win_outer() { arch(WIN_X, WIN_YS, 151, 486); }
module S_glass_all() { arch(WIN_X, WIN_YS, 128.5, 462); }
module S_mun()       { intersection() { S_glass_all();
                        union() { translate([WIN_X-10, 150]) square([20, 330]); translate([660, 292]) square([300, 20]); } } }
module S_glass()     { S_glass_all(); }
module S_frame()     { difference() { S_win_outer(); S_glass_all(); } rrect(650, 458, 972, 488, 4); }

// ---- 둥근 다락창 + 못걸이 (지붕 아래, 창 위의 벽) ----
function roof_under(x) = 72 + 0.675 * abs(803 - x);                       // 지붕 아래면 (px)
function arch_top(x)   = 308 - sqrt(max(0, 151*151 - (x - 811)*(x - 811)));  // 창틀 위쪽 곡선 (px)
NAIL_YC = (roof_under(NAIL_X) + arch_top(NAIL_X)) / 2;                    // 다락창 중심 y
VENT_R  = min(30, (arch_top(NAIL_X) - roof_under(NAIL_X)) / 2 - 1.5);        // 다락창 반지름
NAIL_LEN_PX = NAIL_LEN / S;
NAIL_YE = NAIL_YC + (NAIL_LEN_PX + NAIL_POCK/S/2 - NAIL_HEAD/S/2) / 2;     // 키홀 전체를 다락창 중앙에 맞춤
module S_vent()       { translate([NAIL_X, NAIL_YC]) circle(r = VENT_R, $fn = 64); }
module vent_pane2d()  { translate([NAIL_X, NAIL_YC]) circle(r = VENT_R - 6, $fn = 64); }
module vent_ring2d()  { difference() { S_vent(); vent_pane2d(); } }
module vent_bars2d()  { intersection() { vent_pane2d();
                          union() { translate([NAIL_X - 2.5, NAIL_YC - VENT_R]) square([5, 2*VENT_R]); translate([NAIL_X - VENT_R, NAIL_YC - 2.5]) square([2*VENT_R, 5]); } } }
module nail_entry()   { translate([NAIL_X, NAIL_YE]) circle(d = NAIL_HEAD / S, $fn = 36); }
module nail_slot(w)   { hull() { translate([NAIL_X, NAIL_YE]) circle(d = w / S, $fn = 24); translate([NAIL_X, NAIL_YE - NAIL_LEN_PX]) circle(d = w / S, $fn = 24); } }
module nail_narrow()  { nail_entry(); nail_slot(NAIL_NECK); }
module nail_wide()    { nail_entry(); nail_slot(NAIL_POCK); }

// ---- 랜턴 ----
module L_brown() {
  translate([603,160]) square([6, 72]);                                  // 걸이 막대
  ell2([606,178], 9, 9); // 고리 바깥(안쪽은 아래에서 뺌)
  polygon([[592,228],[620,228],[646,248],[566,248]]);                      // 지붕
  translate([575,246]) square([62, 5]);
  L_bars();                                                               // 세로 살대
  polygon([[571,298],[641,298],[630,313],[582,313]]);                      // 바닥
  polygon([[601,313],[611,313],[606,326]]);                                // 끝 장식
}
module L_bars()  { for (x = [575, 603, 631]) translate([x, 250]) square([6, 50]); }
module L_glass() { difference() { translate([575,251]) square([62, 47]); L_bars(); } }
module L_flame() { difference() { ell2([606,276], 9, 17); L_bars(); } }
module S_lant()  { L_brown(); L_glass(); L_flame(); }

// ---- 덩굴/담쟁이 줄기 ----
TWIG_PATH = [[1138,408],[1088,372],[1044,346],[1012,312],[992,272],[962,218],[936,168],[918,132]];
STEM_PATH = [[1186,556],[1198,600],[1190,650],[1200,700],[1190,750],[1203,812]];
STEM_CLUSTER = [[1060,430],[1096,420],[1126,414],[1160,424],[1192,430]];
module S_twig() { polyline(TWIG_PATH, 11); }
STEM_W = 10;       // 담쟁이 줄기 굵기(px) ≈ 2mm — 가는 목으로 위태롭게 붙지 않도록
module plant_stems() { for (p = PLANT) line2([POT_TOP[0], POT_TOP[1]], [p[0], p[1]], 8); }
POT_TOP = [478, 366];
module S_stem() { polyline(STEM_PATH, STEM_W); polyline(STEM_CLUSTER, STEM_W); line2([1150,426],[1160,530],STEM_W); line2([1186,556],[1160,530],STEM_W);
                  line2([1192,430],[1208,492],STEM_W); line2([1208,492],[1186,556],STEM_W); plant_stems(); }

// ---- 울타리 ----
PICKETS = [for (k = [0:5]) 1006 + k*24];
module S_pickets() { intersection() { above_shelf(); union() { for (x = PICKETS) polygon([[x,490],[x+7,478],[x+14,490],[x+14,560],[x,560]]); } } }
module S_rails()   { for (y = [498, 530]) translate([1000, y]) square([155, 9]); }
module S_fence()   { intersection() { above_shelf(); union() { S_pickets(); S_rails(); } } }

// ---- 미니 호박 (솔방울이 있던 자리) ----
//         cx   cy   rx  ry  ztop
PUMPS  = [[812, 530, 25, 21, 10.4], [782, 538, 17, 13, 9.0]];   // 바닥이 선반선(548)을 살짝 넘게 → 접점(핀치) 방지
PK_DX  = [-0.62, -0.31, 0, 0.31, 0.62];     // 호박 5조각(세로 골) 배치
PK_RY  = [ 0.84,  0.95, 1.0, 0.95, 0.84];
PK_RZ  = [ 0.86,  0.95, 1.0, 0.95, 0.86];
module pump_lobes2d(p) { for (k = [0:4]) ell2([p[0] + PK_DX[k]*p[2], p[1]], 0.42*p[2], PK_RY[k]*p[3]); }
module pump_stem2d(p)  { line2([p[0], p[1] - 0.80*p[3]], [p[0] + 0.20*p[2], p[1] - 1.28*p[3]], 7); }
module S_pump() { intersection() { above_shelf(); union() { for (p = PUMPS) { pump_lobes2d(p); pump_stem2d(p); } } } }

// ---- 책 ----
//        x0   y0   x1   y1  표지끝  색
BOOKS = [[365, 405, 692, 452, 592, "G"],
         [370, 452, 700, 503, 602, "O"],
         [395, 503, 700, 552, 606, "G"]];
module book_all(b)   { translate([b[0], b[1]]) square([b[2]-b[0], b[3]-b[1]]); }
module book_cover(b) { translate([b[0], b[1]]) square([b[4]-b[0], b[3]-b[1]]); }
module book_pages(b) { translate([b[4], b[1] + 3]) square([b[2]-b[4], b[3]-b[1]-6]); }
module S_books() { intersection() { above_shelf(); for (b = BOOKS) book_all(b); } }

// ---- 컵 ----
module cup_body2d()   { hull() { ell2([897,442], 56, 8); ell2([897,541], 41, 8); } }
module cup_handle2d() { difference() { ell2([966,489], 27, 40); ell2([966,489], 11, 25); } }
module cup_whip2d()   { ell2([897,428], 38, 14); ell2([900,411], 28, 14); ell2([902,399], 15, 12); ell2([904,392], 8, 9); }
module S_cup() { intersection() { above_shelf(); union() { cup_body2d(); cup_handle2d(); cup_whip2d(); } } }

// ---- 책 위 장식 : 작은 화분 + 도토리 2개 (고양이가 있던 자리) ----
BOOK_TOP = 405;
module above_books() { translate([-100,-100]) square([2000, 100 + BOOK_TOP]); }
module pot_body2d() { polygon([[454,405],[502,405],[508,373],[448,373]]); }
module pot_rim2d()  { rrect(441, 358, 515, 376, 4); }
module pot_band2d() { intersection() { pot_body2d(); translate([440,386]) square([80, 8]); } }
//            x   y(바닥)  배율
ACORNS = [[574, 408, 1.00], [606, 408, 0.82]];   // 바닥이 책 윗선(405)을 살짝 넘게 → 잘려서 평평하게 앉음
module acorn_cap_ell(a) { ell2([a[0], a[1] - 32*a[2]], 13.5*a[2], 8*a[2]); }
module acorn_stem(a)    { line2([a[0], a[1] - 37*a[2]], [a[0] + 3*a[2], a[1] - 46*a[2]], 6*a[2]); }
module acorn_nut(a)     { difference() { ell2([a[0], a[1] - 15*a[2]], 11*a[2], 15*a[2]); acorn_cap_ell(a); } }
module S_deco() { intersection() { above_books(); union() { pot_body2d(); pot_rim2d(); for (a = ACORNS) { acorn_nut(a); acorn_cap_ell(a); acorn_stem(a); } } } }

// ============================================================================
//  아이템 선택 / 앞쪽 가림(carve)
// ============================================================================
module item(i) {
  if      (i == I_SHELF) { S_lip(); S_panel(); }
  else if (i == I_NOOK)  S_nook();
  else if (i == I_WALL)  S_wall();
  else if (i == I_LEAN)  S_lean();
  else if (i == I_WAIN)  S_wain();
  else if (i == I_FBACK) S_fback();
  else if (i == I_PIER)  S_pier();
  else if (i == I_CHIM)  S_chim();
  else if (i == I_ROOF)  S_roof();
  else if (i == I_GLASS) S_glass();
  else if (i == I_MUN)   S_mun();
  else if (i == I_FRAME) S_frame();
  else if (i == I_VENT)  S_vent();
  else if (i == I_LANT)  S_lant();
  else if (i == I_TWIG)  S_twig();
  else if (i == I_STEM)  S_stem();
  else if (i == I_LEAF0) leaves_group(0);
  else if (i == I_FENCE) S_fence();
  else if (i == I_PUMP)  S_pump();
  else if (i == I_BOOKS) S_books();
  else if (i == I_CUP)   S_cup();
  else if (i == I_DECO)  S_deco();
  else if (i == I_LEAF1) leaves_group(1);
}
module above(i) { if (i < N_ITEMS - 1) for (j = [i + 1 : N_ITEMS - 1]) item(j); }   // (역순 범위 주의: 2021.01은 양 끝을 뒤집음)
module carved(i) { difference() { item(i); above(i); } }
module all_items() { for (i = [0 : N_ITEMS - 1]) item(i); }
// 연결/외곽용 뒷판 형상
module base2d() { offset(delta = 0.8) union() { all_items(); } }

// ============================================================================
//  패턴(음각) 2D
// ============================================================================
function gx(r, j) = 505 + j*31 + (r % 2)*15.5;
module roof_grooves_left() {
  for (k = [1:3]) line2([470, 24 + (803-470)*0.675 + k*14], [803, 24 + k*14], 3.4);
  for (r = [0:3]) for (j = [0:10]) if (gx(r,j) < 800)
    line2([gx(r,j), 24 + (803-gx(r,j))*0.675 + r*14], [gx(r,j), 24 + (803-gx(r,j))*0.675 + (r+1)*14], 3.4);
}
module roof_grooves() {
  roof_grooves_left();
  translate([803,0]) mirror([1,0]) translate([-803,0]) roof_grooves_left();
}
module wall_joints() {
  for (k = [0:14]) { y = 70 + k*34;
    translate([480, y]) square([640, 2.6]);
    for (j = [0:12]) translate([497 + j*52 + (k % 2)*26, y]) square([2.6, 34]);
  }
}
module mortar() {
  for (k = [0:17]) { y = 96 + k*22;
    translate([980, y]) square([120, 2.8]);
    for (j = [0:3]) translate([1000 + j*36 + (k % 2)*18, y]) square([2.8, 22]);
  }
}
module plank_grain() {
  for (k = [0:5]) { y = 593 + k*10.5;
    polyline([[334, y],[480, y+1.5],[640, y-1],[820, y+1.2],[990, y-1.2],[1146, y+1]], 2.0);
  }
}
module hk_pockets() {
  for (x = HK_X) translate([x - (HK_W + 2*HK_CLR)/S/2, HK_YC - (HK_TENON_H + 2*HK_CLR)/S/2]) square([(HK_W + 2*HK_CLR)/S, (HK_TENON_H + 2*HK_CLR)/S]);
}
module kh_entry()  { for (x = KH_X) translate([x, KH_Y]) circle(d = KH_HEAD/S, $fn = 40); }
module kh_slot(w)  { for (x = KH_X) hull() { translate([x, KH_Y]) circle(d = w/S, $fn = 30); translate([x, KH_Y - KH_LEN/S]) circle(d = w/S, $fn = 30); } }
module kh_narrow() { kh_entry(); kh_slot(KH_SLOT); }
module kh_wide()   { kh_entry(); kh_slot(KH_POCK); }

// ============================================================================
//  책 장식 / 창 잎사귀 (크림)
// ============================================================================
module star4(c, r) { translate(c) polygon([[0,-r],[r*0.28,-r*0.28],[r,0],[r*0.28,r*0.28],[0,r],[-r*0.28,r*0.28],[-r,0],[-r*0.28,-r*0.28]]); }
module book_ornaments() {
  for (b = BOOKS) { ym = (b[1] + min(b[3], 548)) / 2 - 1;
    for (x = [b[0] + 26, b[0] + 40]) translate([x, b[1] + 8]) square([3.4, min(b[3],548) - b[1] - 16]);   // 책등 띠
    star4([b[0] + 62, ym], 9);
    star4([b[4] - 26, ym], 8);
    translate([b[4] - 14, b[1] + 8]) square([3.4, min(b[3],548) - b[1] - 16]);
  }
  translate([478, 478]) rotate(8) scale([1,-1]) leaf_shape(16, 0);          // 가운데 책의 단풍 문양
}
module page_lines() {
  for (b = BOOKS) for (k = [1:4]) translate([b[4] + 4, b[1] + 3 + k * (b[3]-b[1]-6)/5 - 1.3]) square([b[2]-b[4]-8, 2.6]);
}
GLASS_LEAVES = [[735,235,30,-30],[788,205,22,20],[872,232,30,45],[907,292,24,-10],[838,262,20,10],
                [744,362,32,15],[792,425,28,-25],[872,372,34,30],[905,432,26,60],[760,318,22,70],[862,430,22,-40]];
module glass_leaves() { for (g = GLASS_LEAVES) translate([g[0], g[1]]) rotate(g[3]) leaf_shape(g[2], 0); }

// ============================================================================
//  3D 몸체 : 고양이 / 컵 / 솔방울
// ============================================================================
module ell3(c, rx, ry, ztop, zc = ZB) {
  translate([X(c[0]), Y(c[1]), zc]) scale([rx * S, ry * S, ztop - zc]) sphere(r = 1, $fn = 40);
}
module skin(depth) {   // children(0)=몸체, children(1)=칠할 2D(px) → 위쪽 depth(mm) 껍질만
  difference() {
    intersection() { children(0); prismZ(0, 60) children(1); }
    translate([0,0,-depth]) children(0);
  }
}

// ---- 컵 ----
CUP_RIM = 56 * S;  CUP_BOT = 41 * S;  CUP_H = (548 - 437) * S;
module cup_body3d() {
  intersection() {
    translate([X(897), Y(548), ZB]) scale([1, 1, (Z_CUP - ZB) / (CUP_RIM * 1.04)]) rotate([-90, 0, 0])
      rotate_extrude($fn = 64) polygon([[0,0],[CUP_BOT,0],[CUP_BOT*1.03,1.4],[CUP_RIM*0.97,CUP_H*0.55],[CUP_RIM,CUP_H*0.93],
                                        [CUP_RIM*1.04,CUP_H*0.95],[CUP_RIM*1.04,CUP_H],[CUP_RIM*0.9,CUP_H],[0,CUP_H*0.97]]);
    prismZ(ZB, 40) carved(I_CUP);
  }
}
module cup_whip3d() {
  intersection() {
    union() { ell3([897,428], 38, 14, 12.4); ell3([900,411], 28, 14, 12.9); ell3([902,399], 15, 12, 13.0); ell3([904,392], 8, 9, 11.8); }
    prismZ(ZB, 40) carved(I_CUP);
  }
}
module cup_handle3d() {
  intersection() {
    union() { ext(ZB, 7.2) cup_handle2d(); ext(7.2, 8.2) offset(delta = -2.6) cup_handle2d(); }
    prismZ(ZB, 40) carved(I_CUP);
  }
}
module pumpkin2d() {
  for (p = [[-22,9,18],[-11,11,22],[0,12,24],[11,11,22],[22,9,18]]) ell2([890 + p[0], 499], p[1], p[2]);
}
module pumpkin_stem2d() { polyline([[891,478],[892,466],[903,461]], 7); }
module cup_rim2d() { difference() { translate([830,438]) square([135, 10]); cup_whip2d(); } }
module cup_cream() {
  if (FLAT) { difference() { carved(I_CUP); cup_rim2d(); pumpkin2d(); pumpkin_stem2d(); } }
  else {
    difference() { cup_body3d(); skin(2.4) { cup_body3d(); cup_rim2d(); } skin(2.4) { cup_body3d(); pumpkin2d(); } skin(2.4) { cup_body3d(); pumpkin_stem2d(); } }
    cup_whip3d(); cup_handle3d();
  }
}
module cup_orange() { if (FLAT) intersection() { carved(I_CUP); union() { cup_rim2d(); pumpkin2d(); } } else { skin(2.4) { cup_body3d(); cup_rim2d(); } skin(2.4) { cup_body3d(); difference() { pumpkin2d(); pumpkin_stem2d(); } } } }
module cup_green()  { if (FLAT) intersection() { carved(I_CUP); pumpkin_stem2d(); } else skin(2.4) { cup_body3d(); pumpkin_stem2d(); } }

// ---- 미니 호박 3D ----
module pump_lobes3d(p) {
  for (k = [0:4]) ell3([p[0] + PK_DX[k]*p[2], p[1]], 0.42*p[2], PK_RY[k]*p[3], ZB + (p[4] - ZB)*PK_RZ[k]);
}
module pump_body3d() {
  intersection() {
    for (p = PUMPS) pump_lobes3d(p);
    prismZ(ZB, 40) difference() { carved(I_PUMP); for (p = PUMPS) pump_stem2d(p); }
  }
}
module pump_orange() { if (FLAT) difference() { carved(I_PUMP); for (p = PUMPS) pump_stem2d(p); } else pump_body3d(); }
module pump_green()  { for (p = PUMPS) ext(ZB, p[4] + 1.0) intersection() { carved(I_PUMP); pump_stem2d(p); } }

// ---- 책 위 장식 (화분·도토리) 층 ----
module deco_brown() {      // 도토리 모자 + 꼭지
  ext(ZB, 10.2)  for (a = ACORNS) intersection() { carved(I_DECO); union() { acorn_cap_ell(a); acorn_stem(a); } }
  ext(10.2, 10.8) for (a = ACORNS) intersection() { carved(I_DECO); union() { offset(delta = -1.6) acorn_cap_ell(a); acorn_stem(a); } }
}
module deco_orange() {     // 화분 + 도토리 알맹이
  ext(ZB, 9.8)    intersection() { carved(I_DECO); pot_body2d(); }
  ext(9.8, 10.8)  intersection() { carved(I_DECO); difference() { pot_body2d(); pot_band2d(); } }
  ext(ZB, 11.0)   intersection() { carved(I_DECO); pot_rim2d(); }
  ext(11.0, 11.6) intersection() { carved(I_DECO); offset(delta = -2) pot_rim2d(); }
  ext(ZB, 9.4)    for (a = ACORNS) intersection() { carved(I_DECO); acorn_nut(a); }
  ext(9.4, 10.0)  for (a = ACORNS) intersection() { carved(I_DECO); offset(delta = -1.6) acorn_nut(a); }
}
module deco_cream() { ext(9.8, 10.8) intersection() { carved(I_DECO); pot_band2d(); } }    // 화분 띠

// ============================================================================
//  색상별 본체 (plaque)
// ============================================================================
module shelf_brown() {
  // 선반 윗턱
  ext(ZB, KH_Z) difference() { carved_part(I_SHELF, "lip"); kh_wide(); }
  ext(KH_Z, Z_LIP - 0.8) carved_part(I_SHELF, "lip");
  ext(Z_LIP - 0.8, Z_LIP) offset(delta = -2.5) S_lip();
  // 앞판 : 후크 장부 구멍 · 키홀 포켓 포함
  ext(ZB, 3.0)        difference() { carved_part(I_SHELF, "panel"); kh_wide(); }
  ext(3.0, KH_Z)      difference() { carved_part(I_SHELF, "panel"); kh_wide(); hk_pockets(); }
  ext(KH_Z, 11.2)     difference() { carved_part(I_SHELF, "panel"); hk_pockets(); }
  ext(11.2, Z_PLANK)  difference() { carved_part(I_SHELF, "panel"); hk_pockets(); plank_grain(); }
  ext(Z_PLANK, Z_BORDER) difference() { carved_part(I_SHELF, "panel"); S_plank(); }
}
// 선반은 lip / panel 을 따로 다뤄야 해서 별도 carve
module carved_part(i, which) {
  difference() {
    if (which == "lip") S_lip(); else S_panel();
    above(i);
  }
}

module brown_plaque() {
  // 0) 뒷판 (모든 아이템 외곽 + 키홀 입구)
  ext(0, ZB) difference() { base2d(); kh_narrow(); nail_narrow(); }
  // 1) 선반
  shelf_brown();
  // 2) 처마 아래 둥지/경사지붕
  ext(ZB, Z_NOOK) carved(I_NOOK);
  ext(ZB, Z_LEAN - 0.6) carved(I_LEAN);
  ext(Z_LEAN - 0.6, Z_LEAN) intersection() { carved(I_LEAN); offset(delta = -2.5) S_lean(); }
  // 3) 창 아래 판자벽, 울타리 뒤판
  ext(ZB, Z_WAIN) carved(I_WAIN);
  ext(ZB, Z_FBACK) carved(I_FBACK);
  // 4) 벽돌 기둥/굴뚝 (회반죽 줄눈은 크림으로 상감)
  ext(ZB, Z_PIER - 0.6) { carved(I_PIER); carved(I_CHIM); }
  ext(Z_PIER - 0.6, Z_PIER) difference() { union() { carved(I_PIER); carved(I_CHIM); } mortar(); }
  ext(Z_PIER, Z_CAP) intersection() { carved(I_CHIM); rrect(985, 68, 1093, 100, 6); }
  // 5) 지붕 (타일 홈)
  ext(ZB, Z_ROOF - 0.6) carved(I_ROOF);
  ext(Z_ROOF - 0.6, Z_ROOF) difference() { carved(I_ROOF); roof_grooves(); }
  // 6) 창틀 + 창살
  ext(ZB, Z_FRAME - 0.8) carved(I_FRAME);
  ext(Z_FRAME - 0.8, Z_FRAME) intersection() { carved(I_FRAME); offset(delta = -4) S_frame(); }
  ext(ZB, Z_MUN - 0.5) carved(I_MUN);
  ext(Z_MUN - 0.5, Z_MUN) intersection() { carved(I_MUN); offset(delta = -3) S_mun(); }
  // 6.5) 둥근 다락창 : 테두리 + 십자살 (십자살 아래에 못걸이 포켓)
  ext(ZB, NAIL_Z) difference() { intersection() { carved(I_VENT); vent_ring2d(); } nail_wide(); }
  ext(NAIL_Z, 7.2) intersection() { carved(I_VENT); vent_ring2d(); }
  ext(7.2, 7.8) intersection() { carved(I_VENT); offset(delta = -1.5) vent_ring2d(); }
  ext(ZB, NAIL_Z) difference() { intersection() { carved(I_VENT); vent_bars2d(); } nail_wide(); }
  ext(NAIL_Z, 7.0) intersection() { carved(I_VENT); vent_bars2d(); }
  // 7) 랜턴 프레임
  ext(ZB, Z_LANT) intersection() { carved(I_LANT); L_brown(); }
  // 8) 덩굴 가지
  ext(ZB, Z_TWIG) carved(I_TWIG);
  // 9) 솔방울, 고양이 줄무늬
  deco_brown();
}

module cream_plaque() {
  // 벽 (석재 줄눈 음각) + 벽돌 줄눈 상감
  ext(ZB, 4.5) carved(I_WALL);
  ext(4.5, Z_WALL) difference() { carved(I_WALL); wall_joints(); }
  ext(Z_PIER - 0.6, Z_PIER) intersection() { union() { carved(I_PIER); carved(I_CHIM); } mortar(); }
  // 창 유리 위 잎사귀 장식
  ext(Z_GLASS, Z_GLASS + 0.6) intersection() { carved(I_GLASS); glass_leaves(); }
  // 랜턴 불꽃
  ext(Z_LANT - 1.0, Z_LANT - 0.4) intersection() { carved(I_LANT); L_flame(); }
  // 책 : 페이지 + 장식
  ext(ZB, Z_PAGES - 0.5) for (b = BOOKS) intersection() { carved(I_BOOKS); book_pages(b); }
  ext(Z_PAGES - 0.5, Z_PAGES) for (b = BOOKS) difference() { intersection() { carved(I_BOOKS); book_pages(b); } page_lines(); }
  ext(Z_BOOK, Z_BOOK + 0.5) intersection() { carved(I_BOOKS); for (b = BOOKS) book_cover(b); book_ornaments(); }
  // 울타리
  ext(ZB, Z_FENCE - 1.4) carved(I_FENCE);
  ext(Z_FENCE - 1.4, Z_FENCE) intersection() { carved(I_FENCE); S_pickets(); }
  // 컵, 고양이, 노란 잎
  cup_cream();
  deco_cream();
  leaves_color("C");
}

module orange_plaque() {
  ext(ZB, Z_GLASS) carved(I_GLASS);                                      // 창 유리(주황 빛)
  ext(ZB, NAIL_Z) difference() { intersection() { carved(I_VENT); difference() { vent_pane2d(); vent_bars2d(); } } nail_wide(); }   // 다락창 유리
  ext(NAIL_Z, 6.4) intersection() { carved(I_VENT); difference() { vent_pane2d(); vent_bars2d(); } }   // 포켓 천장 두께 1.8mm
  ext(ZB, Z_LANT - 1.0) intersection() { carved(I_LANT); L_glass(); }     // 랜턴 유리
  // 가운데 책 (주황)
  ext(ZB, Z_BOOK - 0.8) for (b = BOOKS) if (b[5] == "O") intersection() { carved(I_BOOKS); book_cover(b); }
  ext(Z_BOOK - 0.8, Z_BOOK) for (b = BOOKS) if (b[5] == "O") intersection() { carved(I_BOOKS); offset(delta = -3) book_cover(b); }
  deco_orange();
  pump_orange();
  cup_orange();
  leaves_color("O");
}

module green_plaque() {
  ext(ZB, Z_BOOK - 0.8) for (b = BOOKS) if (b[5] == "G") intersection() { carved(I_BOOKS); book_cover(b); }
  ext(Z_BOOK - 0.8, Z_BOOK) for (b = BOOKS) if (b[5] == "G") intersection() { carved(I_BOOKS); offset(delta = -3) book_cover(b); }
  ext(ZB, Z_STEM) carved(I_STEM);
  cup_green();
  pump_green();
  leaves_color("G");
}

// ============================================================================
//  후크 (분리 출력) — 벽 좌표: x 폭, y 위, z 벽에서 앞으로, 원점 = 속널 표면
// ============================================================================
module hook_arm_profile() {         // (u=z, v=y) 2D
  path = [[2, 0], [HK_REACH - 4, 0], [HK_REACH, -4], [HK_REACH, -14]];
  for (i = [0 : len(path) - 2]) hull() { translate(path[i]) circle(d = HK_ARM, $fn = 24); translate(path[i+1]) circle(d = HK_ARM, $fn = 24); }
}
module hook_wall() {
  // 장부(속널 구멍에 끼움)
  translate([-HK_W/2, -HK_TENON_H/2, -HK_TENON_L]) cube([HK_W, HK_TENON_H, HK_TENON_L + 0.01]);
  // 칼라(속널 위에 얹히는 판)
  linear_extrude(height = 3) offset(r = 2) square([HK_W - 4, 12 - 4], center = true);
  // 팔 (프로파일을 x 방향으로 압출)
  translate([HK_W/2, 0, 0]) rotate([0, -90, 0]) linear_extrude(height = HK_W) hook_arm_profile();
  // 공 모양 끝 (옆면을 팔 폭으로 평평하게)
  intersection() {
    translate([0, -17.5, HK_REACH]) sphere(r = 5.6, $fn = 36);
    translate([-HK_W/2, -40, 0]) cube([HK_W, 60, 60]);
  }
}
// 프린트 자세: 옆으로 눕힘 (x→베드 z, z→베드 x), 층결이 팔 방향을 따라가 강함
module hook_print() { translate([HK_TENON_L, 23.2, HK_W/2]) rotate([0, 90, 0]) hook_wall(); }
HK_PRINT_W = HK_TENON_L + HK_REACH + 5.6;     // 베드상 x 길이
HK_PRINT_H = 29.4;                              // 베드상 y 길이

module hooks_layout() {
  for (k = [0:3]) translate([BED_M + k * (HK_PRINT_W + HK_GAP), BED_M + PL_SIZE[1] + 8, 0]) hook_print();
}
module hooks_assembled() {
  for (x = HK_X) translate([X(x), Y(HK_YC), Z_PLANK]) hook_wall();
}

// ============================================================================
//  출력 선택
// ============================================================================
module plaque_color(c) {
  if      (c == "brown")  brown_plaque();
  else if (c == "cream")  cream_plaque();
  else if (c == "orange") orange_plaque();
  else if (c == "green")  green_plaque();
}
module out(c) {
  if (ASSEMBLED || FLAT) { plaque_color(c); if (c == "brown" && !FLAT) hooks_assembled(); }
  else {
    translate([PL_OFF[0], PL_OFF[1], 0]) plaque_color(c);
    if (c == "brown") hooks_layout();
  }
}

if (PART == "all") {
  color(C_BROWN)  out("brown");
  color(C_CREAM)  out("cream");
  color(C_ORANGE) out("orange");
  color(C_GREEN)  out("green");
} else out(PART);
