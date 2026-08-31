package unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.innowise.edi_carrier_integration_service.edi.infrastructure.as2.As2MdnGenerator;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class As2MdnGeneratorTest {

  private final As2MdnGenerator generator = new As2MdnGenerator();

  @BeforeAll
  static void setupSecurity() {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  @Test
  @DisplayName("Should generate valid AS2 MDN string with mandatory headers")
  void generateMdn_success() {
    byte[] payload = "UNB+UNOA:1+SENDER+RECEIVER+201026:1022+1'".getBytes();
    String msgId = "<MSG-12345@carrier.com>";
    String senderAs2 = "CARRIER_AS2";
    String receiverAs2 = "MY_COMPANY_AS2";

    String mdn = generator.generateMdn(payload, msgId, senderAs2, receiverAs2);

    assertThat(mdn)
        .isNotNull()
        .contains("AS2-Version: 1.2")
        .contains("From: MY_COMPANY_AS2")
        .contains("To: CARRIER_AS2")
        .contains("Original-Message-ID: " + msgId)
        .contains("Disposition: automatic-action/MDN-sent-automatically; processed")
        .contains("sha-256");
  }

  @Test
  @DisplayName("Should throw NullPointerException when payload is null")
  void generateMdn_nullPayload_throwsNpe() {
    assertThatThrownBy(() -> generator.generateMdn(null, "id", "sender", "receiver"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Original payload byte array must not be null");
  }
}
