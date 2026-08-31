package unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.innowise.edi_carrier_integration_service.edi.domain.exception.EdiProcessingException;
import com.innowise.edi_carrier_integration_service.edi.infrastructure.archive.EdiArchiveService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
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

  @InjectMocks private EdiArchiveService ediArchiveService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(ediArchiveService, "bucketName", "test-bucket");
  }

  @Test
  @DisplayName("Should create bucket during init if bucket does not exist")
  void initBucket_bucketDoesNotExist_createsBucket() throws Exception {
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

    ediArchiveService.initBucket();

    verify(minioClient).makeBucket(any(MakeBucketArgs.class));
  }

  @Test
  @DisplayName("Should store payload successfully in MinIO")
  void storeRawPayload_success() throws Exception {
    byte[] payload = "EDI DATA".getBytes();

    String result =
        ediArchiveService.storeRawPayload("doc123.edi", payload, "application/edi-consent");

    assertThat(result).isEqualTo("doc123.edi");
    verify(minioClient).putObject(any(PutObjectArgs.class));
  }

  @Test
  @DisplayName("Should throw EdiProcessingException when payload is empty")
  void storeRawPayload_emptyPayload_throwsException() {
    assertThatThrownBy(
            () -> ediArchiveService.storeRawPayload("doc.edi", new byte[0], "text/plain"))
        .isInstanceOf(EdiProcessingException.class)
        .hasMessage("Payload bytes must not be empty for archiving");
  }
}
