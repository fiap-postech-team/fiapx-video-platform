package br.com.fiapx.processor.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
public class StorageConfig {
    @Bean
    MinioClient minio(@Value("${app.storage.endpoint}") String endpoint, @Value("${app.storage.access-key}") String access, @Value("${app.storage.secret-key}") String secret) {
        return MinioClient.builder().endpoint(endpoint).credentials(access, secret).build();
    }
}
