package com.innowisetrainees.camel_as2_server.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MdnStubProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);

        log.info("AS2 message received");
        log.info("EDI body:\n{}", body);
    }
}