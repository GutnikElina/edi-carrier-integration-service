package com.innowise.edi_carrier_integration_service.service;

public interface As2MdnGeneratorService {
    String generateMdn(byte[] originalPayload, String originalMessageId,
            String senderAs2Id, String receiverAs2Id);
}
