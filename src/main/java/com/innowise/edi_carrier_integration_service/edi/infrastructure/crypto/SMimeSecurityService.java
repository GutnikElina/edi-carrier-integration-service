package com.innowise.edi_carrier_integration_service.edi.infrastructure.crypto;

import com.innowise.edi_carrier_integration_service.edi.domain.exception.EdiSecurityException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.bouncycastle.util.Store;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SMimeSecurityService {

    private final KeyManagementService keyManagementService;

    @Value("${edi.pipeline.max-payload-bytes:20971520}")
    private long maxAllowedPayloadBytes;

    public byte[] decryptAndVerify(
            byte[] smimeMessageBytes, String recipientAlias, String senderAlias) {
        Objects.requireNonNull(smimeMessageBytes, "S/MIME payload byte array must not be null");
        if (smimeMessageBytes.length == 0) {
            throw new EdiSecurityException("S/MIME payload byte array is empty");
        }
        Objects.requireNonNull(recipientAlias, "Recipient KeyStore alias must not be null");
        Objects.requireNonNull(senderAlias, "Sender TrustStore alias must not be null");

        try (InputStream is = new ByteArrayInputStream(smimeMessageBytes)) {
            var encryptedPart = new MimeBodyPart(is);
            var decryptedPart = decrypt(encryptedPart, recipientAlias);

            try {
                return verifyAndExtractPayload(decryptedPart, senderAlias);
            } finally {
                cleanupMimePart(decryptedPart);
            }
        } catch (EdiSecurityException e) {
            log.error("S/MIME processing pipeline failed");
            throw e;
        } catch (Exception e) {
            throw new EdiSecurityException("S/MIME processing pipeline failed", e);
        }
    }

    private MimeBodyPart decrypt(MimeBodyPart encryptedPart, String recipientAlias) {
        try {
            var enveloped = new SMIMEEnveloped(encryptedPart);
            var privateKey = keyManagementService.getPrivateKey(recipientAlias);
            var recipientCert = keyManagementService.getCertificate(recipientAlias);

            var recipients = enveloped.getRecipientInfos();
            var recipientId = new JceKeyTransRecipientId(recipientCert);
            var recipient = recipients.get(recipientId);

            Optional.ofNullable(recipient)
                .orElseThrow(
                        () -> (new EdiSecurityException(
                                "No recipient matching certificate alias '"
                                        + recipientAlias
                                        + "' found in S/MIME EnvelopedData")));

            return SMIMEUtil.toMimeBodyPart(
                    recipient.getContent(
                            new JceKeyTransEnvelopedRecipient(privateKey)
                                .setProvider(BouncyCastleProvider.PROVIDER_NAME)));
        } catch (EdiSecurityException e) {
            log.error("S/MIME Decryption operation failed");
            throw e;
        } catch (Exception e) {
            throw new EdiSecurityException("S/MIME Decryption operation failed", e);
        }
    }

    private byte[] verifyAndExtractPayload(MimeBodyPart decryptedPart, String senderAlias) {
        try {
            validateMimeType(decryptedPart);

            var multipart = (MimeMultipart) decryptedPart.getContent();
            var signed = new SMIMESigned(multipart);
            var trustedAnchor = keyManagementService.getTrustCertificate(senderAlias);

            signed.getSignerInfos().getSigners().stream()
                .forEach(signer -> verifySigner(signer, signed.getCertificates(), trustedAnchor));

            return extractPayloadBytes((MimeBodyPart) multipart.getBodyPart(0));

        } catch (EdiSecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new EdiSecurityException("S/MIME signature verification pipeline failed", e);
        }
    }

    private void validateMimeType(MimeBodyPart part) {
        Optional.ofNullable(part)
            .filter(p -> uncheck(() -> p.isMimeType("multipart/signed")))
            .orElseThrow(
                    () -> new EdiSecurityException(
                            "Decrypted payload is not multipart/signed. Digital signature is missing"));
    }

    private void verifySigner(
            SignerInformation signer, Store<X509CertificateHolder> certs,
            X509Certificate trustedAnchor) {
        try {
            var certHolder = certs.getMatches(signer.getSID()).stream()
                .findFirst()
                .orElseThrow(
                        () -> new EdiSecurityException(
                                "Signer certificate missing from S/MIME SignedData payload"));

            var signerCert = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate((X509CertificateHolder) certHolder);

            validateCertificateChain(signerCert, trustedAnchor, certs);

            boolean verified = signer.verify(
                    new JcaSimpleSignerInfoVerifierBuilder()
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build(signerCert));

            if (!verified) {
                throw new EdiSecurityException(
                        "Cryptographic signature verification failed for signer: "
                                + signer.getSID());
            }
        } catch (EdiSecurityException e) {
            log.error("Cryptographic signature verification failed for signer");
            throw e;
        } catch (Throwable e) {
            throw new EdiSecurityException("Failed to verify signer: " + signer.getSID(), e);
        }
    }

    private byte[] extractPayloadBytes(MimeBodyPart contentPart) throws Exception {
        try (InputStream is = contentPart.getInputStream()) {
            byte[] bytes = is.readNBytes((int) maxAllowedPayloadBytes + 1);

            if (bytes.length > maxAllowedPayloadBytes) {
                throw new EdiSecurityException(
                        "Extracted payload size exceeds limit: " + maxAllowedPayloadBytes
                                + " bytes");
            }
            return bytes;
        }
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

    private void validateCertificateChain(
            X509Certificate signerCert,
            X509Certificate trustAnchorCert,
            Store<X509CertificateHolder> certStore)
            throws Exception {
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

        CertStore intermediateCertStore = CertStore.getInstance(
                "Collection",
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
        log.debug(
                "Validated intermediate certificate chain to anchor: {}",
                result.getTrustAnchor().getTrustedCert().getSubjectDN());
    }

    private void cleanupMimePart(MimeBodyPart part) {
        if (part != null) {
            try {
                Object content = part.getContent();
                if (content instanceof InputStream is) {
                    is.close();
                }
            } catch (Exception e) {
                log.debug("Resource cleanup failed for MimeBodyPart", e);
            }
        }
    }
}
