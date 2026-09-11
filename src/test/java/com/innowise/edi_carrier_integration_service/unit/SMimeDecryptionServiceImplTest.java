package com.innowise.edi_carrier_integration_service.unit;

import com.innowise.edi_carrier_integration_service.exception.EdiSecurityException;
import com.innowise.edi_carrier_integration_service.service.SMimeValidatorService;
import com.innowise.edi_carrier_integration_service.service.impl.SMimeDecryptionServiceImpl;
import com.innowise.edi_carrier_integration_service.util.SMimeDecryptionUtil;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SMimeDecryptionServiceImplTest {

    @Mock
    private SMimeValidatorService sMimeValidatorService;
    @Mock
    private SMimeDecryptionUtil sMimeDecryptionUtil;

    @InjectMocks
    private SMimeDecryptionServiceImpl service;

    @Test
    @DisplayName("decrypt: success")
    void decrypt_success() throws Exception {
        // Construct valid mime string to pass new MimeBodyPart(inputStream)
        byte[] payload = "Content-Type: text/plain\r\n\r\nHello".getBytes();

        MimeBodyPart mockDecryptedBody = mock(MimeBodyPart.class);
        MimeMultipart mockMultipart = mock(MimeMultipart.class);
        MimeBodyPart mockInnerPart = mock(MimeBodyPart.class);

        when(sMimeDecryptionUtil.decryptMimeBody(any(MimeBodyPart.class), eq("rec-alias")))
            .thenReturn(mockDecryptedBody);
        when(mockDecryptedBody.getContent()).thenReturn(mockMultipart);
        when(mockMultipart.getBodyPart(0)).thenReturn(mockInnerPart);
        when(sMimeDecryptionUtil.extractPayloadBytes(mockInnerPart))
            .thenReturn("result".getBytes());

        byte[] result = service.decrypt(payload, "rec-alias", "send-alias");

        assertThat(result).isEqualTo("result".getBytes());
        verify(sMimeValidatorService).verifyPayload(mockDecryptedBody, "send-alias", mockMultipart);
    }

    @Test
    @DisplayName("decrypt: fails on null payload")
    void decrypt_nullPayload() {
        assertThatThrownBy(() -> service.decrypt(null, "rec", "send"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("S/MIME payload byte array must not be null");
    }

    @Test
    @DisplayName("decrypt: fails on empty payload")
    void decrypt_emptyPayload() {
        assertThatThrownBy(() -> service.decrypt(new byte[0], "rec", "send"))
            .isInstanceOf(EdiSecurityException.class)
            .hasMessageContaining("S/MIME payload byte array is empty");
    }
}
