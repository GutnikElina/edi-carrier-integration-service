package unit;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class TestSmimeUtils {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    gen.initialize(2048);
    return gen.generateKeyPair();
  }

  public static X509Certificate generateSelfSignedCert(KeyPair keyPair, String commonName)
      throws Exception {
    X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
    nameBuilder.addRDN(BCStyle.CN, commonName);
    X500Name issuer = nameBuilder.build();
    X500Name subject = issuer;
    BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
    Date notBefore = new Date(System.currentTimeMillis() - 86400000);
    Date notAfter = new Date(System.currentTimeMillis() + 864000000L);

    X509v3CertificateBuilder builder =
        new JcaX509v3CertificateBuilder(
            issuer, serial, notBefore, notAfter, subject, keyPair.getPublic());
    ContentSigner signer =
        new JcaContentSignerBuilder("SHA256withRSA")
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(keyPair.getPrivate());
    return new JcaX509CertificateConverter()
        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
        .getCertificate(builder.build(signer));
  }
}
