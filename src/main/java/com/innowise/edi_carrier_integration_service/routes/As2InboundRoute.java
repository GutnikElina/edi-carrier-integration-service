package com.innowise.edi_carrier_integration_service.routes;

import com.innowise.edi_carrier_integration_service.config.As2Configuration;
import com.innowise.edi_carrier_integration_service.processor.MdnStubProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class As2InboundRoute extends RouteBuilder {

    private static final String AS2_URI_PATTERN = "as2://server/listen?serverPortNumber=%d&requestUriPattern=%s";
    private static final String LOG_SERVER_START = "Starting AS2 server on: {}";
    private static final String LOG_RECEIVED = "Incoming AS2 message received";
    private static final String LOG_FINISHED = "AS2 message processing finished successfully";

    private final As2Configuration as2Config;
    private final MdnStubProcessor mdnStubProcessor;

    @Override
    public void configure() {
        String serverUrl = "http://" + as2Config.getHost() + ":" + as2Config.getPort() + as2Config.getPath();
        log.info(LOG_SERVER_START, serverUrl);

        String as2Uri = String.format(AS2_URI_PATTERN, as2Config.getPort(), as2Config.getPath());

        from(as2Uri)
                .routeId("as2-inbound-route")
                .log(LOG_RECEIVED)
                .process(mdnStubProcessor)
                .log(LOG_FINISHED);
    }
}