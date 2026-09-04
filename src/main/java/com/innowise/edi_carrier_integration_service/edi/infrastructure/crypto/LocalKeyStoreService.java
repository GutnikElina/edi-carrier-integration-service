package com.innowise.edi_carrier_integration_service.edi.infrastructure.crypto;

import com.innowise.edi_carrier_integration_service.edi.domain.exception.EdiSecurityException;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;
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
        loadStore(
                keyStorePath,
                keyStoreType,
                keyStorePassword,
                "KeyStore",
                store -> {
                    processAliases(
                            store,
                            alias -> {
                                if (store.isKeyEntry(alias)) {
                                    Optional
                                        .ofNullable(
                                                (PrivateKey) store.getKey(alias, keyStorePassword))
                                        .ifPresent(key -> privateKeyCache.put(alias, key));
                                }
                                Optional.ofNullable((X509Certificate) store.getCertificate(alias))
                                    .ifPresent(cert -> certificateCache.put(alias, cert));
                            });
                    log.info(
                            "KeyStore initialized successfully. Loaded {} private keys, {} certificates.",
                            privateKeyCache.size(),
                            certificateCache.size());
                });

        loadStore(
                trustStorePath,
                trustStoreType,
                trustStorePassword,
                "TrustStore",
                store -> {
                    processAliases(
                            store,
                            alias -> Optional
                                .ofNullable((X509Certificate) store.getCertificate(alias))
                                .ifPresent(cert -> trustCertificateCache.put(alias, cert)));
                    log.info(
                            "TrustStore initialized successfully. Loaded {} trusted certificates.",
                            trustCertificateCache.size());
                });
    }

    private void loadStore(
            String path, String type, char[] password, String storeName,
            KeyStoreConsumer consumer) {
        try (InputStream is = Files.newInputStream(Paths.get(path))) {
            KeyStore store = KeyStore.getInstance(type);
            store.load(is, password);
            consumer.accept(store);
        } catch (Exception e) {
            throw new EdiSecurityException("Failed to load " + storeName + " from path: " + path,
                    e);
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    private void processAliases(KeyStore store, AliasConsumer consumer) throws Exception {
        for (String alias : Collections.list(store.aliases())) {
            consumer.accept(alias);
        }
    }

    @FunctionalInterface
    private interface KeyStoreConsumer {
        void accept(KeyStore store) throws Exception;
    }

    @FunctionalInterface
    private interface AliasConsumer {
        void accept(String alias) throws Exception;
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
