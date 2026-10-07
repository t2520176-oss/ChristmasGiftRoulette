// 별자리판 + 해시계 (Planisphere + Gnomon) v4.1 - Color Edition (흰색 / 검정 / 노랑)
// 250x250 베드 한 장에 전부 배치 (전체 약 190 x 135mm, 원점 기준 x -50~140 / y -85~50)
//
// 색상 3가지
//   white  : 베이스 본체, 힌지 핀                        -> 별 구멍으로 보이는 "별빛" 바탕
//   black  : 별판, 눈금, 방위 글자(S/E/W), N 배지        -> 밤하늘
//   yellow : 베이스 테두리, 별판 링/별 테두리, 지시 마크, 중앙 핀, 그노몬, N 글자
//
// 사용법
//   part = "all"   : 전체 색상 미리보기 (F5)
//   part = "white" / "black" / "yellow" : 색상별로 따로 렌더(F6) 후 STL 내보내기
//   3개 STL은 좌표가 동일하므로 슬라이서에 한꺼번에 불러오면 자동으로 정렬됩니다.
//   (커맨드라인: openscad -o black.stl -D 'part="black"' planisphere_gnomon_color.scad)

$fn = 128;

part       = "all";  // [all, white, black, yellow]
tick_inlay = true;   // true: 눈금 홈을 검정으로 채움 / false: 홈으로 남김 (단색 출력용)
star_halo  = true;   // 별 구멍 둘레 노란 테두리
north_badge = true;  // N 글자 뒤에 검정 원형 배지 (흰 베이스 위에서 노란 N이 잘 보이게)
show_bed   = true;   // 미리보기에 250x250 베드 윤곽 표시 (STL 내보내기에는 영향 없음)

// ---- 팔레트 ----
c_white  = "#F6F6F1";
c_black  = "#17181C";
c_yellow = "#FFD21F";

// ---- 치수 ----
base_d = 100;   // 베이스 지름
base_h = 3;     // 베이스 두께
disc_d = 80;    // 별판 지름
disc_h = 2;     // 별판 두께
pivot  = 5;     // 중앙 핀 기준 지름

// 힌지 설정
hy = 38;        // 힌지 축 Y 위치 (별판 바깥쪽 가장자리)
hz = 12;        // 힌지 축 높이 (접었을 때 별판 위로 뜨도록)
blk_in  = 22;   // 힌지 블록 안쪽 X
blk_out = 27;   // 힌지 블록 바깥쪽 X
g_w = 21.5;     // 그노몬 반폭 (블록과 편측 0.5mm 여유)
g_h = 31;       // 그노몬 높이 (43:31 ≈ 위도 36°)
g_t = 6;        // 그노몬 두께
hole_hinge = 3.2;
pin_hinge  = 3.0;

// 장식 치수
accent_h = 0.4; // 노란 장식(링/별 테두리) 높이
mark_h   = 0.8; // 지시 마크 높이
halo_d   = 3.6; // 별 테두리 바깥 지름
star_d   = 1.5; // 별 구멍 지름

// 별 좌표 (두 별무리)
stars_a = [[-15,10],[-10,8],[-5,6],[0,4],[5,0],[10,-2],[15,-5]];
stars_b = [[-20,-12],[-15,-15],[-10,-13],[-5,-15],[0,-17]];
stars   = concat(stars_a, stars_b);

// 방위 글자 [글자, x, y]
compass = [["N", 0, 44], ["S", 0, -44], ["E", 44, 0], ["W", -44, 0]];

// 파트 선택: part=="all"이면 색칠해서 모두 표시, 아니면 해당 색 파트만 표시
module paint(name, c) {
  if (part == "all") color(c) children();
  else if (part == name) children();
}

// ---------- 공용 형상 ----------
module hinge_blocks() {
  translate([-blk_out, hy - 3, 0]) cube([blk_out - blk_in, 6, hz + 3]);
  translate([blk_in,   hy - 3, 0]) cube([blk_out - blk_in, 6, hz + 3]);
}

// 걸이 구멍 (225도 방향, 방위 글자와 겹치지 않게)
module hang_hole() {
  rotate([0, 0, 225]) translate([0, 44, 0]) cylinder(d = 6, h = 10, center = true);
}

// 5도 간격 눈금 (방위 글자가 있는 0/90/180/270도, 걸이 구멍 자리 225도, N 배지 옆 5/355도는 비움)
// depth: 베이스 윗면 아래로 파인 깊이, extra: 윗면 위로 튀어나오는 여유(절단용)
module ticks(depth = 1.0, extra = 0) {
  for (a = [0 : 5 : 355]) if (a % 90 != 0 && a != 225 && !(north_badge && (a == 5 || a == 355)))
    rotate([0, 0, a])
      translate([0, 44.5, base_h - depth / 2 + extra / 2])
        cube([0.8, 5, depth + extra], center = true);
}

module letter(i, z = base_h) {
  t = compass[i];
  translate([t[1], t[2], z])
    linear_extrude(0.6)
      text(t[0], size = 5, font = "Liberation Sans:style=Bold",
           halign = "center", valign = "center");
}

// N 글자 뒤 배지 (지름 7.6mm: 별판 가장자리 r=40과 테두리 안쪽 r=48 사이에 들어감)
badge_d = 7.6;
badge_h = 0.6;
module north_badge_body() {
  translate([compass[0][1], compass[0][2], base_h]) cylinder(d = badge_d, h = badge_h);
}

// ---------- white : 베이스 ----------
module base_body() {
  difference() {
    union() {
      difference() {
        cylinder(d = base_d, h = base_h);
        ticks(1.0, 0.2);                       // 눈금 홈
      }
      hinge_blocks();                          // 힌지 블록 2개 (눈금 홈보다 우선)
    }
    cylinder(d = pivot + 0.3, h = 10, center = true);   // 중앙 핀 구멍 (여유 0.3)
    hang_hole();
    translate([0, hy, hz]) rotate([0, 90, 0])           // 힌지 구멍 (X축 관통)
      cylinder(d = hole_hinge, h = 56, center = true);
  }
}

// ---------- yellow : 베이스 테두리 ----------
module base_rim() {
  difference() {
    translate([0, 0, base_h]) linear_extrude(1.5)
      difference() { circle(d = base_d); circle(d = base_d - 4); }
    hinge_blocks();
  }
}

// ---------- black : 눈금 인레이 ----------
module tick_inlay_body() {
  difference() {
    ticks(1.0, 0);
    hinge_blocks();
    hang_hole();
  }
}

// ---------- black : 별판 ----------
module star_disc_body() {
  difference() {
    cylinder(d = disc_d, h = disc_h);
    cylinder(d = pivot + 0.6, h = 10, center = true);   // 중앙 구멍 (회전 여유 0.6)
    for (p = stars) translate([p[0], p[1], 0])
      cylinder(d = star_d, h = 10, center = true);
    for (a = [0 : 30 : 330]) rotate([0, 0, a])          // 가장자리 손잡이 홈
      translate([0, disc_d / 2 - 5, 0]) cube([1, 4, 10], center = true);
  }
}

// ---------- yellow : 별판 장식 ----------
module star_disc_yellow() {
  // 가장자리 링
  translate([0, 0, disc_h]) linear_extrude(accent_h)
    difference() { circle(d = disc_d - 1); circle(d = disc_d - 3.4); }
  // 별 구멍 테두리
  if (star_halo) for (p = stars) translate([p[0], p[1], disc_h]) linear_extrude(accent_h)
    difference() { circle(d = halo_d, $fn = 32); circle(d = star_d, $fn = 32); }
  // 지시 마크
  translate([0, 28, disc_h + mark_h / 2]) cube([3, 10, mark_h], center = true);
}

// ---------- yellow : 그노몬 ----------
module gnomon_v3() {
  difference() {
    linear_extrude(g_t)
      polygon([[-g_w, 0], [g_w, 0], [-g_w, g_h]]);
    // 힌지 구멍: 판 중간 높이, 밑변에서 3mm 안쪽
    translate([0, 3, g_t / 2]) rotate([0, 90, 0]) cylinder(d = hole_hinge, h = 60, center = true);
  }
}

// ---------- white : 힌지 핀 (3mm 필라멘트 조각으로 대체 가능) ----------
module hinge_pin() {
  translate([0, 0, pin_hinge / 2])        // 베드 위에 올려놓기 (v3는 절반이 베드 아래로 파묻혀 있었음)
    rotate([0, 90, 0]) cylinder(d = pin_hinge, h = 53);
}

// ---------- yellow : 중앙 핀 (머리를 아래로 두어 오버행 없이 출력) ----------
module pivot_pin() {
  cylinder(d = 9, h = 2);
  cylinder(d = pivot - 0.2, h = 2 + base_h + disc_h + 0.2);
}

// ---- 출력 배치 (한 베드) ----
// 베이스(원점) / 별판(+100,0) / 그노몬(0,-85) / 힌지 핀(60,-75) / 중앙 핀(130,-75)
paint("white", c_white) {
  base_body();
  translate([60, -75, 0]) hinge_pin();
}

paint("black", c_black) {
  translate([100, 0, 0]) star_disc_body();
  if (tick_inlay) tick_inlay_body();
  for (i = [1 : 3]) letter(i);                  // S, E, W
  if (north_badge) north_badge_body();
}

paint("yellow", c_yellow) {
  base_rim();
  translate([100, 0, 0]) star_disc_yellow();
  translate([130, -75, 0]) pivot_pin();
  translate([0, -85, 0]) gnomon_v3();
  letter(0, north_badge ? base_h + badge_h : base_h);   // N (배지 위에 얹음)
}

// 미리보기용 베드 윤곽 (배치 중심을 베드 중앙에 맞춤). %는 렌더/내보내기에서 무시됨
if (show_bed && part == "all")
  %translate([45 - 125, -17.5 - 125, -0.4]) cube([250, 250, 0.4]);
