package com.innowise.edi_carrier_integration_service.service;

public interface SMimeDecryptionService {
    byte[] decrypt(byte[] smimeMessageBytes, String recipientAlias, String senderAlias);
}
