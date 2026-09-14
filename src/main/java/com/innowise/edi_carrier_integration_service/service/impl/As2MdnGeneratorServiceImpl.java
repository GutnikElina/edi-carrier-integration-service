package com.innowise.edi_carrier_integration_service.service.impl;

import com.innowise.edi_carrier_integration_service.service.As2MdnGeneratorService;
import com.innowise.edi_carrier_integration_service.service.MessageIntegrityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

import static com.innowise.edi_carrier_integration_service.util.EdiCarrierIntegrationConstants.MDN_TEMPLATE;

@Service
@RequiredArgsConstructor
public class As2MdnGeneratorServiceImpl implements As2MdnGeneratorService {

    private final MessageIntegrityService messageIntegrityService;

    private String messageIntegrityCheck;
    private String mdnMessageId;
    private String timestamp;
    private String boundary;

    @Override
    public String generateMdn(byte[] originalPayload, String originalMessageId,
            String senderAs2Id, String receiverAs2Id) {
        validateInputData(originalPayload, originalMessageId, senderAs2Id, receiverAs2Id);
        initializeMdnData(originalPayload, receiverAs2Id);
        return generateRawMdn(originalMessageId, senderAs2Id, receiverAs2Id)
            .replace("\r\n", "\n")
            .replace("\n", "\r\n");
    }

    private void validateInputData(byte[] originalPayload, String originalMessageId,
            String senderAs2Id, String receiverAs2Id) {
        Objects.requireNonNull(originalPayload, "Original payload byte array must not be null");
        Objects.requireNonNull(originalMessageId, "Original Message-ID must not be null");
        Objects.requireNonNull(senderAs2Id, "Sender AS2 ID must not be null");
        Objects.requireNonNull(receiverAs2Id, "Receiver AS2 ID must not be null");
    }

    private void initializeMdnData(byte[] originalPayload, String receiverAs2Id) {
        messageIntegrityCheck = messageIntegrityService
            .computeMessageIntegrityCheck(originalPayload);
        mdnMessageId = "<" + UUID.randomUUID() + "@" + receiverAs2Id + ">";
        timestamp = DateTimeFormatter.RFC_1123_DATE_TIME
            .format(ZonedDateTime.now(Clock.systemDefaultZone()));
        boundary = "----=_Part_MDN_" + UUID.randomUUID()
            .toString()
            .replace("-", "");
    }

    private String generateRawMdn(String originalMessageId, String senderAs2Id,
            String receiverAs2Id) {
        return MDN_TEMPLATE.formatted(receiverAs2Id, senderAs2Id, mdnMessageId, timestamp,
                boundary, boundary, receiverAs2Id, originalMessageId, boundary, receiverAs2Id,
                receiverAs2Id, originalMessageId, messageIntegrityCheck, boundary);
    }

}