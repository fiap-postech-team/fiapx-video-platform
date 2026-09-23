CREATE TABLE identity_users
(
    id              uuid PRIMARY KEY,
    email           varchar(320) NOT NULL UNIQUE,
    password_hash   varchar(255) NOT NULL,
    role            varchar(16)  NOT NULL,
    status          varchar(16)  NOT NULL,
    failed_attempts integer      NOT NULL DEFAULT 0,
    locked_until    timestamptz
);

CREATE TABLE auth_sessions
(
    id         uuid PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES identity_users (id),
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz
);

CREATE TABLE refresh_tokens
(
    id          uuid PRIMARY KEY,
    session_id  uuid        NOT NULL REFERENCES auth_sessions (id),
    token_hash  varchar(64) NOT NULL UNIQUE,
    issued_at   timestamptz NOT NULL,
    expires_at  timestamptz NOT NULL,
    consumed_at timestamptz
);

CREATE INDEX auth_sessions_user_idx ON auth_sessions (user_id);
CREATE INDEX refresh_tokens_session_idx ON refresh_tokens (session_id);
