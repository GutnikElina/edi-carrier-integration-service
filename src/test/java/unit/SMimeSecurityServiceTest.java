package unit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.innowise.edi_carrier_integration_service.edi.domain.exception.EdiSecurityException;
import com.innowise.edi_carrier_integration_service.edi.infrastructure.crypto.KeyManagementService;
import com.innowise.edi_carrier_integration_service.edi.infrastructure.crypto.SMimeSecurityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SMimeSecurityServiceTest {

  @Mock private KeyManagementService keyManagementService;

  @InjectMocks private SMimeSecurityService sMimeSecurityService;

  @Test
  @DisplayName("Should throw NullPointerException when input payload is null")
  void decryptAndVerify_nullPayload_throwsException() {
    assertThatThrownBy(() -> sMimeSecurityService.decryptAndVerify(null, "alias1", "alias2"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("S/MIME payload byte array must not be null");

    verifyNoInteractions(keyManagementService);
  }

  @Test
  @DisplayName("Should throw EdiSecurityException when input payload is empty")
  void decryptAndVerify_emptyPayload_throwsEdiSecurityException() {
    byte[] emptyBytes = new byte[0];

    assertThatThrownBy(() -> sMimeSecurityService.decryptAndVerify(emptyBytes, "alias1", "alias2"))
        .isInstanceOf(EdiSecurityException.class)
        .hasMessage("S/MIME payload byte array is empty");

    verifyNoInteractions(keyManagementService);
  }

  @Test
  @DisplayName("Should throw NullPointerException when recipientAlias is null")
  void decryptAndVerify_nullRecipientAlias_throwsException() {
    byte[] payload = new byte[] {1, 2, 3};

    assertThatThrownBy(() -> sMimeSecurityService.decryptAndVerify(payload, null, "alias2"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Recipient KeyStore alias must not be null");
  }

  @Test
  @DisplayName("Should throw EdiSecurityException on invalid SMIME payload format")
  void decryptAndVerify_invalidSmimeData_throwsEdiSecurityException() {
    byte[] invalidPayload = "not-a-valid-smime-message".getBytes();

    assertThatThrownBy(
            () -> sMimeSecurityService.decryptAndVerify(invalidPayload, "recAlias", "sendAlias"))
        .isInstanceOf(EdiSecurityException.class)
        .hasMessage("S/MIME Decryption operation failed"); // <-- Поправлен текст ошибки
  }
}
