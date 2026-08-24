package com.innowise.edi_carrier_integration_service.edi.infrastructure.config;

import com.innowise.edi_carrier_integration_service.edi.domain.exception.EdiParseException;
import java.io.IOException;
import java.io.InputStream;
import org.smooks.Smooks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xml.sax.SAXException;

@Configuration
public class SmooksConfig {

  private static final String SMOOKS_CONFIG_PATH = "smooks/smooks-iftmin-config.xml";

  @Bean(destroyMethod = "close")
  public Smooks smooksIftminEngine() {
    try (InputStream configStream =
        getClass().getClassLoader().getResourceAsStream(SMOOKS_CONFIG_PATH)) {
      if (configStream == null) {
        throw new EdiParseException(
            "Smooks configuration resource file not found at path: " + SMOOKS_CONFIG_PATH);
      }
      return new Smooks(configStream);
    } catch (IOException | SAXException e) {
      throw new EdiParseException(
          "Failed to construct Singleton Smooks engine instance from: " + SMOOKS_CONFIG_PATH, e);
    }
  }
}
