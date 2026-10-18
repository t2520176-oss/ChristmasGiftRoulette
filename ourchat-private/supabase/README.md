# Database setup

Run `migrations/202610180001_ourchat.sql` as the project database owner, either
through Supabase migrations or the SQL Editor. Do not expose the `private` schema
through the Data API. The exposed application schema is `public` only.

Create two real users in Auth with administrator access, turn off public signups
and anonymous sign-in, then follow `seed-members.sql`. The sample deliberately
contains no real UUIDs, email addresses or passwords. There is no client member
management policy. A trigger assigns a unique slot in the domain `{1, 2}` and
serializes membership administration; a third member fails at the database level.
Existing messages prevent deletion of their sender or message through restrictive
foreign keys, preserving chat history. Use an explicit owner-run archival plan if
retiring either account.

## API contracts

- `messages` supports member reads and self inserts only. A database trigger trims
  Unicode whitespace, stamps server time and limits each member to 30 messages per
  UTC clock-minute, even for direct authenticated Data API calls. A duplicate
  `(sender_id, client_nonce)` insert fails with SQLSTATE `23505`; the API should
  select and return that member's existing row instead of sending another push.
  A private **BEFORE INSERT FOR EACH STATEMENT** trigger acquires one single-room
  advisory transaction lock before identity/default evaluation. PostgreSQL holds
  it through commit, so another sender cannot allocate and commit a higher ID
  while a lower ID is still uncommitted. This makes ID cursor synchronization and
  read cursors consistent with committed message order. A BEFORE ROW lock would
  be too late. Keep message inserts in short transactions; do not perform network
  calls while holding their database transaction open.
- `mark_read(p_message_id bigint)` runs with the caller's RLS permissions. It
  rejects invisible/nonexistent IDs, then performs one atomic upsert with
  `GREATEST`. It returns `{ user_id, last_read_message_id: string, updated_at }[]`.
  Direct receipt updates also cannot move the cursor backward. Only an actually
  visible message should be submitted by the UI.
- `check_api_rate_limit(p_action text)` accepts only `read` (120/minute),
  `push_subscribe` (20/minute), or `push_delete` (20/minute), authenticating member
  status inside the DB. Route Handlers call this after their Origin/session check.
  These limits are atomic fixed windows, not a globally distributed in-memory map.
- Push endpoint uniqueness is global, but reads/writes are owner-only. An upsert
  that would take another member's endpoint fails. Its owner, endpoint, and UUID
  are immutable. The server role can only read/delete subscriptions; use it only
  after a verified member's message insert, targeting the other member.

Use textual PostgREST casts for bigint IDs (`id::text`,
`last_read_message_id::text`) and string JSON IDs. Do not convert database IDs to
JavaScript `Number` or sort numeric identifiers lexicographically.

## Realtime

The migration puts `messages` and `read_receipts` into `supabase_realtime` with
`REPLICA IDENTITY FULL`. Their SELECT policies are the authorization boundary for
Postgres Changes. Subscribe only after the real user session is set, and clean up
channels on sign-out. There are no nonmember/anonymous SELECT grants.

Presence uses one private channel: `ourchat:couple`, configured with
`{ config: { private: true } }`. Authenticated membership and this exact topic are
checked on `realtime.messages`, limited to Presence messages. Do not fall back to
a public channel. Disable public channel access in Realtime settings when that
Dashboard option is available. An older/local installation without
`realtime.messages` skips those policies; upgrade/configure Realtime before using
Presence. Validate actual channel joins with both members, an outsider, and anon
on your hosted project; the local SQL harness verifies row policies, not the
hosted WebSocket authorization service.

## Local policy regression tests

`npm run test:db` runs `tests/database/run.mjs` in isolated PGlite with synthetic
Auth/Realtime fixtures. It executes the real migration, switches actual SQL roles,
and tests ACLs, RLS, write forgery, limits, ownership and receipt monotonicity. It
does not require or modify a hosted Supabase project. See `tests/database/README.md`
for the live-project SQL check and the limitations of the local simulation.
