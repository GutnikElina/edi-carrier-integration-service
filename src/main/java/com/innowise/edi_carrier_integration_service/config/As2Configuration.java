package com.innowise.edi_carrier_integration_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "as2.server")
public class As2Configuration {
    private String id = "AS2-SERVER-001";
    private String host = "localhost";
    private int port = 9090;
    private String path = "/as2/inbound";
    private String partnerId = "TRADING-PARTNER-001";
}