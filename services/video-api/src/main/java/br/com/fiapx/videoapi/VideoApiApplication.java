package br.com.fiapx.videoapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the Video API Spring Boot process and defines its component-scan root.
 */
@SpringBootApplication
public class VideoApiApplication {

    /**
     * Creates the Spring configuration root.
     */
    public VideoApiApplication() {
    }

    /**
     * Starts the application.
     *
     * @param args command-line configuration arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(VideoApiApplication.class, args);
    }
}
