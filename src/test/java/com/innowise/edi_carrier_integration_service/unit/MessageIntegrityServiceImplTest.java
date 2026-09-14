package com.innowise.edi_carrier_integration_service.unit;

import com.innowise.edi_carrier_integration_service.service.impl.MessageIntegrityServiceImpl;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.Security;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MessageIntegrityServiceImplTest {

    private final MessageIntegrityServiceImpl service = new MessageIntegrityServiceImpl();

    @BeforeAll
    static void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    @DisplayName("computeMessageIntegrityCheck: success basic payload")
    void computeMessageIntegrityCheck_success() {
        byte[] payload = "test".getBytes();
        String mic = service.computeMessageIntegrityCheck(payload);
        assertThat(mic).isNotBlank();
        assertThatCode(() -> Base64.getDecoder()
            .decode(mic))
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