package com.innowise.edi_carrier_integration_service.service;

public interface EdiArchiveService {
    void initBucket();
    String storeRawPayload(String objectName, byte[] payload, String contentType);
}
