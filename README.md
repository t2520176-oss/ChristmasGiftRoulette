# OurChat 전달 파일과 설치 상태

**현재 상태:** 사용자의 Windows에서 Smart App Control이 `OurChat-Setup.cmd` 실행을 차단했습니다. 이 설치기는 디지털 서명이 없는 스크립트이며, 해당 PC에서 자동 설치를 완료할 수 있는 방법으로 안내해서는 안 됩니다. 보안 기능을 비활성화하거나 확장자를 바꿔 실행하지 마세요.

## 브라우저 앱으로 설치하는 경로

OurChat에는 PWA manifest·아이콘·브라우저 설치 기능이 이미 포함되어 있습니다. 전용 Supabase 프로젝트에 데이터베이스와 두 사용자 계정을 설정하고, 앱을 HTTPS로 배포한 다음, Edge/Chrome에서 배포 주소를 열어 **앱 설치**를 사용할 수 있습니다. 이 방식은 다운로드한 CMD를 실행하지 않습니다.

현재 실제 Supabase 프로젝트·두 계정·운영 키·호스팅 연결과 HTTPS 배포 주소는 준비되지 않았습니다. 공개된 파일은 소스와 설치 자료이며, 실제 채팅 서비스가 배포되었다는 뜻이 아닙니다. 프로젝트 키·비밀번호를 이 저장소나 대화에 게시하지 마세요. 소스 ZIP의 `README.ko.md`에 관리자 설정 절차가 있습니다.

## 기존 로컬 설치 자료

아래 CMD는 검사와 개발용 자료로 남겨 둡니다. **Smart App Control이 차단한 PC에서는 실행을 진행하지 마세요.** 체크섬과 helper 검사는 Windows의 앱 신뢰 승인이나 실제 설치 성공을 보장하지 않습니다.

[기존 CMD 설치 자료 — 해당 PC에서 차단됨](https://github.com/t2520176-oss/ChristmasGiftRoulette/raw/refs/heads/codex/ourchat-download/OurChat-Setup.cmd)

이 스크립트는 Windows 10/11 64비트 환경의 Node.js·앱 설치, 빌드, 바탕화면 바로가기 생성과 브라우저 열기를 구현한 자료입니다. 실제 Windows 설치는 검증하지 못했으며, Smart App Control에 의한 실행 차단이 보고되었습니다.

설정 창에서 Supabase URL과 publishable 키를 입력합니다. **Skip / keep settings**를 누르면 미리보기로 실행됩니다. Supabase 프로젝트·SQL·두 사용자 계정 설정은 실제 채팅을 위해 한 번 필요합니다. 휴대폰에서 함께 쓰려면 별도 HTTPS 배포가 필요합니다. 자세한 설명은 설치 후 **Setup guide**에서 확인하세요.

설치 후 바탕화면 **OurChat**으로 실행하고 **OurChat Settings**에서 연결값을 변경합니다. 앱 사용 중 콘솔 창을 열어 두고 Q로 종료하세요. 재설치해도 기존 설정과 알림 키는 보존합니다.

이 파일은 Windows 실기기 설치를 검증하지 않았습니다. 클라우드에서 설치/설정 helper 검사 37개, PowerShell 구문, ZIP·체크섬 및 앱 lint/TypeScript 검사를 통과했습니다.

[소스 ZIP 다운로드](https://github.com/t2520176-oss/ChristmasGiftRoulette/raw/refs/heads/codex/ourchat-download/OurChat.zip) · [SHA256 체크섬](SHA256SUMS.txt)

This isolated delivery branch contains only the independent OurChat source archive, installer, checksums, and these instructions. No actual service credentials, user accounts, dependencies, or build output are published. The existing ChristmasGiftRoulette app branch is unchanged.
