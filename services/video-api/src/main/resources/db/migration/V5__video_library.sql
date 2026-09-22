ALTER TABLE jobs ADD COLUMN video_library_visible boolean NOT NULL DEFAULT false;

UPDATE jobs AS candidate
SET video_library_visible = true
WHERE candidate.video_id IS NOT NULL
  AND candidate.id = (
    SELECT ranked.id
    FROM jobs AS ranked
    WHERE ranked.video_id = candidate.video_id
    ORDER BY ranked.updated_at DESC NULLS LAST, ranked.created_at DESC, ranked.id DESC
    LIMIT 1
  );

CREATE UNIQUE INDEX jobs_one_visible_per_video_idx
    ON jobs (video_id)
    WHERE video_library_visible AND video_id IS NOT NULL;

CREATE INDEX videos_owner_created_idx ON videos (user_id, created_at DESC, id DESC);
CREATE INDEX jobs_video_visible_updated_idx ON jobs (video_id, video_library_visible, updated_at DESC);
CREATE INDEX job_status_history_job_occurred_idx ON job_status_history (job_id, occurred_at);

DO $$
DECLARE
    hidden_count integer;
BEGIN
    SELECT count(*) INTO hidden_count
    FROM jobs
    WHERE video_id IS NOT NULL AND NOT video_library_visible;
    RAISE NOTICE 'video_library hidden duplicate jobs: %', hidden_count;
END $$;
