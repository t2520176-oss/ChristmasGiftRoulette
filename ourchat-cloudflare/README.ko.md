# OurChat — Cloudflare 무료 웹 앱

두 사람만 사용하는 웹 채팅을 Cloudflare Workers와 D1에 별도로 설치하는 소스입니다. 화면, 로그인, 채팅 API와 데이터베이스가 새 Cloudflare 계정에서 실행됩니다. 브라우저와 인터넷 연결이 필요하며 APK는 필요하지 않습니다.

## 새 Cloudflare 계정에 설치

[![Deploy to Cloudflare](https://deploy.workers.cloudflare.com/button)](https://deploy.workers.cloudflare.com/?url=https%3A%2F%2Fgithub.com%2Ft2520176-oss%2FChristmasGiftRoulette%2Ftree%2Fcodex%2Fourchat-download%2Fourchat-cloudflare)

1. **이번에 만든 별도 Cloudflare 계정**으로 로그인했는지 확인하세요. 기존 작업이 있는 계정을 선택하면 그 계정의 무료 한도를 함께 사용하게 됩니다.
2. 위 버튼을 누르고 GitHub 연결을 허용합니다. 공개 브랜치 `codex/ourchat-download`의 `ourchat-cloudflare/`만 독립 저장소로 복사하는 경로입니다. 원본 앱이나 기존 서버를 덮어쓰는 작업이 아닙니다.
3. Worker 이름은 해당 계정에 **아직 없는 새 이름**으로 정하세요. 기존 Worker나 D1 데이터베이스를 선택하지 마세요. 이 소스는 D1 이름과 ID를 지정하지 않으며 새 Worker용 `DB`를 자동 생성하도록 구성합니다. 같은 이름의 기존 Worker를 사용하면 그 Worker의 DB 연결을 이어받을 수 있으므로 이름 확인이 필요합니다.
4. 다음 네 값을 배포 화면의 **암호화된 Secret** 입력란에 입력하세요. 두 이메일은 서로 달라야 하며, 비밀번호도 **각각 8자 이상인 서로 다른 새 값**으로 정합니다. 실제 값은 채팅, 공개 저장소, 일반 환경 변수에 넣지 마세요.

   | Secret 이름 | 입력할 내용 |
   |---|---|
   | `MEMBER_1_EMAIL` | 첫 번째 사람의 로그인 이메일 |
   | `MEMBER_1_PASSWORD` | 첫 번째 사람의 새 비밀번호 |
   | `MEMBER_2_EMAIL` | 두 번째 사람의 로그인 이메일 |
   | `MEMBER_2_PASSWORD` | 두 번째 사람의 새 비밀번호 |

5. 배포 설정의 **Deploy command가 `npm run deploy`**인지 확인하세요. 배포 버튼은 `package.json`의 사용자 정의 deploy 스크립트를 자동 감지합니다. 이 명령은 Worker와 새 D1 연결을 배포한 뒤 **`DB` 바인딩 이름**으로 원격 D1 스키마를 적용합니다. 전체 명령이 성공할 때까지 기다린 다음 콘솔에 표시되는 새 `https://….workers.dev` 주소를 열고 두 계정으로 각각 로그인하세요. 첫 배포에서 스키마 적용이 끝나기 전에는 채팅 API를 사용할 수 없습니다.

이 안내와 배포 버튼은 실제 배포 성공의 증거가 아닙니다. 생성된 주소에서 로그인, 서로 주고받기, 새로고침 뒤 저장 여부, 실제 Cron 성공을 확인해야 합니다. Secret 입력란이 표시되지 않거나 다른 계정·기존 리소스가 선택되면 배포를 진행하지 말고 선택을 확인하세요.

## 사용과 보관

채팅 주소는 Android Chrome의 **앱 설치/홈 화면에 추가**, iPhone Safari의 **공유 → 홈 화면에 추가**로 보관할 수 있습니다. 이전 사이트의 바로가기는 새 주소로 자동 변경되지 않으므로 새 주소를 저장하세요.

메시지의 **Copy message** 버튼으로 본문을 복사합니다. 자동 복사가 허용되지 않으면 선택된 본문을 수동으로 복사할 수 있습니다. 상대방의 새 메시지는 앱을 보고 있을 때 팝업으로 안내하며, 도착음은 사용자가 켭니다. 메시지의 이전 2,000자 제한은 사용하지 않지만 요청 전체 용량은 **4MiB**까지이므로 매우 큰 글은 나눠 보내야 합니다.

메시지는 서버 전송 시각부터 **168시간(7일)** 보관합니다. 만료 시 조회에서 제외하고 매분 예약 작업이 정리합니다. 예약 실행이 지연되더라도 만료된 글이 다시 조회되어서는 안 됩니다. 이미 복사한 글, 스크린샷과 기기 알림에 남은 내용까지 지우는 기능은 아닙니다.

백그라운드 알림은 설정의 **Enable background notifications**에서 새로 허용합니다. iPhone은 iOS 16.4 이상에서 홈 화면에 추가한 앱을 사용하세요. 로컬 실제 Workers/D1 검사 30개, 휴대폰 크기의 Chromium·서비스 워커 검사, 두 계정의 실제 로컬 서버 왕복·새로고침·복사·로그아웃 검사를 통과했습니다. 실제 로그인과 휴대폰 두 대의 소리·백그라운드 알림, 운영 Cron 실행은 배포한 새 주소에서 확인합니다. 새 사이트는 이전 사이트의 알림 권한이나 구독을 자동으로 이어받지 않습니다.

## 이전 서버와 데이터

기존 Supabase 프로젝트는 중지되어 있으며 **복구하지 않도록 요청받은 상태**입니다. 이 설치는 이전 서버를 켜거나 변경하지 않습니다. 이전 로그인 계정·비밀번호·대화는 자동으로 가져오지 않으며, 여기서는 위 Secret으로 지정한 두 로그인과 새 데이터베이스를 사용합니다. 이전 대화의 가져오기는 접근 가능한 별도 내보내기 자료가 있어야 검토할 수 있습니다.

## 무료 범위

Workers Free와 D1 Free를 사용합니다. 별도 계정의 다른 앱도 같은 계정 한도를 공유하므로 이번 앱을 위한 새 계정을 선택하세요.

| 항목 | 무료 한도 |
|---|---|
| Worker 요청 | 계정당 하루 100,000회 |
| Worker CPU | 호출당 10ms |
| D1 읽기/쓰기 | 하루 500만/10만 행 |
| D1 저장 | DB당 500MB, 계정당 5GB |
| Cron | 계정당 5개; 이 앱은 매분 1개 사용 |

한도를 넘으면 요청이나 DB 작업이 실패할 수 있습니다. 유료 플랜으로 업그레이드하거나 별도 유료 제품을 활성화하지 마세요. 무료 사용을 영구 보장하거나 무제한 운영을 약속하는 구성은 아닙니다. 카드 요구 여부는 실제 계정 화면에서 확인해야 합니다.

## 개발자 배포 확인

Wrangler는 **4.151.2**로 고정합니다. 공개 원본은 다음 D1 구성으로 시작합니다.

```json
"d1_databases": [{ "binding": "DB", "migrations_dir": "migrations" }]
```

`database_name`을 지정하면 같은 계정의 기존 DB를 찾아 연결할 수 있습니다. `database_id`도 기존 DB를 직접 가리킬 수 있으므로 공개 배포 원본에는 둘 다 넣지 않습니다. 새 Worker 이름의 자동 생성 DB 이름이 이미 사용 중이면 생성 오류를 확인하고 기존 DB를 임의로 채택하지 마세요. 재배포는 같은 Worker에 이미 연결된 DB를 이어받으므로 매번 새 DB를 만들지 않습니다.

배포 전 공개 템플릿만 검사하려면 `node scripts/check-deploy.mjs`를 실행합니다. deploy 명령의 `--provisioned` 모드는 Cloudflare가 자동 생성한 DB ID·이름을 추가한 경우에도 배포 설정을 검사합니다. 두 모드 모두 네트워크 요청이나 배포를 하지 않으며 실제 계정 선택, 새 DB 여부, 운영 성공을 증명하지 않습니다. 원본 템플릿의 기존 DB 지정 방지는 기본 모드로 별도 확인해야 합니다.

CLI를 사용한다면 새 계정 ID를 `CLOUDFLARE_ACCOUNT_ID`로 명시하고 `wrangler whoami` 결과와 대조하세요. 비밀번호 입력은 `wrangler secret put MEMBER_1_PASSWORD`처럼 가려진 입력을 지원합니다. 이 명령은 Secret을 저장하고 Worker 버전을 배포하므로 공개 원본 검사와 구분해 사용하세요. 공개 `.dev.vars.example`에는 네 이름과 빈 값만 남깁니다.

관리자가 비밀번호 Secret을 바꾸면 다음 로그인부터 새 비밀번호가 적용됩니다. 이미 로그인한 세션은 최대 30일간 유지되므로 모두 로그아웃시키려면 **이번에 만든 OurChat 전용 D1에서만** `DELETE FROM sessions;`를 실행합니다. 이 작업은 해당 세션에 연결된 기기 푸시 등록도 취소합니다. 기존 Supabase나 다른 앱의 DB에서는 실행하지 않습니다.

공식 근거: [Deploy 버튼](https://developers.cloudflare.com/workers/platform/deploy-buttons/), [D1 자동 생성](https://developers.cloudflare.com/workers/wrangler/configuration/#automatic-provisioning), [Secret 설정](https://developers.cloudflare.com/workers/configuration/secrets/), [Workers 요금](https://developers.cloudflare.com/workers/platform/pricing/), [D1 한도](https://developers.cloudflare.com/d1/platform/limits/).
