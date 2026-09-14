package com.innowise.edi_carrier_integration_service.unit;

import com.innowise.edi_carrier_integration_service.exception.EdiProcessingException;
import com.innowise.edi_carrier_integration_service.service.impl.EdiArchiveServiceImpl;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EdiArchiveServiceImplTest {

    @Mock
    private MinioClient minioClient;
    @InjectMocks
    private EdiArchiveServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "bucketName", "test-bucket");
    }

    @Test
    @DisplayName("initBucket: creates bucket if missing")
    void initBucket_createsIfMissing() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        service.initBucket();
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("initBucket: does nothing if bucket exists")
    void initBucket_doesNothingIfExists() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        service.initBucket();
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("initBucket: throws EdiProcessingException on error")
    void initBucket_throwsOnError() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenThrow(new MinioException("boom"));
        assertThatThrownBy(() -> service.initBucket())
                .isInstanceOf(EdiProcessingException.class)
                .hasMessageContaining("Failed to verify or create MinIO bucket");
    }

    @Test
    @DisplayName("storeRawPayload: success")
    void saveRawPayload_success() throws Exception {
        byte[] payload = "data".getBytes();
        String result = service.saveRawPayload("obj", payload, "text/plain");
        assertThat(result).isEqualTo("obj");
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("storeRawPayload: throws on empty payload")
    void saveRawPayload_emptyPayload() {
        assertThatThrownBy(() -> service.saveRawPayload("obj", new byte[0], "text/plain"))
            .isInstanceOf(EdiProcessingException.class)
            .hasMessage("Payload bytes must not be empty for archiving");
    }

    @Test
    @DisplayName("storeRawPayload: throws EdiProcessingException on minio exception")
    void saveRawPayload_minioException() throws Exception {
        doThrow(new MinioException("error")).when(minioClient).putObject(any(PutObjectArgs.class));
        assertThatThrownBy(() -> service.saveRawPayload("obj", "data".getBytes(), "text/plain"))
            .isInstanceOf(EdiProcessingException.class)
            .hasMessageContaining("S3 payload storage operation failed for object: obj");
    }
}
