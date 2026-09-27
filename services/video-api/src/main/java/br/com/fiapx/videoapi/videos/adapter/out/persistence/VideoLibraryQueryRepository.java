package br.com.fiapx.videoapi.videos.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface VideoLibraryQueryRepository extends Repository<VideoEntity, UUID> {
    @Query(value = """
        select v.id as videoId,
               v.original_filename as originalFilename,
               v.upload_status as uploadStatus,
               v.created_at as submittedAt,
               v.uploaded_at as uploadedAt,
               j.id as jobId,
               j.status as jobStatus,
               j.created_at as jobCreatedAt,
               j.updated_at as jobUpdatedAt
        from videos v
        left join jobs j on j.video_id = v.id and j.video_library_visible = true
        where v.user_id = :ownerId and v.upload_status <> 'DELETED'
          and (
            :normalizedName is null
            or (:nameMatch = 'EXACT' and lower(v.original_filename) = :normalizedName)
            or (:nameMatch = 'PREFIX' and starts_with(lower(v.original_filename), :normalizedName))
          )
          and (
            :statusFilter = 'ALL'
            or (:statusFilter = 'PROCESSED' and j.status = 'COMPLETED')
            or (:statusFilter = 'PROCESSING' and (
              j.status in ('PENDING', 'PROCESSING')
              or (j.id is null and v.upload_status = 'UPLOADED')
            ))
            or (:statusFilter = 'FAILED' and (
              j.status = 'FAILED'
              or (j.id is null and v.upload_status in ('REJECTED', 'EXPIRED'))
            ))
          )
        order by coalesce(j.updated_at, j.created_at, v.created_at) desc, v.id desc
        """,
        countQuery = """
        select count(*)
        from videos v
        left join jobs j on j.video_id = v.id and j.video_library_visible = true
        where v.user_id = :ownerId and v.upload_status <> 'DELETED'
          and (
            :normalizedName is null
            or (:nameMatch = 'EXACT' and lower(v.original_filename) = :normalizedName)
            or (:nameMatch = 'PREFIX' and starts_with(lower(v.original_filename), :normalizedName))
          )
          and (
            :statusFilter = 'ALL'
            or (:statusFilter = 'PROCESSED' and j.status = 'COMPLETED')
            or (:statusFilter = 'PROCESSING' and (
              j.status in ('PENDING', 'PROCESSING')
              or (j.id is null and v.upload_status = 'UPLOADED')
            ))
            or (:statusFilter = 'FAILED' and (
              j.status = 'FAILED'
              or (j.id is null and v.upload_status in ('REJECTED', 'EXPIRED'))
            ))
          )
        """,
        nativeQuery = true)
    Page<VideoLibraryQuery> findPage(
        @Param("ownerId") UUID ownerId,
        @Param("normalizedName") String normalizedName,
        @Param("nameMatch") String nameMatch,
        @Param("statusFilter") String statusFilter,
        Pageable pageable
    );

    @Query(value = """
        select v.id as videoId,
               v.original_filename as originalFilename,
               v.upload_status as uploadStatus,
               v.created_at as submittedAt,
               v.uploaded_at as uploadedAt,
               j.id as jobId,
               j.status as jobStatus,
               j.created_at as jobCreatedAt,
               j.updated_at as jobUpdatedAt
        from videos v
        left join jobs j on j.video_id = v.id and j.video_library_visible = true
        where v.user_id = :ownerId and v.id = :videoId and v.upload_status <> 'DELETED'
        """, nativeQuery = true)
    Optional<VideoLibraryQuery> findDetail(@Param("ownerId") UUID ownerId, @Param("videoId") UUID videoId);

    @Query(value = """
        select status as status, occurred_at as occurredAt
        from job_status_history
        where job_id = :jobId
        order by occurred_at
        """, nativeQuery = true)
    List<JobHistoryInstant> findHistory(@Param("jobId") UUID jobId);

    interface VideoLibraryQuery {
        UUID getVideoId();
        String getOriginalFilename();
        String getUploadStatus();
        Instant getSubmittedAt();
        Instant getUploadedAt();
        UUID getJobId();
        String getJobStatus();
        Instant getJobCreatedAt();
        Instant getJobUpdatedAt();
    }

    interface JobHistoryInstant {
        String getStatus();
        Instant getOccurredAt();
    }
}
