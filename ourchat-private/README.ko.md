# OurChat 관리자 설정 안내

두 사람이 Windows·Android·iPhone에서 사용하는 문자 전용 개인 메신저입니다. **앱 화면·알림·설치 안내는 영어**이며, 한국어와 이모지 메시지는 지원합니다. 이 문서는 관리자용 한국어 요약입니다. 상세 절차는 [README.md](README.md), 검증 항목은 [docs/TESTING.md](docs/TESTING.md)를 확인하세요.

기존 사이트를 수정하거나 합치지 않는 독립 프로젝트입니다. 별도 비공개 Git 저장소, 별도 Supabase 프로젝트, 별도 Vercel 프로젝트로 운영하세요. 집 PC를 서버로 계속 켜둘 필요는 없습니다.

## 1. 브라우저 설치와 새 프로젝트

Windows Smart App Control에서 기존 CMD 설치기 실행 차단이 확인되었습니다. 해당 PC에서는 CMD를 실행하거나 보안 기능을 끄지 마세요. 새 웹 배포를 만든 뒤 Edge/Chrome의 앱 설치 기능을 사용합니다.

전달 파일 **OurChat-Start.html**은 브라우저에서 여는 단일 설정 도우미입니다. 새 프로젝트 설정 SQL, 새 Vercel 복제 화면, 앱 주소 연결과 설치 안내를 모았습니다. 파일을 여는 것만으로 서버 생성이나 실제 채팅 설치가 완료되는 것은 아닙니다. 로그인한 Supabase와 Vercel 화면에서 최초 연결과 배포는 필요합니다.

기존 작업 보호를 위해 반드시 **새 Supabase 프로젝트, 새 비공개 GitHub 저장소, 새 Vercel 프로젝트**를 사용합니다. 기존 프로젝트·데이터·도메인·환경변수는 수정하지 않습니다. 상세한 순서는 [브라우저 배포 안내](docs/DEPLOY-KO.md)를 확인하세요.

도우미가 만드는 통합 SQL은 새 프로젝트 검사, 테이블 생성, 두 사용자 등록을 한 트랜잭션으로 처리합니다. 기존 public 테이블/사용자 함수, private 스키마, 세 명 이상의 Auth 사용자가 있는 프로젝트는 변경 전에 거부합니다. 프로젝트 선택을 확인하는 일을 이 검사로 대신할 수는 없습니다.

### 수동 로컬 실행

Node.js 22 LTS 이상 지원 버전과 npm을 사용합니다.

```sh
cd ourchat-private
npm install
cp .env.example .env.local
```

아래 외부 설정을 마친 후 `.env.local`의 빈 값을 채웁니다. 비밀 키를 Git에 올리지 마세요.

```sh
npm run dev
```

브라우저에서 `http://localhost:3000`을 엽니다. 환경변수가 없을 때 보이는 안내는 설정 전 상태이며, 실제 서비스 준비 완료를 뜻하지 않습니다.

## 2. Supabase와 두 계정

1. OurChat 전용 Supabase 프로젝트를 만듭니다.
2. Authentication 설정에서 이메일·비밀번호 로그인을 켜고 **Allow new users to sign up**을 끕니다. 익명 로그인도 끕니다. 공개 가입 화면은 제공하지 않습니다.
3. 관리자가 Authentication → Users에서 실제 사용자 두 명을 직접 생성하고 확인 완료 상태로 만듭니다. 각자 다른 강한 비밀번호를 사용합니다. 이메일·비밀번호를 SQL이나 Git에 넣지 마세요.
4. `supabase/migrations/`의 SQL을 파일명 순서대로 전용 프로젝트에 적용합니다. 기존 설치에서는 아직 적용하지 않은 증분 migration만 실행합니다. Supabase CLI의 `supabase link --project-ref YOUR_PROJECT_REF`, `supabase db push`를 사용하거나 SQL Editor에서 각 파일을 한 번씩 실행합니다. 마지막 파일은 pg_cron을 통한 7일 보관 정리를 예약하므로 실제 예약 작업의 성공도 확인합니다. 자세한 내용은 [DB 안내](supabase/README.md)를 보세요.
5. [관리자 seed](supabase/seed-members.sql)에 각 사용자의 **실제 Auth UUID**를 넣어 `members`에 등록합니다. 임의 UUID를 만들지 마세요. 기본 이름 Husband/Wife는 관리자 seed에서 바꿀 수 있습니다. `psql` 변수 문법을 사용하는 파일이므로 SQL Editor에서는 해당 표현식을 실제 따옴표로 감싼 UUID로 바꿔 실행합니다. 작성한 실제 seed는 Git에 저장하지 않습니다.
6. API 노출 스키마에 `private`를 추가하지 마세요. 테이블 RLS를 끄지 마세요. 인증에 성공한 사용자라도 `members`에 없으면 채팅에 접근할 수 없습니다.
7. `messages`와 `read_receipts`가 `supabase_realtime` publication에 있는지 확인합니다. Presence는 `ourchat:couple` 비공개 채널과 migration의 회원 정책을 사용합니다. Realtime 설정에 공개 채널 허용 옵션이 있으면 끕니다.

데이터베이스에서 회원을 최대 두 명으로 제한합니다. 저장된 메시지가 있는 사용자의 삭제는 기록 보존을 위해 제한됩니다. 계정 교체는 일반 로그아웃과 다른 관리자 작업입니다.

메시지 아래 **Copy message** 버튼으로 본문 전체를 복사합니다. 복사 권한이 없으면 전체 글이 선택된 창에서 수동으로 복사할 수 있습니다. 메시지는 보낸 시각부터 **168시간(7일)** 보관합니다. 만료되면 조회와 화면에서 제외하고 서버가 매분 정리합니다. 이 정책은 기존 메시지에도 적용됩니다. 이미 사용자가 복사한 글은 기기의 클립보드에 남을 수 있습니다.

## 3. 환경변수와 알림 키

프로젝트 API 설정의 URL과 publishable 키를 `.env.local`에 넣습니다. 서버용 service-role 키는 `SUPABASE_SERVICE_ROLE_KEY`에만 넣고, `NEXT_PUBLIC_` 이름으로 절대 공개하지 않습니다.

```sh
npm run vapid
```

위 명령은 관리자가 직접 실행할 때 공개·비공개 키를 터미널에 출력하며 파일에 저장하지 않습니다. 출력된 키 쌍을 로컬과 Vercel 환경변수에 복사하세요. 비공개 키는 공개하거나 커밋하지 않습니다. 배포마다 키를 새로 만들지 마세요. 키 교체 시 기기 알림 구독을 다시 등록해야 합니다.

`.env.example`에는 실제 값 없이 아래 항목이 들어 있습니다.

- `NEXT_PUBLIC_SUPABASE_URL`: 전용 Supabase URL
- `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY`: 브라우저용 publishable 키
- `SUPABASE_SERVICE_ROLE_KEY`: 서버 전용 service-role 키
- `NEXT_PUBLIC_VAPID_PUBLIC_KEY`: 알림 공개 키
- `VAPID_PRIVATE_KEY`: 서버 전용 알림 비공개 키
- `VAPID_SUBJECT`: `mailto:` 뒤에 실제 관리자 이메일 주소
- `NEXT_PUBLIC_SITE_URL`: 로컬은 `http://localhost:3000`, 배포는 실제 HTTPS origin. 경로·끝 슬래시 제외

## 4. Vercel 배포

1. 이 디렉터리를 별도 비공개 저장소에 올린 뒤 **새 Vercel 프로젝트**로 가져옵니다. `.env.local`과 비밀 값을 올리지 마세요.
2. 프로젝트 환경변수에 위 모든 값을 등록합니다. `NEXT_PUBLIC_SITE_URL`은 실제 배포 HTTPS 주소로 설정합니다.
3. 배포 후 Supabase Authentication → URL Configuration의 **Site URL**도 동일한 HTTPS origin으로 맞춥니다. 허용 redirect URL에는 실제 앱 주소와 필요한 로컬 주소만 등록합니다. 광범위한 운영 wildcard는 피하세요.
4. 배포 주소·커스텀 도메인·환경변수를 바꾸면 관련 설정을 갱신하고 **재배포**합니다. 공개 환경변수는 빌드 시 포함됩니다. 도메인이 바뀌면 알림 구독도 새 origin에서 등록합니다.

## 5. 기기 설치와 알림

**iPhone/iPad는 iOS/iPadOS 16.4 이상**에서 Safari로 접속 → 공유 → 홈 화면에 추가 → 설치된 홈 화면 앱 실행 → 로그인 → **Enable notifications** 순서입니다. 일반 Safari 탭과 설치된 홈 화면 앱의 푸시 지원은 다릅니다. 실제 iPhone 시험은 사람이 기기에서 해야 합니다.

Android는 지원되는 Chrome 등의 설치 메뉴, Windows는 Edge/Chrome 등의 앱 설치 메뉴를 사용합니다. 설치 후 로그인하고 **Enable notifications**를 누릅니다. 사용자가 버튼을 누르기 전 자동 권한 팝업을 띄우지 않습니다.

상대방 계정의 등록된 여러 기기로만 알림을 보냅니다. 기본 본문은 **You have a new message.**이며 메시지 원문을 잠금 화면에 표시하지 않습니다. OS 절전·알림 거부·백그라운드 중단·네트워크 상태에 따라 지연되거나 도착하지 않을 수 있습니다. 저장된 메시지는 다시 접속하여 확인할 수 있습니다.

## 6. 검증과 남은 작업

```sh
npm run lint
npm run typecheck
npm run test
npm run test:db
npm run build
npx playwright install chromium
npm run test:e2e
```

`test:db`는 로컬 PGlite에서 실제 migration과 SQL 권한을 검사하며 Docker나 실제 Supabase 계정이 필요하지 않습니다. 운영 Supabase의 Auth·Realtime 서버·실제 기기 알림까지 검증하는 테스트는 아닙니다.

사용자만 설정할 수 있는 값은 Supabase 프로젝트·두 계정 UUID·비밀번호·실제 키·Vercel 배포 주소입니다. 키와 실제 기기가 제공되지 않았다면 **두 계정 실시간 채팅, 운영 RLS, HTTPS 푸시와 실제 iPhone 동작은 미검증**으로 남습니다. [검증표](docs/TESTING.md)에 실제 수행한 결과만 기록하세요.

V1은 종단간 암호화가 아닙니다. HTTPS와 RLS로 전송·접근을 보호하며, DB/호스팅 관리자는 저장 메시지에 접근할 수 있습니다. 서비스 워커는 대화나 인증 API 응답을 Cache Storage에 저장하지 않습니다. 무료 플랜의 DB·Realtime·전송량·서버 실행 한도와 이용 조건은 제공업체의 현재 정책을 확인해야 합니다.
