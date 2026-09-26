ALTER TABLE inbox_events DROP CONSTRAINT IF EXISTS inbox_status_check;
ALTER TABLE inbox_events ADD COLUMN IF NOT EXISTS ignored_reason varchar(64);
ALTER TABLE inbox_events ADD COLUMN IF NOT EXISTS routing_key varchar(128);
ALTER TABLE inbox_events ADD CONSTRAINT inbox_status_check CHECK (status IN ('RECEIVED', 'PROCESSED', 'IGNORED'));
ALTER TABLE inbox_events DROP CONSTRAINT IF EXISTS inbox_events_job_id_fkey;
