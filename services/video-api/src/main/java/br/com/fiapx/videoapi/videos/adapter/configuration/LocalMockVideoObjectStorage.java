package br.com.fiapx.videoapi.videos.adapter.configuration;

import br.com.fiapx.videoapi.videos.application.StorageUnavailableException;
import br.com.fiapx.videoapi.videos.application.port.out.VideoObjectStorage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Local-only object storage used to exercise the browser PUT flow without S3. */
public final class LocalMockVideoObjectStorage implements VideoObjectStorage {
    private final long maxSizeBytes;
    private final String publicEndpoint;
    private final Path directory;
    private final Clock clock;
    private final Map<String, Grant> grants = new ConcurrentHashMap<>();
    private final Map<String, StoredObject> objects = new ConcurrentHashMap<>();

    public LocalMockVideoObjectStorage(long maxSizeBytes, String publicEndpoint, Path directory, Clock clock) {
        this.maxSizeBytes = maxSizeBytes;
        this.publicEndpoint = publicEndpoint.replaceAll("/+$", "");
        this.directory = directory.toAbsolutePath().normalize();
        this.clock = clock;
    }

    @Override
    public SignedUpload signUpload(String key, String contentType, String checksumSha256, Duration ttl) {
        var capability = randomCapability();
        var expiresAt = clock.instant().plus(ttl);
        // Keep only a digest of the bearer capability in memory; the raw value is URL-only.
        grants.put(capabilityHash(capability), new Grant(key, contentType, checksumSha256, expiresAt));
        var headers = new java.util.LinkedHashMap<String, String>();
        headers.put("Content-Type", contentType);
        headers.put("If-None-Match", "*");
        if (checksumSha256 != null) {
            headers.put("x-amz-checksum-sha256", Base64.getEncoder().encodeToString(HexFormat.of().parseHex(checksumSha256)));
        }
        return new SignedUpload(publicEndpoint + "/_local/mock-storage/uploads/" + capability, expiresAt, Map.copyOf(headers));
    }

    public void put(String capability, String contentType, String ifNoneMatch, String checksumHeader, InputStream input) {
        var grant = grants.remove(capabilityHash(capability));
        if (grant == null) throw new UploadConflictException();
        if (!clock.instant().isBefore(grant.expiresAt())) throw new UploadExpiredException();
        if (!grant.contentType().equals(contentType) || !"*".equals(ifNoneMatch)) throw new UploadValidationException();
        if (grant.checksumSha256() != null && !Base64.getEncoder().encodeToString(HexFormat.of().parseHex(grant.checksumSha256())).equals(checksumHeader)) {
            throw new UploadValidationException();
        }
        var target = pathFor(grant.key());
        if (objects.containsKey(grant.key()) || Files.exists(target)) throw new UploadConflictException();
        Path temporary = null;
        try {
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), ".upload-", ".part");
            var digest = MessageDigest.getInstance("SHA-256");
            long size = copy(input, temporary, digest);
            var checksum = HexFormat.of().formatHex(digest.digest());
            if (grant.checksumSha256() != null && !grant.checksumSha256().equalsIgnoreCase(checksum)) throw new UploadValidationException();
            move(temporary, target);
            temporary = null;
            objects.put(grant.key(), new StoredObject(size, contentType, grant.checksumSha256() == null ? null : checksum));
        } catch (UploadValidationException | UploadTooLargeException exception) {
            throw exception;
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new StorageUnavailableException(exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    @Override public Optional<StoredObject> stat(String key) { return Optional.ofNullable(objects.get(key)); }

    public void seed(String key, byte[] content, String contentType) {
        var target = pathFor(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            objects.put(key, new StoredObject(content.length, contentType, null));
        } catch (IOException exception) {
            throw new StorageUnavailableException(exception);
        }
    }

    @Override public void delete(String key) {
        objects.remove(key);
        try { Files.deleteIfExists(pathFor(key)); } catch (IOException exception) { throw new StorageUnavailableException(exception); }
    }

    private long copy(InputStream input, Path output, MessageDigest digest) throws IOException {
        long total = 0;
        try (var stream = Files.newOutputStream(output)) {
            byte[] buffer = new byte[8192];
            for (int read; (read = input.read(buffer)) != -1;) {
                total += read;
                if (total > maxSizeBytes) throw new UploadTooLargeException();
                stream.write(buffer, 0, read);
                digest.update(buffer, 0, read);
            }
        }
        return total;
    }

    private Path pathFor(String key) {
        var path = directory.resolve(key).normalize();
        if (!path.startsWith(directory)) throw new StorageUnavailableException(new IllegalArgumentException("Invalid object key"));
        return path;
    }

    private void move(Path from, Path to) throws IOException {
        try { Files.move(from, to, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ignored) { Files.move(from, to); }
    }

    private String randomCapability() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String capabilityHash(String capability) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(capability.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JVM", exception);
        }
    }

    private record Grant(String key, String contentType, String checksumSha256, Instant expiresAt) { }
    public static final class UploadExpiredException extends RuntimeException { }
    public static final class UploadConflictException extends RuntimeException { }
    public static final class UploadValidationException extends IllegalArgumentException { }
    public static final class UploadTooLargeException extends RuntimeException { }
}
