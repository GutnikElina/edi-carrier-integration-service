package com.innowise.edi_carrier_integration_service.service;

public interface MessageIntegrityService {
    String computeMessageIntegrityCheck(byte[] payload);
}
