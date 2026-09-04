package com.innowise.edi_carrier_integration_service.edi.infrastructure.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${edi.archive.minio.endpoint:http://localhost:9000}")
    private String url;

    @Value("${edi.archive.minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${edi.archive.minio.secret-key:minioadmin}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder().endpoint(url).credentials(accessKey, secretKey).build();
    }
}
