-- Add private, expiring image attachments without modifying existing messages.
-- All bytes remain in the existing free D1 binding; no public object storage.
PRAGMA foreign_keys = ON;

CREATE TABLE image_storage (
  id INTEGER PRIMARY KEY CHECK (id = 1),
  bytes_used INTEGER NOT NULL CHECK (bytes_used >= 0 AND bytes_used <= 67108864)
);
INSERT INTO image_storage(id, bytes_used) VALUES (1, 0);

CREATE TABLE message_images (
  message_id INTEGER PRIMARY KEY REFERENCES messages(id) ON DELETE CASCADE,
  mime_type TEXT NOT NULL CHECK (mime_type = 'image/jpeg'),
  byte_length INTEGER NOT NULL CHECK (byte_length > 0 AND byte_length <= 131072),
  width INTEGER NOT NULL CHECK (width > 0 AND width <= 1600),
  height INTEGER NOT NULL CHECK (height > 0 AND height <= 1600)
);

-- At most four bounded BLOB rows per image, below D1's per-row limit.
CREATE TABLE image_chunks (
  message_id INTEGER NOT NULL REFERENCES message_images(message_id) ON DELETE CASCADE,
  chunk_index INTEGER NOT NULL CHECK (chunk_index >= 0 AND chunk_index < 4),
  data BLOB NOT NULL CHECK (typeof(data) = 'blob' AND length(data) > 0 AND length(data) <= 32768),
  PRIMARY KEY (message_id, chunk_index)
) WITHOUT ROWID;

-- The quota check, accounting and image/chunk writes share the message batch
-- transaction. A failing reservation rolls back the text and every image row.
CREATE TRIGGER message_images_quota_check BEFORE INSERT ON message_images
WHEN NEW.byte_length > (SELECT 67108864 - bytes_used FROM image_storage WHERE id = 1)
BEGIN
  SELECT RAISE(ABORT, 'image_storage_quota_exceeded');
END;

CREATE TRIGGER message_images_quota_add AFTER INSERT ON message_images
BEGIN
  UPDATE image_storage SET bytes_used = bytes_used + NEW.byte_length WHERE id = 1;
END;

CREATE TRIGGER message_images_quota_release AFTER DELETE ON message_images
BEGIN
  UPDATE image_storage SET bytes_used = bytes_used - OLD.byte_length WHERE id = 1;
END;

-- Attachment metadata is immutable; retries use INSERT ... DO NOTHING.
CREATE TRIGGER message_images_no_update BEFORE UPDATE ON message_images
BEGIN
  SELECT RAISE(ABORT, 'image_metadata_is_immutable');
END;
