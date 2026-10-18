# OurChat 시작

**폰에 보내는 것은 최초 배포 후의 앱 주소입니다.** 아래 HTML은 설정 도우미이며, 파일만 폰에 보내면 채팅이 되는 설치 파일이 아닙니다. 아직 사용자의 계정에 배포하거나 실제 메신저 주소를 만든 상태가 아닙니다.

[**OurChat-Start.html 다운로드**](https://github.com/t2520176-oss/ChristmasGiftRoulette/raw/refs/heads/codex/ourchat-download/OurChat-Start.html)

파일을 다운로드하여 브라우저에서 열면 새 프로젝트 설정 SQL 복사, Vercel 배포 시작, 앱 주소 연결과 앱 열기를 한 화면에서 진행할 수 있습니다. 로그인과 최초 서비스 연결·배포는 각 서비스 화면에서 한 번 필요합니다. HTML이 글자로 열리면 [파일 페이지](https://github.com/t2520176-oss/ChristmasGiftRoulette/blob/codex/ourchat-download/OurChat-Start.html)의 **Download raw file(↓)**로 저장하세요.

[**파일 없이 Vercel 새 프로젝트 복제 화면 열기**](https://vercel.com/new/clone?repository-url=https%3A%2F%2Fgithub.com%2Ft2520176-oss%2FChristmasGiftRoulette%2Ftree%2Ffe19d69fb83c16449da0b82c5269ac51d3bd9c6d%2Fourchat-private&project-name=ourchat&repository-name=ourchat-private&env=NEXT_PUBLIC_SUPABASE_URL%2CNEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY&envDescription=Use+only+the+URL+and+publishable+key+of+your+NEW+OurChat+Supabase+project.)

기존 작업을 보호하기 위해 **새 Supabase 프로젝트, 새 비공개 GitHub 저장소, 새 Vercel 프로젝트**만 사용합니다. 기존 프로젝트·데이터·사이트·도메인·환경변수는 수정하지 마세요. 이름이 이미 사용 중이면 다른 새 이름을 고릅니다. 새 기본 HTTPS 주소를 사용합니다.

1. [Supabase](https://supabase.com/dashboard)에서 OurChat 전용 새 프로젝트와 확인된 Auth 사용자 두 명을 만듭니다. 공개 가입과 익명 로그인은 끕니다.
2. 도우미에 실제 Auth UUID 두 개를 넣고, 복사한 통합 SQL을 **새 프로젝트의 SQL Editor**에서 한 번 실행합니다. 기존 public 테이블·사용자 함수·private 스키마 등이 있으면 변경 전에 중단합니다. 프로젝트 선택 확인을 이 검사로 대신할 수는 없습니다.
3. Vercel 복제 화면에서 새 비공개 저장소와 새 Vercel 프로젝트를 만들고, 새 Supabase URL과 publishable 키를 직접 넣어 배포합니다. 기존 앱 저장소 전체를 기존 Vercel 프로젝트에 연결하지 않습니다.
4. 새 운영 주소를 Vercel의 `NEXT_PUBLIC_SITE_URL`과 새 Supabase의 Site URL에 넣고 재배포합니다. 이후 주소를 폰에 보내 등록한 계정으로 로그인하고 브라우저에서 앱으로 설치합니다.

계정 비밀번호나 API 키는 대화·HTML·GitHub에 넣지 마세요. Vercel 환경변수 화면에 직접 입력합니다.

[상세한 브라우저 배포 안내](ourchat-private/docs/DEPLOY-KO.md) · [현재 소스](ourchat-private) · [검증 범위](ourchat-private/docs/BROWSER-HELPER.md)

브라우저/DB 검사 11개, lint와 TypeScript 검사를 통과했습니다. 실제 Windows HTML 파일 열기, 로그인한 Vercel 복제 화면, 운영 Supabase·휴대폰 설치는 미검증입니다. 클라우드 브라우저의 file: 정책 때문에 브라우저 동작 검사는 같은 HTML을 로컬 HTTP로 제공하여 수행했습니다. 보안 정책을 변경하지 않았습니다.

기존 `OurChat-Setup.cmd`는 Smart App Control에서 차단된 과거 자료입니다. 해당 PC에서는 실행하거나 보안 기능을 비활성화하지 마세요. `OurChat.zip`은 이전 소스 스냅샷이며 현재 웹 배포 소스는 위 폴더입니다. 체크섬 검사는 Windows의 앱 신뢰 승인을 뜻하지 않습니다.

This isolated delivery branch contains the OurChat materials. Existing app branches and external Supabase/Vercel projects have not been modified. The new-template link is pinned to source commit `fe19d69fb83c16449da0b82c5269ac51d3bd9c6d` and contains no credentials.
