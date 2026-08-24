package com.innowise.edi_carrier_integration_service.edi.infrastructure.crypto;

import com.innowise.edi_carrier_integration_service.edi.domain.exception.EdiSecurityException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.bouncycastle.util.Store;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SMimeSecurityService {

  private final KeyManagementService keyManagementService;

  public byte[] decryptAndVerify(
      byte[] smimeMessageBytes, String recipientAlias, String senderAlias) {
    Objects.requireNonNull(smimeMessageBytes, "S/MIME payload byte array must not be null");
    if (smimeMessageBytes.length == 0) {
      throw new EdiSecurityException("S/MIME payload byte array is empty");
    }
    Objects.requireNonNull(recipientAlias, "Recipient KeyStore alias must not be null");
    Objects.requireNonNull(senderAlias, "Sender TrustStore alias must not be null");

    try (InputStream is = new ByteArrayInputStream(smimeMessageBytes)) {
      MimeBodyPart encryptedPart = new MimeBodyPart(is);
      MimeBodyPart decryptedPart = decrypt(encryptedPart, recipientAlias);

      try {
        return verifyAndExtractPayload(decryptedPart, senderAlias);
      } finally {
        cleanupMimePart(decryptedPart);
      }
    } catch (EdiSecurityException e) {
      throw e;
    } catch (Exception e) {
      throw new EdiSecurityException("S/MIME processing pipeline failed", e);
    }
  }

  private MimeBodyPart decrypt(MimeBodyPart encryptedPart, String recipientAlias) {
    try {
      SMIMEEnveloped enveloped = new SMIMEEnveloped(encryptedPart);
      PrivateKey privateKey = keyManagementService.getPrivateKey(recipientAlias);
      X509Certificate recipientCert = keyManagementService.getCertificate(recipientAlias);

      RecipientInformationStore recipients = enveloped.getRecipientInfos();
      RecipientId recipientId = new JceKeyTransRecipientId(recipientCert);
      RecipientInformation recipient = recipients.get(recipientId);

      if (recipient == null) {
        throw new EdiSecurityException(
            "No recipient matching certificate alias '"
                + recipientAlias
                + "' found in S/MIME EnvelopedData");
      }

      return SMIMEUtil.toMimeBodyPart(
          recipient.getContent(
              new JceKeyTransEnvelopedRecipient(privateKey)
                  .setProvider(BouncyCastleProvider.PROVIDER_NAME)));
    } catch (EdiSecurityException e) {
      throw e;
    } catch (Exception e) {
      throw new EdiSecurityException("S/MIME Decryption operation failed", e);
    }
  }

  private byte[] verifyAndExtractPayload(MimeBodyPart decryptedPart, String senderAlias) {
    try {
      if (!decryptedPart.isMimeType("multipart/signed")) {
        throw new EdiSecurityException(
            "Decrypted payload is not multipart/signed. Digital signature is missing");
      }

      MimeMultipart multipart = (MimeMultipart) decryptedPart.getContent();
      SMIMESigned signed = new SMIMESigned(multipart);
      SignerInformationStore signers = signed.getSignerInfos();
      Store<X509CertificateHolder> certs = signed.getCertificates();

      X509Certificate trustedAnchor = keyManagementService.getTrustCertificate(senderAlias);

      for (SignerInformation signer : signers.getSigners()) {
        var certCollection = certs.getMatches(signer.getSID());
        if (certCollection.isEmpty()) {
          throw new EdiSecurityException(
              "Signer certificate missing from S/MIME SignedData payload");
        }

        X509CertificateHolder certHolder = (X509CertificateHolder) certCollection.iterator().next();
        X509Certificate signerCert =
            new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certHolder);

        validateCertificateChain(signerCert, trustedAnchor, certs);

        boolean verified =
            signer.verify(
                new JcaSimpleSignerInfoVerifierBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(signerCert));

        if (!verified) {
          throw new EdiSecurityException(
              "Cryptographic signature verification failed for signer: " + signer.getSID());
        }
      }

      MimeBodyPart contentPart = (MimeBodyPart) multipart.getBodyPart(0);
      try (InputStream contentStream = contentPart.getInputStream()) {
        return contentStream.readAllBytes();
      }
    } catch (EdiSecurityException e) {
      throw e;
    } catch (Exception e) {
      throw new EdiSecurityException("S/MIME signature verification pipeline failed", e);
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

    JcaX509CertificateConverter converter =
        new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
    if (certStore != null) {
      for (Object holder : certStore.getMatches(null)) {
        if (holder instanceof X509CertificateHolder certHolder) {
          certList.add(converter.getCertificate(certHolder));
        }
      }
    }

    CertStore intermediateCertStore =
        CertStore.getInstance(
            "Collection",
            new CollectionCertStoreParameters(certList),
            BouncyCastleProvider.PROVIDER_NAME);

    X509CertSelector targetConstraints = new X509CertSelector();
    targetConstraints.setCertificate(signerCert);

    TrustAnchor anchor = new TrustAnchor(trustAnchorCert, null);
    PKIXBuilderParameters builderParams =
        new PKIXBuilderParameters(Collections.singleton(anchor), targetConstraints);
    builderParams.addCertStore(intermediateCertStore);
    builderParams.setRevocationEnabled(false);

    CertPathBuilder builder =
        CertPathBuilder.getInstance("PKIX", BouncyCastleProvider.PROVIDER_NAME);
    PKIXCertPathBuilderResult result = (PKIXCertPathBuilderResult) builder.build(builderParams);
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
