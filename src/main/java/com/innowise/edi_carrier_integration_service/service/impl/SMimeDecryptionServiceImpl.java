package com.innowise.edi_carrier_integration_service.service.impl;

import com.innowise.edi_carrier_integration_service.exception.EdiSecurityException;
import com.innowise.edi_carrier_integration_service.service.SMimeDecryptionService;
import com.innowise.edi_carrier_integration_service.service.SMimeValidatorService;
import com.innowise.edi_carrier_integration_service.util.SMimeDecryptionUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SMimeDecryptionServiceImpl implements SMimeDecryptionService {

    private final SMimeValidatorService sMimeValidatorService;
    private final SMimeDecryptionUtil sMimeDecryptionUtil;

    @Override
    public byte[] decrypt(byte[] sMimeMessageBytes, String recipientAlias,
            String senderAlias) {

        validateInputData(sMimeMessageBytes, recipientAlias, senderAlias);

        try (var inputStream = new ByteArrayInputStream(sMimeMessageBytes)) {
            var encryptedMimeBody = new MimeBodyPart(inputStream);
            var decryptedMimeBody = sMimeDecryptionUtil.decryptMimeBody(encryptedMimeBody,
                    recipientAlias);

            var mimeMultipart = (MimeMultipart) decryptedMimeBody.getContent();
            sMimeValidatorService.verifyPayload(decryptedMimeBody, senderAlias, mimeMultipart);

            return sMimeDecryptionUtil
                .extractPayloadBytes((MimeBodyPart) mimeMultipart.getBodyPart(0));
        } catch (MessagingException | IOException e) {
            log.error("S/MIME processing pipeline failed");
            throw new EdiSecurityException("S/MIME processing pipeline failed", e);
        }
    }

    private void validateInputData(byte[] sMimeMessageBytes, String recipientAlias,
            String senderAlias) {
        Objects.requireNonNull(sMimeMessageBytes, "S/MIME payload byte array must not be null");
        if (sMimeMessageBytes.length == 0) {
            throw new EdiSecurityException("S/MIME payload byte array is empty");
        }
        Objects.requireNonNull(recipientAlias, "Recipient KeyStore alias must not be null");
        Objects.requireNonNull(senderAlias, "Sender TrustStore alias must not be null");
    }
}