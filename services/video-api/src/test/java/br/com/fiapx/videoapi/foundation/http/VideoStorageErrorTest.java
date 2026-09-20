package br.com.fiapx.videoapi.foundation.http;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiapx.videoapi.videos.application.StorageUnavailableException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class VideoStorageErrorTest {
    @Test
    void mapsStorageFailureToSanitizedTemporaryError() {
        var handler = new ApiExceptionHandler(new ApiProblemFactory());
        var request = new MockHttpServletRequest("POST", "/v1/videos/id/confirm");

        var response = handler.handleStorageUnavailable(new StorageUnavailableException(new IOException()), request);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody().getProperties()).containsEntry("code", "STORAGE_UNAVAILABLE");
        assertThat(response.getBody().getDetail()).doesNotContain("IOException");
    }
}
