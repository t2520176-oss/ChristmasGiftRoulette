# Verification checklist

This checklist separates local automated checks from tests requiring a real Supabase project, HTTPS deployment, and devices. An unchecked item has **not** been verified. Do not mark a test passed just because the UI or test code exists. Record the date, commit, browser/OS, and actual result when you run a deployment test. Do not record account passwords, tokens, keys, message text, or push subscription credentials in reports.

## Local automated checks

```sh
npm install
npm run lint
npm run typecheck
npm run test
npm run test:db
npm run build
```

Browser smoke checks (after the one-time Chromium installation):

```sh
npx playwright install chromium
npm run test:e2e
```

`test:db` runs the database test harness in `tests/database/run.mjs`. It uses an isolated PostgreSQL-compatible PGlite instance and fixtures, not your production database. Read the script and migration before running it. This verifies modeled database permissions and SQL behavior, but does not emulate Supabase Auth infrastructure, the hosted Realtime server, a browser, or Web Push delivery.

The implementation's local validation run passed **61 unit checks, 33 SQL database checks, and 6 browser smoke checks**. These counts describe local source/security/setup verification; they are not a hosted two-account or device-push result. Four hosted test instances were skipped because live credentials were not supplied.

- [ ] Dependency install succeeds using the committed manifest/lockfile.
- [ ] Lint, TypeScript, unit tests, database tests, and production build pass.
- [ ] No real `.env.local`, VAPID private key, service-role key, password, or filled administrator seed is included in the deliverable/Git history.
- [ ] Production bundle and browser network responses expose no server-only keys.

The build can be checked without service credentials. That result establishes compilation only. An unconfigured application must show an understandable configuration error without leaking secret values.

## Optional hosted browser integration suite

`tests/e2e/live-chat.spec.ts` provides executable tests for a separately configured **HTTPS** deployment with two real member accounts. It is skipped unless **all five** of these variables exist in the test process environment:

| Environment variable | Supply privately |
| --- | --- |
| `OURCHAT_E2E_BASE_URL` | The deployed HTTPS origin, without a path. |
| `OURCHAT_E2E_HUSBAND_EMAIL` | First pre-created member's real sign-in email. |
| `OURCHAT_E2E_HUSBAND_PASSWORD` | That account's real password. |
| `OURCHAT_E2E_WIFE_EMAIL` | Second pre-created member's real sign-in email. |
| `OURCHAT_E2E_WIFE_PASSWORD` | That account's real password. |

Use a private terminal/secret manager to supply these values to the process. Do not add them to `.env.example`, source files, committed fixtures, shared command output, or CI logs. The suite disables traces, screenshots, and video and uses generic sign-in failure messages to avoid recording credentials. It does not create or guess account identities.

After configuring the deployment, migration, two users, and member seed, run:

```sh
npm run test:e2e -- live-chat.spec.ts --project=desktop-chromium
```

With `OURCHAT_E2E_BASE_URL` provided, Playwright uses that deployment rather than launching the unconfigured local development server. The regular setup-only smoke suite is skipped. Only the desktop project executes these account tests, to avoid repeating private account activity for each viewport.

**Use a dedicated staging couple/project where practical and pause normal use during this test.** The suite adds three recognizable, randomly tagged messages to permanent history, advances read receipts, and invokes normal message delivery (including any existing push subscriptions). V1 intentionally does not allow deleting test messages. An offline draft is created but never sent. The tests use separate browser contexts to check two-account Unicode delivery, visible-message Read state, reload persistence, concurrent duplicate nonce retries, and missed-message recovery while retaining an unsent offline draft. A second test verifies signed-out message API access returns 401.

This hosted suite has **not been run here**: no real account credentials or deployment were supplied. It does not prove third-account/hosted WebSocket policy isolation or perform push provider/iPhone tests. Complete the remaining manual cases below even after it passes.

## Dedicated Supabase setup

- [ ] Migration applied to the independent OurChat project; no business-site project was modified.
- [ ] Email/password login enabled, public sign-ups disabled, anonymous sign-ins disabled.
- [ ] Exactly two confirmed Auth users manually created and their real UUIDs registered as members.
- [ ] `private` excluded from exposed API schemas; no client-editable member list.
- [ ] RLS enabled on all four public tables, required grants/policies present, and message/read-receipt Realtime publication enabled.
- [ ] Private Realtime Presence topic `ourchat:couple` is restricted to members; public channel access disabled where the setting exists.

## Authentication and database access

Use separate browser profiles/devices for Member A, Member B, a confirmed **nonmember** Auth account, and a signed-out browser. Temporarily create a third Auth account with administrator privileges for the negative test, without adding it to `members`; remove it afterward. Do not enable public sign-up just to create the negative fixture.

| Actor / operation | Expected result |
| --- | --- |
| Signed-out visitor opens `/chat` | Redirect to sign-in; no chat/push data. |
| Signed-out direct message/read/push mutations | Denied; no database changes. |
| Nonmember signs in / requests protected pages or APIs | Denied; no member names, messages, read positions, subscriptions, or Presence leaked. |
| Anon/nonmember direct Supabase SELECT/INSERT/UPDATE/DELETE | No unauthorized rows accessible or changed; RLS remains enabled. |
| Member reads messages / writes own valid message | Allowed. |
| Member forges the other `sender_id` | Denied. |
| Member updates/deletes a stored message | Denied in V1. |
| Member modifies `members` or the other person's push subscriptions | Denied. |
| Member attempts a third membership | Denied, including concurrent attempts. |
| Missing/foreign Origin on mutation API | Denied; matching configured origin accepted with valid session. |
| Client submits invalid types, empty/oversized text, bad IDs/nonces | Controlled validation error; no insertion or stack/secrets exposed. |
| Excessive requests | Limited by the implemented server/database controls. |

- [ ] Exercise the rows above against the actual hosted API using the appropriate authenticated sessions. Do not use a service-role key to test client RLS.
- [ ] Refresh `/chat` and navigate across routes; the authorized session persists/refreshes correctly.
- [ ] Sign out; protected data disappears and active Realtime subscriptions/listeners stop.
- [ ] After server-side member removal/session revocation, refresh/reconnect no longer grants chat access.

## Two-person messages and read state

- [ ] A sends to B and B sends to A; messages arrive without manual refresh in both browser profiles.
- [ ] One person's account is open on two devices; both receive the correct Realtime history.
- [ ] Refresh each browser; stored history and chronological order remain correct.
- [ ] More than 100 messages: latest 50 load first; scrolling up fetches earlier pages without duplicates, gaps, or scroll jumps.
- [ ] Send Korean text, emoji, mixed scripts, line breaks, and `<script>`/HTML-looking text: display as text, not executable markup.
- [ ] Verify blank/whitespace-only rejection, trimmed boundaries, exactly 2,000 Unicode code points accepted, and 2,001 rejected. Include emoji so UTF-16 code units are not mistaken for characters.
- [ ] On Windows, Enter sends and Shift+Enter adds a newline. IME composition Enter does not accidentally send. Mobile has a usable Send button.
- [ ] Retry the same sender/client nonce, including concurrent duplicate requests: one stored row and one rendered bubble, with the saved row returned to the retry.
- [ ] A Realtime event arriving before/after the POST response merges with the pending bubble without duplication.
- [ ] Take a browser offline: Offline appears; draft stays in the open interface; no false “sent” state. Restore the connection; missed history is fetched, sorted, and deduplicated.
- [ ] While viewing older history, an incoming message shows **New messages** without forcing scroll. At the bottom, new messages scroll into view.
- [ ] Read receipts advance only while the chat is visible and the relevant message is actually in the viewport. A background tab must not acknowledge unseen messages.
- [ ] Read updates are atomic and never move backward, including two devices posting out of order.
- [ ] The other person's read position produces the correct **Read** state on the sender's last relevant message.
- [ ] Close/disconnect one device and verify Presence falls back to offline; after reconnect it recovers without duplicate listeners. Presence is a connection hint, not proof that a message was seen.

## HTTPS PWA and Web Push

Use the final HTTPS origin with matching `NEXT_PUBLIC_SITE_URL`, Supabase Site URL, and correctly configured VAPID keys. Localhost secure-context support is useful for development but does not replace deployment testing.

- [ ] Manifest loads; name is OurChat, language/copy is English, start URL is `/chat`, standalone display and 192/512 icons are correct. Apple touch icon loads.
- [ ] Windows/Android install from a supported browser; launch the installed application.
- [ ] Service worker registers/updates at the intended origin and scope. Cache Storage contains no conversation, authenticated page, or login/API response.
- [ ] Permission is requested only immediately after the user presses **Enable notifications**; no automatic prompt at page load/sign-in.
- [ ] Permission denied, unsupported Push API, and registration/network failures produce clear English states without breaking chat.
- [ ] Register B on two supported devices, send from A: both valid B subscriptions receive a private notification. A's devices are not targeted.
- [ ] Inspect lock-screen content: title/body reveal no message text; body is **You have a new message.**
- [ ] Click notification with an existing app window: focus that window and open chat. With no app window: open `/chat`. Payload/user-provided URLs cannot redirect clicks elsewhere.
- [ ] Force a push provider 404/410 in a controlled test: expired subscription is deleted. Simulate a network/provider error: saved chat message remains and other valid subscriptions are still attempted.
- [ ] Sign out and switch accounts on a shared device: the prior account's device subscription is no longer used to show private notifications for the new account.
- [ ] Test OS notification restrictions, Do Not Disturb, background suspension, and offline behavior. Document observed limitations; do not promise delivery while the OS blocks it.

### Human-only iPhone test

**Requires a real iPhone running iOS 16.4 or later and a real HTTPS deployment. A desktop/mobile emulator is insufficient.**

- [ ] Safari → Share → Add to Home Screen; launch the installed Home Screen app.
- [ ] Sign in there, press **Enable notifications**, allow permission, and send from the other account.
- [ ] Check notification display and click navigation while foregrounded/backgrounded as the OS permits.
- [ ] Confirm the installation guide distinguishes the installed Home Screen app from an ordinary Safari tab.

## Deployment result record

Record only observations actually completed. Leave anything requiring unavailable credentials or hardware as **Not run**.

| Check | Result / environment |
| --- | --- |
| npm install / lint / typecheck / build | Passed in the local implementation environment. |
| Unit / SQL / local browser smoke checks | Passed: 61 / 33 / 6 respectively. |
| Optional hosted integration browser suite | Skipped; live credentials and a configured HTTPS deployment were not supplied. |
| Hosted two-account authentication / Realtime / RLS | Not run without the administrator's configured project and accounts. |
| HTTPS push / notification click / expired subscription cleanup | Not run without deployment credentials and devices. |
| Real iPhone Home Screen push | Human device test required. |
