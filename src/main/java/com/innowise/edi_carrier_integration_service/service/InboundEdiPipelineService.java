package com.innowise.edi_carrier_integration_service.service;

import com.innowise.edi_carrier_integration_service.dto.InboundEdiResult;

import java.util.concurrent.CompletableFuture;

public interface InboundEdiPipelineService {
    CompletableFuture<InboundEdiResult> processInboundSmimeMessage(
            byte[] rawSmimeBytes,
            String originalMessageId,
            String recipientAlias,
            String senderAlias,
            String senderAs2Id,
            String receiverAs2Id);
}
