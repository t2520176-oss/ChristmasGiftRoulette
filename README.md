# 손금 타로 (Palm Tarot)

손금 사진 AI 분석 + 타로 3장 리딩을 결합한 안드로이드 앱 (한국어 / English).

## APK 받기
GitHub **Releases** 에서 최신 `palm-tarot.apk` 를 내려받아 설치하세요.
`main` 브랜치에 push 하면 GitHub Actions 가 자동으로 새 APK 를 빌드합니다.

## AI 설정
앱 오른쪽 위 ⚙ → Claude API 키 입력 (console.anthropic.com → API Keys).
키는 기기 안에만 저장됩니다. 키가 없으면 손금 모양을 직접 고르는 모드로 동작합니다.

## 구조
- `www/index.html` — 앱 화면 전체 (HTML/CSS/JS 한 파일)
- `android/` — Capacitor 안드로이드 프로젝트
- `.github/workflows/build-apk.yml` — APK 자동 빌드
