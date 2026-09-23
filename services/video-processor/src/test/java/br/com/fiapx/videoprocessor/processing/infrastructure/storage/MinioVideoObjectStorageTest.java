package br.com.fiapx.videoprocessor.processing.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiapx.videoprocessor.processing.domain.ResultLocation;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MinioVideoObjectStorageTest {

    private static final StorageProperties PROPERTIES =
            new StorageProperties("http://localhost:9000", "fiapx", "fiapx-secret", "videos");

    @Mock
    private MinioClient client;

    @TempDir
    Path workspace;

    private final ResultLocation resultLocation = ResultLocation.forJob(UUID.randomUUID());

    @Test
    void reportsTheResultAsPresentWhenTheObjectCanBeStatted() throws Exception {
        assertThat(storage().exists(resultLocation)).isTrue();

        ArgumentCaptor<StatObjectArgs> captor = ArgumentCaptor.forClass(StatObjectArgs.class);
        verify(client).statObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("videos");
        assertThat(captor.getValue().object()).isEqualTo(resultLocation.key());
    }

    @Test
    void reportsTheResultAsAbsentWhenTheObjectDoesNotExist() throws Exception {
        when(client.statObject(any(StatObjectArgs.class))).thenThrow(notFound());

        assertThat(storage().exists(resultLocation)).isFalse();
    }

    @Test
    void surfacesAnyOtherStatFailureAsTransient() throws Exception {
        when(client.statObject(any(StatObjectArgs.class))).thenThrow(new IOException("connection reset"));

        assertThatThrownBy(() -> storage().exists(resultLocation)).isInstanceOf(StorageException.class);
    }

    @Test
    void downloadsTheSourceIntoTheWorkspaceFile() throws Exception {
        Path target = Paths.get("workspace", "job-1", "source");

        assertThat(storage().download("uploads/video.mp4", target)).isEqualTo(target);

        ArgumentCaptor<DownloadObjectArgs> captor = ArgumentCaptor.forClass(DownloadObjectArgs.class);
        verify(client).downloadObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("videos");
        assertThat(captor.getValue().object()).isEqualTo("uploads/video.mp4");
        assertThat(captor.getValue().filename()).isEqualTo(target.toString());
    }

    @Test
    void surfacesADownloadFailureAsTransient() throws Exception {
        doThrow(new IOException("connection reset")).when(client).downloadObject(any(DownloadObjectArgs.class));

        assertThatThrownBy(() -> storage().download("uploads/video.mp4", Paths.get("source")))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void uploadsTheArchiveUnderTheDeterministicResultKey() throws Exception {
        Path archive = anArchiveOnDisk();

        storage().upload(resultLocation, archive);

        ArgumentCaptor<UploadObjectArgs> captor = ArgumentCaptor.forClass(UploadObjectArgs.class);
        verify(client).uploadObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("videos");
        assertThat(captor.getValue().object()).isEqualTo(resultLocation.key());
        assertThat(captor.getValue().filename()).isEqualTo(archive.toString());
    }

    @Test
    void surfacesAnUploadFailureAsTransient() throws Exception {
        doThrow(new IOException("broken pipe")).when(client).uploadObject(any(UploadObjectArgs.class));
        Path archive = anArchiveOnDisk();

        assertThatThrownBy(() -> storage().upload(resultLocation, archive)).isInstanceOf(StorageException.class);
    }

    private MinioVideoObjectStorage storage() {
        return new MinioVideoObjectStorage(client, PROPERTIES);
    }

    private Path anArchiveOnDisk() throws IOException {
        return Files.writeString(workspace.resolve("frames.zip"), "archive bytes");
    }

    private ErrorResponseException notFound() {
        ErrorResponse error =
                new ErrorResponse("NoSuchKey", "Object does not exist", "videos", resultLocation.key(), "", "", "");
        Response response = new Response.Builder()
                .request(new Request.Builder().url("http://localhost:9000/videos").build())
                .protocol(Protocol.HTTP_1_1)
                .code(404)
                .message("Not Found")
                .build();
        return new ErrorResponseException(error, response, null);
    }
}
