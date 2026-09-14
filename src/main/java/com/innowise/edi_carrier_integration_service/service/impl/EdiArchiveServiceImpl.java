package com.innowise.edi_carrier_integration_service.service.impl;

import com.innowise.edi_carrier_integration_service.exception.EdiProcessingException;
import com.innowise.edi_carrier_integration_service.service.EdiArchiveService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EdiArchiveServiceImpl implements EdiArchiveService {

    private final MinioClient minioClient;

    @Value("${edi.archive.minio.bucket:edi-archive}")
    private String bucketName;

    @Override
    @PostConstruct
    public void initBucket() {
        try {
            var isExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());
            if (!isExists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
                log.info("MinIO bucket '{}' created successfully during initialization",
                        bucketName);
            } else {
                log.info("MinIO bucket '{}' verified and ready", bucketName);
            }
        } catch (MinioException e) {
            throw new EdiProcessingException(
                    "Failed to verify or create MinIO bucket during service initialization: "
                            + bucketName,
                    e);
        }
    }

    @Override
    @Retryable(retryFor = EdiProcessingException.class, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public String saveRawPayload(@NotNull String objectKey, @NotNull byte[] payload,
            String contentType) {
        if (payload.length == 0) {
            throw new EdiProcessingException("Payload bytes must not be empty for archiving");
        }
        try (var inputStream = new ByteArrayInputStream(payload)) {
            minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .stream(inputStream, (long) payload.length, -1L)
                .contentType(contentType)
                .build());
            log.info("Successfully stored non-repudiation document to S3: {}/{}",
                    bucketName, objectKey);
            return objectKey;
        } catch (MinioException | IOException e) {
            log.error("S3 upload attempt failed for object: {}. Retrying...", objectKey);
            throw new EdiProcessingException("S3 payload storage operation failed for object: "
                    + objectKey, e);
        }
    }
}