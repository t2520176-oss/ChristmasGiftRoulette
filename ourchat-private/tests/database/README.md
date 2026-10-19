# Database regression tests

Run `npm run test:db`. The harness creates an ephemeral PGlite PostgreSQL database,
minimal Supabase Auth/Realtime fixtures, three random synthetic identities, and
actual `anon`, `authenticated`, and `service_role` SQL roles. It applies the
unmodified original migration followed by the incremental migrations, and tests actual SQL grants, row policies, check constraints,
foreign keys, triggers and RPC calls. No production connection or secret is used.

Coverage includes two-member cap, sender forgery, Unicode trimming and long messages,
nonces, immutable messages, outsider/anon denial, monotonic read cursors through
RPC and direct writes, endpoint takeover, scoped server access, DB message limits,
API limits, and private Presence topic policies. Message bodies above the old
2000-character cap, including Unicode and 50000-character text, must round-trip
intact. Empty or whitespace-only bodies must still fail; the message rate limit
remains 30 per member per minute.

The character-limit migration is tested against an existing conversation, with
before/after snapshots of rows, table/sequence grants, RLS, policies, columns,
functions, triggers, publications, and all other constraints. Replaying the
migration must preserve even the replacement constraint's identity. Unexpected,
missing, or extra body checks and a nullable body column must be refused without
further schema or data changes. These checks run only in the ephemeral database.

The message-order regression test checks the actual trigger's PostgreSQL type
(`BEFORE INSERT FOR EACH STATEMENT`), then temporarily observes the **real identity
sequence** in a later statement trigger. It verifies that the advisory lock is
already held while the identity is still unallocated, and that it remains held
after the insert until COMMIT, then releases. Test instrumentation is removed.

PGlite runs PostgreSQL SQL in one local process. These tests do not reproduce
multi-session lock scheduling, hosted Realtime WebSocket authorization, Supabase
Auth itself, or HTTP Origin checks. The two-value unique member-slot constraint
provides the cap independently of transaction snapshot visibility; atomic receipt
upserts and rate-counter upserts use PostgreSQL row-conflict serialization.
The statement-lock test verifies PostgreSQL executor ordering, not actual hosted
two-session scheduling. On staging, use two member transactions: keep the first
insert uncommitted, start the other's insert and confirm it blocks, then commit
the first and verify the second returns a larger ID. Roll back the second or use
a disposable staging room; no administrator should run destructive cleanup on
the real conversation.

On a configured staging Supabase project additionally run the app's documented
two-real-account browser tests and verify an anonymous and a third authenticated
client cannot subscribe to chat changes or the private Presence topic. Do not
infer hosted WebSocket access from publication membership alone.
