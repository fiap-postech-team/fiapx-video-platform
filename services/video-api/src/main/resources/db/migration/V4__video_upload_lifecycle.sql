ALTER TABLE videos ALTER COLUMN checksum_sha256 DROP NOT NULL;
ALTER TABLE videos DROP CONSTRAINT videos_checksum_check;
ALTER TABLE videos ADD CONSTRAINT videos_checksum_check
    CHECK (checksum_sha256 IS NULL OR checksum_sha256 ~ '^[0-9a-f]{64}$');

ALTER TABLE videos ADD COLUMN expires_at timestamptz;
UPDATE videos SET expires_at = created_at + interval '24 hours';
ALTER TABLE videos ALTER COLUMN expires_at SET NOT NULL;

UPDATE videos SET uploaded_at = updated_at
WHERE upload_status = 'UPLOADED' AND uploaded_at IS NULL;

ALTER TABLE videos ADD COLUMN cleanup_completed_at timestamptz;
CREATE INDEX videos_pending_expiry_idx ON videos (expires_at, id)
WHERE upload_status = 'PENDING';
CREATE INDEX videos_expired_cleanup_idx ON videos (expires_at, id)
WHERE upload_status = 'EXPIRED' AND cleanup_completed_at IS NULL;
