# OurChat on Cloudflare

[한국어 설치 안내](README.ko.md)

A separate two-person web chat deployment using Cloudflare Workers, static assets and D1. Use the newly created Cloudflare account so this application does not share the original account's free allowance. This source does not restore or modify the stopped Supabase service.

## Deploy to your new account

[![Deploy to Cloudflare](https://deploy.workers.cloudflare.com/button)](https://deploy.workers.cloudflare.com/?url=https%3A%2F%2Fgithub.com%2Ft2520176-oss%2FChristmasGiftRoulette%2Ftree%2Fcodex%2Fourchat-download%2Fourchat-cloudflare)

1. Verify that the selected Cloudflare account is the separate new account.
2. Connect GitHub. The button imports only the self-contained `ourchat-cloudflare/` folder from the approved `codex/ourchat-download` branch into a new repository.
3. Choose a Worker name that does not already exist in that account. Do not select an existing Worker or D1 database. Wrangler 4.151.2 provisions the `DB` binding automatically when both `database_name` and `database_id` are omitted. Reusing an existing Worker name can inherit that Worker's existing binding.
4. Enter these four values as **encrypted Worker secrets**: `MEMBER_1_EMAIL`, `MEMBER_1_PASSWORD`, `MEMBER_2_EMAIL`, `MEMBER_2_PASSWORD`. Use different emails and two different new passwords, each at least 8 characters long. Keep values out of chat, source files and plaintext configuration.
5. Confirm that **Deploy command is `npm run deploy`**. The deployment button automatically detects the repository's custom deploy script. It deploys the Worker and its new D1 binding, then applies remote D1 migrations using the binding name `DB`. Wait for the complete command to succeed; the chat API is unavailable until the initial schema migration completes. Open the resulting `workers.dev` URL and check both logins, two-way delivery, persistence after reload and an actual successful retention Cron run.

The button and instructions do not establish that a deployment has succeeded. Confirm the generated account, resources and URL in the dashboard. Stop to check the selection if the secret fields are missing or an existing resource is selected.

## Behavior and migration

Use the website directly or install it as a PWA through your browser. No APK is required. Save the new address; old bookmarks and notification permissions do not move automatically.

Messages have a copy button with a manual-copy fallback. Incoming messages can display a private in-app notice, and arrival sound is opt-in. The former 2,000-character limit is removed; a complete request is limited to 4MiB. Messages expire **168 elapsed hours after their server timestamp**; reads hide expired rows and a once-per-minute Cron cleans them up. Device clipboard copies and screenshots are outside this cleanup. Enable background notifications in settings; on iPhone, use a home-screen app on iOS 16.4 or later. Verify clipboard, sound and background notifications on the actual phones.

The old Supabase project is inactive, and the user has explicitly requested that it **not be restored**. Old accounts, passwords and chat history are not imported automatically. This deployment uses the two newly supplied logins and a new D1 database. Importing old history would require a separately available export.

Local verification passed 40 real Workers/D1 checks, Chromium/service-worker scenarios, and two-account real local server tests for reciprocal chat, persistence, copying and sign-out. Local results do not establish a working hosted login; verify each deployment at its own URL. Actual hosted Cron execution and physical-phone notification behavior also need verification there.

## Free-plan limits

### Photos

Use the photo button beside the message box, select a photo, optionally add a caption, and send. Photos are resized and compressed on the device to JPEG, at most128KiB and1600px on the longest edge; difficult images may be resized further. Decodable JPEG, PNG and WebP inputs work; HEIC depends on the browser. Originals larger than20MiB are rejected before processing. The original file is not uploaded or retained by OurChat.

Photos appear inline and can be opened or saved. Their URLs require an active login, are never cached by the service worker, and stop working at the same168-hour boundary as the message. Existing text copy, retries and notifications remain available. The dedicated D1 database stores small chunks with a64MiB total-photo budget; expired-message cleanup releases the space. A full photo budget rejects the photo without creating a partial message, while text still works. No additional storage service, credit card or paid resource is required for this implementation; Cloudflare's free request/database limits still apply.

Workers Free allows 100,000 requests per account per day and 10ms CPU per invocation. D1 Free includes 5 million rows read and 100,000 rows written per day, 500MB per database and 5GB per account. The account can have five Cron triggers; this app uses one every minute. D1 values and rows are limited to 2,000,000 bytes. Exceeding free limits can stop requests or database operations. Do not enable a paid plan or additional paid products for this free-only setup. Card requirements must be checked in the actual signup/account screen.

## Template checks and CLI use

`node scripts/check-deploy.mjs` checks the public template without network calls or deployment. It verifies the pinned Wrangler version, blank secret examples, the `DB` binding, the retention schedule and the deploy command. The deploy command uses `--provisioned` to allow D1 IDs/names supplied automatically by Cloudflare. Neither mode can verify the selected account, resource freshness, deployed secrets or production behavior. Check the public template without the flag before provisioning to confirm that it does not select an existing database.

For CLI deployment, set `CLOUDFLARE_ACCOUNT_ID` to the new account and compare `wrangler whoami` with the intended account. `wrangler secret put NAME` prompts for a hidden value and immediately creates/deploys a Worker version. The public `.dev.vars.example` contains only the four secret names with blank values.

Changing a password secret affects subsequent logins. Existing random sessions can remain valid for up to 30 days; to revoke them all, run `DELETE FROM sessions;` **only in this new OurChat D1**. Session deletion also removes its device push registrations. Never run this administrative command against the stopped Supabase project or another application's database.

Official references: [Deploy buttons](https://developers.cloudflare.com/workers/platform/deploy-buttons/), [automatic provisioning](https://developers.cloudflare.com/workers/wrangler/configuration/#automatic-provisioning), [secrets](https://developers.cloudflare.com/workers/configuration/secrets/), [Workers pricing](https://developers.cloudflare.com/workers/platform/pricing/), [D1 limits](https://developers.cloudflare.com/d1/platform/limits/).
