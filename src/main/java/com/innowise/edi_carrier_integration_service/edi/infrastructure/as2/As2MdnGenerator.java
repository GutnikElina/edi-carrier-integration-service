package com.innowise.edi_carrier_integration_service.edi.infrastructure.as2;

import com.innowise.edi_carrier_integration_service.edi.domain.exception.EdiProcessingException;
import java.security.MessageDigest;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

@Component
public class As2MdnGenerator {

    public String generateMdn(
            byte[] originalPayload, String originalMessageId, String senderAs2Id,
            String receiverAs2Id) {
        Objects.requireNonNull(originalPayload, "Original payload byte array must not be null");
        Objects.requireNonNull(originalMessageId, "Original Message-ID must not be null");
        Objects.requireNonNull(senderAs2Id, "Sender AS2 ID must not be null");
        Objects.requireNonNull(receiverAs2Id, "Receiver AS2 ID must not be null");

        try {
            String mic = calculateCanonicalSha256Mic(originalPayload);
            String mdnMessageId = "<" + UUID.randomUUID() + "@" + receiverAs2Id + ">";
            String timestamp = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now());
            String boundary = "----=_Part_MDN_" + UUID.randomUUID().toString().replace("-", "");

            String rawTemplate = """
                    AS2-Version: 1.2
                    From: %s
                    To: %s
                    Message-ID: %s
                    Date: %s
                    Content-Type: multipart/report; report-type=disposition-notification; boundary="%s"

                    --%s
                    Content-Type: text/plain; charset=us-ascii

                    The EDIFACT IFTMIN message sent to %s with Message ID %s has been received and processed successfully.

                    --%s
                    Content-Type: message/disposition-notification

                    Reporting-UA: Innowise-EDI-Carrier-Integration-Service
                    Original-Recipient: rfc822; %s
                    Final-Recipient: rfc822; %s
                    Original-Message-ID: %s
                    Disposition: automatic-action/MDN-sent-automatically; processed
                    Received-Content-MIC: %s, sha-256

                    --%s--
                    """
                .formatted(
                        receiverAs2Id,
                        senderAs2Id,
                        mdnMessageId,
                        timestamp,
                        boundary,
                        boundary,
                        receiverAs2Id,
                        originalMessageId,
                        boundary,
                        receiverAs2Id,
                        receiverAs2Id,
                        originalMessageId,
                        mic,
                        boundary);

            return rawTemplate.replace("\r\n", "\n").replace("\n", "\r\n");
        } catch (Exception e) {
            throw new EdiProcessingException(
                    "Failed to construct RFC 4130 AS2 MDN for Message-ID: " + originalMessageId, e);
        }
    }

    private String calculateCanonicalSha256Mic(byte[] payload) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256",
                BouncyCastleProvider.PROVIDER_NAME);
        int len = payload.length;
        for (int i = 0; i < len; i++) {
            byte b = payload[i];
            if (b == '\r') {
                digest.update((byte) '\r');
                digest.update((byte) '\n');
                if (i + 1 < len && payload[i + 1] == '\n') {
                    i++;
                }
            } else if (b == '\n') {
                digest.update((byte) '\r');
                digest.update((byte) '\n');
            } else {
                digest.update(b);
            }
        }
        byte[] hash = digest.digest();
        return Base64.getEncoder().encodeToString(hash);
    }
}
