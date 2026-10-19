# OurChat 시작 파일

[OurChat 열기](https://ourchat-lwnvrnlkdwjpfwnzugfr.vercel.app/)

[**안드로이드 설치 파일 다운로드 — OurChat.apk**](https://github.com/t2520176-oss/ChristmasGiftRoulette/raw/refs/heads/codex/ourchat-download/OurChat.apk)

Android 7.0 이상에서 APK를 내려받아 열고 **설치**를 누르세요. 처음에는 내려받은 브라우저 또는 파일 앱의 **이 출처에서 설치 허용**을 요구할 수 있습니다. 설치 후 OurChat을 열고 기존 계정으로 로그인합니다. 최신 Chrome 등 지원 브라우저와 인터넷 연결이 필요합니다. 아이폰은 아래 Safari 홈 화면 추가 방법을 사용합니다.

알림은 앱 안의 휴대폰 버튼 → **Enable notifications**를 누른 뒤 요청되는 Android·브라우저 알림을 허용합니다. 실제 휴대폰의 설치·전체 화면 연결·백그라운드 알림은 아직 확인하지 않았습니다.

[**시작 파일 다운로드 — OurChat-Start.zip**](https://github.com/t2520176-oss/ChristmasGiftRoulette/raw/refs/heads/codex/ourchat-download/OurChat-Start.zip)

ZIP을 한 번 풀면 **OurChat.html** 파일 하나가 나옵니다. 바탕화면이나 다운로드 폴더에 보관하고, 다음에도 이 파일을 열면 이미 배포된 OurChat으로 연결됩니다. 서버를 다시 설치할 필요는 없습니다.

파일 미리보기에서 자동으로 이동하지 않으면 **OurChat 열기** 버튼을 누르세요. 인터넷 연결이 필요하며, 이 파일에 대화 내용이나 계정 비밀번호가 들어 있지는 않습니다.

화면은 기존 크림색 배경과 녹색 포인트로 복원했습니다. 같은 주소로 업데이트되므로 기존 시작 파일과 홈 화면 아이콘을 계속 사용할 수 있습니다. 이미 열어 둔 앱은 새로고침하거나 닫았다 다시 열어 주세요.

앱을 보고 있을 때 상대방의 새 메시지가 오면 화면 위에 팝업이 뜹니다. **View**로 최신 메시지를 보거나 **×**로 닫을 수 있습니다. 작성 중인 메시지와 입력 위치는 유지되며, 팝업에는 메시지 본문을 표시하지 않습니다. 앱이 닫힌 상태의 알림에는 아래의 기기 알림 허용이 필요합니다.

메시지의 기존 2,000자 제한과 글자 수 카운터를 제거했습니다. 긴 한글·이모지·여러 줄 메시지도 보낼 수 있습니다. 서버 전송 용량은 요청 전체 4 MiB까지이므로 매우 큰 메시지는 나눠 보내야 합니다.

휴대폰에서는 다음 방법으로 홈 화면 아이콘을 추가할 수도 있습니다.

- Android: Chrome에서 앱 주소 열기 → 메뉴 **⋮** → **앱 설치** 또는 **홈 화면에 추가**.
- iPhone: Safari에서 앱 주소 열기 → **Share(공유)** → **Add to Home Screen(홈 화면에 추가)** → **Add(추가)**. 공유 버튼이 보이지 않으면 먼저 **⋯**를 누르세요.

메시지 도착음은 앱을 새로고침한 뒤 채팅창의 **Sound off**를 눌러 **Sound on**으로 켭니다. 상대방의 새 메시지에만 도착음을 재생하며, 소리를 켰다는 선택은 이 브라우저에 기억됩니다. 다시 열었을 때 화면을 한 번 누르거나 입력하면 브라우저의 오디오가 준비됩니다.

아이폰의 백그라운드 알림은 홈 화면에 추가한 앱에서 휴대폰 버튼 → **Enable notifications** → **Allow**로 허용합니다. iOS 16.4 이상이 필요하며, 실제 소리는 아이폰의 알림 소리·음량·무음 모드·집중 모드 설정을 따릅니다. 실제 휴대폰의 소리와 잠금 화면 알림은 아직 확인하지 않았습니다.

두 휴대폰은 등록된 서로 다른 계정으로 로그인합니다. 가입 차단과 두 회원의 DB 접근 제한을 확인했습니다. 사용자 화면에서 로그인과 첫 메시지 저장을 확인했으며, 양방향 대화·실제 기기 알림은 별도 확인이 필요합니다.

이 전달용 브랜치는 기존 앱 브랜치와 분리되어 있습니다. 시작 파일에는 공개 앱 주소만 있으며, 기존 프로젝트·사이트·도메인·Git 연결은 변경하지 않았습니다.

`ourchat-private/`와 [OurChat-source.zip](https://github.com/t2520176-oss/ChristmasGiftRoulette/raw/refs/heads/codex/ourchat-download/OurChat-source.zip)은 기존 색상 복원과 새 메시지 팝업·도착음·긴 메시지·Android 연결을 포함한 현재 웹 소스 백업입니다. [OurChat-android-source.zip](https://github.com/t2520176-oss/ChristmasGiftRoulette/raw/refs/heads/codex/ourchat-download/OurChat-android-source.zip)은 별도의 Android 빌드 소스입니다. 계정 비밀번호, 배포 환경의 비밀 키, 개인 서명 키, 대화 데이터는 포함하지 않습니다. 이 백업으로 복구하려면 서버 설정이 별도로 필요합니다. 평소 메신저를 여는 용도는 **OurChat.apk** 또는 위의 **OurChat-Start.zip**입니다.

APK 빌드·서명·권한 검사 결과는 [OurChat-Android-Verification.md](OurChat-Android-Verification.md), 다운로드 파일 체크섬은 [OurChat-Android-SHA256.txt](OurChat-Android-SHA256.txt)에 있습니다. 개인 APK 서명 키는 공개하지 않았으며 이후 같은 앱의 업데이트에 필요합니다.

`OurChat-Start.html`, `OurChat-Setup.cmd`, `OurChat.zip`은 이전 설정 자료·초기 소스 스냅샷입니다. 지금 메신저를 다시 열기 위해 이 자료로 새 프로젝트를 만들거나 설치를 반복할 필요는 없습니다. `OurChat-Setup.cmd`는 해당 Windows PC의 Smart App Control에서 차단됐던 자료입니다.
