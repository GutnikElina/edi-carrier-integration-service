package com.innowise.edi_carrier_integration_service.service;

import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;

public interface SMimeValidatorService {
    void verifyPayload(MimeBodyPart decryptedPart, String senderAlias,
            MimeMultipart multipart);
}
