package br.com.fiapx.videoprocessor.processing.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiapx.videoprocessor.processing.domain.ResultLocation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class MinioVideoObjectStorageTest {

    private static final StorageProperties PROPERTIES = new StorageProperties("http://localhost:9000", "videos", "us-east-1");

    @Mock
    private S3Client client;

    @TempDir
    Path workspace;

    private final ResultLocation resultLocation = ResultLocation.forJob(UUID.randomUUID());

    @Test
    void reportsTheResultAsPresentWhenTheObjectCanBeStatted() {
        assertThat(storage().exists(resultLocation)).isTrue();

        ArgumentCaptor<HeadObjectRequest> captor = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(client).headObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("videos");
        assertThat(captor.getValue().key()).isEqualTo(resultLocation.key());
    }

    @Test
    void reportsTheResultAsAbsentWhenTheObjectDoesNotExist() {
        when(client.headObject(any(HeadObjectRequest.class))).thenThrow(S3Exception.builder().statusCode(404).build());

        assertThat(storage().exists(resultLocation)).isFalse();
    }

    @Test
    void surfacesAnyOtherStatFailureAsTransient() {
        when(client.headObject(any(HeadObjectRequest.class))).thenThrow(S3Exception.builder().statusCode(503).build());

        assertThatThrownBy(() -> storage().exists(resultLocation)).isInstanceOf(StorageException.class);
    }

    @Test
    void downloadsTheSourceIntoTheWorkspaceFile() {
        Path target = workspace.resolve("source.mp4");
        when(client.<Path>getObject(any(GetObjectRequest.class), any(ResponseTransformer.class))).thenReturn(target);

        assertThat(storage().download("uploads/video.mp4", target)).isEqualTo(target);

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(client).getObject(captor.capture(), any(ResponseTransformer.class));
        assertThat(captor.getValue().bucket()).isEqualTo("videos");
        assertThat(captor.getValue().key()).isEqualTo("uploads/video.mp4");
    }

    @Test
    void surfacesADownloadFailureAsTransient() {
        when(client.<Path>getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
                .thenThrow(new RuntimeException("connection reset"));

        assertThatThrownBy(() -> storage().download("uploads/video.mp4", workspace.resolve("source.mp4")))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void uploadsTheArchiveUnderTheDeterministicResultKey() throws IOException {
        Path archive = Files.writeString(workspace.resolve("frames.zip"), "archive bytes");
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(PutObjectResponse.builder().build());

        storage().upload(resultLocation, archive);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("videos");
        assertThat(captor.getValue().key()).isEqualTo(resultLocation.key());
        assertThat(captor.getValue().contentType()).isEqualTo("application/zip");
    }

    @Test
    void surfacesAnUploadFailureAsTransient() throws IOException {
        Path archive = Files.writeString(workspace.resolve("frames.zip"), "archive bytes");
        doThrow(new RuntimeException("broken pipe"))
                .when(client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        assertThatThrownBy(() -> storage().upload(resultLocation, archive)).isInstanceOf(StorageException.class);
    }

    private MinioVideoObjectStorage storage() {
        return new MinioVideoObjectStorage(client, PROPERTIES);
    }
}
