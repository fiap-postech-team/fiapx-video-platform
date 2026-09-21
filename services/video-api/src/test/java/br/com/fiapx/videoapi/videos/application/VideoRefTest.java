package br.com.fiapx.videoapi.videos.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class VideoRefTest {

    @Test
    void encodesSixteenBytesWithoutPaddingAndDecodesBack() {
        var id = UUID.fromString("b7b9ec4d-012e-4b82-8f18-cf90f0d9662b");
        var ref = VideoRef.encode(id);
        assertThat(ref).doesNotContain("=").doesNotContain("/");
        assertThat(VideoRef.decode(ref)).contains(id);
    }

    @Test
    void treatsMalformedReferencesAsMissing() {
        assertThat(VideoRef.decode("not-a-ref")).isEmpty();
        assertThat(VideoRef.decode("")).isEmpty();
        assertThat(VideoRef.decode(null)).isEmpty();
    }
}
