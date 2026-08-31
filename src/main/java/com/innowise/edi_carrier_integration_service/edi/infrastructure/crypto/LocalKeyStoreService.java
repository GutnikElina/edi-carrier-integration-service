package com.innowise.edi_carrier_integration_service.edi.infrastructure.crypto;

import com.innowise.edi_carrier_integration_service.edi.domain.exception.EdiSecurityException;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LocalKeyStoreService implements KeyManagementService {

    @Value("${edi.security.keystore.path}")
    private String keyStorePath;

    @Value("${edi.security.keystore.password}")
    private char[] keyStorePassword;

    @Value("${edi.security.keystore.type:PKCS12}")
    private String keyStoreType;

    @Value("${edi.security.truststore.path}")
    private String trustStorePath;

    @Value("${edi.security.truststore.password}")
    private char[] trustStorePassword;

    @Value("${edi.security.truststore.type:PKCS12}")
    private String trustStoreType;

    private final Map<String, PrivateKey> privateKeyCache = new ConcurrentHashMap<>();
    private final Map<String, X509Certificate> certificateCache = new ConcurrentHashMap<>();
    private final Map<String, X509Certificate> trustCertificateCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try (InputStream ksStream = Files.newInputStream(Paths.get(keyStorePath))) {
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(ksStream, keyStorePassword);

            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isKeyEntry(alias)) {
                    PrivateKey key = (PrivateKey) keyStore.getKey(alias, keyStorePassword);
                    if (key != null) {
                        privateKeyCache.put(alias, key);
                    }
                }
                X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                if (cert != null) {
                    certificateCache.put(alias, cert);
                }
            }
            log.info(
                    "KeyStore initialized successfully. Loaded {} private keys, {} certificates.",
                    privateKeyCache.size(),
                    certificateCache.size());
        } catch (Exception e) {
            throw new EdiSecurityException("Failed to load KeyStore from path: " + keyStorePath, e);
        } finally {
            if (keyStorePassword != null) {
                Arrays.fill(keyStorePassword, '\0');
            }
        }

        try (InputStream tsStream = Files.newInputStream(Paths.get(trustStorePath))) {
            KeyStore trustStore = KeyStore.getInstance(trustStoreType);
            trustStore.load(tsStream, trustStorePassword);

            Enumeration<String> aliases = trustStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                X509Certificate cert = (X509Certificate) trustStore.getCertificate(alias);
                if (cert != null) {
                    trustCertificateCache.put(alias, cert);
                }
            }
            log.info(
                    "TrustStore initialized successfully. Loaded {} trusted certificates.",
                    trustCertificateCache.size());
        } catch (Exception e) {
            throw new EdiSecurityException("Failed to load TrustStore from path: " + trustStorePath,
                    e);
        } finally {
            if (trustStorePassword != null) {
                Arrays.fill(trustStorePassword, '\0');
            }
        }
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        Objects.requireNonNull(alias, "KeyStore alias must not be null");
        PrivateKey key = privateKeyCache.get(alias);
        if (key == null) {
            throw new EdiSecurityException(
                    "PrivateKey alias not found in KeyStore cache: " + alias);
        }
        return key;
    }

    @Override
    public X509Certificate getCertificate(String alias) {
        Objects.requireNonNull(alias, "Certificate alias must not be null");
        X509Certificate cert = certificateCache.get(alias);
        if (cert == null) {
            throw new EdiSecurityException(
                    "Certificate not found in KeyStore cache for alias: " + alias);
        }
        return cert;
    }

    @Override
    public X509Certificate getTrustCertificate(String alias) {
        Objects.requireNonNull(alias, "TrustStore alias must not be null");
        X509Certificate cert = trustCertificateCache.get(alias);
        if (cert == null) {
            throw new EdiSecurityException(
                    "Certificate not found in TrustStore cache for alias: " + alias);
        }
        return cert;
    }
}
