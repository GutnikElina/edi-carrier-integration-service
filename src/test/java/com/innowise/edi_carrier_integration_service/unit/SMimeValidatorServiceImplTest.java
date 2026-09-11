package com.innowise.edi_carrier_integration_service.unit;

import com.innowise.edi_carrier_integration_service.service.impl.SMimeValidatorServiceImpl;
import com.innowise.edi_carrier_integration_service.util.SMimeValidatorUtil;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SMimeValidatorServiceImplTest {

    @Mock
    private SMimeValidatorUtil validatorUtil;

    @InjectMocks
    private SMimeValidatorServiceImpl service;

    @Test
    @DisplayName("verifyPayload: success")
    void verifyPayload_success() {
        MimeBodyPart bodyPart = mock(MimeBodyPart.class);
        MimeMultipart multipart = mock(MimeMultipart.class);

        service.verifyPayload(bodyPart, "sender-alias", multipart);

        verify(validatorUtil).validateMimeType(bodyPart);
        verify(validatorUtil).verifySignatures("sender-alias", multipart);
    }
}