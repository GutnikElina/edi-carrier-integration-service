package com.innowisetrainees.camel_as2_server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "as2.server")
public class As2Configuration {
    private String id = "AS2-SERVER-001";
    private String host = "localhost";
    private int port = 9090;
    private String path = "/as2/inbound";
    private String partnerId = "TRADING-PARTNER-001";
}