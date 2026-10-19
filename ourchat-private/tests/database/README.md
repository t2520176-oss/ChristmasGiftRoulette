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

The retention core migration is also applied without replacing the original
migrations. Its installation must preserve all message rows, original grants,
foreign keys, triggers, checks, function bodies and publication membership; only
the message SELECT policy, cleanup index and new owner-only function may change.
Replay must preserve object identities. Tests reject changed read policies or
duration, an extra permissive policy, restored character cap, changed receipt FK
or trigger security, client DELETE or cleanup EXECUTE grants, and conflicting
cleanup/index implementations without additional changes.

Actual SQL role tests cover both members, an outsider, anon and service_role.
A single PostgreSQL DO command fixes `statement_timestamp()` while testing rows
one millisecond before, exactly at, and one millisecond after the 168-hour cutoff.
Expired rows must be invisible and rejected by `mark_read` before physical
deletion. Owner cleanup must delete only expired messages and receipts that refer
to them, retain fresh and null receipts, preserve long text, and permit normal
sending/receipt recreation afterwards. A DST test distinguishes 168 elapsed hours
from seven calendar days. The cleanup returns a row count, never message bodies.

PGlite does not supply pg_cron. The separate scheduling migration is deliberately
not replaced with a fake scheduler or reported as a successful automatic run.
On the dedicated hosted project, verify the exact active
`ourchat-prune-messages-7d` job has the owner, current database, every-minute
schedule and command `SELECT private.prune_expired_messages();`, then verify a
successful real scheduled execution. This hosted check is necessary to establish
that cleanup runs without an open browser.

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
a disposable staging room. Production pruning must use only the reviewed fixed
retention helper after the conversation owner has authorized that retention.
Do not insert synthetic aged messages or weaken production policies for a test.

On a configured staging Supabase project additionally run the app's documented
two-real-account browser tests and verify an anonymous and a third authenticated
client cannot subscribe to chat changes or the private Presence topic. Do not
infer hosted WebSocket access from publication membership alone.
