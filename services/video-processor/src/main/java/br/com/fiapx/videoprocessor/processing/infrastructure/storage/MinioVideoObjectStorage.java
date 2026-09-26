package br.com.fiapx.videoprocessor.processing.infrastructure.storage;

import br.com.fiapx.videoprocessor.processing.application.port.out.VideoObjectStorage;
import br.com.fiapx.videoprocessor.processing.domain.ResultLocation;
import java.nio.file.Path;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MinioVideoObjectStorage implements VideoObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(MinioVideoObjectStorage.class);

    private static final String ARCHIVE_CONTENT_TYPE = "application/zip";

    private final S3Client client;
    private final StorageProperties properties;

    public MinioVideoObjectStorage(S3Client client, StorageProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public boolean exists(ResultLocation location) {
        try {
            client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(location.key())
                    .build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
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
            client.getObject(GetObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(sourceKey)
                    .build(), ResponseTransformer.toFile(target));
            log.debug("downloaded {} to the job workspace", sourceKey);
            return target;
        } catch (Exception e) {
            throw new StorageException("could not download the source object", e);
        }
    }

    @Override
    public void upload(ResultLocation location, Path source) {
        try {
            client.putObject(PutObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(location.key())
                    .contentType(ARCHIVE_CONTENT_TYPE)
                    .build(), RequestBody.fromFile(source));
            log.debug("uploaded the frame archive to {}", location.key());
        } catch (Exception e) {
            throw new StorageException("could not upload the frame archive", e);
        }
    }

}
