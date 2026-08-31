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
  private final String ID = "AS2-SERVER-001";

  /** Server hostname or IP. */
  private final String HOST = "localhost";

  /** Server port number (must be >= 1024). */
  private final int PORT = DEFAULT_PORT;

  /** URL path for inbound AS2 messages. */
  private final String PATH = "/as2/inbound";

  /** Default trading partner ID. */
  private final String PARTNER_ID = "TRADING-PARTNER-001";

  /** Default AS2 server port. */
  private static final int DEFAULT_PORT = 9090;
}
