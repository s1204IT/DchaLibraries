package com.android.org.bouncycastle.jce.provider;

import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import com.android.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import com.android.org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import com.android.org.bouncycastle.jcajce.provider.config.ProviderConfiguration;
import com.android.org.bouncycastle.jcajce.provider.util.AlgorithmProvider;
import com.android.org.bouncycastle.jcajce.provider.util.AsymmetricKeyInfoConverter;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivateKey;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public final class BouncyCastleProvider extends Provider implements ConfigurableProvider {
    private static final String ASYMMETRIC_PACKAGE = "com.android.org.bouncycastle.jcajce.provider.asymmetric.";
    private static final String DIGEST_PACKAGE = "com.android.org.bouncycastle.jcajce.provider.digest.";
    private static final String KEYSTORE_PACKAGE = "com.android.org.bouncycastle.jcajce.provider.keystore.";
    private static final String SYMMETRIC_PACKAGE = "com.android.org.bouncycastle.jcajce.provider.symmetric.";
    private static String info = "BouncyCastle Security Provider v1.50";
    public static final ProviderConfiguration CONFIGURATION = new BouncyCastleProviderConfiguration();
    private static final Map keyInfoConverters = new HashMap();
    private static final String[] SYMMETRIC_GENERIC = {"PBEPBKDF2", "PBEPKCS12"};
    private static final String[] SYMMETRIC_MACS = new String[0];
    private static final String[] SYMMETRIC_CIPHERS = {"AES", "ARC4", "Blowfish", "DES", "DESede", "RC2", "Twofish"};
    private static final String[] ASYMMETRIC_GENERIC = {"X509"};
    private static final String[] ASYMMETRIC_CIPHERS = {"DSA", "DH", "EC", "RSA"};
    private static final String[] DIGESTS = {"MD5", "SHA1", "SHA224", "SHA256", "SHA384", "SHA512"};
    public static final String PROVIDER_NAME = "BC";
    private static final String[] KEYSTORES = {PROVIDER_NAME, "PKCS12"};

    public BouncyCastleProvider() {
        super(PROVIDER_NAME, 1.5d, info);
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                BouncyCastleProvider.this.setup();
                return null;
            }
        });
    }

    private void setup() {
        loadAlgorithms(DIGEST_PACKAGE, DIGESTS);
        loadAlgorithms(SYMMETRIC_PACKAGE, SYMMETRIC_GENERIC);
        loadAlgorithms(SYMMETRIC_PACKAGE, SYMMETRIC_MACS);
        loadAlgorithms(SYMMETRIC_PACKAGE, SYMMETRIC_CIPHERS);
        loadAlgorithms(ASYMMETRIC_PACKAGE, ASYMMETRIC_GENERIC);
        loadAlgorithms(ASYMMETRIC_PACKAGE, ASYMMETRIC_CIPHERS);
        loadAlgorithms(KEYSTORE_PACKAGE, KEYSTORES);
        put("CertPathValidator.PKIX", "com.android.org.bouncycastle.jce.provider.PKIXCertPathValidatorSpi");
        put("CertPathBuilder.PKIX", "com.android.org.bouncycastle.jce.provider.PKIXCertPathBuilderSpi");
        put("CertStore.Collection", "com.android.org.bouncycastle.jce.provider.CertStoreCollectionSpi");
    }

    private void loadAlgorithms(String packageName, String[] names) {
        for (int i = 0; i != names.length; i++) {
            Class<?> cls = null;
            try {
                ClassLoader loader = getClass().getClassLoader();
                if (loader != null) {
                    cls = loader.loadClass(packageName + names[i] + "$Mappings");
                } else {
                    cls = Class.forName(packageName + names[i] + "$Mappings");
                }
            } catch (ClassNotFoundException e) {
            }
            if (cls != null) {
                try {
                    ((AlgorithmProvider) cls.newInstance()).configure(this);
                } catch (Exception e2) {
                    throw new InternalError("cannot create instance of " + packageName + names[i] + "$Mappings : " + e2);
                }
            }
        }
    }

    @Override
    public void setParameter(String parameterName, Object parameter) {
        synchronized (CONFIGURATION) {
            ((BouncyCastleProviderConfiguration) CONFIGURATION).setParameter(parameterName, parameter);
        }
    }

    @Override
    public boolean hasAlgorithm(String type, String name) {
        return containsKey(new StringBuilder().append(type).append(".").append(name).toString()) || containsKey(new StringBuilder().append("Alg.Alias.").append(type).append(".").append(name).toString());
    }

    @Override
    public void addAlgorithm(String key, String value) {
        if (containsKey(key)) {
            throw new IllegalStateException("duplicate provider key (" + key + ") found");
        }
        put(key, value);
    }

    @Override
    public void addKeyInfoConverter(ASN1ObjectIdentifier oid, AsymmetricKeyInfoConverter keyInfoConverter) {
        keyInfoConverters.put(oid, keyInfoConverter);
    }

    public static PublicKey getPublicKey(SubjectPublicKeyInfo publicKeyInfo) throws IOException {
        AsymmetricKeyInfoConverter converter = (AsymmetricKeyInfoConverter) keyInfoConverters.get(publicKeyInfo.getAlgorithm().getAlgorithm());
        if (converter == null) {
            return null;
        }
        return converter.generatePublic(publicKeyInfo);
    }

    public static PrivateKey getPrivateKey(PrivateKeyInfo privateKeyInfo) throws IOException {
        AsymmetricKeyInfoConverter converter = (AsymmetricKeyInfoConverter) keyInfoConverters.get(privateKeyInfo.getPrivateKeyAlgorithm().getAlgorithm());
        if (converter == null) {
            return null;
        }
        return converter.generatePrivate(privateKeyInfo);
    }
}
