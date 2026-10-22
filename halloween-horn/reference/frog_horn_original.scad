/*
  Fixed Frog Acoustic Horn (고정형 & 무게중심 안정화 버전)
  - 관절 핀 제거 및 다리를 몸통에 일체형(Union)으로 깊숙이 병합
  - 무게 중심을 뒤로 잡아주는 '킥스탠드'형 뒷다리 연장
  - 뒤로 넘어지지 않도록 개구리 배와 발바닥의 접지 면적 극대화
*/

$fn = 60;

// [ 스마트폰 투입구 설정 (아이폰 13) ]
phone_w = 78;    
phone_t = 14;    
slot_d = 40;     

// [ 본체 기본 변수 ]
body_r = 42;
body_z = 35;

// ==========================================
// 1. 전체 외부 형태 (몸통 + 고정형 다리 결합)
// ==========================================
module frog_body_with_legs() {
    union() {
        // --- A. 통통한 개구리 몸통 ---
        hull() {
            translate([0, -5, body_z])
            scale([1.35, 1.25, 0.9]) sphere(r = body_r);
            
            // 머리 위 둥근 투입구 덮개 (폰을 튼튼하게 감싸줌)
            translate([0, -5, body_z + 18])
            scale([1, 0.6, 0.3]) sphere(r = 45); 
        }
        
        // --- B. 개구리 눈 ---
        for (x = [-42, 42]) {
            translate([x, 15, body_z + 25]) {
                sphere(r = 11); // 흰자
                translate([0, 8, 4]) sphere(r = 5); // 눈동자
            }
        }
        
        // --- C. 개구리 입 (앞으로 튀어나온 확성기) ---
        translate([0, 18, body_z - 5])
        rotate([-75, 0, 0]) 
        scale([1.5, 1, 0.8]) 
        union() {
            cylinder(h = 42, r1 = 18, r2 = 38);
            translate([0, 0, 42])
            rotate_extrude() translate([38, 0, 0]) circle(r=3.5);
        }
        
        // --- D. 일체형 뒷다리 (킥스탠드 역할) ---
        // 폰 무게 때문에 뒤로 넘어가지 않도록 Y축 뒤쪽(-40)으로 아주 길고 넓게 뻗습니다.
        for (is_left = [true, false]) {
            x_pos = is_left ? -48 : 48;
            translate([x_pos, -15, body_z - 15])
            union() {
                // 몸통과 깊게 결합되는 튼튼한 허벅지
                hull() {
                    sphere(r = 14);
                    translate([0, -20, -10]) sphere(r = 12);
                }
                // 바닥을 넓게 딛는 커다란 오리발
                hull() {
                    translate([0, -20, -10]) sphere(r = 12);
                    // 밖으로 살짝 벌어지며 뒤를 단단히 지지함
                    translate([is_left ? -10 : 10, -35, -20]) 
                    scale([1.5, 2.2, 0.5]) sphere(r = 18);
                }
            }
        }
        
        // --- E. 일체형 앞다리 (앞쪽 밸런스 유지) ---
        for (is_left = [true, false]) {
            x_pos = is_left ? -45 : 45;
            translate([x_pos, 25, body_z - 15])
            union() {
                hull() {
                    sphere(r = 12);
                    translate([is_left ? -5 : 5, 15, -20]) sphere(r = 10);
                }
                translate([is_left ? -5 : 5, 15, -20]) 
                scale([1.3, 1.8, 0.5]) sphere(r = 13);
            }
        }
    }
}

// ==========================================
// 2. 최종 파내기 및 바닥 평탄화
// ==========================================
module stable_frog_assembly() {
    difference() {
        
        // 전체 외형 불러오기
        frog_body_with_legs();
        
        // --- 내부 기능 파내기 ---
        
        // A. 스마트폰 슬롯
        translate([0, -5, body_z + 25])
        cube([phone_w, phone_t, slot_d + 20], center=true);
        
        // B. 소리 증폭 챔버
        translate([0, -5, body_z - 5])
        scale([1.3, 1.1, 1]) sphere(r = 16);
        
        // C. 스마트폰 ~ 나팔관 목구멍 연결
        hull() {
            translate([0, -5, body_z - 5]) sphere(r = 13);
            translate([0, 18, body_z - 5]) sphere(r = 13);
        }
        
        // D. 개구리 입 내부 (확성관 통로)
        translate([0, 18, body_z - 5])
        rotate([-75, 0, 0])
        scale([1.5, 1, 0.8])
        translate([0, 0, -5])
        cylinder(h = 50, r1 = 13, r2 = 35);
        
        // --- E. 바닥 평탄화 (매우 중요) ---
        // z = 2 이하를 모두 날려버립니다. 
        // 개구리의 둥근 배 밑부분과 4개의 발바닥이 크게 썰려 나가면서, 
        // 책상 바닥에 거대한 면적으로 찰싹 달라붙어 절대 넘어지지 않습니다.
        translate([0, 0, -48]) 
        cube([300, 300, 100], center=true);
    }
}

// 렌더링 실행
stable_frog_assembly();