-- This schema belongs only to the new OurChat Cloudflare D1 database.
-- Do not apply it to an existing application's database.
PRAGMA foreign_keys = ON;

CREATE TABLE messages (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  sender_id TEXT NOT NULL CHECK (sender_id IN ('member-1', 'member-2')),
  client_nonce TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  body_bytes INTEGER NOT NULL CHECK (body_bytes > 0 AND body_bytes <= 4194304),
  body_sha256 TEXT NOT NULL,
  UNIQUE (sender_id, client_nonce)
);
CREATE INDEX messages_created_at_idx ON messages(created_at, id);

-- D1 limits a single string/row to 2 MB. Large messages are stored as smaller
-- ordered pieces in the same transaction as their metadata.
CREATE TABLE message_chunks (
  message_id INTEGER NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
  chunk_index INTEGER NOT NULL CHECK (chunk_index >= 0),
  body TEXT NOT NULL,
  PRIMARY KEY (message_id, chunk_index)
) WITHOUT ROWID;

CREATE TABLE read_receipts (
  user_id TEXT PRIMARY KEY CHECK (user_id IN ('member-1', 'member-2')),
  last_read_message_id INTEGER REFERENCES messages(id) ON DELETE SET NULL,
  updated_at INTEGER NOT NULL
) WITHOUT ROWID;

CREATE TABLE sessions (
  token_hash TEXT PRIMARY KEY,
  user_id TEXT NOT NULL CHECK (user_id IN ('member-1', 'member-2')),
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL
) WITHOUT ROWID;
CREATE INDEX sessions_expires_at_idx ON sessions(expires_at);
CREATE INDEX sessions_user_created_at_idx ON sessions(user_id, created_at DESC);

CREATE TABLE presence (
  user_id TEXT PRIMARY KEY CHECK (user_id IN ('member-1', 'member-2')),
  online INTEGER NOT NULL CHECK (online IN (0, 1)),
  last_seen INTEGER NOT NULL
) WITHOUT ROWID;

-- Only salted/HMAC identifiers are stored here, never raw network addresses.
CREATE TABLE rate_limits (
  bucket TEXT PRIMARY KEY,
  count INTEGER NOT NULL,
  expires_at INTEGER NOT NULL
) WITHOUT ROWID;
CREATE INDEX rate_limits_expires_at_idx ON rate_limits(expires_at);

CREATE TABLE push_subscriptions (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL CHECK (user_id IN ('member-1', 'member-2')),
  session_hash TEXT NOT NULL REFERENCES sessions(token_hash) ON DELETE CASCADE,
  endpoint TEXT NOT NULL,
  created_at INTEGER NOT NULL
) WITHOUT ROWID;
CREATE INDEX push_subscriptions_user_idx ON push_subscriptions(user_id, created_at);

-- Contains new server-generated VAPID and rate-identifier keys, independent of
-- account passwords. No browser/API route can read the private values.
CREATE TABLE app_settings (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
) WITHOUT ROWID;
