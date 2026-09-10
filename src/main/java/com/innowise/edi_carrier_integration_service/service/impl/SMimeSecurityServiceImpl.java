package com.innowise.edi_carrier_integration_service.service.impl;

import com.innowise.edi_carrier_integration_service.exception.EdiSecurityException;
import com.innowise.edi_carrier_integration_service.exception.PayloadExtractionException;
import com.innowise.edi_carrier_integration_service.exception.PayloadTooLargeException;
import com.innowise.edi_carrier_integration_service.service.KeyManagementService;
import com.innowise.edi_carrier_integration_service.service.SMimeSecurityService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SMimeSecurityServiceImpl implements SMimeSecurityService {

    private final SMimeValidatorServiceImpl sMimeValidatorService;
    private final KeyManagementService keyManagementService;

    @Value("${edi.pipeline.max-payload-bytes:20971520}")
    private long maxAllowedPayloadBytes;

    @Override
    public byte[] decryptAndVerify(byte[] sMimeMessageBytes, String recipientAlias,
            String senderAlias) {

        validateInputData(sMimeMessageBytes, recipientAlias, senderAlias);

        try (var inputStream = new ByteArrayInputStream(sMimeMessageBytes)) {
            var encryptedPart = new MimeBodyPart(inputStream);
            var decryptedPart = decrypt(encryptedPart, recipientAlias);
            try {
                var multipart = (MimeMultipart) decryptedPart.getContent();
                sMimeValidatorService.verifyPayload(decryptedPart, senderAlias, multipart);
                return extractPayloadBytes((MimeBodyPart) multipart.getBodyPart(0));
            } finally {
                cleanupMimePart(decryptedPart);
            }
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

    private MimeBodyPart decrypt(MimeBodyPart encryptedPart, String recipientAlias) {
        try {
            var enveloped = new SMIMEEnveloped(encryptedPart);
            var privateKey = keyManagementService.getPrivateKey(recipientAlias);
            var recipientCert = keyManagementService.getCertificate(recipientAlias);

            var recipients = enveloped.getRecipientInfos();
            var recipientId = new JceKeyTransRecipientId(recipientCert);
            var recipient = Optional.ofNullable(recipients.get(recipientId))
                .orElseThrow(
                        () -> new EdiSecurityException("No recipient matching certificate alias '"
                                + recipientAlias + "' found in S/MIME EnvelopedData"));

            return SMIMEUtil.toMimeBodyPart(
                    recipient.getContent(new JceKeyTransEnvelopedRecipient(privateKey)
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)));
        } catch (SMIMEException | CMSException | MessagingException ex) {
            throw new EdiSecurityException("S/MIME Decryption operation failed", ex);
        }
    }

    private byte[] extractPayloadBytes(MimeBodyPart contentPart) {
        try (var is = contentPart.getInputStream()) {
            var bytes = is.readNBytes((int) maxAllowedPayloadBytes + 1);
            if (bytes.length > maxAllowedPayloadBytes) {
                throw new PayloadTooLargeException(
                        "Extracted payload size exceeds limit: " + maxAllowedPayloadBytes
                                + " bytes");
            }
            return bytes;
        } catch (MessagingException | IOException e) {
            throw new PayloadExtractionException(e.getMessage());
        }
    }

    private void cleanupMimePart(MimeBodyPart part) {
        if (part != null) {
            try {
                Object content = part.getContent();
                if (content instanceof InputStream is) {
                    is.close();
                }
            } catch (IOException | MessagingException e) {
                log.debug("Resource cleanup failed for MimeBodyPart", e);
            }
        }
    }
}
