package com.innowise.edi_carrier_integration_service.service.impl;

import com.innowise.edi_carrier_integration_service.dto.InboundEdiResult;
import com.innowise.edi_carrier_integration_service.exception.PayloadTooLargeException;
import com.innowise.edi_carrier_integration_service.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.innowise.edi_carrier_integration_service.util.EdiCarrierIntegrationConstants.RECIPIENT_ALIAS;
import static com.innowise.edi_carrier_integration_service.util.EdiCarrierIntegrationConstants.SENDER_ALIAS;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboundEdiPipelineServiceImpl implements InboundEdiPipelineService {

    @Value("${edi.pipeline.max-payload-bytes:20971520}")
    private long maxAllowedPayloadBytes;

    private final SMimeDecryptionService sMimeDecryptionService;
    private final EdiParserService ediParserService;
    private final EdiArchiveService ediArchiveService;
    private final As2MdnGeneratorService as2MdnGeneratorService;

    @Async
    @Override
    public void processInboundSmimeMessage(Message message) {
        String messageId = message.getHeader("Message-ID", String.class);
        String as2From = message.getHeader("AS2-From", String.class);
        String as2To = message.getHeader("AS2-To", String.class);
        String asyncMdnUrl = message.getHeader("Receipt-Delivery-Option", String.class);
        byte[] rawPayload = message.getBody(byte[].class);
        runPipeline(rawPayload, messageId, RECIPIENT_ALIAS, SENDER_ALIAS,
                as2From, as2To, asyncMdnUrl); // TODO change alias to actual and use compleatable
                                              // future
    }

    private CompletableFuture<InboundEdiResult> runPipeline(byte[] rawSmimeBytes,
            String originalMessageId,
            String recipientAlias,
            String senderAlias,
            String senderAs2Id,
            String receiverAs2Id,
            String asyncMdnUrl) {

        validateInputData(rawSmimeBytes, originalMessageId, recipientAlias, senderAlias,
                senderAs2Id, receiverAs2Id);

        String objectKey = "raw/" + UUID.randomUUID() + ".smime";
        String s3ObjectKey = ediArchiveService.saveRawPayload(objectKey, rawSmimeBytes,
                "application/pkcs7-mime");

        byte[] decryptedEdifactPayload = sMimeDecryptionService.decrypt(rawSmimeBytes,
                recipientAlias, senderAlias);

        String mdn = as2MdnGeneratorService.generateMdn(decryptedEdifactPayload, originalMessageId,
                senderAs2Id, receiverAs2Id);

        if (asyncMdnUrl != null && !asyncMdnUrl.isEmpty()) {
            sendAsyncMdn(asyncMdnUrl, mdn);
        }

        var instructionDto = ediParserService.parseIftmin(decryptedEdifactPayload);
        log.info("Pipeline decrypted and parsed IFTMIN document. ControlNumber: {}",
                instructionDto.controlNumber());

        return CompletableFuture
            .completedFuture(new InboundEdiResult(instructionDto, mdn, s3ObjectKey));
    }

    private void sendAsyncMdn(String targetUrl, String mdnPayload) {
        // TODO async MDN sending to url
        log.info("Sending Async MDN to URL: {}", targetUrl);
    }

    private void validateInputData(byte[] rawSmimeBytes,
            String originalMessageId,
            String recipientAlias,
            String senderAlias,
            String senderAs2Id,
            String receiverAs2Id) {

        Objects.requireNonNull(rawSmimeBytes, "Raw S/MIME bytes array must not be null");
        Objects.requireNonNull(originalMessageId, "Original Message-ID must not be null");
        Objects.requireNonNull(recipientAlias, "Recipient KeyStore alias must not be null");
        Objects.requireNonNull(senderAlias, "Sender TrustStore alias must not be null");
        Objects.requireNonNull(senderAs2Id, "Sender AS2 ID must not be null");
        Objects.requireNonNull(receiverAs2Id, "Receiver AS2 ID must not be null");

        if (rawSmimeBytes.length > maxAllowedPayloadBytes) {
            throw new PayloadTooLargeException(
                    "Payload size "
                            + rawSmimeBytes.length
                            + " bytes exceeds maximum allowed limit of "
                            + maxAllowedPayloadBytes
                            + " bytes");
        }
    }

}
