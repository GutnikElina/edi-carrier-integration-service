package com.innowise.edi_carrier_integration_service.edi.infrastructure.archive;

import com.innowise.edi_carrier_integration_service.edi.domain.exception.EdiProcessingException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EdiArchiveService {

  private final MinioClient minioClient;

  @Value("${edi.archive.minio.bucket:edi-archive}")
  private String bucketName;

  @PostConstruct
  public void initBucket() {
    try {
      boolean found =
          minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
      if (!found) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        log.info("MinIO bucket '{}' created successfully during initialization", bucketName);
      } else {
        log.info("MinIO bucket '{}' verified and ready", bucketName);
      }
    } catch (Exception e) {
      throw new EdiProcessingException(
          "Failed to verify or create MinIO bucket during service initialization: " + bucketName,
          e);
    }
  }

  @Retryable(
      retryFor = {EdiProcessingException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 1000, multiplier = 2.0))
  public String storeRawPayload(String objectName, byte[] payload, String contentType) {
    Objects.requireNonNull(objectName, "Object name must not be null");
    Objects.requireNonNull(payload, "Payload bytes must not be null");
    if (payload.length == 0) {
      throw new EdiProcessingException("Payload bytes must not be empty for archiving");
    }

    try (InputStream is = new ByteArrayInputStream(payload)) {
      minioClient.putObject(
          PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                  is, payload.length, -1)
              .contentType(contentType)
              .build());
      log.info("Successfully stored non-repudiation document to S3: {}/{}", bucketName, objectName);
      return objectName;
    } catch (MinioException | IOException e) {
      log.warn("S3 upload attempt failed for object: {}. Retrying...", objectName);
      throw new EdiProcessingException(
          "S3 payload storage operation failed for object: " + objectName, e);
    } catch (Exception e) {
      throw new EdiProcessingException(
          "Unexpected error during S3 payload storage for object: " + objectName, e);
    }
  }
}
