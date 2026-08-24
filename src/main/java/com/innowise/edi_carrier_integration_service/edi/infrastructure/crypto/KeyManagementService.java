package com.innowise.edi_carrier_integration_service.edi.infrastructure.crypto;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public interface KeyManagementService {
  PrivateKey getPrivateKey(String alias);

  X509Certificate getCertificate(String alias);

  X509Certificate getTrustCertificate(String alias);
}
