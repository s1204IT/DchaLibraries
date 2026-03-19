package sun.security.pkcs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.CryptoPrimitive;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.Timestamp;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import sun.misc.HexDumpEncoder;
import sun.security.timestamp.TimestampToken;
import sun.security.util.Debug;
import sun.security.util.DerEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.DisabledAlgorithmConstraints;
import sun.security.util.KeyUtil;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;
import sun.security.x509.KeyUsageExtension;
import sun.security.x509.X500Name;

public class SignerInfo implements DerEncoder {
    PKCS9Attributes authenticatedAttributes;
    BigInteger certificateSerialNumber;
    AlgorithmId digestAlgorithmId;
    AlgorithmId digestEncryptionAlgorithmId;
    byte[] encryptedDigest;
    private boolean hasTimestamp;
    X500Name issuerName;
    Timestamp timestamp;
    PKCS9Attributes unauthenticatedAttributes;
    BigInteger version;
    private static final Set<CryptoPrimitive> DIGEST_PRIMITIVE_SET = Collections.unmodifiableSet(EnumSet.of(CryptoPrimitive.MESSAGE_DIGEST));
    private static final Set<CryptoPrimitive> SIG_PRIMITIVE_SET = Collections.unmodifiableSet(EnumSet.of(CryptoPrimitive.SIGNATURE));
    private static final DisabledAlgorithmConstraints JAR_DISABLED_CHECK = new DisabledAlgorithmConstraints(DisabledAlgorithmConstraints.PROPERTY_JAR_DISABLED_ALGS);

    public SignerInfo(X500Name x500Name, BigInteger bigInteger, AlgorithmId algorithmId, AlgorithmId algorithmId2, byte[] bArr) {
        this.hasTimestamp = true;
        this.version = BigInteger.ONE;
        this.issuerName = x500Name;
        this.certificateSerialNumber = bigInteger;
        this.digestAlgorithmId = algorithmId;
        this.digestEncryptionAlgorithmId = algorithmId2;
        this.encryptedDigest = bArr;
    }

    public SignerInfo(X500Name x500Name, BigInteger bigInteger, AlgorithmId algorithmId, PKCS9Attributes pKCS9Attributes, AlgorithmId algorithmId2, byte[] bArr, PKCS9Attributes pKCS9Attributes2) {
        this.hasTimestamp = true;
        this.version = BigInteger.ONE;
        this.issuerName = x500Name;
        this.certificateSerialNumber = bigInteger;
        this.digestAlgorithmId = algorithmId;
        this.authenticatedAttributes = pKCS9Attributes;
        this.digestEncryptionAlgorithmId = algorithmId2;
        this.encryptedDigest = bArr;
        this.unauthenticatedAttributes = pKCS9Attributes2;
    }

    public SignerInfo(DerInputStream derInputStream) throws IOException {
        this(derInputStream, false);
    }

    public SignerInfo(DerInputStream derInputStream, boolean z) throws IOException {
        this.hasTimestamp = true;
        this.version = derInputStream.getBigInteger();
        DerValue[] sequence = derInputStream.getSequence(2);
        this.issuerName = new X500Name(new DerValue((byte) 48, sequence[0].toByteArray()));
        this.certificateSerialNumber = sequence[1].getBigInteger();
        this.digestAlgorithmId = AlgorithmId.parse(derInputStream.getDerValue());
        if (z) {
            derInputStream.getSet(0);
        } else if (((byte) derInputStream.peekByte()) == -96) {
            this.authenticatedAttributes = new PKCS9Attributes(derInputStream);
        }
        this.digestEncryptionAlgorithmId = AlgorithmId.parse(derInputStream.getDerValue());
        this.encryptedDigest = derInputStream.getOctetString();
        if (z) {
            derInputStream.getSet(0);
        } else if (derInputStream.available() != 0 && ((byte) derInputStream.peekByte()) == -95) {
            this.unauthenticatedAttributes = new PKCS9Attributes(derInputStream, true);
        }
        if (derInputStream.available() != 0) {
            throw new ParsingException("extra data at the end");
        }
    }

    public void encode(DerOutputStream derOutputStream) throws IOException {
        derEncode(derOutputStream);
    }

    @Override
    public void derEncode(OutputStream outputStream) throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        derOutputStream.putInteger(this.version);
        DerOutputStream derOutputStream2 = new DerOutputStream();
        this.issuerName.encode(derOutputStream2);
        derOutputStream2.putInteger(this.certificateSerialNumber);
        derOutputStream.write((byte) 48, derOutputStream2);
        this.digestAlgorithmId.encode(derOutputStream);
        if (this.authenticatedAttributes != null) {
            this.authenticatedAttributes.encode((byte) -96, derOutputStream);
        }
        this.digestEncryptionAlgorithmId.encode(derOutputStream);
        derOutputStream.putOctetString(this.encryptedDigest);
        if (this.unauthenticatedAttributes != null) {
            this.unauthenticatedAttributes.encode((byte) -95, derOutputStream);
        }
        DerOutputStream derOutputStream3 = new DerOutputStream();
        derOutputStream3.write((byte) 48, derOutputStream);
        outputStream.write(derOutputStream3.toByteArray());
    }

    public X509Certificate getCertificate(PKCS7 pkcs7) throws IOException {
        return pkcs7.getCertificate(this.certificateSerialNumber, this.issuerName);
    }

    public ArrayList<X509Certificate> getCertificateChain(PKCS7 pkcs7) throws IOException {
        boolean z;
        X509Certificate certificate = pkcs7.getCertificate(this.certificateSerialNumber, this.issuerName);
        if (certificate == null) {
            return null;
        }
        ArrayList<X509Certificate> arrayList = new ArrayList<>();
        arrayList.add(certificate);
        X509Certificate[] certificates = pkcs7.getCertificates();
        if (certificates == null || certificate.getSubjectDN().equals(certificate.getIssuerDN())) {
            return arrayList;
        }
        Principal issuerDN = certificate.getIssuerDN();
        int length = 0;
        do {
            int i = length;
            while (true) {
                if (i < certificates.length) {
                    if (issuerDN.equals(certificates[i].getSubjectDN())) {
                        arrayList.add(certificates[i]);
                        if (certificates[i].getSubjectDN().equals(certificates[i].getIssuerDN())) {
                            length = certificates.length;
                        } else {
                            issuerDN = certificates[i].getIssuerDN();
                            X509Certificate x509Certificate = certificates[length];
                            certificates[length] = certificates[i];
                            certificates[i] = x509Certificate;
                            length++;
                        }
                        z = true;
                    } else {
                        i++;
                    }
                } else {
                    z = false;
                    break;
                }
            }
        } while (z);
        return arrayList;
    }

    SignerInfo verify(PKCS7 pkcs7, byte[] bArr) throws NoSuchAlgorithmException, SignatureException {
        try {
            return verify(pkcs7, new ByteArrayInputStream(bArr));
        } catch (IOException e) {
            return null;
        }
    }

    SignerInfo verify(PKCS7 pkcs7, InputStream inputStream) throws NoSuchAlgorithmException, SignatureException, IOException {
        byte[] bArr;
        try {
            try {
                ContentInfo contentInfo = pkcs7.getContentInfo();
                if (inputStream == null) {
                    inputStream = new ByteArrayInputStream(contentInfo.getContentBytes());
                }
                String name = getDigestAlgorithmId().getName();
                if (this.authenticatedAttributes != null) {
                    ObjectIdentifier objectIdentifier = (ObjectIdentifier) this.authenticatedAttributes.getAttributeValue(PKCS9Attribute.CONTENT_TYPE_OID);
                    if (objectIdentifier == null || !objectIdentifier.equals((Object) contentInfo.contentType) || (bArr = (byte[]) this.authenticatedAttributes.getAttributeValue(PKCS9Attribute.MESSAGE_DIGEST_OID)) == null) {
                        return null;
                    }
                    if (!JAR_DISABLED_CHECK.permits(DIGEST_PRIMITIVE_SET, name, null)) {
                        throw new SignatureException("Digest check failed. Disabled algorithm used: " + name);
                    }
                    MessageDigest messageDigest = MessageDigest.getInstance(name);
                    byte[] bArr2 = new byte[4096];
                    while (true) {
                        int i = inputStream.read(bArr2);
                        if (i == -1) {
                            break;
                        }
                        messageDigest.update(bArr2, 0, i);
                    }
                    byte[] bArrDigest = messageDigest.digest();
                    if (bArr.length != bArrDigest.length) {
                        return null;
                    }
                    for (int i2 = 0; i2 < bArr.length; i2++) {
                        if (bArr[i2] != bArrDigest[i2]) {
                            return null;
                        }
                    }
                    inputStream = new ByteArrayInputStream(this.authenticatedAttributes.getDerEncoding());
                }
                String name2 = getDigestEncryptionAlgorithmId().getName();
                String encAlgFromSigAlg = AlgorithmId.getEncAlgFromSigAlg(name2);
                if (encAlgFromSigAlg != null) {
                    name2 = encAlgFromSigAlg;
                }
                String strMakeSigAlg = AlgorithmId.makeSigAlg(name, name2);
                if (!JAR_DISABLED_CHECK.permits(SIG_PRIMITIVE_SET, strMakeSigAlg, null)) {
                    throw new SignatureException("Signature check failed. Disabled algorithm used: " + strMakeSigAlg);
                }
                X509Certificate certificate = getCertificate(pkcs7);
                PublicKey publicKey = certificate.getPublicKey();
                if (certificate == null) {
                    return null;
                }
                if (!JAR_DISABLED_CHECK.permits(SIG_PRIMITIVE_SET, publicKey)) {
                    throw new SignatureException("Public key check failed. Disabled key used: " + KeyUtil.getKeySize(publicKey) + " bit " + publicKey.getAlgorithm());
                }
                if (certificate.hasUnsupportedCriticalExtension()) {
                    throw new SignatureException("Certificate has unsupported critical extension(s)");
                }
                boolean[] keyUsage = certificate.getKeyUsage();
                if (keyUsage != null) {
                    try {
                        KeyUsageExtension keyUsageExtension = new KeyUsageExtension(keyUsage);
                        boolean zBooleanValue = keyUsageExtension.get(KeyUsageExtension.DIGITAL_SIGNATURE).booleanValue();
                        boolean zBooleanValue2 = keyUsageExtension.get(KeyUsageExtension.NON_REPUDIATION).booleanValue();
                        if (!zBooleanValue && !zBooleanValue2) {
                            throw new SignatureException("Key usage restricted: cannot be used for digital signatures");
                        }
                    } catch (IOException e) {
                        throw new SignatureException("Failed to parse keyUsage extension");
                    }
                }
                Signature signature = Signature.getInstance(strMakeSigAlg);
                signature.initVerify(publicKey);
                byte[] bArr3 = new byte[4096];
                while (true) {
                    int i3 = inputStream.read(bArr3);
                    if (i3 == -1) {
                        break;
                    }
                    signature.update(bArr3, 0, i3);
                }
                if (signature.verify(this.encryptedDigest)) {
                    return this;
                }
                return null;
            } catch (IOException e2) {
                throw new SignatureException("IO error verifying signature:\n" + e2.getMessage());
            }
        } catch (InvalidKeyException e3) {
            throw new SignatureException("InvalidKey: " + e3.getMessage());
        }
    }

    SignerInfo verify(PKCS7 pkcs7) throws NoSuchAlgorithmException, SignatureException {
        return verify(pkcs7, (byte[]) null);
    }

    public BigInteger getVersion() {
        return this.version;
    }

    public X500Name getIssuerName() {
        return this.issuerName;
    }

    public BigInteger getCertificateSerialNumber() {
        return this.certificateSerialNumber;
    }

    public AlgorithmId getDigestAlgorithmId() {
        return this.digestAlgorithmId;
    }

    public PKCS9Attributes getAuthenticatedAttributes() {
        return this.authenticatedAttributes;
    }

    public AlgorithmId getDigestEncryptionAlgorithmId() {
        return this.digestEncryptionAlgorithmId;
    }

    public byte[] getEncryptedDigest() {
        return this.encryptedDigest;
    }

    public PKCS9Attributes getUnauthenticatedAttributes() {
        return this.unauthenticatedAttributes;
    }

    public PKCS7 getTsToken() throws IOException {
        PKCS9Attribute attribute;
        if (this.unauthenticatedAttributes == null || (attribute = this.unauthenticatedAttributes.getAttribute(PKCS9Attribute.SIGNATURE_TIMESTAMP_TOKEN_OID)) == null) {
            return null;
        }
        return new PKCS7((byte[]) attribute.getValue());
    }

    public Timestamp getTimestamp() throws NoSuchAlgorithmException, SignatureException, IOException, CertificateException {
        if (this.timestamp != null || !this.hasTimestamp) {
            return this.timestamp;
        }
        PKCS7 tsToken = getTsToken();
        if (tsToken == null) {
            this.hasTimestamp = false;
            return null;
        }
        byte[] data = tsToken.getContentInfo().getData();
        CertPath certPathGenerateCertPath = CertificateFactory.getInstance("X.509").generateCertPath(tsToken.verify(data)[0].getCertificateChain(tsToken));
        TimestampToken timestampToken = new TimestampToken(data);
        verifyTimestamp(timestampToken);
        this.timestamp = new Timestamp(timestampToken.getDate(), certPathGenerateCertPath);
        return this.timestamp;
    }

    private void verifyTimestamp(TimestampToken timestampToken) throws NoSuchAlgorithmException, SignatureException {
        String name = timestampToken.getHashAlgorithm().getName();
        if (!JAR_DISABLED_CHECK.permits(DIGEST_PRIMITIVE_SET, name, null)) {
            throw new SignatureException("Timestamp token digest check failed. Disabled algorithm used: " + name);
        }
        if (!Arrays.equals(timestampToken.getHashedMessage(), MessageDigest.getInstance(name).digest(this.encryptedDigest))) {
            throw new SignatureException("Signature timestamp (#" + ((Object) timestampToken.getSerialNumber()) + ") generated on " + ((Object) timestampToken.getDate()) + " is inapplicable");
        }
    }

    public String toString() {
        HexDumpEncoder hexDumpEncoder = new HexDumpEncoder();
        String str = ((("Signer Info for (issuer): " + ((Object) this.issuerName) + "\n") + "\tversion: " + Debug.toHexString(this.version) + "\n") + "\tcertificateSerialNumber: " + Debug.toHexString(this.certificateSerialNumber) + "\n") + "\tdigestAlgorithmId: " + ((Object) this.digestAlgorithmId) + "\n";
        if (this.authenticatedAttributes != null) {
            str = str + "\tauthenticatedAttributes: " + ((Object) this.authenticatedAttributes) + "\n";
        }
        String str2 = (str + "\tdigestEncryptionAlgorithmId: " + ((Object) this.digestEncryptionAlgorithmId) + "\n") + "\tencryptedDigest: \n" + hexDumpEncoder.encodeBuffer(this.encryptedDigest) + "\n";
        if (this.unauthenticatedAttributes != null) {
            return str2 + "\tunauthenticatedAttributes: " + ((Object) this.unauthenticatedAttributes) + "\n";
        }
        return str2;
    }
}
