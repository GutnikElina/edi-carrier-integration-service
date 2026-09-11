package com.innowise.edi_carrier_integration_service.service;

import org.apache.camel.Message;

public interface InboundEdiPipelineService {

    void processInboundSmimeMessage(Message message);
}
