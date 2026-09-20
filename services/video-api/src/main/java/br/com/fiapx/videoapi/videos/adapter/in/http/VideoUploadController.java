package br.com.fiapx.videoapi.videos.adapter.in.http;

import br.com.fiapx.videoapi.identity.domain.AuthenticatedIdentity;
import br.com.fiapx.videoapi.videos.application.ConfirmVideo;
import br.com.fiapx.videoapi.videos.application.CreateVideoUpload;
import br.com.fiapx.videoapi.videos.domain.Video;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/videos")
public class VideoUploadController {
    private final CreateVideoUpload create;
    private final ConfirmVideo confirm;

    public VideoUploadController(CreateVideoUpload create, ConfirmVideo confirm) {
        this.create = create;
        this.confirm = confirm;
    }

    @PostMapping("/uploads")
    ResponseEntity<UploadResponse> create(@Valid @RequestBody UploadRequest request,
                                          @AuthenticationPrincipal AuthenticatedIdentity identity) {
        var upload = create.execute(identity.userId(), request.originalFilename(), request.contentType(),
            request.sizeBytes(), request.checksumSha256());
        return ResponseEntity.status(201).body(new UploadResponse(upload.videoId(), upload.sourceKey(),
            upload.signed().url(), upload.signed().expiresAt(), "PUT", upload.signed().headers()));
    }

    @PostMapping("/{videoId}/confirm")
    ConfirmResponse confirm(@PathVariable UUID videoId,
                            @AuthenticationPrincipal AuthenticatedIdentity identity) {
        return ConfirmResponse.from(confirm.execute(identity.userId(), videoId));
    }

    record UploadRequest(@NotBlank @Size(max = 512) String originalFilename,
                         @NotBlank String contentType, @NotNull @Positive Long sizeBytes,
                         @Pattern(regexp = "(?i)[0-9a-f]{64}") String checksumSha256) { }
    record UploadResponse(UUID videoId, String sourceKey, String uploadUrl,
                          Instant expiresAt, String method, Map<String, String> headers) { }
    record ConfirmResponse(UUID videoId, String sourceKey, String status, Instant uploadedAt) {
        static ConfirmResponse from(Video video) {
            return new ConfirmResponse(video.id(), video.objectKey(), video.status().name(), video.uploadedAt());
        }
    }
}
