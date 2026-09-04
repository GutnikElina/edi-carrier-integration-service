package com.innowise.edi_carrier_integration_service.edi.infrastructure.config;

import jakarta.annotation.PostConstruct;
import java.security.Security;
import java.util.Optional;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BouncyCastleConfig {

    @PostConstruct
    public void init() {
        Optional.ofNullable(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME))
            .ifPresentOrElse(provider -> {
            }, () -> Security.addProvider(new BouncyCastleProvider()));
    }
}
