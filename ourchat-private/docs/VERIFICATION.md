# Local verification — 2026-10-18

Implemented independently in `ourchat-private`; the pre-existing repository was not modified.

| Check | Observed result |
| --- | --- |
| `npm install` | Completed; reproducible dependency lockfile included. |
| `npm run lint` | Passed, no warnings. |
| `npm run typecheck` | Passed, strict TypeScript. |
| `npm test` | 61 passing unit/component/service-worker/API checks. |
| `npm run test:db` | 33 passing PostgreSQL/PGlite ACL, RLS, constraint, trigger and RPC checks against the actual migration. |
| `npm run build` | Passed with Next.js 16.4.2 and React 19.3.1, without production credentials. |
| `npm run test:e2e` | 6 passing desktop/mobile Chromium setup/security/PWA smoke checks; 4 hosted test instances skipped. |
| Browser bundle inspection | No server-only environment-variable names in `.next/static`. |
| Secret-file inspection | Only the empty `.env.example` exists in the delivered source. |
| Windows installer helpers | 20 passing ZIP safety, SHA256 and architecture assertions on Linux PowerShell 7. |
| Installer private configuration | 17 passing VAPID generation, repeated-run preservation, partial-pair rejection and output-privacy assertions. |
| Single-file installer packaging | Embedded ZIP and every manifest checksum verified; Windows CRLF framing and extracted PowerShell parsing passed. |

The environment's Chromium executable was selected with `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH=/usr/bin/chromium`. Normal developer machines can install the browser with `npx playwright install chromium` instead. The installed Next.js official guides for Proxy, Route Handlers, async cookies, `after`, and manifest conventions and the installed Supabase SSR guidance were reviewed.

The message insert statement acquires a room transaction lock before identity allocation, avoiding missed messages from IDs committed out of order. A database regression check observes the real sequence and lock lifetime. True hosted two-session scheduling is still a deployment test.

The tests cover nonce retry/deduplication, visible-only monotonic read positions, Unicode limits, IME composition, offline draft preservation, reconnect pages, member access, Origin/body-size boundaries, push target restrictions, expired endpoint cleanup, notification privacy and current-device sign-out cleanup.

## Not performed

- Creation or configuration of external Supabase/Vercel projects or real accounts.
- Hosted authentication, WebSocket authorization, two-account traffic and cross-device delivery.
- Actual HTTPS push-service delivery or real iPhone Home Screen notification testing.
- Windows CMD, Windows PowerShell 5.1, desktop shortcuts, WinForms, folder ACLs, or actual double-click installation.
- A claim of end-to-end encryption or production readiness.

`tests/e2e/live-chat.spec.ts` and `TESTING.md` supply the next checks after the administrator configures the real deployment. The preview images show the real unconfigured login screen; they are not evidence of a connected chat service.
