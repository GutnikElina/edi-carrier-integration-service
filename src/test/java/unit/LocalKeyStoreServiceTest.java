package unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.innowise.edi_carrier_integration_service.edi.domain.exception.EdiSecurityException;
import com.innowise.edi_carrier_integration_service.edi.infrastructure.crypto.LocalKeyStoreService;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LocalKeyStoreServiceTest {

  private LocalKeyStoreService service;
  private File keyStoreFile, trustStoreFile;
  private char[] keyStorePass = "changeit".toCharArray();
  private char[] trustStorePass = "changeit".toCharArray();

  @BeforeEach
  void setUp() throws Exception {
    service = new LocalKeyStoreService();

    var keyPair = TestSmimeUtils.generateKeyPair();
    var cert = TestSmimeUtils.generateSelfSignedCert(keyPair, "CN=test");

    KeyStore ks = KeyStore.getInstance("PKCS12");
    ks.load(null, null);
    ks.setKeyEntry(
        "my-key", keyPair.getPrivate(), keyStorePass, new java.security.cert.Certificate[] {cert});
    ks.setCertificateEntry("my-cert", cert);
    keyStoreFile = File.createTempFile("keystore", ".p12");
    try (FileOutputStream fos = new FileOutputStream(keyStoreFile)) {
      ks.store(fos, keyStorePass);
    }

    KeyStore ts = KeyStore.getInstance("PKCS12");
    ts.load(null, null);
    ts.setCertificateEntry("trust-alias", cert);
    trustStoreFile = File.createTempFile("truststore", ".p12");
    try (FileOutputStream fos = new FileOutputStream(trustStoreFile)) {
      ts.store(fos, trustStorePass);
    }

    ReflectionTestUtils.setField(service, "keyStorePath", keyStoreFile.getAbsolutePath());
    ReflectionTestUtils.setField(service, "keyStorePassword", keyStorePass);
    ReflectionTestUtils.setField(service, "keyStoreType", "PKCS12");
    ReflectionTestUtils.setField(service, "trustStorePath", trustStoreFile.getAbsolutePath());
    ReflectionTestUtils.setField(service, "trustStorePassword", trustStorePass);
    ReflectionTestUtils.setField(service, "trustStoreType", "PKCS12");
  }

  @Test
  @DisplayName("init: loads KeyStore and TrustStore successfully")
  void init_success() {
    service.init();
    assertThat(service.getPrivateKey("my-key")).isNotNull();
    assertThat(service.getCertificate("my-cert")).isNotNull();
    assertThat(service.getTrustCertificate("trust-alias")).isNotNull();
  }

  @Test
  @DisplayName("init: throws EdiSecurityException if KeyStore missing")
  void init_keyStoreMissing() {
    ReflectionTestUtils.setField(service, "keyStorePath", "/non/existent/file");
    assertThatThrownBy(() -> service.init())
        .isInstanceOf(EdiSecurityException.class)
        .hasMessageContaining("Failed to load KeyStore");
  }

  @Test
  @DisplayName("init: throws if TrustStore missing")
  void init_trustStoreMissing() {
    ReflectionTestUtils.setField(service, "trustStorePath", "/non/existent/file");
    assertThatThrownBy(() -> service.init())
        .isInstanceOf(EdiSecurityException.class)
        .hasMessageContaining("Failed to load TrustStore");
  }

  @Test
  @DisplayName("getPrivateKey: returns key if exists")
  void getPrivateKey_success() {
    service.init();
    PrivateKey key = service.getPrivateKey("my-key");
    assertThat(key).isNotNull();
  }

  @Test
  @DisplayName("getPrivateKey: throws if alias missing")
  void getPrivateKey_missing() {
    service.init();
    assertThatThrownBy(() -> service.getPrivateKey("unknown"))
        .isInstanceOf(EdiSecurityException.class)
        .hasMessageContaining("PrivateKey alias not found");
  }

  @Test
  @DisplayName("getPrivateKey: NPE on null alias")
  void getPrivateKey_nullAlias() {
    assertThatThrownBy(() -> service.getPrivateKey(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("KeyStore alias must not be null");
  }

  @Test
  @DisplayName("getCertificate: returns cert if exists")
  void getCertificate_success() {
    service.init();
    X509Certificate cert = service.getCertificate("my-cert");
    assertThat(cert).isNotNull();
  }

  @Test
  @DisplayName("getCertificate: throws if missing")
  void getCertificate_missing() {
    service.init();
    assertThatThrownBy(() -> service.getCertificate("unknown"))
        .isInstanceOf(EdiSecurityException.class)
        .hasMessageContaining("Certificate not found");
  }

  @Test
  @DisplayName("getCertificate: NPE on null alias")
  void getCertificate_nullAlias() {
    assertThatThrownBy(() -> service.getCertificate(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Certificate alias must not be null");
  }

  @Test
  @DisplayName("getTrustCertificate: returns trust cert if exists")
  void getTrustCertificate_success() {
    service.init();
    X509Certificate cert = service.getTrustCertificate("trust-alias");
    assertThat(cert).isNotNull();
  }

  @Test
  @DisplayName("getTrustCertificate: throws if missing")
  void getTrustCertificate_missing() {
    service.init();
    assertThatThrownBy(() -> service.getTrustCertificate("unknown"))
        .isInstanceOf(EdiSecurityException.class)
        .hasMessageContaining("Certificate not found in TrustStore");
  }

  @Test
  @DisplayName("getTrustCertificate: NPE on null alias")
  void getTrustCertificate_nullAlias() {
    assertThatThrownBy(() -> service.getTrustCertificate(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("TrustStore alias must not be null");
  }
}
