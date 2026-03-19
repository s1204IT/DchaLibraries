package com.android.org.bouncycastle.jcajce.provider.asymmetric;

import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi;
import com.android.org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import com.android.org.bouncycastle.jcajce.provider.util.AsymmetricAlgorithmProvider;
import java.util.HashMap;
import java.util.Map;

public class RSA {
    private static final String PREFIX = "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.";
    private static final Map<String, String> generalRsaAttributes = new HashMap();

    static {
        generalRsaAttributes.put("SupportedKeyClasses", "javax.crypto.interfaces.RSAPublicKey|javax.crypto.interfaces.RSAPrivateKey");
        generalRsaAttributes.put("SupportedKeyFormats", "PKCS#8|X.509");
    }

    public static class Mappings extends AsymmetricAlgorithmProvider {
        @Override
        public void configure(ConfigurableProvider configurableProvider) {
            configurableProvider.addAlgorithm("AlgorithmParameters.OAEP", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.AlgorithmParametersSpi$OAEP");
            configurableProvider.addAlgorithm("AlgorithmParameters.PSS", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.AlgorithmParametersSpi$PSS");
            configurableProvider.addAttributes("Cipher.RSA", RSA.generalRsaAttributes);
            configurableProvider.addAlgorithm("Cipher.RSA", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.CipherSpi$NoPadding");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.RSA/RAW", "RSA");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.RSA//RAW", "RSA");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.RSA//NOPADDING", "RSA");
            configurableProvider.addAlgorithm("KeyFactory.RSA", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi");
            configurableProvider.addAlgorithm("KeyPairGenerator.RSA", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyPairGeneratorSpi");
            KeyFactorySpi keyFactorySpi = new KeyFactorySpi();
            registerOid(configurableProvider, PKCSObjectIdentifiers.rsaEncryption, "RSA", keyFactorySpi);
            registerOid(configurableProvider, X509ObjectIdentifiers.id_ea_rsa, "RSA", keyFactorySpi);
            registerOid(configurableProvider, PKCSObjectIdentifiers.id_RSAES_OAEP, "RSA", keyFactorySpi);
            if (configurableProvider.hasAlgorithm("MessageDigest", "MD5")) {
                addDigestSignature(configurableProvider, "MD5", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$MD5", PKCSObjectIdentifiers.md5WithRSAEncryption);
            }
            if (configurableProvider.hasAlgorithm("MessageDigest", "SHA1")) {
                addDigestSignature(configurableProvider, "SHA1", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA1", PKCSObjectIdentifiers.sha1WithRSAEncryption);
                configurableProvider.addAlgorithm("Alg.Alias.Signature." + OIWObjectIdentifiers.sha1WithRSA, "SHA1WITHRSA");
                configurableProvider.addAlgorithm("Alg.Alias.Signature.OID." + OIWObjectIdentifiers.sha1WithRSA, "SHA1WITHRSA");
            }
            addDigestSignature(configurableProvider, "SHA224", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA224", PKCSObjectIdentifiers.sha224WithRSAEncryption);
            addDigestSignature(configurableProvider, "SHA256", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA256", PKCSObjectIdentifiers.sha256WithRSAEncryption);
            addDigestSignature(configurableProvider, "SHA384", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA384", PKCSObjectIdentifiers.sha384WithRSAEncryption);
            addDigestSignature(configurableProvider, "SHA512", "com.android.org.bouncycastle.jcajce.provider.asymmetric.rsa.DigestSignatureSpi$SHA512", PKCSObjectIdentifiers.sha512WithRSAEncryption);
        }

        private void addDigestSignature(ConfigurableProvider configurableProvider, String str, String str2, ASN1ObjectIdentifier aSN1ObjectIdentifier) {
            String str3 = str + "WITHRSA";
            String str4 = str + "withRSA";
            String str5 = str + "WithRSA";
            String str6 = str + "/RSA";
            String str7 = str + "WITHRSAENCRYPTION";
            String str8 = str + "withRSAEncryption";
            configurableProvider.addAlgorithm("Signature." + str3, str2);
            configurableProvider.addAlgorithm("Alg.Alias.Signature." + str4, str3);
            configurableProvider.addAlgorithm("Alg.Alias.Signature." + str5, str3);
            configurableProvider.addAlgorithm("Alg.Alias.Signature." + str7, str3);
            configurableProvider.addAlgorithm("Alg.Alias.Signature." + str8, str3);
            configurableProvider.addAlgorithm("Alg.Alias.Signature." + (str + "WithRSAEncryption"), str3);
            configurableProvider.addAlgorithm("Alg.Alias.Signature." + str6, str3);
            if (aSN1ObjectIdentifier != null) {
                configurableProvider.addAlgorithm("Alg.Alias.Signature." + aSN1ObjectIdentifier, str3);
                configurableProvider.addAlgorithm("Alg.Alias.Signature.OID." + aSN1ObjectIdentifier, str3);
            }
        }

        private void addISO9796Signature(ConfigurableProvider configurableProvider, String str, String str2) {
            configurableProvider.addAlgorithm("Alg.Alias.Signature." + str + "withRSA/ISO9796-2", str + "WITHRSA/ISO9796-2");
            configurableProvider.addAlgorithm("Alg.Alias.Signature." + str + "WithRSA/ISO9796-2", str + "WITHRSA/ISO9796-2");
            configurableProvider.addAlgorithm("Signature." + str + "WITHRSA/ISO9796-2", str2);
        }

        private void addPSSSignature(ConfigurableProvider configurableProvider, String str, String str2) {
            configurableProvider.addAlgorithm("Alg.Alias.Signature." + str + "withRSA/PSS", str + "WITHRSAANDMGF1");
            configurableProvider.addAlgorithm("Alg.Alias.Signature." + str + "WithRSA/PSS", str + "WITHRSAANDMGF1");
            configurableProvider.addAlgorithm("Alg.Alias.Signature." + str + "withRSAandMGF1", str + "WITHRSAANDMGF1");
            configurableProvider.addAlgorithm("Alg.Alias.Signature." + str + "WithRSAAndMGF1", str + "WITHRSAANDMGF1");
            configurableProvider.addAlgorithm("Signature." + str + "WITHRSAANDMGF1", str2);
        }

        private void addX931Signature(ConfigurableProvider configurableProvider, String str, String str2) {
            configurableProvider.addAlgorithm("Alg.Alias.Signature." + str + "withRSA/X9.31", str + "WITHRSA/X9.31");
            configurableProvider.addAlgorithm("Alg.Alias.Signature." + str + "WithRSA/X9.31", str + "WITHRSA/X9.31");
            configurableProvider.addAlgorithm("Signature." + str + "WITHRSA/X9.31", str2);
        }
    }
}
