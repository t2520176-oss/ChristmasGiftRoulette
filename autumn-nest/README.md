# Autumn Nest 공모전 출품작

3D 프린팅 공모전 "Autumn Nest"에 낼 작품 10개를 OpenSCAD(파라메트릭 형상, 공차)와 Blender(합치기, 렌더)로 만드는 작업 폴더입니다.
이 폴더는 Android 앱(ChristmasGiftRoulette)과 무관한 별도 작업입니다.

| # | 작품 | 부문 | 상태 |
|---|---|---|---|
| 5 | [Samara Twister](05-samara-twister/) 단풍 시과 회전 모빌 | Autumn in Motion | 모델 완료. 베어링 마찰 때문에 약한 바람에서는 안 돌 가능성이 높아 개선 방향 결정 필요 |
| 6 | [Maple Chime](06-maple-chime/) 단풍 펜타토닉 윈드차임 | Autumn in Motion | 모델, 절단표, 배치 검증 완료. 출력과 음정, 바람 테스트 필요 |
| 7 | [Forest Balance Mobile](07-forest-mobile/) 계산형 숲 모빌 | Autumn in Motion | 모델, 균형, 간극 검증 완료. 출력과 실제 바람에서의 움직임 테스트 필요 |

나머지 7개(1 나이테 회전 정리함, 2 솔방울 황금각 정리대, 3 뿌리 트루셰 타일, 4 말굽버섯 선반, 8 자가급수 화분, 9 조립식 덩굴 지지대, 10 잎맥 리소페인 램프)는 아직 시작 전입니다.

## 도구 (`tools/`)

| 파일 | 용도 |
|---|---|
| `check_mesh.py` | STL 검사: 수밀 여부, 가동부 간극, 서포트가 필요한 오버행 면적 |
| `blender_assemble.py` | OpenSCAD가 내보낸 파트들을 Blender Manifold 불리언으로 합침 (CGAL 합집합보다 훨씬 빠름) |
| `blender_render.py` | Cycles CPU로 스튜디오 렌더(제출용 사진 기반). STL 1개 또는 `파일:재질` 여러 개로 조립 장면도 가능 |
| `chime_calc.py` | 윈드차임 튜브 절단표(Euler-Bernoulli와 Timoshenko 유한요소)와 바람 추정 |
| `print_layout.py` | 여러 STL을 한 베드에 외곽선 기준으로 자동 배치(3MF, STL, 도면 PNG). 베드 크기와 간격을 인자로 지정 |
| `mobile_check.py` | 모빌 검증: 장식 질량 측정, 팔별 무게중심 대 받침 링, 연결부 간극, 형제 하위 모빌 회전 여유, 질량 오차 민감도, 연결부 비틀림 범위 |

환경: OpenSCAD 2021.01 이상, Blender 또는 `pip install bpy`(Python 3.11), `pip install trimesh numpy scipy networkx rtree shapely matplotlib lxml`.
