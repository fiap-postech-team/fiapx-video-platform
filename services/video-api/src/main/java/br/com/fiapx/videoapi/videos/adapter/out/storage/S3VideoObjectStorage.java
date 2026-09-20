package br.com.fiapx.videoapi.videos.adapter.out.storage;

import br.com.fiapx.videoapi.videos.application.StorageUnavailableException;
import br.com.fiapx.videoapi.videos.application.port.out.VideoObjectStorage;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

public final class S3VideoObjectStorage implements VideoObjectStorage {
    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3VideoObjectStorage(S3Client client, S3Presigner presigner, String bucket) {
        this.client = client;
        this.presigner = presigner;
        this.bucket = bucket;
    }

    public SignedUpload signUpload(String key, String contentType, String checksumSha256, Duration ttl) {
        try {
            var request = PutObjectRequest.builder().bucket(bucket).key(key)
                .contentType(contentType).ifNoneMatch("*");
            if (checksumSha256 != null) {
                request.checksumSHA256(Base64.getEncoder().encodeToString(HexFormat.of().parseHex(checksumSha256)));
            }
            var signed = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(ttl).putObjectRequest(request.build()).build());
            Map<String, String> headers = signed.signedHeaders().entrySet().stream()
                .filter(entry -> !entry.getKey().equalsIgnoreCase("host"))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().getFirst()));
            return new SignedUpload(signed.url().toString(), signed.expiration(), headers);
        } catch (SdkException exception) {
            throw new StorageUnavailableException(exception);
        }
    }

    public Optional<StoredObject> stat(String key) {
        try {
            var response = client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key)
                .checksumMode(ChecksumMode.ENABLED).build());
            var checksum = response.checksumSHA256() == null ? null
                : HexFormat.of().formatHex(Base64.getDecoder().decode(response.checksumSHA256()));
            return Optional.of(new StoredObject(response.contentLength(), response.contentType(), checksum));
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return Optional.empty();
            }
            throw new StorageUnavailableException(exception);
        } catch (SdkException exception) {
            throw new StorageUnavailableException(exception);
        }
    }

    public void delete(String key) {
        try {
            client.deleteObject(request -> request.bucket(bucket).key(key));
        } catch (SdkException exception) {
            throw new StorageUnavailableException(exception);
        }
    }
}
