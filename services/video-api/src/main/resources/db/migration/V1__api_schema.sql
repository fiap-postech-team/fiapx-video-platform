CREATE TABLE jobs
(
    id         uuid PRIMARY KEY,
    user_id    uuid          NOT NULL,
    source_key varchar(1024) NOT NULL,
    result_key varchar(1024),
    status     varchar(32)   NOT NULL,
    created_at timestamptz   NOT NULL,
    updated_at timestamptz
);
CREATE TABLE job_status_history
(
    id          bigserial PRIMARY KEY,
    job_id      uuid        NOT NULL REFERENCES jobs (id),
    status      varchar(32) NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE outbox_events
(
    id           uuid PRIMARY KEY,
    routing_key  varchar(128) NOT NULL,
    payload      text         NOT NULL,
    created_at   timestamptz  NOT NULL,
    published_at timestamptz
);
CREATE INDEX outbox_unpublished_idx ON outbox_events (created_at) WHERE published_at IS NULL;
