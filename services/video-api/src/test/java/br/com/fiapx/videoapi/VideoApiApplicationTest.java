package br.com.fiapx.videoapi;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class VideoApiApplicationTest {

    @Test
    void startsSpringBootWithItsOwnApplicationClass() {
        new VideoApiApplication();

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            VideoApiApplication.main(new String[0]);

            springApplication.verify(
                () -> SpringApplication.run(VideoApiApplication.class, new String[0])
            );
        }
    }
}
