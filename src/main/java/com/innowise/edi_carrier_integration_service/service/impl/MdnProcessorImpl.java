package com.innowise.edi_carrier_integration_service.service.impl;

import com.innowise.edi_carrier_integration_service.service.InboundEdiPipelineService;
import lombok.RequiredArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MdnProcessorImpl implements Processor {

    private final InboundEdiPipelineService inboundEdiPipelineService;

    @Override
    public void process(Exchange exchange) {
        inboundEdiPipelineService.processInboundSmimeMessage(exchange.getIn());
    }

}
