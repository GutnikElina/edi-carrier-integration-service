package com.innowise.edi_carrier_integration_service.unit;

import com.innowise.edi_carrier_integration_service.service.impl.MessageIntegrityServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MessageIntegrityServiceImplTest {

    private final MessageIntegrityServiceImpl service = new MessageIntegrityServiceImpl();

    @Test
    @DisplayName("computeMessageIntegrityCheck: success basic payload")
    void computeMessageIntegrityCheck_success() {
        byte[] payload = "test".getBytes();
        String mic = service.computeMessageIntegrityCheck(payload);

        assertThat(mic).isNotBlank();

        // Используем assertThatCode вместо обычного assertThat для лямбда-выражений
        assertThatCode(() -> Base64.getDecoder().decode(mic))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("computeMessageIntegrityCheck: handles carriage returns")
    void computeMessageIntegrityCheck_carriageReturns() {
        byte[] payload = "test\r\nand\nmore".getBytes();
        String mic = service.computeMessageIntegrityCheck(payload);
        assertThat(mic).isNotBlank();
    }
}
