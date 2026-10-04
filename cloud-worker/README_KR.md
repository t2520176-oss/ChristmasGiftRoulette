# FigureCraft 클라우드 워커 (GPU 서버)

FigureCraft 데스크톱 프로그램이 보내는 요청을 받아 **GPU에서 3D 모델(GLB)을 만들어 돌려주는 작은 서버**입니다.
데스크톱 앱과 이 서버는 완전히 별개이며, 서버는 RunPod가 아니라 어떤 GPU 서버에서도 돌릴 수 있습니다.

```
데스크톱 앱 ──HTTP──▶ worker.py (FastAPI) ──▶ ModelEngine(TRELLIS / Hunyuan3D / mock) ──▶ GLB
```

## 먼저 알아둘 것 (정직한 상태 표시)

| 항목 | 상태 |
|---|---|
| 서버/프로토콜 (`worker.py`, 인증, 작업 큐, 취소, 결과 다운로드, 종료 API) | ✔ **`ENGINE=mock` 로 실제 실행하며 시험 완료** (데스크톱 앱 자동 테스트 포함) |
| `mock` 엔진 | ✔ GPU 없이 동작하는 **가짜(자리표시) 모델**. AI가 아니며 앱에서 `DEMO` 로 표시됩니다 |
| TRELLIS / Hunyuan3D 어댑터 | ⚠ 공개된 사용법에 맞춰 작성했지만 **실제 GPU에서 실행해 보지 못했습니다.** 처음에는 오류가 날 수 있으니 서버 로그를 확인하세요 |
| `Dockerfile` | ⚠ 서버 부분은 시험, TRELLIS/Hunyuan 설치 단계는 **빌드해 보지 못한 틀**입니다 (CUDA/torch 버전 조정이 필요할 수 있음) |
| RunPod 연동 | ⚠ 공개 API 문서에 맞춰 작성. **가짜 서버로만 시험**했고 실제 계정으로는 못 했습니다 |

## 라이선스 주의
* **TRELLIS**: MIT 라이선스 – 기본 추천.
* **Hunyuan3D-2**: Tencent 커뮤니티 라이선스에 **지역 제한**이 있습니다 (작성 시점 기준 EU·영국·**대한민국** 등이 제외된 것으로 알고 있습니다).
  한국에서 쓸 계획이라면 **반드시 최신 라이선스 원문을 직접 읽고** 사용 가능 여부를 확인하세요. 불확실하면 TRELLIS를 쓰세요.
* 텍스트→이미지 단계의 모델(`T2I_MODEL`, 기본 SDXL)도 각자의 라이선스가 있습니다.

## 구성 파일
```
cloud-worker/
  worker.py          FastAPI 서버 (일반 HTTP GPU 서버 / RunPod Pod 용)
  runpod_handler.py  RunPod Serverless 용 진입점
  config.py          환경변수 설정 (비밀 값은 코드에 없음)
  engines/           교체 가능한 3D 엔진 (base.py = ModelEngine 인터페이스)
  Dockerfile  requirements.txt  .env.example
```

## 1. 먼저 내 PC에서 시험해 보기 (GPU 없이)
```bat
cd cloud-worker
pip install fastapi "uvicorn[standard]" python-multipart pillow numpy trimesh requests
set WORKER_API_KEY=my-test-key-123
set ENGINE=mock
set HOST=127.0.0.1
python worker.py
```
FigureCraft → 설정: 공급자 *일반 HTTP GPU 서버*, 엔드포인트 `http://127.0.0.1:8000`, API 키 `my-test-key-123` → **연결 테스트**.
생성하면 가짜 모델이 오고 프로젝트에 `DEMO`가 붙습니다. 이것으로 연결·다운로드·**자동 GPU 종료**(서버 종료) 흐름을 확인할 수 있습니다.

## 2. 환경변수 (`.env.example` 참고, 실제 `.env` 는 절대 커밋하지 마세요)
| 이름 | 설명 |
|---|---|
| `WORKER_API_KEY` | 앱과 서버가 공유하는 비밀번호 (길고 무작위로). 없으면 서버가 시작되지 않습니다 |
| `ENGINE` | `trellis` / `hunyuan3d` / `mock` |
| `IDLE_SHUTDOWN_MIN` | 일이 없으면 N분 뒤 자동 종료 (0=끔). **유료 GPU에서는 10 정도를 강력 추천** |
| `ALLOW_SHUTDOWN` | 앱이 `/v1/shutdown` 으로 서버를 끄는 것을 허용 (기본 1) |
| `RUNPOD_POD_ID`, `RUNPOD_API_KEY` | RunPod Pod가 *스스로* 중지되도록 할 때 (Pod 안에서는 `RUNPOD_POD_ID` 가 자동으로 설정됩니다) |
| `T2I_MODEL`, `TRELLIS_MODEL`, `HUNYUAN_MODEL`, `HF_TOKEN` | 모델 이름 / Hugging Face 토큰 |
| `S3_BUCKET`, `S3_ENDPOINT_URL` | (Serverless) 15 MB 넘는 결과를 S3 호환 저장소로 돌려줄 때 |

## 3. Docker 이미지 만들기
```bash
docker build -t <내-도커허브-아이디>/figurecraft-worker --build-arg ENGINE=trellis .
docker push <내-도커허브-아이디>/figurecraft-worker
```
> TRELLIS 설치 단계는 공식 저장소 안내(`setup.sh`)를 따릅니다. 처음 빌드할 때 버전 문제로 실패하면 `Dockerfile`의 주석 링크를 보고 맞춰 주세요.
> 모델 파일(수 GB)은 이미지에 넣지 않고 처음 실행 때 내려받습니다 → 첫 생성은 오래 걸립니다. Pod에 볼륨을 붙이면 다음부터 빨라집니다.

## 4. RunPod에서 실행하기

### Pod (직접 켜고 끄기)
1. RunPod → **Pods → Deploy** → 24 GB 이상 VRAM GPU 선택 (예: RTX 4090, L40S 등. 가격은 runpod.io에서 확인).
2. Container Image: `<아이디>/figurecraft-worker`, **Expose HTTP Port: 8000**.
3. Environment Variables: `WORKER_API_KEY`, `ENGINE=trellis`, `IDLE_SHUTDOWN_MIN=10`.
4. Deploy 후 주소 `https://<POD_ID>-8000.proxy.runpod.net` 를 FigureCraft 설정의 *엔드포인트*에, `WORKER_API_KEY` 를 *API 키*에, Pod 번호를 *RunPod Pod ID* 에, RunPod 계정 키를 *RunPod 계정 API 키*에 넣습니다 (두 키는 서로 다른 비밀번호입니다).
5. 앱에서 *생성 완료 후 GPU 자동 종료* 가 켜져 있으면 다 만든 뒤 앱이 RunPod REST API(위의 RunPod 계정 키 사용)로 Pod를 중지합니다. 이 키를 `RUNPOD_API_KEY` 로 Pod에도 넣어 두면 `IDLE_SHUTDOWN_MIN` 이 지났을 때 워커가 스스로 Pod를 멈출 수 있습니다.
   *Pod 중지 API는 이 저장소에서 실제 계정으로 시험하지 못했습니다. 첫 사용 때 runpod.io 콘솔에서 상태를 꼭 확인하세요.*

### Serverless
1. RunPod → **Serverless → New Endpoint** → 같은 이미지, *Container Start Command*: `python -u runpod_handler.py`.
2. 환경변수 `ENGINE=trellis` (Serverless는 `WORKER_API_KEY` 가 필요 없고 RunPod API 키로 인증됩니다).
3. Endpoint ID를 FigureCraft 설정의 엔드포인트에, **RunPod API 키**를 API 키에 넣습니다. **Active workers = 0** 으로 두면 일이 없을 때 요금이 나가지 않습니다.
4. 결과 GLB가 15 MB를 넘으면 `S3_BUCKET` 설정이 필요합니다 (RunPod 응답 크기 제한).

## 5. 프로토콜 (다른 GPU 서버를 직접 만들고 싶을 때)
모든 요청에 `Authorization: Bearer <WORKER_API_KEY>`.
```
GET  /health                 → {"status":"ok","engine":"…","capabilities":{"text":true,"image":true,"text_image":true,"shutdown":true}}
POST /v1/jobs  (multipart)   → {"job_id":"…"}   필드: params(JSON 문자열), image(선택, PNG/JPG/WEBP)
GET  /v1/jobs/{id}           → {"state":"queued|running|succeeded|failed|cancelled","stage":"…","progress":null,"gpu_seconds":12.3,"error":null}
GET  /v1/jobs/{id}/result    → GLB 파일 (Range 이어받기 지원)
POST /v1/jobs/{id}/cancel
POST /v1/shutdown            → GPU/Pod 중지
```
`params` 에는 `prompt, style, style_prompt, engine, seed, height_mm, max_colors, printability_hints, revision` 이 들어 있습니다.

## 6. 새 3D 엔진 추가하기
`engines/base.py` 의 `ModelEngine` 을 상속해 `generate_from_text / generate_from_image / generate_from_text_and_image` 를 구현하고
`engines/registry.py` 에 한 줄 등록하면 됩니다. 이미지→3D 전용 모델은 `engines/text2image.py` 의 `TextToImage` 로 *글 → 참조 이미지 → 3D* 두 단계를 구성합니다.
(글+이미지를 같이 보내면 TRELLIS/Hunyuan은 **이미지**로 형태를 만들고 글은 쓰지 않습니다.)

## 7. 보안 메모
* API 키는 항상 환경변수로. 비교는 상수 시간으로 합니다. 업로드 크기(`MAX_UPLOAD_MB`)와 이미지 형식을 검사합니다.
* 서버는 받은 글로 쉘 명령을 실행하지 않습니다.
* 인터넷에 직접 공개하는 서버라면 HTTPS(RunPod 프록시는 기본 HTTPS)를 쓰세요.
