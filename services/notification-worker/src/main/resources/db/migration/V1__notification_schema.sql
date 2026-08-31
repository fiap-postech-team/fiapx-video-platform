CREATE TABLE notification_deliveries(id uuid PRIMARY KEY,event_id uuid NOT NULL UNIQUE,job_id uuid NOT NULL,recipient varchar(320) NOT NULL,delivered_at timestamptz NOT NULL);
