package com.innowise.edi_carrier_integration_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuration properties for AS2 server settings. */
@Data
@Component
@ConfigurationProperties(prefix = "as2.server")
public final class As2Configuration {

  /** Default AS2 server identifier. */
  private String id = "AS2-SERVER-001";

  /** Server hostname or IP. */
  private String host = "localhost";

  /** Server port number (must be >= 1024). */
  private int port = DEFAULT_PORT;

  /** URL path for inbound AS2 messages. */
  private String path = "/as2/inbound";

  /** Default trading partner ID. */
  private String partnerId = "TRADING-PARTNER-001";

  /** Default AS2 server port. */
  private static final int DEFAULT_PORT = 9090;
}
