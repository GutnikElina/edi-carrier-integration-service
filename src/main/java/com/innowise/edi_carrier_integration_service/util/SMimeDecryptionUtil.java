package com.innowise.edi_carrier_integration_service.util;

import com.innowise.edi_carrier_integration_service.exception.EdiSecurityException;
import com.innowise.edi_carrier_integration_service.exception.PayloadExtractionException;
import com.innowise.edi_carrier_integration_service.exception.PayloadTooLargeException;
import com.innowise.edi_carrier_integration_service.service.KeyManagementService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SMimeDecryptionUtil {

    private final KeyManagementService keyManagementService;

    @Value("${edi.pipeline.max-payload-bytes:20971520}")
    private long maxAllowedPayloadBytes;

    public MimeBodyPart decryptMimeBody(MimeBodyPart encryptedPart, String recipientAlias) {
        try {
            var enveloped = new SMIMEEnveloped(encryptedPart);
            var privateKey = keyManagementService.getPrivateKey(recipientAlias);
            var recipientCertificate = keyManagementService.getCertificate(recipientAlias);

            var recipients = enveloped.getRecipientInfos();
            var recipientId = new JceKeyTransRecipientId(recipientCertificate);
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

    public byte[] extractPayloadBytes(MimeBodyPart contentPart) {
        try (var inputStream = contentPart.getInputStream()) {
            var extractedBytes = inputStream.readNBytes((int) maxAllowedPayloadBytes + 1);
            if (extractedBytes.length > maxAllowedPayloadBytes) {
                throw new PayloadTooLargeException(
                        "Extracted payload size exceeds limit: " + maxAllowedPayloadBytes
                                + " bytes");
            }
            return extractedBytes;
        } catch (MessagingException | IOException e) {
            throw new PayloadExtractionException(e.getMessage());
        }
    }
}