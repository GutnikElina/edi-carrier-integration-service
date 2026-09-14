package com.innowise.edi_carrier_integration_service.util;

import com.innowise.edi_carrier_integration_service.exception.EdiSecurityException;
import com.innowise.edi_carrier_integration_service.service.KeyManagementService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;
import org.springframework.stereotype.Component;

import java.security.cert.X509Certificate;

@Component
@RequiredArgsConstructor
public class SMimeValidatorUtil {

    private final KeyManagementService keyManagementService;
    private final CertificateValidationUtil certificateValidationUtil;

    public void validateMimeType(MimeBodyPart decryptedPart) {
        try {
            if (!isValidMimeType(decryptedPart)) {
                throw new EdiSecurityException("Decrypted payload is not multipart/signed." +
                        " Digital signature is missing");
            }
        } catch (MessagingException e) {
            throw new EdiSecurityException("Failed to read MIME type", e);
        }
    }

    public void verifySignatures(String senderAlias, MimeMultipart multipart) {
        try {
            var signature = new SMIMESigned(multipart);
            var trustCertificate = keyManagementService.getTrustCertificate(senderAlias);

            signature.getSignerInfos()
                .getSigners()
                .forEach(signer -> verifySigner(signer, signature.getCertificates(),
                        trustCertificate));

        } catch (EdiSecurityException | MessagingException | CMSException e) {
            throw new EdiSecurityException("S/MIME signature verification pipeline failed", e);
        }
    }

    private boolean isValidMimeType(MimeBodyPart part) throws MessagingException {
        return part != null && part.isMimeType("multipart/signed");
    }

    private void verifySigner(SignerInformation signerInfo,
            Store<X509CertificateHolder> certificates,
            X509Certificate trustedCertificate) {

        var signerCertificate = certificateValidationUtil.generateSignerCertificate(certificates,
                signerInfo);

        certificateValidationUtil.validateCertificateChain(signerCertificate, trustedCertificate,
                certificates);

        if (!isVerified(signerInfo, signerCertificate)) {
            throw new EdiSecurityException(
                    "Cryptographic signature verification failed for signer: "
                            + signerInfo.getSID());
        }
    }

    private boolean isVerified(SignerInformation signerInfo,
            X509Certificate signerCertificate) {

        try {
            return signerInfo.verify(
                    new JcaSimpleSignerInfoVerifierBuilder()
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build(signerCertificate));
        } catch (OperatorCreationException | CMSException e) {
            throw new EdiSecurityException(
                    "Cryptographic signature verification failed for signer: "
                            + signerInfo.getSID());
        }
    }
}