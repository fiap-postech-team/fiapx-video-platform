CREATE TABLE users (
    id uuid PRIMARY KEY,
    email varchar(320) NOT NULL,
    display_name varchar(120),
    status varchar(16) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED')),
    CONSTRAINT users_email_normalized_check CHECK (email = lower(btrim(email)))
);

INSERT INTO users (id, email, status)
SELECT id, lower(btrim(email)), status FROM identity_users;

CREATE UNIQUE INDEX users_email_unique_idx ON users (email);

CREATE TABLE user_credentials (
    user_id uuid PRIMARY KEY REFERENCES users (id),
    password_hash varchar(255) NOT NULL,
    password_algorithm varchar(32) NOT NULL,
    password_changed_at timestamptz NOT NULL DEFAULT now(),
    failed_attempts integer NOT NULL DEFAULT 0,
    locked_until timestamptz,
    CONSTRAINT user_credentials_attempts_check CHECK (failed_attempts >= 0)
);

INSERT INTO user_credentials (user_id, password_hash, password_algorithm, failed_attempts, locked_until)
SELECT id, password_hash, substring(password_hash FROM '^\{([^}]+)\}'), failed_attempts, locked_until
FROM identity_users;

CREATE TABLE user_roles (
    user_id uuid NOT NULL REFERENCES users (id),
    role varchar(16) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT user_roles_role_check CHECK (role IN ('USER', 'ADMIN'))
);

INSERT INTO user_roles (user_id, role) SELECT id, role FROM identity_users;

ALTER TABLE auth_sessions ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();
ALTER TABLE auth_sessions ADD COLUMN last_used_at timestamptz;
ALTER TABLE auth_sessions ADD COLUMN revocation_reason varchar(32);
ALTER TABLE auth_sessions DROP CONSTRAINT auth_sessions_user_id_fkey;
ALTER TABLE auth_sessions ADD CONSTRAINT auth_sessions_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE refresh_tokens ADD COLUMN replaced_by_id uuid UNIQUE REFERENCES refresh_tokens (id);

CREATE TABLE videos (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users (id),
    object_key varchar(1024) NOT NULL,
    original_filename varchar(512) NOT NULL,
    declared_content_type varchar(255) NOT NULL,
    size_bytes bigint NOT NULL,
    checksum_sha256 varchar(64) NOT NULL,
    upload_status varchar(16) NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    uploaded_at timestamptz,
    CONSTRAINT videos_object_key_unique UNIQUE (object_key),
    CONSTRAINT videos_size_check CHECK (size_bytes > 0),
    CONSTRAINT videos_checksum_check CHECK (checksum_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT videos_status_check CHECK (upload_status IN ('PENDING', 'UPLOADED', 'REJECTED', 'EXPIRED', 'DELETED'))
);
ALTER TABLE videos ADD CONSTRAINT videos_id_user_unique UNIQUE (id, user_id);

ALTER TABLE jobs ADD COLUMN video_id uuid REFERENCES videos (id);
ALTER TABLE jobs ADD COLUMN source_kind varchar(16) NOT NULL DEFAULT 'LEGACY_KEY';
ALTER TABLE jobs ADD COLUMN failure_code varchar(128);
ALTER TABLE jobs ADD COLUMN version bigint NOT NULL DEFAULT 0;
ALTER TABLE jobs ADD COLUMN completed_at timestamptz;
ALTER TABLE jobs ADD CONSTRAINT jobs_source_kind_check CHECK (source_kind IN ('LEGACY_KEY', 'VIDEO'));
ALTER TABLE jobs ADD CONSTRAINT jobs_status_check CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'));
ALTER TABLE jobs ADD CONSTRAINT jobs_video_owner_fkey FOREIGN KEY (video_id, user_id)
    REFERENCES videos (id, user_id) NOT VALID;
CREATE INDEX jobs_owner_cursor_idx ON jobs (user_id, created_at DESC, id DESC);
CREATE INDEX jobs_video_idx ON jobs (video_id);

ALTER TABLE job_status_history ADD COLUMN event_id uuid;
ALTER TABLE job_status_history ADD COLUMN reason_code varchar(128);
ALTER TABLE job_status_history ADD COLUMN recorded_at timestamptz NOT NULL DEFAULT now();
CREATE UNIQUE INDEX job_history_event_unique_idx ON job_status_history (job_id, event_id) WHERE event_id IS NOT NULL;

ALTER TABLE outbox_events ADD COLUMN aggregate_id uuid;
ALTER TABLE outbox_events ADD COLUMN aggregate_type varchar(32);
ALTER TABLE outbox_events ADD COLUMN event_type varchar(128);
ALTER TABLE outbox_events ADD COLUMN schema_version integer NOT NULL DEFAULT 1;
ALTER TABLE outbox_events ADD COLUMN correlation_id uuid;
ALTER TABLE outbox_events ADD COLUMN payload_json jsonb;
ALTER TABLE outbox_events ADD COLUMN status varchar(16) NOT NULL DEFAULT 'PENDING';
ALTER TABLE outbox_events ADD COLUMN attempts integer NOT NULL DEFAULT 0;
ALTER TABLE outbox_events ADD COLUMN next_attempt_at timestamptz NOT NULL DEFAULT now();
ALTER TABLE outbox_events ADD COLUMN claimed_by varchar(128);
ALTER TABLE outbox_events ADD COLUMN claim_token uuid;
ALTER TABLE outbox_events ADD COLUMN claimed_at timestamptz;
ALTER TABLE outbox_events ADD COLUMN claim_expires_at timestamptz;
ALTER TABLE outbox_events ADD COLUMN last_error_code varchar(128);
UPDATE outbox_events SET payload_json = payload::jsonb, aggregate_type = 'JOB', event_type = routing_key
WHERE payload IS JSON;
ALTER TABLE outbox_events ADD CONSTRAINT outbox_status_check CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'));
ALTER TABLE outbox_events ADD CONSTRAINT outbox_attempts_check CHECK (attempts >= 0);
CREATE INDEX outbox_ready_claim_idx ON outbox_events (next_attempt_at, created_at)
    WHERE status = 'PENDING';

CREATE TABLE inbox_events (
    event_id uuid PRIMARY KEY,
    job_id uuid NOT NULL REFERENCES jobs (id),
    event_type varchar(128) NOT NULL,
    schema_version integer NOT NULL,
    correlation_id uuid,
    payload_fingerprint varchar(64) NOT NULL,
    status varchar(16) NOT NULL,
    occurred_at timestamptz,
    received_at timestamptz NOT NULL,
    processed_at timestamptz,
    CONSTRAINT inbox_status_check CHECK (status IN ('PROCESSED', 'IGNORED')),
    CONSTRAINT inbox_fingerprint_check CHECK (payload_fingerprint ~ '^[0-9a-f]{64}$')
);
CREATE INDEX inbox_retention_idx ON inbox_events (processed_at) WHERE status = 'PROCESSED';

CREATE TABLE job_creation_idempotency (
    user_id uuid NOT NULL REFERENCES users (id),
    idempotency_key varchar(128) NOT NULL,
    request_fingerprint varchar(64) NOT NULL,
    job_id uuid NOT NULL UNIQUE REFERENCES jobs (id),
    created_at timestamptz NOT NULL,
    PRIMARY KEY (user_id, idempotency_key),
    CONSTRAINT job_idempotency_fingerprint_check CHECK (request_fingerprint ~ '^[0-9a-f]{64}$')
);
