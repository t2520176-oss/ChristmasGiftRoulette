# OurChat: 브라우저만으로 새 서비스 만들기

Windows 설치 파일이나 명령어를 실행하지 않는 방법입니다. Supabase와 Vercel 계정이 있어도 새 프로젝트 설정과 배포는 한 번 필요합니다. 아래 절차는 사용자가 각 서비스 화면에서 진행합니다. **아직 사용자의 계정에 배포하거나 실제 기기에서 확인한 상태는 아닙니다.**

기존 작업을 보존하기 위해 **새 Supabase 프로젝트, 새 비공개 GitHub 저장소, 새 Vercel 프로젝트**를 사용합니다. 기존 프로젝트의 데이터베이스·도메인·환경변수·연결된 저장소는 바꾸지 마세요. 이름이 이미 사용 중이면 기존 것을 선택하지 말고 다른 새 이름을 입력합니다.

**OurChat-Start.html** 단일 도우미에서도 아래 순서를 진행할 수 있습니다. 파일만 폰에 보내면 메신저가 되는 방식은 아닙니다. 첫 배포를 완료한 뒤 **새 HTTPS 앱 주소**를 폰에 보내고 등록한 두 계정으로 로그인합니다. 도우미가 만드는 통합 SQL을 실행했다면 아래 1절의 migration과 회원 SQL을 다시 실행하지 않습니다.

계정 비밀번호와 키를 대화나 GitHub 파일에 넣지 마세요. Supabase 화면에서 확인한 값은 새 Vercel 프로젝트의 환경변수 입력란에 직접 넣습니다. 화면의 메뉴 이름은 서비스 업데이트에 따라 달라질 수 있습니다.

## 1. OurChat 전용 Supabase 프로젝트

1. [Supabase Dashboard](https://supabase.com/dashboard)에서 **New project**로 프로젝트를 새로 만듭니다. 예시 이름은 `OurChat`입니다. 기존 프로젝트를 열어 수정하는 절차가 아닙니다.
2. 프로젝트가 준비되면 **Authentication**의 설정에서 이메일·비밀번호 로그인을 사용하도록 설정합니다. **Allow new users to sign up**과 익명 로그인은 끕니다. 이메일 확인만으로 아무나 가입할 수 있게 만들지 않습니다.
3. 같은 새 프로젝트의 **Authentication → Users**에서 관리자 기능으로 실제 사용자 두 명을 만듭니다. 각자의 이메일과 서로 다른 비밀번호를 사용하고, 이메일 확인 완료 상태로 생성합니다. **Auto confirm** 같은 선택 항목이 있으면 확인합니다. 사용자 목록에서 두 사람의 **User UID / UUID**를 따로 확인합니다.
4. [데이터베이스 생성 SQL](../supabase/migrations/202610180001_ourchat.sql)을 엽니다. GitHub에서 **Raw**로 전체 내용을 확인하고 복사합니다. 새 프로젝트의 **SQL Editor → New query**에 붙여넣고 전체를 **한 번** 실행합니다. 이미 테이블을 만든 프로젝트에서 다시 실행하는 파일이 아닙니다.
5. [SQL Editor용 회원 등록 파일](../supabase/seed-members-dashboard.sql)을 새 쿼리에 붙여넣습니다. 파일 안내대로 두 사람의 **실제 Auth UUID**를 채운 후 실행합니다. 임의로 만든 UUID나 이메일을 대신 넣지 않습니다. 비밀번호는 SQL에 넣지 않습니다. 결과에 두 사람만 나타나는지 확인합니다.
6. `messages`, `read_receipts`의 Realtime publication 설정을 확인합니다. 앞의 생성 SQL이 두 테이블을 `supabase_realtime`에 등록합니다. Realtime 설정에 공개 채널 허용 옵션이 있으면 끕니다. `private` 스키마를 API 노출 목록에 추가하거나 RLS를 끄지 않습니다.

생성 SQL 실행이 실패하면 오류 내용을 먼저 확인하세요. 기존 데이터를 지우거나 RLS를 꺼서 해결하지 마세요. 실제 값이 든 SQL을 GitHub에 저장하지 않습니다.

## 2. 새 Vercel 프로젝트에 넣을 두 값 준비

방금 만든 Supabase 프로젝트의 연결/API 설정에서 다음을 확인합니다. 화면에 따라 **Connect**, **Project Settings → API**, **API Keys** 등에 있습니다.

| Vercel 환경변수 이름 | Supabase에서 가져올 값 |
| --- | --- |
| `NEXT_PUBLIC_SUPABASE_URL` | 새 프로젝트의 URL. `https://...supabase.co` 형태 |
| `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` | 브라우저용 **publishable** 키 |

`service_role`이나 secret 키를 두 번째 칸에 넣지 마세요. 이 단계에서는 서버용 키와 푸시 알림 키가 필요하지 않습니다.

## 3. 기존 저장소와 분리하여 새로 배포

원본은 [전달용 브랜치의 OurChat 소스](https://github.com/t2520176-oss/ChristmasGiftRoulette/tree/codex/ourchat-download/ourchat-private)에 있습니다. 원본 저장소의 기본 브랜치는 다른 앱이므로 Vercel에서 원본 저장소 전체를 직접 가져오지 않습니다.

[**새 OurChat 프로젝트 만들기**](https://vercel.com/new/clone?repository-url=https%3A%2F%2Fgithub.com%2Ft2520176-oss%2FChristmasGiftRoulette%2Ftree%2Fcodex%2Fourchat-download%2Fourchat-private&project-name=ourchat&repository-name=ourchat-private&env=NEXT_PUBLIC_SUPABASE_URL%2CNEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY)

1. 위 링크로 Vercel의 템플릿 복제 화면을 엽니다. 로그인이나 GitHub 연결이 필요하면 자신의 계정으로 진행합니다.
2. **새 GitHub 저장소**를 만듭니다. 예시 이름은 `ourchat-private`이며, 비공개로 설정합니다. 기존 저장소를 덮어쓰거나 원본 앱의 브랜치를 바꾸지 않습니다. 같은 이름이 있으면 새 이름을 사용합니다.
3. **새 Vercel 프로젝트**를 만듭니다. 예시 이름은 `ourchat`입니다. 기존 프로젝트로 연결하거나 기존 도메인을 옮기지 않습니다.
4. 복제된 앱의 `package.json`이 있는 폴더를 Root Directory로 선택합니다. 위 링크가 앱 폴더만 복제하면 루트 `./`를 사용합니다. 폴더 구조가 유지되어 `ourchat-private/package.json`으로 보이면 Root Directory를 `ourchat-private`로 지정합니다. Framework는 **Next.js**, Node.js는 **24.x**를 선택합니다. 앱이 없는 다른 폴더를 지정하지 않습니다.
5. 요청되는 두 환경변수에 2절에서 확인한 값을 직접 넣고 배포합니다. 추가 설정이 나타나면 기본 Next.js 빌드 설정을 사용합니다. 이 문서의 복제 링크는 사용자의 Vercel 계정에서 아직 검증되지 않았으므로, 다른 앱이 선택되거나 기존 프로젝트 변경을 요구하면 진행하지 마세요.

GitHub 복제 후에는 **새 저장소의 기본 브랜치**가 이 새 Vercel 프로젝트의 운영 브랜치입니다. 원본 저장소의 `codex/ourchat-download`를 기존 Vercel 프로젝트의 운영 브랜치로 바꾸는 절차가 아닙니다.

## 4. 고정 배포 주소 설정 후 다시 배포

첫 배포로 HTTPS 주소를 얻은 뒤 앱이 사용할 주소를 고정합니다. 앞의 두 값만 넣어 빌드가 성공하더라도, 주소 설정을 끝내기 전에는 로그인과 메시지 전송이 준비된 상태가 아닙니다.

1. **새 Vercel 프로젝트**의 Domains 또는 배포 화면에서 운영용 고정 주소를 확인합니다. 예를 들어 `https://새프로젝트이름.vercel.app` 형태입니다. 매 배포마다 달라지는 미리보기 주소를 사용하지 않습니다. 기존 프로젝트 도메인은 사용하지 않습니다.
2. 이 프로젝트의 **Settings → Environment Variables**에 다음 값을 추가합니다. 적용 대상은 **Production**으로 선택합니다.

   | 이름 | 값 |
   | --- | --- |
   | `NEXT_PUBLIC_SITE_URL` | 확인한 운영용 `https://...` 주소. 뒤의 `/`와 `/login` 같은 경로 제외 |

3. **Deployments**에서 방금 만든 프로젝트를 **Redeploy**합니다. 환경변수 저장만으로 이미 빌드한 앱에 반영되지는 않습니다.
4. 새 Supabase 프로젝트의 **Authentication → URL Configuration**에서 **Site URL**을 같은 HTTPS 주소로 설정합니다. redirect URL을 등록할 경우 실제로 사용하는 정확한 앱 주소만 사용합니다.
5. 새 운영 주소를 열어 로그인합니다. **OurChat is not configured yet**가 표시되면 Vercel 환경변수와 재배포 여부를 확인합니다. **You do not have access**라면 Supabase의 두 사용자 UUID가 회원 등록 결과와 일치하는지 확인합니다.

주소를 바꿀 때는 위 두 서비스의 설정도 새 주소에 맞추고 재배포합니다. 기본 채팅은 서버용 service-role 키와 VAPID 키 없이 먼저 확인할 수 있습니다.

## 5. 두 계정 확인 후 앱으로 설치

1. PC 브라우저에서 첫 번째 계정으로 로그인합니다. 휴대폰이나 다른 브라우저 프로필에서 두 번째 계정으로 로그인합니다.
2. 두 방향으로 테스트 메시지를 보냅니다. 같은 계정으로 양쪽에 로그인하는 것은 두 사람의 채팅 확인을 대신하지 않습니다. 수신·읽음 표시·로그아웃 후 재접속을 확인합니다.
3. Windows에서는 Edge 또는 Chrome으로 운영 주소를 엽니다. 주소창의 설치 아이콘이나 브라우저 메뉴의 **앱 설치 / Install app**를 선택합니다. 메뉴 이름은 브라우저 버전에 따라 다릅니다. CMD 파일을 실행하거나 Smart App Control을 끌 필요가 없습니다.
4. Android에서는 Chrome의 앱 설치 메뉴를 사용합니다. iPhone/iPad에서는 Safari의 **공유 → 홈 화면에 추가** 후 홈 화면 아이콘으로 실행합니다.

최초 배포와 각 기기 로그인은 필요하지만, 그 뒤에는 만들어진 앱 아이콘으로 열 수 있습니다. 사용 중인 PC를 서버로 계속 켜둘 필요는 없습니다.

## 푸시 알림은 별도 단계

기본 채팅과 앱 설치를 먼저 확인합니다. 앱을 닫았을 때 받는 웹 푸시에는 `SUPABASE_SERVICE_ROLE_KEY`, `NEXT_PUBLIC_VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`, `VAPID_SUBJECT`가 추가로 필요합니다. 아직 키를 생성하지 않았다면 임의 값을 넣거나 공개 웹사이트에 비공개 키를 붙여넣지 마세요. 같은 키 쌍을 보존해야 하므로 기본 배포 후 별도로 설정합니다.

명령어 없이 알림 키를 만들려면 [오프라인 키 생성 파일](../tools/generate-vapid.html)을 다운로드하여 브라우저에서 엽니다. 버튼을 눌렀을 때만 브라우저 안에서 키를 만들며 외부로 보내거나 저장하지 않습니다. 각 값을 새 Vercel 프로젝트의 해당 환경변수에 직접 넣고 안전하게 보관하세요. 창을 닫으면 이 페이지에서 키가 사라집니다.

iPhone 푸시는 iOS/iPadOS 16.4 이상에서 홈 화면에 추가한 앱을 열고 사용자 동작으로 알림을 허용해야 합니다. 실제 기기의 알림 수신은 이 안내만으로 검증된 것이 아닙니다.
