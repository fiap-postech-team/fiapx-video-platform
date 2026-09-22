CREATE UNIQUE INDEX jobs_video_once_unique_idx ON jobs (video_id)
WHERE source_kind = 'VIDEO' AND video_id IS NOT NULL;
