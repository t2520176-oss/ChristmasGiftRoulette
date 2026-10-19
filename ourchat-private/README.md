# OurChat

A private, text-only messenger for two people. One English-language Next.js PWA runs on Windows, Android, and iPhone. Supabase stores messages and manages authentication and Realtime; Vercel runs the application and push handlers. Your home PC does not need to stay on.

[한국어 관리자 안내](README.ko.md)

This is an independent project. Deploy it to its own Supabase project and its own Vercel project; do not merge its credentials, database, or deployment into an existing business site.

## What is included

- Email/password sign-in for two administrator-approved members, with no public sign-up UI or API.
- Text messages with Unicode/emoji and no product character cap, cursor history, retry deduplication, Realtime updates, and read receipts. Requests still have a 4 MiB transfer budget below the hosting gateway's limit; exceptionally large messages must be split.
- A copy button on every message, with a selected-text fallback when clipboard permission is unavailable. Messages are kept for 168 hours, hidden at expiry and pruned by an owner-only scheduled job every minute.
- Responsive light/dark UI, installation guidance, and optional privacy-preserving Web Push to the other person's registered devices.
- SQL migrations, a two-member administrator seed template, database security tests, and a manual device test checklist.

V1 does **not** provide end-to-end encryption. HTTPS and database access policies protect transport and access; the hosting/database administrator can access stored messages. Notification bodies say **“You have a new message.”** and do not contain message text. There are no file uploads, ads, analytics SDKs, AI APIs, or native IPA/EXE packages. The separate Android APK opens this same website using a browser Trusted Web Activity; it does not contain a separate backend or accounts.

## Browser installation

Windows Smart App Control has blocked the earlier unsigned CMD installer. Keep that protection enabled. The supported next route is a separate HTTPS deployment followed by the browser's app installation UI.

`OurChat-Start.html` is a standalone browser setup helper. It collects no passwords or API keys, performs no API writes, and guides creation of a **new Supabase project, new private Git repository, and new Vercel project**. Opening the file alone does not provision services or install a working chat. Existing applications, domains and databases must remain unchanged. See [the Korean deployment guide](docs/DEPLOY-KO.md).

Its combined SQL checks for an empty project before creating the schema and registering the supplied two Auth UUIDs in one transaction. Existing public application objects/private schema or more than two Auth users cause rejection. Always verify the selected project too.

## Manual local setup

Use Node.js 22 LTS or a supported newer LTS release and npm. The installed package versions and lockfile are authoritative.

```sh
cd ourchat-private
npm install
cp .env.example .env.local
```

Fill `.env.local` only after creating your own Supabase project and VAPID keys below. Do not commit this file. The application explains missing configuration; an unconfigured local preview is not a working production chat service.

```sh
npm run dev
```

Open `http://localhost:3000`. To validate the source locally:

```sh
npm run lint
npm run typecheck
npm run test
npm run test:db
npm run build
```

See [docs/TESTING.md](docs/TESTING.md) for what those checks cover and what still requires real accounts/devices. Passing a build without production credentials does not demonstrate a working Supabase deployment or push delivery.

For the browser smoke suite, install its browser once, then run:

```sh
npx playwright install chromium
npm run test:e2e
```

These browser tests do not replace a configured two-account integration test.

## 1. Create a separate Supabase project

1. Create a new project in your Supabase organization. Keep its database password private.
2. In **Authentication → Sign In / Providers** (Dashboard labels can vary), enable Email/password sign-in. Turn **Allow new users to sign up** off and disable anonymous sign-ins. Public registration must remain disabled.
3. In **Authentication → Users**, use the administrator's **Add/Create user** action to create exactly two email/password accounts. Confirm the users through the administrator option, so sign-in does not depend on an email-confirmation flow that this app does not implement. Choose real, strong passwords and share each password only with its owner. Do not put emails/passwords in SQL or Git.
4. Copy the two users' Auth **UUIDs** for the member seed. Authentication alone does not grant chat access: a user must also appear in `public.members`.
5. Copy the project URL and **publishable** key from the project's API settings. Put them in `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY`. Put the server's legacy `service_role` key in `SUPABASE_SERVICE_ROLE_KEY`; this server implementation uses that exact environment variable. Never put the service-role key in any `NEXT_PUBLIC_` variable, browser code, or client configuration.

Only the URL, publishable key, and VAPID **public** key are intended for the browser. RLS remains required even though a publishable key is public.

## 2. Apply the database migration and register two members

Apply migrations in filename order: [`202610180001_ourchat.sql`](supabase/migrations/202610180001_ourchat.sql), [`202610190001_remove_message_character_limit.sql`](supabase/migrations/202610190001_remove_message_character_limit.sql), [`202610190002_message_retention.sql`](supabase/migrations/202610190002_message_retention.sql), then [`202610190003_schedule_message_retention.sql`](supabase/migrations/202610190003_schedule_message_retention.sql). Existing installations apply only migrations they have not already applied. The final migration requires Supabase pg_cron; verify an actual successful scheduled run. See [database setup](supabase/README.md).

Choose one migration method:

- **Supabase CLI:** install the current Supabase CLI using its official installation instructions, run `supabase login`, then from this directory run `supabase link --project-ref YOUR_PROJECT_REF` and `supabase db push`. Replace the project reference with your own. Run against the dedicated OurChat project only.
- **Dashboard SQL Editor:** open the migration file and run its contents once in the new project's SQL Editor. Do not later reapply the same migration through another method without reconciling CLI migration history.

Next, follow [`supabase/seed-members.sql`](supabase/seed-members.sql). Insert the two **existing Auth UUIDs** using its documented administrator procedure. The seed supplies `Husband` and `Wife` as default display names; change these to other English names or real names if desired. It contains no real account IDs, emails, or passwords. Do not use an invented UUID: the foreign keys must resolve to the Auth users you just created. Keep the filled-in private seed outside Git.

The seed uses `psql` variables. If using the Dashboard SQL Editor instead, replace the two `:'..._user_id'` expressions with your actual quoted Auth UUIDs in the editor before executing; do not paste the unmodified `psql` syntax into SQL Editor. Full CLI/SQL details are in [supabase/README.md](supabase/README.md).

The database allows at most two members, prevents client-side membership edits, and retains message history when an account deletion would otherwise remove its sender. Changing the couple/account ownership later requires a deliberate administrator migration; deleting Auth users is not the normal way to reset this installation.

The migration includes RLS, grants, message deduplication, a monotonically increasing `mark_read` operation, abuse controls, and Realtime publication for messages/read receipts. Leave `private` **out of the API exposed schemas**. Security-definer helper functions are intentionally in that schema and are not a public API.

Realtime setup:

1. Confirm `public.messages` and `public.read_receipts` are in the `supabase_realtime` publication after migration.
2. Presence uses the private channel `ourchat:couple`. The migration adds authenticated member policies on `realtime.messages` for this presence topic; the client must subscribe with `private: true` only after authentication. If the Dashboard offers **Allow public access** for Realtime channels, disable it for this private project.
3. Do not disable table RLS to troubleshoot Realtime. Run the security tests and the third-account checks in [TESTING.md](docs/TESTING.md).

## 3. Generate and configure VAPID keys

Run this locally as the administrator:

```sh
npm run vapid
```

The command prints a new public/private pair without writing a file. Copy the output directly into `.env.local` and the hosting environment settings. Treat the private value like a password; do not commit it or paste it into an issue. Keep the same pair across deployments. Rotating keys requires devices to register new subscriptions.

Set these variables; `.env.example` deliberately leaves every value empty:

| Variable | Purpose |
| --- | --- |
| `NEXT_PUBLIC_SUPABASE_URL` | Your dedicated Supabase project URL. |
| `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` | The project's browser-safe publishable key. |
| `SUPABASE_SERVICE_ROLE_KEY` | Server-only service-role key for authorized push delivery and expired-subscription cleanup. |
| `NEXT_PUBLIC_VAPID_PUBLIC_KEY` | Generated browser-safe VAPID public key. |
| `VAPID_PRIVATE_KEY` | Generated server-only VAPID private key. |
| `VAPID_SUBJECT` | `mailto:` followed by your real administrator email address. |
| `NEXT_PUBLIC_SITE_URL` | Exact trusted origin: `http://localhost:3000` locally, the application's HTTPS origin in production. No path or trailing slash. |

The app checks request origins for mutations. A preview URL, custom domain, and production URL are different origins: configure the value for the deployment you are actually using. Public environment variables are embedded during build, so changing them requires a redeployment.

## 4. Deploy independently to Vercel

1. Create a new private Git repository for this directory, or import this directory into a new private repository yourself. Review `.gitignore` before the first commit; keep `.env.local`, generated keys, database passwords, and filled seeds out of Git.
2. Create a **new** Vercel project from that repository. Select Next.js. Use the Node.js runtime supported by the project; message/push Route Handlers run on the server, not your PC.
3. Add every environment variable above in Vercel's project settings for the intended deployment environment. Set `NEXT_PUBLIC_SITE_URL` to the final HTTPS origin. Do not reuse another app's secrets.
4. Deploy, then open the HTTPS URL. In Supabase **Authentication → URL Configuration**, set **Site URL** to that same origin. Limit allowed redirect URLs to this app's actual URLs and the local development URL if needed; avoid broad production wildcards. Password login does not require inventing an OAuth callback route.
5. If the first deployment URL was unknown when you added variables, update `NEXT_PUBLIC_SITE_URL` and Supabase URL settings, then redeploy. If you add a custom domain later, repeat this step and register push again on the new origin.
6. Run the account, Realtime, and HTTPS push checks below before using it for private conversations.

For later releases, commit the reviewed source changes and deploy through your private repository. Apply new SQL migrations to the same dedicated Supabase project, then redeploy the app as required. Environment changes also require redeployment. Never use the service-role key in the browser to bypass an access failure.

## Install and enable notifications

Notifications are optional and permission is requested only after you press **Enable notifications**. Browser and OS support vary; notifications cannot be guaranteed when an OS suspends background work, an app/browser is force-closed, permission is denied, the device is offline, or power-saving rules apply. Opening the app reconnects and reloads stored messages.

### iPhone / iPad

Use **iOS/iPadOS 16.4 or later**:

1. Open the HTTPS application URL in **Safari**.
2. Tap **Share → Add to Home Screen → Add**.
3. Launch **OurChat from its Home Screen icon** and sign in.
4. Press **Enable notifications** inside the installed app and allow notifications.

Web Push on these Apple devices is for installed Home Screen web apps. An ordinary Safari tab is not equivalent. If permission was denied, review the device's notification settings; repeatedly pressing a button cannot override an OS-level denial. Verify behavior on a real iPhone; a desktop emulator cannot prove Apple push works.

### Android

Open the HTTPS site in a supported browser such as Chrome, use the browser's **Install app** or **Add to Home screen** action when available, launch OurChat, sign in, and press **Enable notifications**. Check site permission and Android notification/battery settings if delivery is delayed.

### Windows

Use a supported browser such as Edge or Chrome. Install through the browser's install-app menu/address-bar action when offered. Sign in and press **Enable notifications**. Check browser/site permission, Windows notifications, and Do Not Disturb settings. Installation support and background behavior depend on the browser.

For each device, use the same person's account; push targets the **other member's** valid subscriptions, not the sender's devices. Old subscriptions returning HTTP 404/410 are removed by the server. A failed push must not remove an already saved message.

## Privacy and operating notes

- Messages and read receipts are stored in Supabase. Browser/session cookies are used for authentication. The service worker does not store conversations, authenticated pages, or login/API responses in Cache Storage.
- An offline draft stays in the current interface; do not assume an unsent draft survives closing/reloading the app. Offline status must not imply a message was delivered.
- Message times are stored as UTC and displayed in an English format in the device's local timezone. Korean text and emoji in messages are supported.
- Do not log message bodies, passwords, service keys, push endpoint credentials, or session tokens. Logs are not a substitute for the test checklist.
- Free/provider plans have changing limits and eligibility. Check the current Supabase database, storage, Realtime connections/events and egress quotas; Vercel execution/bandwidth/plan terms; and browser push-service restrictions. A free tier is not a promise of permanent free operation. This is a personal application, not a 24/7 delivery SLA.

## Verification status

Automated source checks can run without connecting this project to a real Supabase instance. They do not create your two users, verify your deployment's policies, or perform device push delivery. **Until your own environment variables, migrations, two member accounts, and HTTPS deployment are configured and the manual checklist is completed, this project must not be described as production-verified.**

Record actual check results in [docs/TESTING.md](docs/TESTING.md). Real two-account/reconnect testing and an actual iPhone push test remain administrator tasks when no credentials/devices were supplied to the implementer.
