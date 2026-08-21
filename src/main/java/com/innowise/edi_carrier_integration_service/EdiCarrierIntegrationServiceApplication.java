package com.innowise.edi_carrier_integration_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EdiCarrierIntegrationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EdiCarrierIntegrationServiceApplication.class, args);
	}

}
