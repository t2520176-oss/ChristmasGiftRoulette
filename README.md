# OurChat 시작 안내

[**OurChat 웹사이트 열기**](https://ourchat-lwnvrnlkdwjpfwnzugfr.vercel.app/)

위 주소에서 기존 계정으로 로그인하세요. 인터넷 연결이 필요합니다. 기존 시작 파일과 홈 화면 아이콘도 계속 같은 주소로 연결됩니다. 업데이트 뒤에는 열어 둔 앱을 새로고침하거나 닫았다 다시 열어 주세요.

휴대폰이나 PC에서는 웹사이트를 홈 화면·앱으로 설치해 사용할 수 있습니다.

- Android: Chrome에서 앱 주소 열기 → 메뉴 **⋮** → **앱 설치** 또는 **홈 화면에 추가**.
- iPhone: Safari에서 앱 주소 열기 → **Share(공유)** → **Add to Home Screen(홈 화면에 추가)** → **Add(추가)**. 공유 버튼이 보이지 않으면 먼저 **⋯**를 누르세요.
- Windows: Edge 또는 Chrome에서 앱 주소 열기 → 브라우저의 **앱 설치** 메뉴.

[**기존 시작 파일 다운로드 — OurChat-Start.zip**](https://github.com/t2520176-oss/ChristmasGiftRoulette/raw/refs/heads/codex/ourchat-download/OurChat-Start.zip)

ZIP을 풀어 나온 **OurChat.html**을 보관해 두고 열면 웹사이트로 연결됩니다. 자동으로 이동하지 않으면 **OurChat 열기**를 누르세요. 파일에 대화 내용이나 계정 비밀번호는 없으며, 서버를 다시 설치할 필요도 없습니다.

기존 크림색 배경과 녹색 포인트, 새 메시지 팝업·도착음·긴 메시지 기능을 유지합니다. 앱을 보고 있을 때 상대방 메시지가 오면 본문을 노출하지 않는 팝업이 뜹니다. **View**로 최신 메시지를 보거나 **×**로 닫을 수 있고, 작성 중인 글과 입력 위치는 유지됩니다.

도착음은 채팅창의 **Sound off**를 눌러 **Sound on**으로 켭니다. 선택은 해당 브라우저에 저장됩니다. 다시 열었을 때 화면을 한 번 누르거나 입력하면 오디오가 준비됩니다. 기존 2,000자 제한과 글자 수 카운터는 제거했으며, 요청 전체 용량은 4 MiB까지이므로 매우 큰 글은 나눠 보내세요.

새 웹 배포에 메시지 복사와 7일 보관 변경을 반영했습니다. 메시지 아래 **Copy message**를 누르면 본문 전체를 복사합니다. 복사 권한이 없으면 본문 전체가 선택된 창에서 수동으로 복사할 수 있습니다.

메시지 보관 기간은 **전송 시각부터 정확히 168시간(7일)**입니다. 전송 시각에 168시간을 더한 순간 만료되며, 기존 메시지에도 적용됩니다. 만료된 메시지는 조회와 화면에서 제외하고 서버가 매분 정리합니다. 실제 Supabase에 접근 제한(RLS)과 DB 소유자만 실행할 수 있는 매분 정리 예약을 적용했으며, 실제 예약 실행이 **2회 성공**했습니다. 사용자가 이미 복사해 둔 글은 기기의 클립보드 등 별도 복사본이므로 서버 정리 대상이 아닙니다.

기기 알림은 앱 안의 휴대폰 버튼 → **Enable notifications**에서 허용하세요. iPhone의 백그라운드 알림은 iOS 16.4 이상에서 홈 화면에 추가한 앱으로 실행해야 합니다. 실제 소리는 휴대폰의 알림 소리·음량·무음·집중 모드 설정을 따릅니다. 두 휴대폰은 등록된 서로 다른 계정으로 로그인합니다. **실제 휴대폰 두 대의 양방향 대화·푸시·잠금 화면 알림은 아직 확인하지 않았습니다.**

이번 복사·보관 변경은 단위 검사 **160개**, DB 검사 **60개**, 브라우저 검사 **12개**를 통과했습니다. 이 결과와 서버의 예약 실행 확인은 실제 휴대폰 시험을 대신하지 않습니다.

[OurChat.apk](https://github.com/t2520176-oss/ChristmasGiftRoulette/raw/refs/heads/codex/ourchat-download/OurChat.apk)는 이전에 만든 선택용 Android 설치 파일로 보존합니다. **이번 작업에서는 새 APK를 만들지 않았습니다.** 기존 APK의 빌드·서명·권한 기록은 [OurChat-Android-Verification.md](OurChat-Android-Verification.md), 체크섬은 [OurChat-Android-SHA256.txt](OurChat-Android-SHA256.txt)에 있습니다. 개인 APK 서명 키는 공개하지 않았으며 같은 앱의 향후 업데이트에 필요합니다.

`ourchat-private/`와 [OurChat-source.zip](https://github.com/t2520176-oss/ChristmasGiftRoulette/raw/refs/heads/codex/ourchat-download/OurChat-source.zip)은 복사 버튼과 168시간 보관을 포함한 101개 파일의 웹 소스 백업입니다. [OurChat-android-source.zip](https://github.com/t2520176-oss/ChristmasGiftRoulette/raw/refs/heads/codex/ourchat-download/OurChat-android-source.zip)은 이전 Android 빌드의 소스로 보존합니다. 백업에는 계정 비밀번호, 배포 환경의 비밀 키, 개인 서명 키, 대화 데이터는 포함하지 않습니다. 소스로 서버를 복구하려면 별도 설정이 필요합니다.

`OurChat-Start.html`, `OurChat-Setup.cmd`, `OurChat.zip`은 이전 설정 자료·초기 소스 스냅샷입니다. 지금 메신저를 열기 위해 새 프로젝트를 만들거나 설치를 반복할 필요는 없습니다. `OurChat-Setup.cmd`는 해당 Windows PC의 Smart App Control에서 차단됐던 자료입니다.

이 전달용 브랜치는 기존 앱 브랜치와 분리되어 있습니다. 시작 파일에는 공개 앱 주소만 있으며 기존 프로젝트·사이트·도메인·Git 연결은 변경하지 않았습니다.
