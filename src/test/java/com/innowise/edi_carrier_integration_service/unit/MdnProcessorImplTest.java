package com.innowise.edi_carrier_integration_service.unit;

import com.innowise.edi_carrier_integration_service.service.InboundEdiPipelineService;
import com.innowise.edi_carrier_integration_service.service.impl.MdnProcessorImpl;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MdnProcessorImplTest {

    @Mock
    private InboundEdiPipelineService pipelineService;

    @InjectMocks
    private MdnProcessorImpl processor;

    @Test
    @DisplayName("process: triggers pipeline")
    void process_triggersPipeline() {
        Exchange exchange = mock(Exchange.class);
        Message message = mock(Message.class);
        when(exchange.getIn()).thenReturn(message);

        processor.process(exchange);

        verify(pipelineService).processInboundSmimeMessage(message);
    }
}
