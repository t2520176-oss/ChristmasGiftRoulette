# Database setup

Run migrations in timestamp order as the project database owner, either through
Supabase migrations or the SQL Editor. Keep `202610180001_ourchat.sql` unchanged.
An existing installation also applies
`202610190001_remove_message_character_limit.sql`, which replaces only the known
message body check. It removes the 2000-character cap while retaining trimmed,
nonempty `text NOT NULL` bodies. It preserves existing rows, grants, policies,
triggers, nonce uniqueness, and rate limits. The migration accepts the exact
original check or its own completed state, refuses unexpected body schemas or
constraints, and uses an atomic transaction with a five-second table-lock
timeout. If the room is busy and the lock times out, retry the same migration
later. Do not expose the `private` schema through the Data API. The exposed
application schema is `public` only.

Apply `202610190002_message_retention.sql` next, then
`202610190003_schedule_message_retention.sql` on Supabase. The first migration
adds a fixed rolling **168-hour** retention policy, a `created_at` index and the
owner-only `private.prune_expired_messages()` helper. Rows at or older than the
cutoff become unreadable immediately through member RLS, including direct Data
API reads and `mark_read`. Installation alone does not delete stored rows. The
second migration enables pg_cron if available and schedules the helper every
minute as the database owner. It refuses an existing job with conflicting
settings or ownership and leaves other jobs unchanged. Verify a successful
actual scheduled run; merely installing a job or calling the helper manually
does not prove that scheduling works.

After reviewing existing expired-row counts and enabling this retention, the
owner can run `SELECT private.prune_expired_messages();` once for initial
cleanup. The function takes no duration, returns only the deleted-message count,
and is not callable by anon, authenticated members or service_role. It locks
messages and receipts in a fixed order while deleting only receipts that refer
to expired messages, then those messages in the same transaction. This preserves
the original `ON DELETE RESTRICT` foreign key and authenticated, monotonic
receipt trigger. Fresh and null receipts remain unchanged. Reads remain
available during pruning; lock waits fail after five seconds so the next
scheduled run can retry. No client DELETE privilege is introduced.

The migrations accept only the known original/completed policy and known owned
schema, refuse conflicting checks, triggers, functions or indexes, and preserve
the character-limit removal. Keep previous migrations immutable. Seven days
means 168 elapsed hours after the server-created timestamp, not a calendar-week
boundary. Member reads hide expired messages before the next cron run; physical
row deletion normally follows within a minute, but a busy or unavailable database
can delay cleanup. Monitor failed/missing cron runs. Deleted application rows
may remain in provider-managed backups/WAL until that provider's retention ends;
user copies, clipboard text and screenshots are outside this cleanup.

Create two real users in Auth with administrator access, turn off public signups
and anonymous sign-in, then follow `seed-members.sql`. The sample deliberately
contains no real UUIDs, email addresses or passwords. There is no client member
management policy. A trigger assigns a unique slot in the domain `{1, 2}` and
serializes membership administration; a third member fails at the database level.
Existing messages prevent deletion of their sender through a restrictive foreign
key. Message/receipt cleanup is limited to the owner-only retention function;
members cannot edit or delete stored messages themselves. Use an explicit
owner-run plan if retiring either account.

## API contracts

- `messages` supports member reads and self inserts only. A database trigger trims
  Unicode whitespace, stamps server time and limits each member to 30 messages per
  UTC clock-minute, even for direct authenticated Data API calls.
  Message bodies have no application character cap after the character-limit migration; they
  must remain nonempty after trimming. A duplicate
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
Auth/Realtime fixtures. It executes the real migrations, switches actual SQL roles,
and tests ACLs, RLS, write forgery, limits, ownership and receipt monotonicity. It
does not require or modify a hosted Supabase project. See `tests/database/README.md`
for the live-project SQL check and the limitations of the local simulation.
