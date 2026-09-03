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
    static void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    @DisplayName("generateMdn: success")
    void generateMdn_success() {
        byte[] payload = "test".getBytes();
        String mdn = generator.generateMdn(payload, "<msg>", "SENDER", "RECEIVER");
        assertThat(mdn)
            .contains("AS2-Version: 1.2")
            .contains("From: RECEIVER")
            .contains("To: SENDER")
            .contains("Original-Message-ID: <msg>")
            .contains("Received-Content-MIC:")
            .contains("sha-256");
    }

    @Test
    @DisplayName("generateMdn: NPE on null payload")
    void generateMdn_nullPayload() {
        assertThatThrownBy(() -> generator.generateMdn(null, "id", "s", "r"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Original payload byte array must not be null");
    }

    @Test
    @DisplayName("generateMdn: NPE on null messageId")
    void generateMdn_nullMessageId() {
        assertThatThrownBy(() -> generator.generateMdn(new byte[1], null, "s", "r"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Original Message-ID must not be null");
    }

    @Test
    @DisplayName("generateMdn: NPE on null senderAs2Id")
    void generateMdn_nullSender() {
        assertThatThrownBy(() -> generator.generateMdn(new byte[1], "id", null, "r"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Sender AS2 ID must not be null");
    }

    @Test
    @DisplayName("generateMdn: NPE on null receiverAs2Id")
    void generateMdn_nullReceiver() {
        assertThatThrownBy(() -> generator.generateMdn(new byte[1], "id", "s", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Receiver AS2 ID must not be null");
    }
}
