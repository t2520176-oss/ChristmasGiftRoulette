# Autumn Nest 공모전 출품작

3D 프린팅 공모전 "Autumn Nest"에 낼 작품 10개를 OpenSCAD(파라메트릭 형상, 공차)와 Blender(합치기, 렌더)로 만드는 작업 폴더입니다.
이 폴더는 Android 앱(ChristmasGiftRoulette)과 무관한 별도 작업입니다.

| # | 작품 | 부문 | 상태 |
|---|---|---|---|
| 1 | [Tree-Ring Rotary Organizer](01-tree-ring-organizer/) 나이테 회전 정리함 | Autumn Storage | 모델, 간극, 창 위치, 용량 검증 완료. 출력 간극과 걸림 느낌은 시험 필요 |
| 4 | [Bracket Fungus Shelf](04-fungus-shelf/) 말굽버섯 전시 선반 3종 | Woodland Displays | 모델, 슬라이드 경로, 받침, 열쇠 구멍 검증 완료. 끼워 맞춤 간극과 하중은 시험 필요 |
| 5 | [Samara Twister](05-samara-twister/) 단풍 시과 회전 모빌 | Autumn in Motion | 모델 완료. 베어링 마찰 때문에 약한 바람에서는 안 돌 가능성이 높아 개선 방향 결정 필요 |
| 6 | [Maple Chime](06-maple-chime/) 단풍 펜타토닉 윈드차임 | Autumn in Motion | 모델, 절단표, 배치 검증 완료. 출력과 음정, 바람 테스트 필요 |
| 7 | [Forest Balance Mobile](07-forest-mobile/) 계산형 숲 모빌 | Autumn in Motion | 모델, 균형, 간극 검증 완료. 출력과 실제 바람에서의 움직임 테스트 필요 |
| 8 | [Stump Planter](08-bark-planter/) 그루터기 자가급수 화분 | A Forest Corner | 모델, 수위·용량·틈·두께 검증 완료. 방수와 출력, 심지 흡수량은 시험 필요 |
| 10 | [Vein-Glow Lantern](10-vein-lantern/) 잎맥 리소페인 램프 | A Forest Corner | 모델, 결합, 열 여유 검증 완료. 흰색 PLA 투과율과 출력은 시험편으로 확인 필요 |

나머지 3개(2 솔방울 황금각 정리대, 3 뿌리 트루셰 타일, 9 조립식 덩굴 지지대)는 아직 시작 전입니다.

## 도구 (`tools/`)

| 파일 | 용도 |
|---|---|
| `check_mesh.py` | STL 검사: 수밀 여부, 가동부 간극, 서포트가 필요한 오버행 면적 |
| `blender_assemble.py` | OpenSCAD가 내보낸 파트들을 Blender Manifold 불리언으로 합침 (CGAL 합집합보다 훨씬 빠름) |
| `blender_render.py` | Cycles CPU로 스튜디오 렌더(제출용 사진 기반). STL 1개 또는 `파일:재질` 여러 개로 조립 장면도 가능 |
| `chime_calc.py` | 윈드차임 튜브 절단표(Euler-Bernoulli와 Timoshenko 유한요소)와 바람 추정 |
| `print_layout.py` | 여러 STL을 한 베드에 외곽선 기준으로 자동 배치(3MF, STL, 도면 PNG). 베드 크기와 간격을 인자로 지정하고, `--group 색@부품,...`으로 색상별 베드, `--split`으로 여러 베드, `--tries`로 배치 순서 탐색 |
| `mobile_check.py` | 모빌 검증: 장식 질량 측정, 팔별 무게중심 대 받침 링, 연결부 간극, 형제 하위 모빌 회전 여유, 질량 오차 민감도, 연결부 비틀림 범위 |
| `vein_field.py` | 리소페인 갓의 잎맥 벽 두께 지도 생성(주맥, 곁맥, 잔맥 + 부드러운 두께 전이) |
| `blender_vein_shade.py` | 두께 지도로 곡면 갓 메시를 만들고 STL로 내보내며, 켠 모습과 끈 모습을 렌더 |
| `lamp_light.py` | LED 높이에 따른 갓 밝기 분포 모델(거리, 입사각, 가림, 투과) |
| `lantern_check.py` | 램프 검증: 수밀, 벽 두께, 갓-받침 결합, 열 여유, 밝기 균일도 |
| `bark_shell.py` | 껍질 무늬 바깥 화분 생성(세로로 늘린 3D 노이즈, 이음매 없음, 밑동 뿌리 모양), 넘침 구멍을 Blender 불리언으로 뚫고 STL/렌더 출력 |
| `planter_check.py` | 화분 검증: 수밀, 벽 두께(레이 캐스팅), 넘침 구멍과 최고 수위, 저수량과 흙 용량(단면 적분), 화분 사이 틈과 공기층, 심지·배수 구멍, 수위 막대 간극과 눈금 높이 |
| `organizer_check.py` | 정리함 검증: 수밀, 중첩 고리 간극(메쉬 측정), 뚜껑 창이 6개 위치에서 정확히 한 칸만 여는지(레이 캐스팅), 걸림 리브와 홈, 칸 용량과 건전지 수, 창으로 꺼낼 수 있는 수 |
| `bracket_fungus.py` | 말굽버섯 선반과 껍질 판 생성(numpy 높이장 메쉬 + Blender Manifold 불리언으로 도브테일 홈, 레일, 열쇠 구멍, 나이테) |
| `shelf_check.py` | 선반 검증: 수밀, 슬라이드 경로의 간극(단면 비교), 받침 접촉, 열쇠 구멍, 나이테, 하중 추정 |
| `planter_section.py` | 실제 STL을 잘라 그린 화분 단면도(물, 흙, 심지 표시) |

환경: OpenSCAD 2021.01 이상, Blender 또는 `pip install bpy`(Python 3.11), `pip install trimesh numpy scipy networkx rtree shapely matplotlib lxml mapbox-earcut`.
