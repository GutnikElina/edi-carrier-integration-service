package com.innowise.edi_carrier_integration_service.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

/**
 * Processor that logs the incoming AS2 message body. Currently, a stub; full MDN generation will be
 * implemented later.
 */
@Slf4j
@Component
public final class MdnStubProcessor implements Processor {

  /**
   * Processes the incoming exchange by logging the message body.
   *
   * @param exchange the Camel exchange containing the message
   */
  @Override
  public void process(final Exchange exchange) {
    final String body = exchange.getMessage().getBody(String.class);

    log.info("AS2 message received");
    log.info("EDI body:\n{}", body);
  }
}
