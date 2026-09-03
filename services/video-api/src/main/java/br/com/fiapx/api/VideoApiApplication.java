package br.com.fiapx.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class VideoApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(VideoApiApplication.class, args);
    }
}
