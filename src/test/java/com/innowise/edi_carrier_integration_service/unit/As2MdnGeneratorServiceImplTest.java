package com.innowise.edi_carrier_integration_service.unit;

import com.innowise.edi_carrier_integration_service.service.MessageIntegrityService;
import com.innowise.edi_carrier_integration_service.service.impl.As2MdnGeneratorServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class As2MdnGeneratorServiceImplTest {

    @Mock
    private MessageIntegrityService messageIntegrityService;

    @InjectMocks
    private As2MdnGeneratorServiceImpl generator;

    @Test
    @DisplayName("generateMdn: success")
    void generateMdn_success() {
        byte[] payload = "test".getBytes();
        when(messageIntegrityService.computeMessageIntegrityCheck(payload))
            .thenReturn("dummy-mic-hash");

        String mdn = generator.generateMdn(payload, "", "SENDER", "RECEIVER");

        assertThat(mdn)
            .contains("Original-Message-ID: ")
            .contains("dummy-mic-hash")
            .contains("SENDER")
            .contains("RECEIVER");
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