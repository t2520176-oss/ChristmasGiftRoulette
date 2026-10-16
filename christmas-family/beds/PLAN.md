# 프린트 베드 계획 — THE PIXELFORM FAMILY · Christmas 2026

베드 256 × 256 mm, 베드당 최대 4색. 필라멘트 색이 4개를 넘으면 그 색이 필요한 조각은 다음 베드로 넘어갑니다(조각 하나의 모든 색은 항상 같은 베드에서 출력).

| 베드 | 필라멘트 슬롯 1 · 2 · 3 · 4 | 조각 수 | 베드 점유율 | 예상 시간* | 필라멘트(g)* |
|---|---|---|---|---|---|
| **1** | BLUE · YELLOW · WHITE · BROWN | 10 | 63.2 % | 14h 4m 55s | 35 / 28 / 30 / 73 |
| **2** | WHITE · GREEN · RED · BLACK | 10 | 21.5 % | 6h 30m 39s | 28 / 14 / 10 / 15 |
| **3** | WHITE · RED · BLACK · PINK | 2 | 5.1 % | 4h 9m 19s | 12 / 17 / 9 / 8 |

\* PrusaSlicer 2.7 CLI, 0.4 mm 노즐 · 0.2 mm 레이어 · 벽 3 · 인필 12 % · 프라임 타워 포함(`slicer/prusaslicer_4color_256.ini`). 일반적인 느린 프로파일 기준이라 실제 기계(Bambu 등)에서는 달라집니다. 필라멘트 g 은 슬롯 순서이며 색 교환 퍼지(프라임 타워) 분량이 포함됩니다.

## 베드 1

![bed 1](../renders/bed1.png)

| 슬롯 | 색 | HEX | 순수 모델 부피 |
|---|---|---|---|
| 1 | BLUE (파랑) | `#24407A` | 49.3 cm³ |
| 2 | YELLOW (노랑) | `#F2C230` | 21.5 cm³ |
| 3 | WHITE (흰색) | `#F2F2EC` | 46.7 cm³ |
| 4 | BROWN (갈색(진저브레드)) | `#7A4A2B` | 209.6 cm³ |

| 조각 | 의미 | 색 구성 |
|---|---|---|
| `base` | 받침(나무 몸통 + 쌓인 눈) | 나무 몸통=BROWN, 쌓인 눈=WHITE |
| `fig3` | Buddy (dog) | 털=YELLOW, 귀=BROWN, 목도리=WHITE, 눈동자·표정=BROWN |
| `house` | 집 | 벽=BROWN, 쌓인 눈=WHITE, 창문=YELLOW |
| `plaque` | 명판 THE PIXELFORM FAMILY / Christmas 2026 | 판=YELLOW, 글자=BROWN |
| `sky` | 밤하늘 아치 판 | 하늘 판=BLUE, 달·별=YELLOW |
| `tag0` | 이름표 Ethan | 판=YELLOW, 글자=BROWN |
| `tag1` | 이름표 Lily | 판=YELLOW, 글자=BROWN |
| `tag2` | 이름표 Hazel | 판=YELLOW, 글자=BROWN |
| `tag3` | 이름표 Buddy | 판=YELLOW, 글자=BROWN |
| `tag4` | 이름표 Mochi | 판=YELLOW, 글자=BROWN |

- 슬라이서 자동 서포트 점검(45°): 서포트 없음
- 프라임 타워: x 209.8–249.2, y 183.5–247.5 mm (모델과 11.0 mm 이상 떨어짐, 베드 안: 예)
- 색 교환: 슬롯 T0, T1, T2, T3 사용

## 베드 2

![bed 2](../renders/bed2.png)

| 슬롯 | 색 | HEX | 순수 모델 부피 |
|---|---|---|---|
| 1 | WHITE (흰색) | `#F2F2EC` | 15.9 cm³ |
| 2 | GREEN (초록) | `#1F6B3A` | 9.9 cm³ |
| 3 | RED (빨강) | `#C8202F` | 4.4 cm³ |
| 4 | BLACK (검정) | `#1E1E22` | 9.5 cm³ |

| 조각 | 의미 | 색 구성 |
|---|---|---|
| `fig1` | Lily (snowman) | 몸=WHITE, 모자=BLACK, 모자 띠=RED, 목도리=RED, 코=RED, 눈동자·표정=BLACK |
| `fig4` | Mochi (cat) | 털=BLACK, 흰 무늬=WHITE, 빨강 무늬=RED |
| `holly0` | 호랑가시 1 | 잎=GREEN, 열매=RED |
| `holly1` | 호랑가시 2 | 잎=GREEN, 열매=RED |
| `lantern0` | 랜턴 1 | 틀=BLACK, 유리=WHITE |
| `lantern1` | 랜턴 2 | 틀=BLACK, 유리=WHITE |
| `tree0` | 나무 1 | 잎=GREEN, 쌓인 눈=WHITE, 꼭대기 별=RED |
| `tree1` | 나무 2 | 잎=GREEN, 쌓인 눈=WHITE, 꼭대기 별=RED |
| `tree2` | 나무 3 | 잎=GREEN, 쌓인 눈=WHITE, 꼭대기 별=RED |
| `tree3` | 나무 4 | 잎=GREEN, 쌓인 눈=WHITE, 꼭대기 별=RED |

- 슬라이서 자동 서포트 점검(45°): 약 79 mm 필라멘트(≈ 0.24 g) — 사실상 없음
- 프라임 타워: x 210.1–248.9, y 183.5–247.5 mm (모델과 86.4 mm 이상 떨어짐, 베드 안: 예)
- 색 교환: 슬롯 T0, T1, T2, T3 사용

## 베드 3

![bed 3](../renders/bed3.png)

| 슬롯 | 색 | HEX | 순수 모델 부피 |
|---|---|---|---|
| 1 | WHITE (흰색) | `#F2F2EC` | 3.3 cm³ |
| 2 | RED (빨강) | `#C8202F` | 7.6 cm³ |
| 3 | BLACK (검정) | `#1E1E22` | 2.8 cm³ |
| 4 | PINK (핑크) | `#F2B8A8` | 3.8 cm³ |

| 조각 | 의미 | 색 구성 |
|---|---|---|
| `fig0` | Ethan (boy) | 재킷=WHITE, 목도리=RED, 모자=RED, 방울=WHITE, 얼굴=PINK, 눈동자·표정=BLACK |
| `fig2` | Hazel (girl) | 원피스=RED, 칼라=WHITE, 머리=BLACK, 리본=RED, 얼굴=PINK, 눈동자·표정=BLACK |

- 슬라이서 자동 서포트 점검(45°): 서포트 없음
- 프라임 타워: x 210.1–248.9, y 183.5–247.5 mm (모델과 187.0 mm 이상 떨어짐, 베드 안: 예)
- 색 교환: 슬롯 T0, T1, T2, T3 사용

