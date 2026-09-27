CREATE INDEX videos_owner_name_ci_idx
    ON videos (user_id, lower(original_filename) text_pattern_ops)
    WHERE upload_status <> 'DELETED';
