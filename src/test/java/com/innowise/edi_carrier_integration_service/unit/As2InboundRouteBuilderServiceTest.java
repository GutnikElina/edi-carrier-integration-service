package com.innowise.edi_carrier_integration_service.unit;

import com.innowise.edi_carrier_integration_service.config.As2Configuration;
import com.innowise.edi_carrier_integration_service.service.impl.As2InboundRouteBuilderService;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class As2InboundRouteBuilderServiceTest extends CamelTestSupport {

    @Mock
    private As2Configuration config;
    @Mock
    private org.apache.camel.Processor mdnProcessor;

    @Override
    protected RouteBuilder createRouteBuilder() {
        config = mock(As2Configuration.class);
        when(config.getHOST()).thenReturn("localhost");
        when(config.getPORT()).thenReturn(8080);
        when(config.getPATH()).thenReturn("/as2/inbound");

        mdnProcessor = mock(org.apache.camel.Processor.class);

        return new As2InboundRouteBuilderService(config, mdnProcessor);
    }

    @Test
    @DisplayName("Route configures without errors")
    void testRouteConfig() {
        assertThat(context.getRoute("as2-inbound-route")).isNotNull();
    }
}
