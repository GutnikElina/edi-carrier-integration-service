package com.innowise.edi_carrier_integration_service.service;

public interface EdiArchiveService {
    void initBucket();
    String saveRawPayload(String objectKey, byte[] payload, String contentType);
}
