package com.innowise.edi_carrier_integration_service.edi.application;

import com.innowise.edi_carrier_integration_service.edi.domain.exception.PayloadTooLargeException;
import com.innowise.edi_carrier_integration_service.edi.domain.model.IftminInstructionDto;
import com.innowise.edi_carrier_integration_service.edi.infrastructure.archive.EdiArchiveService;
import com.innowise.edi_carrier_integration_service.edi.infrastructure.as2.As2MdnGenerator;
import com.innowise.edi_carrier_integration_service.edi.infrastructure.crypto.SMimeSecurityService;
import com.innowise.edi_carrier_integration_service.edi.infrastructure.parser.EdiParserService;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboundEdiPipeline {

    @Value("${edi.pipeline.max-payload-bytes:20971520}")
    private long maxAllowedPayloadBytes;

    private final SMimeSecurityService sMimeSecurityService;
    private final EdiParserService ediParserService;
    private final EdiArchiveService ediArchiveService;
    private final As2MdnGenerator as2MdnGenerator;

    public record InboundEdiResult(
            IftminInstructionDto payloadDto, String mdnContent, String s3RawObjectPath) {
    }

    @Async("ediAsyncTaskExecutor")
    public CompletableFuture<InboundEdiResult> processInboundSmimeMessage(
            byte[] rawSmimeBytes,
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

        String objectKey = "raw/" + UUID.randomUUID() + ".smime";
        log.info(
                "Starting pipeline execution for MessageID: {}. Assigned S3 Key: {}",
                originalMessageId,
                objectKey);

        String s3Path = ediArchiveService.storeRawPayload(objectKey, rawSmimeBytes,
                "application/pkcs7-mime");

        byte[] decryptedEdifactPayload = sMimeSecurityService.decryptAndVerify(rawSmimeBytes,
                recipientAlias, senderAlias);

        IftminInstructionDto instructionDto = ediParserService.parseIftmin(decryptedEdifactPayload);
        log.info(
                "Pipeline decrypted and parsed IFTMIN document. ControlNumber: {}, MessageID: {}",
                instructionDto.getControlNumber(),
                originalMessageId);

        String mdn = as2MdnGenerator.generateMdn(
                decryptedEdifactPayload, originalMessageId, senderAs2Id, receiverAs2Id);

        return CompletableFuture.completedFuture(new InboundEdiResult(instructionDto, mdn, s3Path));
    }
}
