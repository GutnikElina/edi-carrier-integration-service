package com.innowise.edi_carrier_integration_service.service;

public interface SMimeSecurityService {
    byte[] decryptAndVerify(byte[] smimeMessageBytes, String recipientAlias, String senderAlias);
}
