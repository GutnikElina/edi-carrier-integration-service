package com.innowise.edi_carrier_integration_service.service;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public interface KeyManagementService {
    PrivateKey getPrivateKey(String alias);

    X509Certificate getCertificate(String alias);

    X509Certificate getTrustCertificate(String alias);
}
