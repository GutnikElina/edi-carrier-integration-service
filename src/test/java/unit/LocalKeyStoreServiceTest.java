package unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.innowise.edi_carrier_integration_service.edi.domain.exception.EdiSecurityException;
import com.innowise.edi_carrier_integration_service.edi.infrastructure.crypto.LocalKeyStoreService;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LocalKeyStoreServiceTest {

  private LocalKeyStoreService localKeyStoreService;

  @BeforeEach
  void setUp() {
    localKeyStoreService = new LocalKeyStoreService();
  }

  @Test
  @DisplayName("Should return PrivateKey when alias exists in cache")
  void getPrivateKey_success() {
    PrivateKey mockKey = mock(PrivateKey.class);
    Map<String, PrivateKey> cache =
        (Map<String, PrivateKey>)
            ReflectionTestUtils.getField(localKeyStoreService, "privateKeyCache");
    cache.put("my-key-alias", mockKey);

    PrivateKey result = localKeyStoreService.getPrivateKey("my-key-alias");

    assertThat(result).isNotNull().isEqualTo(mockKey);
  }

  @Test
  @DisplayName("Should throw EdiSecurityException when PrivateKey alias is missing")
  void getPrivateKey_notFound_throwsException() {
    assertThatThrownBy(() -> localKeyStoreService.getPrivateKey("non-existing-alias"))
        .isInstanceOf(EdiSecurityException.class)
        .hasMessageContaining("PrivateKey alias not found in KeyStore cache");
  }

  @Test
  @DisplayName("Should return Certificate when alias exists in cache")
  void getCertificate_success() {
    X509Certificate mockCert = mock(X509Certificate.class);
    Map<String, X509Certificate> cache =
        (Map<String, X509Certificate>)
            ReflectionTestUtils.getField(localKeyStoreService, "certificateCache");
    cache.put("my-cert-alias", mockCert);

    X509Certificate result = localKeyStoreService.getCertificate("my-cert-alias");

    assertThat(result).isNotNull().isEqualTo(mockCert);
  }

  @Test
  @DisplayName("Should return TrustCertificate when alias exists in trust cache")
  void getTrustCertificate_success() {
    X509Certificate mockCert = mock(X509Certificate.class);
    Map<String, X509Certificate> cache =
        (Map<String, X509Certificate>)
            ReflectionTestUtils.getField(localKeyStoreService, "trustCertificateCache");
    cache.put("trust-alias", mockCert);

    X509Certificate result = localKeyStoreService.getTrustCertificate("trust-alias");

    assertThat(result).isNotNull().isEqualTo(mockCert);
  }
}
