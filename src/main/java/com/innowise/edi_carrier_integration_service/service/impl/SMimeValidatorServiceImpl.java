package com.innowise.edi_carrier_integration_service.service.impl;

import com.innowise.edi_carrier_integration_service.exception.EdiSecurityException;
import com.innowise.edi_carrier_integration_service.service.KeyManagementService;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;
import org.springframework.stereotype.Service;

import java.security.cert.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SMimeValidatorServiceImpl {

    private final KeyManagementService keyManagementService;

    public void verifyPayload(MimeBodyPart decryptedPart, String senderAlias,
            MimeMultipart multipart) {
        try {
            validateMimeType(decryptedPart);
            var sign = new SMIMESigned(multipart);
            var trustCertificate = keyManagementService.getTrustCertificate(senderAlias);

            sign.getSignerInfos()
                .getSigners()
                .forEach(signer -> verifySigner(signer, sign.getCertificates(), trustCertificate));

        } catch (EdiSecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new EdiSecurityException("S/MIME signature verification pipeline failed", e);
        }
    }

    private void validateMimeType(MimeBodyPart part) {
        Optional.ofNullable(part)
            .filter(bodyPart -> uncheck(() -> bodyPart.isMimeType("multipart/signed")))
            .orElseThrow(
                    () -> new EdiSecurityException("Decrypted payload is not multipart/signed." +
                            " Digital signature is missing"));
    }

    private void verifySigner(SignerInformation signerInfo,
            Store<X509CertificateHolder> certificates,
            X509Certificate trustCertificate) {
        try {

            var signerCertificate = generateSignerCertificate(certificates, signerInfo);

            validateCertificateChain(signerCertificate, trustCertificate, certificates);

            if (isVerified(signerInfo, signerCertificate)) {
                throw new EdiSecurityException(
                        "Cryptographic signature verification failed for signer: "
                                + signerInfo.getSID());
            }
        } catch (EdiSecurityException e) {
            log.error("Cryptographic signature verification failed for signer");
            throw e;
        } catch (Throwable e) {
            throw new EdiSecurityException("Failed to verify signer: " + signerInfo.getSID(), e);
        }
    }

    private X509Certificate generateSignerCertificate(Store<X509CertificateHolder> certs,
            SignerInformation signer) throws Throwable {
        var certificateHolder = certs.getMatches(signer.getSID())
            .stream()
            .findFirst()
            .orElseThrow(() -> new EdiSecurityException(
                    "Signer certificate missing from S/MIME SignedData payload"));

        return new JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate((X509CertificateHolder) certificateHolder);
    }

    private boolean isVerified(SignerInformation signerInfo, X509Certificate signerCertificate)
            throws OperatorCreationException, CMSException {
        return signerInfo.verify(
                new JcaSimpleSignerInfoVerifierBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(signerCertificate));
    }

    private void validateCertificateChain(X509Certificate signerCert,
            X509Certificate trustAnchorCert,
            Store<X509CertificateHolder> certStore) throws Exception {
        signerCert.checkValidity();
        if (signerCert.equals(trustAnchorCert)) {
            log.debug("Direct Trust verified: Signer certificate is identical to Trust Anchor");
            return;
        }

        try {
            signerCert.verify(trustAnchorCert.getPublicKey());
            log.debug(
                    "Direct Issuer verified: Signer certificate verified directly by Trust Anchor public key");
            return;
        } catch (Exception ignored) {
        }

        List<X509Certificate> certList = new ArrayList<>();
        certList.add(signerCert);

        var converter = new JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (certStore != null) {
            for (Object holder : certStore.getMatches(null)) {
                if (holder instanceof X509CertificateHolder certHolder) {
                    certList.add(converter.getCertificate(certHolder));
                }
            }
        }

        CertStore intermediateCertStore = CertStore.getInstance("Collection",
                new CollectionCertStoreParameters(certList),
                BouncyCastleProvider.PROVIDER_NAME);

        var targetConstraints = new X509CertSelector();
        targetConstraints.setCertificate(signerCert);

        var anchor = new TrustAnchor(trustAnchorCert, null);
        var builderParams = new PKIXBuilderParameters(Collections.singleton(anchor),
                targetConstraints);
        builderParams.addCertStore(intermediateCertStore);
        builderParams.setRevocationEnabled(false);

        var builder = CertPathBuilder.getInstance("PKIX", BouncyCastleProvider.PROVIDER_NAME);
        var result = (PKIXCertPathBuilderResult) builder.build(builderParams);
        log.debug("Validated intermediate certificate chain to anchor: {}",
                result.getTrustAnchor()
                    .getTrustedCert()
                    .getSubjectX500Principal());
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    private static <T> T uncheck(CheckedSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
