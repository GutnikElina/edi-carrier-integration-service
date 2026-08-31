package unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.innowise.edi_carrier_integration_service.edi.domain.exception.EdiProcessingException;
import com.innowise.edi_carrier_integration_service.edi.infrastructure.archive.EdiArchiveService;
import io.minio.*;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EdiArchiveServiceTest {

  @Mock private MinioClient minioClient;
  @InjectMocks private EdiArchiveService service;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "bucketName", "test-bucket");
  }

  @Test
  @DisplayName("initBucket: creates bucket if missing")
  void initBucket_createsIfMissing() throws Exception {
    when(minioClient.bucketExists(any())).thenReturn(false);
    service.initBucket();
    verify(minioClient).makeBucket(any());
  }

  @Test
  @DisplayName("initBucket: does nothing if bucket exists")
  void initBucket_doesNothingIfExists() throws Exception {
    when(minioClient.bucketExists(any())).thenReturn(true);
    service.initBucket();
    verify(minioClient, never()).makeBucket(any());
  }

  @Test
  @DisplayName("initBucket: throws EdiProcessingException on error")
  void initBucket_throwsOnError() throws Exception {
    when(minioClient.bucketExists(any())).thenThrow(new RuntimeException("boom"));
    assertThatThrownBy(() -> service.initBucket())
        .isInstanceOf(EdiProcessingException.class)
        .hasMessageContaining("Failed to verify or create MinIO bucket");
  }

  @Test
  @DisplayName("storeRawPayload: success")
  void storeRawPayload_success() throws Exception {
    byte[] payload = "data".getBytes();
    String result = service.storeRawPayload("obj", payload, "text/plain");
    assertThat(result).isEqualTo("obj");
    verify(minioClient).putObject(any());
  }

  @Test
  @DisplayName("storeRawPayload: throws on null objectName")
  void storeRawPayload_nullObjectName() {
    assertThatThrownBy(() -> service.storeRawPayload(null, new byte[1], "text"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Object name must not be null");
  }

  @Test
  @DisplayName("storeRawPayload: throws on null payload")
  void storeRawPayload_nullPayload() {
    assertThatThrownBy(() -> service.storeRawPayload("obj", null, "text"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Payload bytes must not be null");
  }

  @Test
  @DisplayName("storeRawPayload: throws on empty payload")
  void storeRawPayload_emptyPayload() {
    assertThatThrownBy(() -> service.storeRawPayload("obj", new byte[0], "text"))
        .isInstanceOf(EdiProcessingException.class)
        .hasMessage("Payload bytes must not be empty for archiving");
  }

  @Test
  void storeRawPayload_minioException() throws Exception {
    doThrow(new IOException("IO error")).when(minioClient).putObject(any(PutObjectArgs.class));
    assertThatThrownBy(() -> service.storeRawPayload("obj", "data".getBytes(), "text"))
        .isInstanceOf(EdiProcessingException.class)
        .hasMessageContaining("S3 payload storage operation failed");
  }

  @Test
  @DisplayName("storeRawPayload: rethrows on unexpected exception")
  void storeRawPayload_unexpectedException() throws Exception {
    when(minioClient.putObject(any())).thenThrow(new IllegalStateException("unexpected"));
    assertThatThrownBy(() -> service.storeRawPayload("obj", "data".getBytes(), "text"))
        .isInstanceOf(EdiProcessingException.class)
        .hasMessageContaining("Unexpected error");
  }
}
