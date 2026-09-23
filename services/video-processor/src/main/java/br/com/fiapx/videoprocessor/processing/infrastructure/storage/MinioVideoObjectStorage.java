package br.com.fiapx.videoprocessor.processing.infrastructure.storage;

import br.com.fiapx.videoprocessor.processing.application.port.out.VideoObjectStorage;
import br.com.fiapx.videoprocessor.processing.domain.ResultLocation;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import java.nio.file.Path;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MinioVideoObjectStorage implements VideoObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(MinioVideoObjectStorage.class);

    private static final Set<String> NOT_FOUND_CODES = Set.of("NoSuchKey", "NoSuchObject");
    private static final String ARCHIVE_CONTENT_TYPE = "application/zip";

    private final MinioClient client;
    private final StorageProperties properties;

    public MinioVideoObjectStorage(MinioClient client, StorageProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public boolean exists(ResultLocation location) {
        try {
            client.statObject(StatObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(location.key())
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            if (isNotFound(e)) {
                return false;
            }
            throw new StorageException("could not check the result object", e);
        } catch (Exception e) {
            throw new StorageException("could not check the result object", e);
        }
    }

    @Override
    public Path download(String sourceKey, Path target) {
        try {
            client.downloadObject(DownloadObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(sourceKey)
                    .filename(target.toString())
                    .overwrite(true)
                    .build());
            log.debug("downloaded {} to the job workspace", sourceKey);
            return target;
        } catch (Exception e) {
            throw new StorageException("could not download the source object", e);
        }
    }

    @Override
    public void upload(ResultLocation location, Path source) {
        try {
            client.uploadObject(UploadObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(location.key())
                    .filename(source.toString())
                    .contentType(ARCHIVE_CONTENT_TYPE)
                    .build());
            log.debug("uploaded the frame archive to {}", location.key());
        } catch (Exception e) {
            throw new StorageException("could not upload the frame archive", e);
        }
    }

    private static boolean isNotFound(ErrorResponseException e) {
        ErrorResponse response = e.errorResponse();
        return response != null && NOT_FOUND_CODES.contains(response.code());
    }
}
