package com.innowise.edi_carrier_integration_service.routes;

import com.innowise.edi_carrier_integration_service.config.As2Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/** Camel route for receiving AS2 messages. */
@Slf4j
@Component
@RequiredArgsConstructor
public final class As2InboundRoute extends RouteBuilder {

  /** URI pattern for AS2 server endpoint. */
  private static final String AS2_URI_PATTERN =
      "as2://server/listen?serverPortNumber=%d&requestUriPattern=%s";

  /** Log message for server start. */
  private static final String LOG_SERVER_START = "Starting AS2 server on: {}";

  /** Log message for incoming message. */
  private static final String LOG_RECEIVED = "Incoming AS2 message received";

  /** Log message for processing completion. */
  private static final String LOG_FINISHED = "AS2 message processing" + " finished successfully";

  /** Configuration properties for the AS2 server (host, port, path, etc.). */
  private final As2Configuration as2Config;

  /**
   * Processor that logs incoming AS2 messages. Currently, a stub; full MDN generation will be
   * implemented later.
   */
  private final Processor mdnProcessor;

  /** Configures the AS2 inbound route. * Sets up the endpoint and processing pipeline. */
  @Override
  public void configure() {
    final String serverUrl =
        new StringBuilder("http://")
            .append(as2Config.getHOST())
            .append(":")
            .append(as2Config.getPORT())
            .append(as2Config.getPATH())
            .toString();
    log.info(LOG_SERVER_START, serverUrl);
    final String as2Uri = String.format(AS2_URI_PATTERN, as2Config.getPORT(), as2Config.getPATH());

    from(as2Uri)
        .routeId("as2-inbound-route")
        .log(LOG_RECEIVED)
        .process(mdnProcessor)
        .log(LOG_FINISHED);
  }
}
