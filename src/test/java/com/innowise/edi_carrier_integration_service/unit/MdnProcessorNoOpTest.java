package com.innowise.edi_carrier_integration_service.unit;

import com.innowise.edi_carrier_integration_service.service.impl.MdnProcessorNoOp;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MdnProcessorNoOpTest {

    private final MdnProcessorNoOp processor = new MdnProcessorNoOp();

    @Test
    @DisplayName("process: simply logs and doesn't throw")
    void process_success() {
        Exchange exchange = mock(Exchange.class);
        Message message = mock(Message.class);
        when(exchange.getMessage()).thenReturn(message);
        when(message.getBody(String.class)).thenReturn("dummy-edi");

        assertThatCode(() -> processor.process(exchange))
            .doesNotThrowAnyException();
    }
}