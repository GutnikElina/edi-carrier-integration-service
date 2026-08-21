package com.innowise.edi_carrier_integration_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(proxyBeanMethods = false)
public final class EdiCarrierIntegrationServiceApplication {
  /**
   * Main method to start the Spring Boot application.
   *
   * @param args command line arguments
   */
  public static void main(final String[] args) {
    Class<?> appClass = EdiCarrierIntegrationServiceApplication.class;
    SpringApplication.run(appClass, args);
  }

  private EdiCarrierIntegrationServiceApplication() {
    super();
  }
}
