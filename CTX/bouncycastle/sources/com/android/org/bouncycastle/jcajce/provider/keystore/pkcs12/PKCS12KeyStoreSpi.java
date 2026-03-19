package com.android.org.bouncycastle.jcajce.provider.keystore.pkcs12;

import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1Encoding;
import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.ASN1OctetString;
import com.android.org.bouncycastle.asn1.ASN1OutputStream;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.ASN1Set;
import com.android.org.bouncycastle.asn1.BEROctetString;
import com.android.org.bouncycastle.asn1.BEROutputStream;
import com.android.org.bouncycastle.asn1.DERBMPString;
import com.android.org.bouncycastle.asn1.DERNull;
import com.android.org.bouncycastle.asn1.DEROctetString;
import com.android.org.bouncycastle.asn1.DEROutputStream;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.asn1.DERSet;
import com.android.org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import com.android.org.bouncycastle.asn1.ntt.NTTObjectIdentifiers;
import com.android.org.bouncycastle.asn1.pkcs.AuthenticatedSafe;
import com.android.org.bouncycastle.asn1.pkcs.CertBag;
import com.android.org.bouncycastle.asn1.pkcs.ContentInfo;
import com.android.org.bouncycastle.asn1.pkcs.EncryptedData;
import com.android.org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import com.android.org.bouncycastle.asn1.pkcs.MacData;
import com.android.org.bouncycastle.asn1.pkcs.PBES2Parameters;
import com.android.org.bouncycastle.asn1.pkcs.PBKDF2Params;
import com.android.org.bouncycastle.asn1.pkcs.PKCS12PBEParams;
import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.asn1.pkcs.Pfx;
import com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import com.android.org.bouncycastle.asn1.pkcs.SafeBag;
import com.android.org.bouncycastle.asn1.util.ASN1Dump;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import com.android.org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import com.android.org.bouncycastle.asn1.x509.DigestInfo;
import com.android.org.bouncycastle.asn1.x509.Extension;
import com.android.org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import com.android.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import com.android.org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import com.android.org.bouncycastle.crypto.Digest;
import com.android.org.bouncycastle.crypto.digests.AndroidDigestFactory;
import com.android.org.bouncycastle.jcajce.PKCS12Key;
import com.android.org.bouncycastle.jcajce.PKCS12StoreParameter;
import com.android.org.bouncycastle.jcajce.spec.PBKDF2KeySpec;
import com.android.org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import com.android.org.bouncycastle.jcajce.util.JcaJceHelper;
import com.android.org.bouncycastle.jce.interfaces.BCKeyStore;
import com.android.org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import com.android.org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.android.org.bouncycastle.jce.provider.JDKPKCS12StoreParameter;
import com.android.org.bouncycastle.util.Arrays;
import com.android.org.bouncycastle.util.Integers;
import com.android.org.bouncycastle.util.Strings;
import com.android.org.bouncycastle.util.encoders.Hex;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class PKCS12KeyStoreSpi extends KeyStoreSpi implements PKCSObjectIdentifiers, X509ObjectIdentifiers, BCKeyStore {
    static final int CERTIFICATE = 1;
    static final int KEY = 2;
    static final int KEY_PRIVATE = 0;
    static final int KEY_PUBLIC = 1;
    static final int KEY_SECRET = 2;
    private static final int MIN_ITERATIONS = 1024;
    static final int NULL = 0;
    private static final int SALT_SIZE = 20;
    static final int SEALED = 4;
    static final int SECRET = 3;
    private static final DefaultSecretKeyProvider keySizeProvider = new DefaultSecretKeyProvider();
    private ASN1ObjectIdentifier certAlgorithm;
    private CertificateFactory certFact;
    private IgnoresCaseHashtable certs;
    private ASN1ObjectIdentifier keyAlgorithm;
    private IgnoresCaseHashtable keys;
    private final JcaJceHelper helper = new DefaultJcaJceHelper();
    private Hashtable localIds = new Hashtable();
    private Hashtable chainCerts = new Hashtable();
    private Hashtable keyCerts = new Hashtable();
    protected SecureRandom random = new SecureRandom();

    private class CertId {
        byte[] id;

        CertId(PublicKey publicKey) {
            this.id = PKCS12KeyStoreSpi.this.createSubjectKeyId(publicKey).getKeyIdentifier();
        }

        CertId(byte[] bArr) {
            this.id = bArr;
        }

        public int hashCode() {
            return Arrays.hashCode(this.id);
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof CertId)) {
                return false;
            }
            return Arrays.areEqual(this.id, ((CertId) obj).id);
        }
    }

    public PKCS12KeyStoreSpi(Provider provider, ASN1ObjectIdentifier aSN1ObjectIdentifier, ASN1ObjectIdentifier aSN1ObjectIdentifier2) {
        this.keys = new IgnoresCaseHashtable();
        this.certs = new IgnoresCaseHashtable();
        this.keyAlgorithm = aSN1ObjectIdentifier;
        this.certAlgorithm = aSN1ObjectIdentifier2;
        try {
            if (provider != null) {
                this.certFact = CertificateFactory.getInstance("X.509", provider);
            } else {
                this.certFact = CertificateFactory.getInstance("X.509");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("can't create cert factory - " + e.toString());
        }
    }

    private SubjectKeyIdentifier createSubjectKeyId(PublicKey publicKey) {
        try {
            return new SubjectKeyIdentifier(getDigest(SubjectPublicKeyInfo.getInstance(publicKey.getEncoded())));
        } catch (Exception e) {
            throw new RuntimeException("error creating key");
        }
    }

    private static byte[] getDigest(SubjectPublicKeyInfo subjectPublicKeyInfo) {
        Digest sha1 = AndroidDigestFactory.getSHA1();
        byte[] bArr = new byte[sha1.getDigestSize()];
        byte[] bytes = subjectPublicKeyInfo.getPublicKeyData().getBytes();
        sha1.update(bytes, 0, bytes.length);
        sha1.doFinal(bArr, 0);
        return bArr;
    }

    @Override
    public void setRandom(SecureRandom secureRandom) {
        this.random = secureRandom;
    }

    @Override
    public Enumeration engineAliases() {
        Hashtable hashtable = new Hashtable();
        Enumeration enumerationKeys = this.certs.keys();
        while (enumerationKeys.hasMoreElements()) {
            hashtable.put(enumerationKeys.nextElement(), "cert");
        }
        Enumeration enumerationKeys2 = this.keys.keys();
        while (enumerationKeys2.hasMoreElements()) {
            String str = (String) enumerationKeys2.nextElement();
            if (hashtable.get(str) == null) {
                hashtable.put(str, "key");
            }
        }
        return hashtable.keys();
    }

    @Override
    public boolean engineContainsAlias(String str) {
        return (this.certs.get(str) == null && this.keys.get(str) == null) ? false : true;
    }

    @Override
    public void engineDeleteEntry(String str) throws KeyStoreException {
        Key key = (Key) this.keys.remove(str);
        Certificate certificate = (Certificate) this.certs.remove(str);
        if (certificate != null) {
            this.chainCerts.remove(new CertId(certificate.getPublicKey()));
        }
        if (key != null) {
            String str2 = (String) this.localIds.remove(str);
            if (str2 != null) {
                certificate = (Certificate) this.keyCerts.remove(str2);
            }
            if (certificate != null) {
                this.chainCerts.remove(new CertId(certificate.getPublicKey()));
            }
        }
    }

    @Override
    public Certificate engineGetCertificate(String str) {
        if (str == null) {
            throw new IllegalArgumentException("null alias passed to getCertificate.");
        }
        Certificate certificate = (Certificate) this.certs.get(str);
        if (certificate == null) {
            String str2 = (String) this.localIds.get(str);
            if (str2 != null) {
                return (Certificate) this.keyCerts.get(str2);
            }
            return (Certificate) this.keyCerts.get(str);
        }
        return certificate;
    }

    @Override
    public String engineGetCertificateAlias(Certificate certificate) {
        Enumeration enumerationElements = this.certs.elements();
        Enumeration enumerationKeys = this.certs.keys();
        while (enumerationElements.hasMoreElements()) {
            Certificate certificate2 = (Certificate) enumerationElements.nextElement();
            String str = (String) enumerationKeys.nextElement();
            if (certificate2.equals(certificate)) {
                return str;
            }
        }
        Enumeration enumerationElements2 = this.keyCerts.elements();
        Enumeration enumerationKeys2 = this.keyCerts.keys();
        while (enumerationElements2.hasMoreElements()) {
            Certificate certificate3 = (Certificate) enumerationElements2.nextElement();
            String str2 = (String) enumerationKeys2.nextElement();
            if (certificate3.equals(certificate)) {
                return str2;
            }
        }
        return null;
    }

    @Override
    public Certificate[] engineGetCertificateChain(String str) {
        Certificate certificateEngineGetCertificate;
        Certificate certificate;
        if (str == null) {
            throw new IllegalArgumentException("null alias passed to getCertificateChain.");
        }
        if (!engineIsKeyEntry(str) || (certificateEngineGetCertificate = engineGetCertificate(str)) == null) {
            return null;
        }
        Vector vector = new Vector();
        while (certificateEngineGetCertificate != null) {
            X509Certificate x509Certificate = (X509Certificate) certificateEngineGetCertificate;
            byte[] extensionValue = x509Certificate.getExtensionValue(Extension.authorityKeyIdentifier.getId());
            if (extensionValue != null) {
                try {
                    AuthorityKeyIdentifier authorityKeyIdentifier = AuthorityKeyIdentifier.getInstance(new ASN1InputStream(((ASN1OctetString) new ASN1InputStream(extensionValue).readObject()).getOctets()).readObject());
                    if (authorityKeyIdentifier.getKeyIdentifier() != null) {
                        certificate = (Certificate) this.chainCerts.get(new CertId(authorityKeyIdentifier.getKeyIdentifier()));
                    } else {
                        certificate = null;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e.toString());
                }
            } else {
                certificate = null;
            }
            if (certificate == null) {
                Principal issuerDN = x509Certificate.getIssuerDN();
                if (!issuerDN.equals(x509Certificate.getSubjectDN())) {
                    Enumeration enumerationKeys = this.chainCerts.keys();
                    while (true) {
                        if (!enumerationKeys.hasMoreElements()) {
                            break;
                        }
                        X509Certificate x509Certificate2 = (X509Certificate) this.chainCerts.get(enumerationKeys.nextElement());
                        if (x509Certificate2.getSubjectDN().equals(issuerDN)) {
                            try {
                                x509Certificate.verify(x509Certificate2.getPublicKey());
                                certificate = x509Certificate2;
                                break;
                            } catch (Exception e2) {
                            }
                        }
                    }
                }
            }
            if (!vector.contains(certificateEngineGetCertificate)) {
                vector.addElement(certificateEngineGetCertificate);
                if (certificate != certificateEngineGetCertificate) {
                    certificateEngineGetCertificate = certificate;
                }
            }
            certificateEngineGetCertificate = null;
        }
        Certificate[] certificateArr = new Certificate[vector.size()];
        for (int i = 0; i != certificateArr.length; i++) {
            certificateArr[i] = (Certificate) vector.elementAt(i);
        }
        return certificateArr;
    }

    @Override
    public Date engineGetCreationDate(String str) {
        if (str == null) {
            throw new NullPointerException("alias == null");
        }
        if (this.keys.get(str) == null && this.certs.get(str) == null) {
            return null;
        }
        return new Date();
    }

    @Override
    public Key engineGetKey(String str, char[] cArr) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        if (str == null) {
            throw new IllegalArgumentException("null alias passed to getKey.");
        }
        return (Key) this.keys.get(str);
    }

    @Override
    public boolean engineIsCertificateEntry(String str) {
        return this.certs.get(str) != null && this.keys.get(str) == null;
    }

    @Override
    public boolean engineIsKeyEntry(String str) {
        return this.keys.get(str) != null;
    }

    @Override
    public void engineSetCertificateEntry(String str, Certificate certificate) throws KeyStoreException {
        if (this.keys.get(str) != null) {
            throw new KeyStoreException("There is a key entry with the name " + str + ".");
        }
        this.certs.put(str, certificate);
        this.chainCerts.put(new CertId(certificate.getPublicKey()), certificate);
    }

    @Override
    public void engineSetKeyEntry(String str, byte[] bArr, Certificate[] certificateArr) throws KeyStoreException {
        throw new RuntimeException("operation not supported");
    }

    @Override
    public void engineSetKeyEntry(String str, Key key, char[] cArr, Certificate[] certificateArr) throws KeyStoreException {
        boolean z = key instanceof PrivateKey;
        if (!z) {
            throw new KeyStoreException("PKCS12 does not support non-PrivateKeys");
        }
        if (z && certificateArr == null) {
            throw new KeyStoreException("no certificate chain for private key");
        }
        if (this.keys.get(str) != null) {
            engineDeleteEntry(str);
        }
        this.keys.put(str, key);
        if (certificateArr != null) {
            this.certs.put(str, certificateArr[0]);
            for (int i = 0; i != certificateArr.length; i++) {
                this.chainCerts.put(new CertId(certificateArr[i].getPublicKey()), certificateArr[i]);
            }
        }
    }

    @Override
    public int engineSize() {
        Hashtable hashtable = new Hashtable();
        Enumeration enumerationKeys = this.certs.keys();
        while (enumerationKeys.hasMoreElements()) {
            hashtable.put(enumerationKeys.nextElement(), "cert");
        }
        Enumeration enumerationKeys2 = this.keys.keys();
        while (enumerationKeys2.hasMoreElements()) {
            String str = (String) enumerationKeys2.nextElement();
            if (hashtable.get(str) == null) {
                hashtable.put(str, "key");
            }
        }
        return hashtable.size();
    }

    protected PrivateKey unwrapKey(AlgorithmIdentifier algorithmIdentifier, byte[] bArr, char[] cArr, boolean z) throws IOException {
        ASN1ObjectIdentifier algorithm = algorithmIdentifier.getAlgorithm();
        try {
            if (algorithm.on(PKCSObjectIdentifiers.pkcs_12PbeIds)) {
                PKCS12PBEParams pKCS12PBEParams = PKCS12PBEParams.getInstance(algorithmIdentifier.getParameters());
                PBEParameterSpec pBEParameterSpec = new PBEParameterSpec(pKCS12PBEParams.getIV(), pKCS12PBEParams.getIterations().intValue());
                Cipher cipherCreateCipher = this.helper.createCipher(algorithm.getId());
                cipherCreateCipher.init(4, new PKCS12Key(cArr, z), pBEParameterSpec);
                return (PrivateKey) cipherCreateCipher.unwrap(bArr, "", 2);
            }
            if (algorithm.equals(PKCSObjectIdentifiers.id_PBES2)) {
                return (PrivateKey) createCipher(4, cArr, algorithmIdentifier).unwrap(bArr, "", 2);
            }
            throw new IOException("exception unwrapping private key - cannot recognise: " + algorithm);
        } catch (Exception e) {
            throw new IOException("exception unwrapping private key - " + e.toString());
        }
    }

    protected byte[] wrapKey(String str, Key key, PKCS12PBEParams pKCS12PBEParams, char[] cArr) throws IOException {
        PBEKeySpec pBEKeySpec = new PBEKeySpec(cArr);
        try {
            SecretKeyFactory secretKeyFactoryCreateSecretKeyFactory = this.helper.createSecretKeyFactory(str);
            PBEParameterSpec pBEParameterSpec = new PBEParameterSpec(pKCS12PBEParams.getIV(), pKCS12PBEParams.getIterations().intValue());
            Cipher cipherCreateCipher = this.helper.createCipher(str);
            cipherCreateCipher.init(3, secretKeyFactoryCreateSecretKeyFactory.generateSecret(pBEKeySpec), pBEParameterSpec);
            return cipherCreateCipher.wrap(key);
        } catch (Exception e) {
            throw new IOException("exception encrypting data - " + e.toString());
        }
    }

    protected byte[] cryptData(boolean z, AlgorithmIdentifier algorithmIdentifier, char[] cArr, boolean z2, byte[] bArr) throws IOException {
        ASN1ObjectIdentifier algorithm = algorithmIdentifier.getAlgorithm();
        int i = z ? 1 : 2;
        if (algorithm.on(PKCSObjectIdentifiers.pkcs_12PbeIds)) {
            PKCS12PBEParams pKCS12PBEParams = PKCS12PBEParams.getInstance(algorithmIdentifier.getParameters());
            new PBEKeySpec(cArr);
            try {
                PBEParameterSpec pBEParameterSpec = new PBEParameterSpec(pKCS12PBEParams.getIV(), pKCS12PBEParams.getIterations().intValue());
                PKCS12Key pKCS12Key = new PKCS12Key(cArr, z2);
                Cipher cipherCreateCipher = this.helper.createCipher(algorithm.getId());
                cipherCreateCipher.init(i, pKCS12Key, pBEParameterSpec);
                return cipherCreateCipher.doFinal(bArr);
            } catch (Exception e) {
                throw new IOException("exception decrypting data - " + e.toString());
            }
        }
        if (algorithm.equals(PKCSObjectIdentifiers.id_PBES2)) {
            try {
                return createCipher(i, cArr, algorithmIdentifier).doFinal(bArr);
            } catch (Exception e2) {
                throw new IOException("exception decrypting data - " + e2.toString());
            }
        }
        throw new IOException("unknown PBE algorithm: " + algorithm);
    }

    private Cipher createCipher(int i, char[] cArr, AlgorithmIdentifier algorithmIdentifier) throws InvalidKeySpecException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, InvalidAlgorithmParameterException {
        SecretKey secretKeyGenerateSecret;
        PBES2Parameters pBES2Parameters = PBES2Parameters.getInstance(algorithmIdentifier.getParameters());
        PBKDF2Params pBKDF2Params = PBKDF2Params.getInstance(pBES2Parameters.getKeyDerivationFunc().getParameters());
        AlgorithmIdentifier algorithmIdentifier2 = AlgorithmIdentifier.getInstance(pBES2Parameters.getEncryptionScheme());
        SecretKeyFactory secretKeyFactoryCreateSecretKeyFactory = this.helper.createSecretKeyFactory(pBES2Parameters.getKeyDerivationFunc().getAlgorithm().getId());
        if (pBKDF2Params.isDefaultPrf()) {
            secretKeyGenerateSecret = secretKeyFactoryCreateSecretKeyFactory.generateSecret(new PBEKeySpec(cArr, pBKDF2Params.getSalt(), pBKDF2Params.getIterationCount().intValue(), keySizeProvider.getKeySize(algorithmIdentifier2)));
        } else {
            secretKeyGenerateSecret = secretKeyFactoryCreateSecretKeyFactory.generateSecret(new PBKDF2KeySpec(cArr, pBKDF2Params.getSalt(), pBKDF2Params.getIterationCount().intValue(), keySizeProvider.getKeySize(algorithmIdentifier2), pBKDF2Params.getPrf()));
        }
        Cipher cipher = Cipher.getInstance(pBES2Parameters.getEncryptionScheme().getAlgorithm().getId());
        AlgorithmIdentifier.getInstance(pBES2Parameters.getEncryptionScheme());
        ASN1Encodable parameters = pBES2Parameters.getEncryptionScheme().getParameters();
        if (parameters instanceof ASN1OctetString) {
            cipher.init(i, secretKeyGenerateSecret, new IvParameterSpec(ASN1OctetString.getInstance(parameters).getOctets()));
        }
        return cipher;
    }

    @Override
    public void engineLoad(InputStream inputStream, char[] cArr) throws IOException {
        boolean z;
        boolean z2;
        String string;
        ASN1OctetString aSN1OctetString;
        ASN1Sequence aSN1Sequence;
        ASN1Primitive aSN1Primitive;
        Object obj;
        ?? r6;
        ASN1Primitive aSN1Primitive2;
        boolean z3;
        if (inputStream == null) {
            return;
        }
        if (cArr == null) {
            throw new NullPointerException("No password supplied for PKCS#12 KeyStore.");
        }
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        bufferedInputStream.mark(10);
        if (bufferedInputStream.read() != 48) {
            throw new IOException("stream does not represent a PKCS12 key store");
        }
        bufferedInputStream.reset();
        Pfx pfx = Pfx.getInstance((ASN1Sequence) new ASN1InputStream(bufferedInputStream).readObject());
        ContentInfo authSafe = pfx.getAuthSafe();
        Vector vector = new Vector();
        int i = 1;
        int i2 = 0;
        if (pfx.getMacData() != null) {
            MacData macData = pfx.getMacData();
            DigestInfo mac = macData.getMac();
            AlgorithmIdentifier algorithmId = mac.getAlgorithmId();
            byte[] salt = macData.getSalt();
            int iIntValue = macData.getIterationCount().intValue();
            byte[] octets = ((ASN1OctetString) authSafe.getContent()).getOctets();
            try {
                byte[] bArrCalculatePbeMac = calculatePbeMac(algorithmId.getAlgorithm(), salt, iIntValue, cArr, false, octets);
                byte[] digest = mac.getDigest();
                if (!Arrays.constantTimeAreEqual(bArrCalculatePbeMac, digest)) {
                    if (cArr.length > 0) {
                        throw new IOException("PKCS12 key store mac invalid - wrong password or corrupted file.");
                    }
                    if (!Arrays.constantTimeAreEqual(calculatePbeMac(algorithmId.getAlgorithm(), salt, iIntValue, cArr, true, octets), digest)) {
                        throw new IOException("PKCS12 key store mac invalid - wrong password or corrupted file.");
                    }
                    z3 = true;
                } else {
                    z3 = false;
                }
                z = z3;
            } catch (IOException e) {
                throw e;
            } catch (Exception e2) {
                throw new IOException("error constructing MAC: " + e2.toString());
            }
        } else {
            z = false;
        }
        AnonymousClass1 anonymousClass1 = null;
        this.keys = new IgnoresCaseHashtable();
        this.localIds = new Hashtable();
        if (authSafe.getContentType().equals(data)) {
            ContentInfo[] contentInfo = AuthenticatedSafe.getInstance(new ASN1InputStream(((ASN1OctetString) authSafe.getContent()).getOctets()).readObject()).getContentInfo();
            int i3 = 0;
            z2 = false;
            while (i3 != contentInfo.length) {
                if (contentInfo[i3].getContentType().equals(data)) {
                    ASN1Sequence aSN1Sequence2 = (ASN1Sequence) new ASN1InputStream(((ASN1OctetString) contentInfo[i3].getContent()).getOctets()).readObject();
                    int i4 = i2;
                    while (i4 != aSN1Sequence2.size()) {
                        SafeBag safeBag = SafeBag.getInstance(aSN1Sequence2.getObjectAt(i4));
                        if (safeBag.getBagId().equals(pkcs8ShroudedKeyBag)) {
                            EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = EncryptedPrivateKeyInfo.getInstance(safeBag.getBagValue());
                            PrivateKey privateKeyUnwrapKey = unwrapKey(encryptedPrivateKeyInfo.getEncryptionAlgorithm(), encryptedPrivateKeyInfo.getEncryptedData(), cArr, z);
                            PKCS12BagAttributeCarrier pKCS12BagAttributeCarrier = (PKCS12BagAttributeCarrier) privateKeyUnwrapKey;
                            if (safeBag.getBagAttributes() != null) {
                                Enumeration objects = safeBag.getBagAttributes().getObjects();
                                Object obj2 = anonymousClass1;
                                Object obj3 = obj2;
                                Object obj4 = obj2;
                                while (objects.hasMoreElements()) {
                                    ASN1Sequence aSN1Sequence3 = (ASN1Sequence) objects.nextElement();
                                    ASN1ObjectIdentifier aSN1ObjectIdentifier = (ASN1ObjectIdentifier) aSN1Sequence3.getObjectAt(i2);
                                    ASN1Set aSN1Set = (ASN1Set) aSN1Sequence3.getObjectAt(i);
                                    if (aSN1Set.size() > 0) {
                                        aSN1Primitive2 = (ASN1Primitive) aSN1Set.getObjectAt(0);
                                        ASN1Encodable bagAttribute = pKCS12BagAttributeCarrier.getBagAttribute(aSN1ObjectIdentifier);
                                        if (bagAttribute != null) {
                                            if (!bagAttribute.toASN1Primitive().equals(aSN1Primitive2)) {
                                                throw new IOException("attempt to add existing attribute with different value");
                                            }
                                        } else {
                                            pKCS12BagAttributeCarrier.setBagAttribute(aSN1ObjectIdentifier, aSN1Primitive2);
                                        }
                                    } else {
                                        aSN1Primitive2 = null;
                                    }
                                    if (aSN1ObjectIdentifier.equals(pkcs_9_at_friendlyName)) {
                                        String string2 = ((DERBMPString) aSN1Primitive2).getString();
                                        this.keys.put(string2, privateKeyUnwrapKey);
                                        obj3 = string2;
                                    } else if (aSN1ObjectIdentifier.equals(pkcs_9_at_localKeyId)) {
                                        obj4 = (ASN1OctetString) aSN1Primitive2;
                                    }
                                    i = 1;
                                    i2 = 0;
                                    obj4 = obj4;
                                    obj3 = obj3;
                                }
                                obj = obj3;
                                r6 = obj4;
                            } else {
                                obj = null;
                                r6 = 0;
                            }
                            if (r6 != 0) {
                                String str = new String(Hex.encode(r6.getOctets()));
                                if (obj == null) {
                                    this.keys.put(str, privateKeyUnwrapKey);
                                } else {
                                    this.localIds.put(obj, str);
                                }
                            } else {
                                this.keys.put("unmarked", privateKeyUnwrapKey);
                                z2 = true;
                            }
                        } else if (safeBag.getBagId().equals(certBag)) {
                            vector.addElement(safeBag);
                        } else {
                            System.out.println("extra in data " + safeBag.getBagId());
                            System.out.println(ASN1Dump.dumpAsString(safeBag));
                        }
                        i4++;
                        i = 1;
                        i2 = 0;
                        anonymousClass1 = null;
                    }
                } else if (contentInfo[i3].getContentType().equals(encryptedData)) {
                    EncryptedData encryptedData = EncryptedData.getInstance(contentInfo[i3].getContent());
                    ASN1Sequence aSN1Sequence4 = (ASN1Sequence) ASN1Primitive.fromByteArray(cryptData(false, encryptedData.getEncryptionAlgorithm(), cArr, z, encryptedData.getContent().getOctets()));
                    int i5 = 0;
                    while (i5 != aSN1Sequence4.size()) {
                        SafeBag safeBag2 = SafeBag.getInstance(aSN1Sequence4.getObjectAt(i5));
                        if (safeBag2.getBagId().equals(certBag)) {
                            vector.addElement(safeBag2);
                            aSN1Sequence = aSN1Sequence4;
                        } else if (safeBag2.getBagId().equals(pkcs8ShroudedKeyBag)) {
                            EncryptedPrivateKeyInfo encryptedPrivateKeyInfo2 = EncryptedPrivateKeyInfo.getInstance(safeBag2.getBagValue());
                            PrivateKey privateKeyUnwrapKey2 = unwrapKey(encryptedPrivateKeyInfo2.getEncryptionAlgorithm(), encryptedPrivateKeyInfo2.getEncryptedData(), cArr, z);
                            PKCS12BagAttributeCarrier pKCS12BagAttributeCarrier2 = (PKCS12BagAttributeCarrier) privateKeyUnwrapKey2;
                            Enumeration objects2 = safeBag2.getBagAttributes().getObjects();
                            ASN1OctetString aSN1OctetString2 = null;
                            String str2 = null;
                            while (objects2.hasMoreElements()) {
                                ASN1Sequence aSN1Sequence5 = (ASN1Sequence) objects2.nextElement();
                                ASN1ObjectIdentifier aSN1ObjectIdentifier2 = (ASN1ObjectIdentifier) aSN1Sequence5.getObjectAt(0);
                                ASN1Sequence aSN1Sequence6 = aSN1Sequence4;
                                ASN1Set aSN1Set2 = (ASN1Set) aSN1Sequence5.getObjectAt(1);
                                if (aSN1Set2.size() > 0) {
                                    aSN1Primitive = (ASN1Primitive) aSN1Set2.getObjectAt(0);
                                    ASN1Encodable bagAttribute2 = pKCS12BagAttributeCarrier2.getBagAttribute(aSN1ObjectIdentifier2);
                                    if (bagAttribute2 != null) {
                                        if (!bagAttribute2.toASN1Primitive().equals(aSN1Primitive)) {
                                            throw new IOException("attempt to add existing attribute with different value");
                                        }
                                    } else {
                                        pKCS12BagAttributeCarrier2.setBagAttribute(aSN1ObjectIdentifier2, aSN1Primitive);
                                    }
                                } else {
                                    aSN1Primitive = null;
                                }
                                if (aSN1ObjectIdentifier2.equals(pkcs_9_at_friendlyName)) {
                                    String string3 = ((DERBMPString) aSN1Primitive).getString();
                                    this.keys.put(string3, privateKeyUnwrapKey2);
                                    str2 = string3;
                                } else if (aSN1ObjectIdentifier2.equals(pkcs_9_at_localKeyId)) {
                                    aSN1OctetString2 = (ASN1OctetString) aSN1Primitive;
                                }
                                aSN1Sequence4 = aSN1Sequence6;
                            }
                            aSN1Sequence = aSN1Sequence4;
                            String str3 = new String(Hex.encode(aSN1OctetString2.getOctets()));
                            if (str2 == null) {
                                this.keys.put(str3, privateKeyUnwrapKey2);
                            } else {
                                this.localIds.put(str2, str3);
                            }
                        } else {
                            aSN1Sequence = aSN1Sequence4;
                            if (safeBag2.getBagId().equals(keyBag)) {
                                PrivateKey privateKey = BouncyCastleProvider.getPrivateKey(PrivateKeyInfo.getInstance(safeBag2.getBagValue()));
                                PKCS12BagAttributeCarrier pKCS12BagAttributeCarrier3 = (PKCS12BagAttributeCarrier) privateKey;
                                Enumeration objects3 = safeBag2.getBagAttributes().getObjects();
                                ASN1OctetString aSN1OctetString3 = null;
                                String string4 = null;
                                while (objects3.hasMoreElements()) {
                                    ASN1Sequence aSN1Sequence7 = ASN1Sequence.getInstance(objects3.nextElement());
                                    ASN1ObjectIdentifier aSN1ObjectIdentifier3 = ASN1ObjectIdentifier.getInstance(aSN1Sequence7.getObjectAt(0));
                                    ASN1Set aSN1Set3 = ASN1Set.getInstance(aSN1Sequence7.getObjectAt(1));
                                    if (aSN1Set3.size() > 0) {
                                        ASN1Primitive aSN1Primitive3 = (ASN1Primitive) aSN1Set3.getObjectAt(0);
                                        ASN1Encodable bagAttribute3 = pKCS12BagAttributeCarrier3.getBagAttribute(aSN1ObjectIdentifier3);
                                        if (bagAttribute3 != null) {
                                            if (!bagAttribute3.toASN1Primitive().equals(aSN1Primitive3)) {
                                                throw new IOException("attempt to add existing attribute with different value");
                                            }
                                        } else {
                                            pKCS12BagAttributeCarrier3.setBagAttribute(aSN1ObjectIdentifier3, aSN1Primitive3);
                                        }
                                        if (aSN1ObjectIdentifier3.equals(pkcs_9_at_friendlyName)) {
                                            string4 = ((DERBMPString) aSN1Primitive3).getString();
                                            this.keys.put(string4, privateKey);
                                        } else if (aSN1ObjectIdentifier3.equals(pkcs_9_at_localKeyId)) {
                                            aSN1OctetString3 = (ASN1OctetString) aSN1Primitive3;
                                        }
                                    }
                                }
                                String str4 = new String(Hex.encode(aSN1OctetString3.getOctets()));
                                if (string4 == null) {
                                    this.keys.put(str4, privateKey);
                                } else {
                                    this.localIds.put(string4, str4);
                                }
                            } else {
                                System.out.println("extra in encryptedData " + safeBag2.getBagId());
                                System.out.println(ASN1Dump.dumpAsString(safeBag2));
                            }
                        }
                        i5++;
                        aSN1Sequence4 = aSN1Sequence;
                    }
                } else {
                    System.out.println("extra " + contentInfo[i3].getContentType().getId());
                    System.out.println("extra " + ASN1Dump.dumpAsString(contentInfo[i3].getContent()));
                }
                i3++;
                i = 1;
                i2 = 0;
                anonymousClass1 = null;
            }
        } else {
            z2 = false;
        }
        this.certs = new IgnoresCaseHashtable();
        this.chainCerts = new Hashtable();
        this.keyCerts = new Hashtable();
        for (int i6 = 0; i6 != vector.size(); i6++) {
            SafeBag safeBag3 = (SafeBag) vector.elementAt(i6);
            CertBag certBag = CertBag.getInstance(safeBag3.getBagValue());
            if (!certBag.getCertId().equals(x509Certificate)) {
                throw new RuntimeException("Unsupported certificate type: " + certBag.getCertId());
            }
            try {
                ?? GenerateCertificate = this.certFact.generateCertificate(new ByteArrayInputStream(((ASN1OctetString) certBag.getCertValue()).getOctets()));
                if (safeBag3.getBagAttributes() != null) {
                    Enumeration objects4 = safeBag3.getBagAttributes().getObjects();
                    string = null;
                    aSN1OctetString = null;
                    while (objects4.hasMoreElements()) {
                        ASN1Sequence aSN1Sequence8 = ASN1Sequence.getInstance(objects4.nextElement());
                        ASN1ObjectIdentifier aSN1ObjectIdentifier4 = ASN1ObjectIdentifier.getInstance(aSN1Sequence8.getObjectAt(0));
                        ASN1Set aSN1Set4 = ASN1Set.getInstance(aSN1Sequence8.getObjectAt(1));
                        if (aSN1Set4.size() > 0) {
                            ASN1Primitive aSN1Primitive4 = (ASN1Primitive) aSN1Set4.getObjectAt(0);
                            if (GenerateCertificate instanceof PKCS12BagAttributeCarrier) {
                                PKCS12BagAttributeCarrier pKCS12BagAttributeCarrier4 = (PKCS12BagAttributeCarrier) GenerateCertificate;
                                ASN1Encodable bagAttribute4 = pKCS12BagAttributeCarrier4.getBagAttribute(aSN1ObjectIdentifier4);
                                if (bagAttribute4 != null) {
                                    if (!bagAttribute4.toASN1Primitive().equals(aSN1Primitive4)) {
                                        throw new IOException("attempt to add existing attribute with different value");
                                    }
                                } else {
                                    pKCS12BagAttributeCarrier4.setBagAttribute(aSN1ObjectIdentifier4, aSN1Primitive4);
                                }
                            }
                            if (aSN1ObjectIdentifier4.equals(pkcs_9_at_friendlyName)) {
                                string = ((DERBMPString) aSN1Primitive4).getString();
                            } else if (aSN1ObjectIdentifier4.equals(pkcs_9_at_localKeyId)) {
                                aSN1OctetString = (ASN1OctetString) aSN1Primitive4;
                            }
                        }
                    }
                } else {
                    string = null;
                    aSN1OctetString = null;
                }
                this.chainCerts.put(new CertId(GenerateCertificate.getPublicKey()), GenerateCertificate);
                if (z2) {
                    if (this.keyCerts.isEmpty()) {
                        String str5 = new String(Hex.encode(createSubjectKeyId(GenerateCertificate.getPublicKey()).getKeyIdentifier()));
                        this.keyCerts.put(str5, GenerateCertificate);
                        this.keys.put(str5, this.keys.remove("unmarked"));
                    }
                } else {
                    if (aSN1OctetString != null) {
                        this.keyCerts.put(new String(Hex.encode(aSN1OctetString.getOctets())), GenerateCertificate);
                    }
                    if (string != null) {
                        this.certs.put(string, GenerateCertificate);
                    }
                }
            } catch (Exception e3) {
                throw new RuntimeException(e3.toString());
            }
        }
    }

    @Override
    public void engineStore(KeyStore.LoadStoreParameter loadStoreParameter) throws NoSuchAlgorithmException, IOException, CertificateException {
        PKCS12StoreParameter pKCS12StoreParameter;
        char[] password;
        if (loadStoreParameter == null) {
            throw new IllegalArgumentException("'param' arg cannot be null");
        }
        boolean z = loadStoreParameter instanceof PKCS12StoreParameter;
        if (!z && !(loadStoreParameter instanceof JDKPKCS12StoreParameter)) {
            throw new IllegalArgumentException("No support for 'param' of type " + loadStoreParameter.getClass().getName());
        }
        if (z) {
            pKCS12StoreParameter = (PKCS12StoreParameter) loadStoreParameter;
        } else {
            JDKPKCS12StoreParameter jDKPKCS12StoreParameter = (JDKPKCS12StoreParameter) loadStoreParameter;
            pKCS12StoreParameter = new PKCS12StoreParameter(jDKPKCS12StoreParameter.getOutputStream(), loadStoreParameter.getProtectionParameter(), jDKPKCS12StoreParameter.isUseDEREncoding());
        }
        KeyStore.ProtectionParameter protectionParameter = loadStoreParameter.getProtectionParameter();
        if (protectionParameter == null) {
            password = null;
        } else if (protectionParameter instanceof KeyStore.PasswordProtection) {
            password = ((KeyStore.PasswordProtection) protectionParameter).getPassword();
        } else {
            throw new IllegalArgumentException("No support for protection parameter of type " + protectionParameter.getClass().getName());
        }
        doStore(pKCS12StoreParameter.getOutputStream(), password, pKCS12StoreParameter.isForDEREncoding());
    }

    @Override
    public void engineStore(OutputStream outputStream, char[] cArr) throws IOException {
        doStore(outputStream, cArr, false);
    }

    private void doStore(OutputStream outputStream, char[] cArr, boolean z) throws IOException {
        ASN1OutputStream bEROutputStream;
        ASN1OutputStream bEROutputStream2;
        Enumeration enumeration;
        boolean z2;
        Enumeration enumeration2;
        boolean z3;
        boolean z4;
        if (cArr == null) {
            throw new NullPointerException("No password supplied for PKCS#12 KeyStore.");
        }
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        Enumeration enumerationKeys = this.keys.keys();
        while (enumerationKeys.hasMoreElements()) {
            byte[] bArr = new byte[20];
            this.random.nextBytes(bArr);
            String str = (String) enumerationKeys.nextElement();
            PrivateKey privateKey = (PrivateKey) this.keys.get(str);
            PKCS12PBEParams pKCS12PBEParams = new PKCS12PBEParams(bArr, MIN_ITERATIONS);
            EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(new AlgorithmIdentifier(this.keyAlgorithm, pKCS12PBEParams.toASN1Primitive()), wrapKey(this.keyAlgorithm.getId(), privateKey, pKCS12PBEParams, cArr));
            ASN1EncodableVector aSN1EncodableVector2 = new ASN1EncodableVector();
            if (privateKey instanceof PKCS12BagAttributeCarrier) {
                PKCS12BagAttributeCarrier pKCS12BagAttributeCarrier = (PKCS12BagAttributeCarrier) privateKey;
                DERBMPString dERBMPString = (DERBMPString) pKCS12BagAttributeCarrier.getBagAttribute(pkcs_9_at_friendlyName);
                if (dERBMPString == null || !dERBMPString.getString().equals(str)) {
                    pKCS12BagAttributeCarrier.setBagAttribute(pkcs_9_at_friendlyName, new DERBMPString(str));
                }
                if (pKCS12BagAttributeCarrier.getBagAttribute(pkcs_9_at_localKeyId) == null) {
                    pKCS12BagAttributeCarrier.setBagAttribute(pkcs_9_at_localKeyId, createSubjectKeyId(engineGetCertificate(str).getPublicKey()));
                }
                Enumeration bagAttributeKeys = pKCS12BagAttributeCarrier.getBagAttributeKeys();
                z4 = false;
                while (bagAttributeKeys.hasMoreElements()) {
                    ASN1ObjectIdentifier aSN1ObjectIdentifier = (ASN1ObjectIdentifier) bagAttributeKeys.nextElement();
                    ASN1EncodableVector aSN1EncodableVector3 = new ASN1EncodableVector();
                    aSN1EncodableVector3.add(aSN1ObjectIdentifier);
                    aSN1EncodableVector3.add(new DERSet(pKCS12BagAttributeCarrier.getBagAttribute(aSN1ObjectIdentifier)));
                    aSN1EncodableVector2.add(new DERSequence(aSN1EncodableVector3));
                    z4 = true;
                }
            } else {
                z4 = false;
            }
            if (!z4) {
                ASN1EncodableVector aSN1EncodableVector4 = new ASN1EncodableVector();
                Certificate certificateEngineGetCertificate = engineGetCertificate(str);
                aSN1EncodableVector4.add(pkcs_9_at_localKeyId);
                aSN1EncodableVector4.add(new DERSet(createSubjectKeyId(certificateEngineGetCertificate.getPublicKey())));
                aSN1EncodableVector2.add(new DERSequence(aSN1EncodableVector4));
                ASN1EncodableVector aSN1EncodableVector5 = new ASN1EncodableVector();
                aSN1EncodableVector5.add(pkcs_9_at_friendlyName);
                aSN1EncodableVector5.add(new DERSet(new DERBMPString(str)));
                aSN1EncodableVector2.add(new DERSequence(aSN1EncodableVector5));
            }
            aSN1EncodableVector.add(new SafeBag(pkcs8ShroudedKeyBag, encryptedPrivateKeyInfo.toASN1Primitive(), new DERSet(aSN1EncodableVector2)));
        }
        BEROctetString bEROctetString = new BEROctetString(new DERSequence(aSN1EncodableVector).getEncoded(ASN1Encoding.DER));
        byte[] bArr2 = new byte[20];
        this.random.nextBytes(bArr2);
        ASN1EncodableVector aSN1EncodableVector6 = new ASN1EncodableVector();
        AlgorithmIdentifier algorithmIdentifier = new AlgorithmIdentifier(this.certAlgorithm, new PKCS12PBEParams(bArr2, MIN_ITERATIONS).toASN1Primitive());
        ?? hashtable = new Hashtable();
        Enumeration enumerationKeys2 = this.keys.keys();
        while (enumerationKeys2.hasMoreElements()) {
            try {
                String str2 = (String) enumerationKeys2.nextElement();
                ?? EngineGetCertificate = engineGetCertificate(str2);
                CertBag certBag = new CertBag(x509Certificate, new DEROctetString(EngineGetCertificate.getEncoded()));
                ASN1EncodableVector aSN1EncodableVector7 = new ASN1EncodableVector();
                if (EngineGetCertificate instanceof PKCS12BagAttributeCarrier) {
                    PKCS12BagAttributeCarrier pKCS12BagAttributeCarrier2 = (PKCS12BagAttributeCarrier) EngineGetCertificate;
                    DERBMPString dERBMPString2 = (DERBMPString) pKCS12BagAttributeCarrier2.getBagAttribute(pkcs_9_at_friendlyName);
                    if (dERBMPString2 == null || !dERBMPString2.getString().equals(str2)) {
                        pKCS12BagAttributeCarrier2.setBagAttribute(pkcs_9_at_friendlyName, new DERBMPString(str2));
                    }
                    if (pKCS12BagAttributeCarrier2.getBagAttribute(pkcs_9_at_localKeyId) == null) {
                        pKCS12BagAttributeCarrier2.setBagAttribute(pkcs_9_at_localKeyId, createSubjectKeyId(EngineGetCertificate.getPublicKey()));
                    }
                    Enumeration bagAttributeKeys2 = pKCS12BagAttributeCarrier2.getBagAttributeKeys();
                    z3 = false;
                    while (bagAttributeKeys2.hasMoreElements()) {
                        ASN1ObjectIdentifier aSN1ObjectIdentifier2 = (ASN1ObjectIdentifier) bagAttributeKeys2.nextElement();
                        Enumeration enumeration3 = enumerationKeys2;
                        ASN1EncodableVector aSN1EncodableVector8 = new ASN1EncodableVector();
                        aSN1EncodableVector8.add(aSN1ObjectIdentifier2);
                        aSN1EncodableVector8.add(new DERSet(pKCS12BagAttributeCarrier2.getBagAttribute(aSN1ObjectIdentifier2)));
                        aSN1EncodableVector7.add(new DERSequence(aSN1EncodableVector8));
                        enumerationKeys2 = enumeration3;
                        bagAttributeKeys2 = bagAttributeKeys2;
                        z3 = true;
                    }
                    enumeration2 = enumerationKeys2;
                } else {
                    enumeration2 = enumerationKeys2;
                    z3 = false;
                }
                if (!z3) {
                    ASN1EncodableVector aSN1EncodableVector9 = new ASN1EncodableVector();
                    aSN1EncodableVector9.add(pkcs_9_at_localKeyId);
                    aSN1EncodableVector9.add(new DERSet(createSubjectKeyId(EngineGetCertificate.getPublicKey())));
                    aSN1EncodableVector7.add(new DERSequence(aSN1EncodableVector9));
                    ASN1EncodableVector aSN1EncodableVector10 = new ASN1EncodableVector();
                    aSN1EncodableVector10.add(pkcs_9_at_friendlyName);
                    aSN1EncodableVector10.add(new DERSet(new DERBMPString(str2)));
                    aSN1EncodableVector7.add(new DERSequence(aSN1EncodableVector10));
                }
                aSN1EncodableVector6.add(new SafeBag(certBag, certBag.toASN1Primitive(), new DERSet(aSN1EncodableVector7)));
                hashtable.put(EngineGetCertificate, EngineGetCertificate);
                enumerationKeys2 = enumeration2;
            } catch (CertificateEncodingException e) {
                throw new IOException("Error encoding certificate: " + e.toString());
            }
        }
        Enumeration enumerationKeys3 = this.certs.keys();
        while (enumerationKeys3.hasMoreElements()) {
            try {
                String str3 = (String) enumerationKeys3.nextElement();
                ?? r5 = (Certificate) this.certs.get(str3);
                if (this.keys.get(str3) == null) {
                    CertBag certBag2 = new CertBag(x509Certificate, new DEROctetString(r5.getEncoded()));
                    ASN1EncodableVector aSN1EncodableVector11 = new ASN1EncodableVector();
                    if (r5 instanceof PKCS12BagAttributeCarrier) {
                        PKCS12BagAttributeCarrier pKCS12BagAttributeCarrier3 = (PKCS12BagAttributeCarrier) r5;
                        DERBMPString dERBMPString3 = (DERBMPString) pKCS12BagAttributeCarrier3.getBagAttribute(pkcs_9_at_friendlyName);
                        if (dERBMPString3 == null || !dERBMPString3.getString().equals(str3)) {
                            pKCS12BagAttributeCarrier3.setBagAttribute(pkcs_9_at_friendlyName, new DERBMPString(str3));
                        }
                        Enumeration bagAttributeKeys3 = pKCS12BagAttributeCarrier3.getBagAttributeKeys();
                        z2 = false;
                        while (bagAttributeKeys3.hasMoreElements()) {
                            Enumeration enumeration4 = enumerationKeys3;
                            ASN1ObjectIdentifier aSN1ObjectIdentifier3 = (ASN1ObjectIdentifier) bagAttributeKeys3.nextElement();
                            Enumeration enumeration5 = bagAttributeKeys3;
                            if (aSN1ObjectIdentifier3.equals(PKCSObjectIdentifiers.pkcs_9_at_localKeyId)) {
                                enumerationKeys3 = enumeration4;
                                bagAttributeKeys3 = enumeration5;
                            } else {
                                ASN1EncodableVector aSN1EncodableVector12 = new ASN1EncodableVector();
                                aSN1EncodableVector12.add(aSN1ObjectIdentifier3);
                                aSN1EncodableVector12.add(new DERSet(pKCS12BagAttributeCarrier3.getBagAttribute(aSN1ObjectIdentifier3)));
                                aSN1EncodableVector11.add(new DERSequence(aSN1EncodableVector12));
                                enumerationKeys3 = enumeration4;
                                bagAttributeKeys3 = enumeration5;
                                z2 = true;
                            }
                        }
                        enumeration = enumerationKeys3;
                    } else {
                        enumeration = enumerationKeys3;
                        z2 = false;
                    }
                    if (!z2) {
                        ASN1EncodableVector aSN1EncodableVector13 = new ASN1EncodableVector();
                        aSN1EncodableVector13.add(pkcs_9_at_friendlyName);
                        aSN1EncodableVector13.add(new DERSet(new DERBMPString(str3)));
                        aSN1EncodableVector11.add(new DERSequence(aSN1EncodableVector13));
                    }
                    aSN1EncodableVector6.add(new SafeBag(certBag, certBag2.toASN1Primitive(), new DERSet(aSN1EncodableVector11)));
                    hashtable.put(r5, r5);
                    enumerationKeys3 = enumeration;
                }
            } catch (CertificateEncodingException e2) {
                throw new IOException("Error encoding certificate: " + e2.toString());
            }
        }
        ?? usedCertificateSet = getUsedCertificateSet();
        Enumeration enumerationKeys4 = this.chainCerts.keys();
        while (enumerationKeys4.hasMoreElements()) {
            try {
                ?? r52 = (Certificate) this.chainCerts.get((CertId) enumerationKeys4.nextElement());
                if (usedCertificateSet.contains(r52) && hashtable.get(r52) == null) {
                    CertBag certBag3 = new CertBag(x509Certificate, new DEROctetString(r52.getEncoded()));
                    ASN1EncodableVector aSN1EncodableVector14 = new ASN1EncodableVector();
                    if (r52 instanceof PKCS12BagAttributeCarrier) {
                        PKCS12BagAttributeCarrier pKCS12BagAttributeCarrier4 = (PKCS12BagAttributeCarrier) r52;
                        Enumeration bagAttributeKeys4 = pKCS12BagAttributeCarrier4.getBagAttributeKeys();
                        while (bagAttributeKeys4.hasMoreElements()) {
                            ASN1ObjectIdentifier aSN1ObjectIdentifier4 = (ASN1ObjectIdentifier) bagAttributeKeys4.nextElement();
                            if (!aSN1ObjectIdentifier4.equals(PKCSObjectIdentifiers.pkcs_9_at_localKeyId)) {
                                ASN1EncodableVector aSN1EncodableVector15 = new ASN1EncodableVector();
                                aSN1EncodableVector15.add(aSN1ObjectIdentifier4);
                                aSN1EncodableVector15.add(new DERSet(pKCS12BagAttributeCarrier4.getBagAttribute(aSN1ObjectIdentifier4)));
                                aSN1EncodableVector14.add(new DERSequence(aSN1EncodableVector15));
                                hashtable = hashtable;
                            }
                        }
                    }
                    ?? r20 = hashtable;
                    aSN1EncodableVector6.add(new SafeBag(certBag, certBag3.toASN1Primitive(), new DERSet(aSN1EncodableVector14)));
                    hashtable = r20;
                }
            } catch (CertificateEncodingException e3) {
                throw new IOException("Error encoding certificate: " + e3.toString());
            }
        }
        AuthenticatedSafe authenticatedSafe = new AuthenticatedSafe(new ContentInfo[]{new ContentInfo(data, bEROctetString), new ContentInfo(encryptedData, new EncryptedData(data, algorithmIdentifier, new BEROctetString(cryptData(true, algorithmIdentifier, cArr, false, new DERSequence(aSN1EncodableVector6).getEncoded(ASN1Encoding.DER)))).toASN1Primitive())});
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (z) {
            bEROutputStream = new DEROutputStream(byteArrayOutputStream);
        } else {
            bEROutputStream = new BEROutputStream(byteArrayOutputStream);
        }
        bEROutputStream.writeObject(authenticatedSafe);
        ContentInfo contentInfo = new ContentInfo(data, new BEROctetString(byteArrayOutputStream.toByteArray()));
        byte[] bArr3 = new byte[20];
        this.random.nextBytes(bArr3);
        try {
            Pfx pfx = new Pfx(contentInfo, new MacData(new DigestInfo(new AlgorithmIdentifier(id_SHA1, DERNull.INSTANCE), calculatePbeMac(id_SHA1, bArr3, MIN_ITERATIONS, cArr, false, ((ASN1OctetString) contentInfo.getContent()).getOctets())), bArr3, MIN_ITERATIONS));
            if (z) {
                bEROutputStream2 = new DEROutputStream(outputStream);
            } else {
                bEROutputStream2 = new BEROutputStream(outputStream);
            }
            bEROutputStream2.writeObject(pfx);
        } catch (Exception e4) {
            throw new IOException("error constructing MAC: " + e4.toString());
        }
    }

    private Set getUsedCertificateSet() {
        HashSet hashSet = new HashSet();
        Enumeration enumerationKeys = this.keys.keys();
        while (enumerationKeys.hasMoreElements()) {
            Certificate[] certificateArrEngineGetCertificateChain = engineGetCertificateChain((String) enumerationKeys.nextElement());
            for (int i = 0; i != certificateArrEngineGetCertificateChain.length; i++) {
                hashSet.add(certificateArrEngineGetCertificateChain[i]);
            }
        }
        Enumeration enumerationKeys2 = this.certs.keys();
        while (enumerationKeys2.hasMoreElements()) {
            hashSet.add(engineGetCertificate((String) enumerationKeys2.nextElement()));
        }
        return hashSet;
    }

    private byte[] calculatePbeMac(ASN1ObjectIdentifier aSN1ObjectIdentifier, byte[] bArr, int i, char[] cArr, boolean z, byte[] bArr2) throws Exception {
        PBEParameterSpec pBEParameterSpec = new PBEParameterSpec(bArr, i);
        Mac macCreateMac = this.helper.createMac(aSN1ObjectIdentifier.getId());
        macCreateMac.init(new PKCS12Key(cArr, z), pBEParameterSpec);
        macCreateMac.update(bArr2);
        return macCreateMac.doFinal();
    }

    public static class BCPKCS12KeyStore extends PKCS12KeyStoreSpi {
        public BCPKCS12KeyStore() {
            super(new BouncyCastleProvider(), pbeWithSHAAnd3_KeyTripleDES_CBC, pbeWithSHAAnd40BitRC2_CBC);
        }
    }

    private static class IgnoresCaseHashtable {
        private Hashtable keys;
        private Hashtable orig;

        private IgnoresCaseHashtable() {
            this.orig = new Hashtable();
            this.keys = new Hashtable();
        }

        public void put(String str, Object obj) {
            String lowerCase = str == null ? null : Strings.toLowerCase(str);
            String str2 = (String) this.keys.get(lowerCase);
            if (str2 != null) {
                this.orig.remove(str2);
            }
            this.keys.put(lowerCase, str);
            this.orig.put(str, obj);
        }

        public Enumeration keys() {
            return this.orig.keys();
        }

        public Object remove(String str) {
            String str2 = (String) this.keys.remove(str == null ? null : Strings.toLowerCase(str));
            if (str2 == null) {
                return null;
            }
            return this.orig.remove(str2);
        }

        public Object get(String str) {
            String str2 = (String) this.keys.get(str == null ? null : Strings.toLowerCase(str));
            if (str2 == null) {
                return null;
            }
            return this.orig.get(str2);
        }

        public Enumeration elements() {
            return this.orig.elements();
        }
    }

    private static class DefaultSecretKeyProvider {
        private final Map KEY_SIZES;

        DefaultSecretKeyProvider() {
            HashMap map = new HashMap();
            map.put(new ASN1ObjectIdentifier("1.2.840.113533.7.66.10"), Integers.valueOf(128));
            map.put(PKCSObjectIdentifiers.des_EDE3_CBC, Integers.valueOf(192));
            map.put(NISTObjectIdentifiers.id_aes128_CBC, Integers.valueOf(128));
            map.put(NISTObjectIdentifiers.id_aes192_CBC, Integers.valueOf(192));
            map.put(NISTObjectIdentifiers.id_aes256_CBC, Integers.valueOf(256));
            map.put(NTTObjectIdentifiers.id_camellia128_cbc, Integers.valueOf(128));
            map.put(NTTObjectIdentifiers.id_camellia192_cbc, Integers.valueOf(192));
            map.put(NTTObjectIdentifiers.id_camellia256_cbc, Integers.valueOf(256));
            this.KEY_SIZES = Collections.unmodifiableMap(map);
        }

        public int getKeySize(AlgorithmIdentifier algorithmIdentifier) {
            Integer num = (Integer) this.KEY_SIZES.get(algorithmIdentifier.getAlgorithm());
            if (num != null) {
                return num.intValue();
            }
            return -1;
        }
    }
}
