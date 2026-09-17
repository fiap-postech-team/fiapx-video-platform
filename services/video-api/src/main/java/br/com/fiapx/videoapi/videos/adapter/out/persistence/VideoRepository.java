package br.com.fiapx.videoapi.videos.adapter.out.persistence;

import br.com.fiapx.videoapi.videos.domain.VideoStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface VideoRepository extends JpaRepository<VideoEntity, UUID> {
    Optional<VideoEntity> findByUserIdAndObjectKey(UUID userId, String objectKey);
    Optional<VideoEntity> findByUserIdAndObjectKeyAndUploadStatus(UUID userId, String objectKey, VideoStatus status);
}
