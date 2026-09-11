package com.innowise.edi_carrier_integration_service.util;

import com.innowise.edi_carrier_integration_service.exception.CertificateValidationException;
import com.innowise.edi_carrier_integration_service.exception.EdiSecurityException;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Selector;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.StoreException;
import org.springframework.stereotype.Component;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class CertificateUtil {

    public X509Certificate generateSignerCertificate(Store<X509CertificateHolder> certificates,
            SignerInformation signer) {
        try {
            Selector<X509CertificateHolder> certificateSelector = signer.getSID();
            var certificateHolders = certificates.getMatches(certificateSelector);
            var certificateHolder = certificateHolders
                .stream()
                .findFirst()
                .orElseThrow(() -> new EdiSecurityException(
                        "Signer certificate missing from S/MIME SignedData payload"));
            return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certificateHolder);
        } catch (CertificateException | StoreException e) {
            throw new EdiSecurityException(
                    "Signer certificate missing from S/MIME SignedData payload");
        }
    }

    public void validateCertificateChain(X509Certificate signerCertificate,
            X509Certificate trustedCertificate,
            Store<X509CertificateHolder> certificatesStore) {
        try {
            signerCertificate.checkValidity();
            if (isValid(signerCertificate, trustedCertificate)) {
                log.debug("Direct Trust verified: Signer certificate is identical to Trust Anchor");
                return;
            }
            var certList = generateCertificateList(signerCertificate, certificatesStore);
            var buildParams = configureBuildParams(trustedCertificate, signerCertificate, certList);
            var certificatePathBuilder = CertPathBuilder.getInstance("PKIX",
                    BouncyCastleProvider.PROVIDER_NAME);
            var result = (PKIXCertPathBuilderResult) certificatePathBuilder.build(buildParams);
            log.debug("Validated intermediate certificate chain to anchor: {}",
                    result.getTrustAnchor()
                        .getTrustedCert()
                        .getSubjectX500Principal());
        } catch (Exception e) {
            throw new CertificateValidationException("Certificate validation failed");
        }
    }

    private boolean isValid(X509Certificate signerCertificate,
            X509Certificate trustedCertificate) {
        return signerCertificate.equals(trustedCertificate)
                || isVerifiableWithPublicKey(signerCertificate, trustedCertificate);
    }

    private boolean isVerifiableWithPublicKey(X509Certificate signerCertificate,
            X509Certificate trustedCertificate) {
        try {
            signerCertificate.verify(trustedCertificate.getPublicKey());
            log.debug("Direct Issuer verified:" +
                    " Signer certificate verified directly by Trust Anchor public key");
            return true;
        } catch (Exception ex) {
            log.info("Direct Issuer can't be verified directly by Trust Anchor public key");
        }
        return false;
    }

    private List<X509Certificate> generateCertificateList(X509Certificate signerCertificate,
            Store<X509CertificateHolder> certificatesStore) {
        List<X509Certificate> certList = new ArrayList<>();
        certList.add(signerCertificate);

        var converter = new JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME);

        if (certificatesStore != null) {
            var storeCerts = certificatesStore.getMatches(null)
                .stream()
                .map(holder -> {
                    try {
                        return converter.getCertificate(holder);
                    } catch (CertificateException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
            certList.addAll(storeCerts);
        }
        return certList;
    }

    private CertPathParameters configureBuildParams(X509Certificate trustedCertificate,
            X509Certificate signerCertificate,
            List<X509Certificate> certList) {
        try {
            CertStore intermediateCertStore = CertStore.getInstance("Collection",
                    new CollectionCertStoreParameters(certList),
                    BouncyCastleProvider.PROVIDER_NAME);

            var anchor = new TrustAnchor(trustedCertificate, null);
            var targetConstraints = new X509CertSelector();
            targetConstraints.setCertificate(signerCertificate);

            var builderParams = new PKIXBuilderParameters(Collections.singleton(anchor),
                    targetConstraints);
            builderParams.addCertStore(intermediateCertStore);
            builderParams.setRevocationEnabled(false);
            return builderParams;

        } catch (NoSuchProviderException | InvalidAlgorithmParameterException
                | NoSuchAlgorithmException e) {
            throw new CertificateValidationException("Certificate validation failed");
        }

    }
}