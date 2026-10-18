# Browser setup helper

`OurChat-Start.html` is a single-file setup guide, not an installed or hosted messenger. It cannot sign into third-party accounts, provision services without user approval, or install an app on a phone by file transfer. A working HTTPS deployment and two registered accounts must exist before sharing the app's URL to phones.

The helper embeds the actual migration, the SQL Editor member seed, and a preflight for a new project. It requires two syntactically valid distinct Auth UUIDs and an explicit new-project confirmation. It produces one transaction so a missing Auth user rolls back table and member creation together. Existing public application relations/functions, an existing private schema, or more than two Auth users are rejected before changes. This guard does not replace checking the project in the Supabase Dashboard. The helper never calls a database API or submits SQL itself.

The Vercel clone link is pinned to an immutable source commit and starts a new project/repository flow. The user must select a new private repository and new Vercel project, supply only the new Supabase URL and publishable key, and later add the fixed Production origin and redeploy. No password or API key is requested by the HTML helper, embedded in links, or sent to a third-party script. No browser storage is used. External links are ordinary user-clicked navigations to the named services.

Build the artifact from a published source commit:

```sh
python scripts/build-browser-setup.py --source-ref YOUR_40_CHARACTER_SOURCE_COMMIT --output ../downloads/OurChat-Start.html
node tests/browser-setup/check.mjs ../downloads/OurChat-Start.html
```

Eleven Chromium/PGlite scenarios passed: button and input gates, safe URL handling, clipboard fallback, no automatic external requests, no saved inputs, matching offline VAPID generation, atomic new-project setup, existing row/RLS/policy preservation, rejection of existing functions/private schemas, rollback on missing Auth users, and rejection of excess Auth users. Lint passed after these files were added.

Cloud Chromium's administrative policy blocks `file:` navigation. Browser behavior was therefore tested by serving the exact HTML over loopback without changing browser policy. Actual Windows file opening, Smart App Control compatibility, Vercel's logged-in clone flow, hosted Supabase behavior, mobile installation, and push delivery are not verified by those tests.
