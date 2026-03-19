package sun.security.pkcs;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.security.auth.x500.X500Principal;
import sun.security.util.Debug;
import sun.security.util.DerEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;
import sun.security.x509.X509CRLImpl;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

public class PKCS7 {
    private Principal[] certIssuerNames;
    private X509Certificate[] certificates;
    private ContentInfo contentInfo;
    private ObjectIdentifier contentType;
    private X509CRL[] crls;
    private AlgorithmId[] digestAlgorithmIds;
    private boolean oldStyle;
    private SignerInfo[] signerInfos;
    private BigInteger version;

    public PKCS7(InputStream inputStream) throws Throwable {
        this.version = null;
        this.digestAlgorithmIds = null;
        this.contentInfo = null;
        this.certificates = null;
        this.crls = null;
        this.signerInfos = null;
        this.oldStyle = false;
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        byte[] bArr = new byte[dataInputStream.available()];
        dataInputStream.readFully(bArr);
        parse(new DerInputStream(bArr));
    }

    public PKCS7(DerInputStream derInputStream) throws Throwable {
        this.version = null;
        this.digestAlgorithmIds = null;
        this.contentInfo = null;
        this.certificates = null;
        this.crls = null;
        this.signerInfos = null;
        this.oldStyle = false;
        parse(derInputStream);
    }

    public PKCS7(byte[] bArr) throws Throwable {
        this.version = null;
        this.digestAlgorithmIds = null;
        this.contentInfo = null;
        this.certificates = null;
        this.crls = null;
        this.signerInfos = null;
        this.oldStyle = false;
        try {
            parse(new DerInputStream(bArr));
        } catch (IOException e) {
            ParsingException parsingException = new ParsingException("Unable to parse the encoded bytes");
            parsingException.initCause(e);
            throw parsingException;
        }
    }

    private void parse(DerInputStream derInputStream) throws Throwable {
        try {
            derInputStream.mark(derInputStream.available());
            parse(derInputStream, false);
        } catch (IOException e) {
            try {
                derInputStream.reset();
                parse(derInputStream, true);
                this.oldStyle = true;
            } catch (IOException e2) {
                ParsingException parsingException = new ParsingException(e2.getMessage());
                parsingException.initCause(e);
                parsingException.addSuppressed(e2);
                throw parsingException;
            }
        }
    }

    private void parse(DerInputStream derInputStream, boolean z) throws Throwable {
        this.contentInfo = new ContentInfo(derInputStream, z);
        this.contentType = this.contentInfo.contentType;
        DerValue content = this.contentInfo.getContent();
        if (this.contentType.equals((Object) ContentInfo.SIGNED_DATA_OID)) {
            parseSignedData(content);
            return;
        }
        if (this.contentType.equals((Object) ContentInfo.OLD_SIGNED_DATA_OID)) {
            parseOldSignedData(content);
            return;
        }
        if (this.contentType.equals((Object) ContentInfo.NETSCAPE_CERT_SEQUENCE_OID)) {
            parseNetscapeCertChain(content);
            return;
        }
        throw new ParsingException("content type " + ((Object) this.contentType) + " not supported.");
    }

    public PKCS7(AlgorithmId[] algorithmIdArr, ContentInfo contentInfo, X509Certificate[] x509CertificateArr, X509CRL[] x509crlArr, SignerInfo[] signerInfoArr) {
        this.version = null;
        this.digestAlgorithmIds = null;
        this.contentInfo = null;
        this.certificates = null;
        this.crls = null;
        this.signerInfos = null;
        this.oldStyle = false;
        this.version = BigInteger.ONE;
        this.digestAlgorithmIds = algorithmIdArr;
        this.contentInfo = contentInfo;
        this.certificates = x509CertificateArr;
        this.crls = x509crlArr;
        this.signerInfos = signerInfoArr;
    }

    public PKCS7(AlgorithmId[] algorithmIdArr, ContentInfo contentInfo, X509Certificate[] x509CertificateArr, SignerInfo[] signerInfoArr) {
        this(algorithmIdArr, contentInfo, x509CertificateArr, null, signerInfoArr);
    }

    private void parseNetscapeCertChain(DerValue derValue) throws Throwable {
        CertificateFactory certificateFactory;
        DerValue[] sequence = new DerInputStream(derValue.toByteArray()).getSequence(2, true);
        this.certificates = new X509Certificate[sequence.length];
        ByteArrayInputStream byteArrayInputStream = null;
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            certificateFactory = null;
        }
        for (int i = 0; i < sequence.length; i++) {
            try {
                try {
                    byte[] originalEncodedForm = sequence[i].getOriginalEncodedForm();
                    if (certificateFactory == null) {
                        this.certificates[i] = new X509CertImpl(sequence[i], originalEncodedForm);
                    } else {
                        ByteArrayInputStream byteArrayInputStream2 = new ByteArrayInputStream(originalEncodedForm);
                        try {
                            this.certificates[i] = new VerbatimX509Certificate((X509Certificate) certificateFactory.generateCertificate(byteArrayInputStream2), originalEncodedForm);
                            byteArrayInputStream2.close();
                        } catch (IOException e2) {
                            e = e2;
                            ParsingException parsingException = new ParsingException(e.getMessage());
                            parsingException.initCause(e);
                            throw parsingException;
                        } catch (CertificateException e3) {
                            e = e3;
                            ParsingException parsingException2 = new ParsingException(e.getMessage());
                            parsingException2.initCause(e);
                            throw parsingException2;
                        } catch (Throwable th) {
                            th = th;
                            byteArrayInputStream = byteArrayInputStream2;
                            if (byteArrayInputStream != null) {
                                byteArrayInputStream.close();
                            }
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (IOException e4) {
                e = e4;
            } catch (CertificateException e5) {
                e = e5;
            }
        }
    }

    private void parseSignedData(DerValue derValue) throws Throwable {
        CertificateFactory certificateFactory;
        Throwable th;
        DerInputStream derInputStream = derValue.toDerInputStream();
        this.version = derInputStream.getBigInteger();
        DerValue[] set = derInputStream.getSet(1);
        int length = set.length;
        this.digestAlgorithmIds = new AlgorithmId[length];
        for (int i = 0; i < length; i++) {
            try {
                this.digestAlgorithmIds[i] = AlgorithmId.parse(set[i]);
            } catch (IOException e) {
                ParsingException parsingException = new ParsingException("Error parsing digest AlgorithmId IDs: " + e.getMessage());
                parsingException.initCause(e);
                throw parsingException;
            }
        }
        this.contentInfo = new ContentInfo(derInputStream);
        ByteArrayInputStream byteArrayInputStream = null;
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e2) {
            certificateFactory = null;
        }
        if (((byte) derInputStream.peekByte()) == -96) {
            DerValue[] set2 = derInputStream.getSet(2, true, true);
            int length2 = set2.length;
            this.certificates = new X509Certificate[length2];
            int i2 = 0;
            for (int i3 = 0; i3 < length2; i3++) {
                try {
                    try {
                        if (set2[i3].getTag() == 48) {
                            byte[] originalEncodedForm = set2[i3].getOriginalEncodedForm();
                            if (certificateFactory == null) {
                                this.certificates[i2] = new X509CertImpl(set2[i3], originalEncodedForm);
                            } else {
                                ByteArrayInputStream byteArrayInputStream2 = new ByteArrayInputStream(originalEncodedForm);
                                try {
                                    this.certificates[i2] = new VerbatimX509Certificate((X509Certificate) certificateFactory.generateCertificate(byteArrayInputStream2), originalEncodedForm);
                                    byteArrayInputStream2.close();
                                } catch (IOException e3) {
                                    e = e3;
                                    ParsingException parsingException2 = new ParsingException(e.getMessage());
                                    parsingException2.initCause(e);
                                    throw parsingException2;
                                } catch (CertificateException e4) {
                                    e = e4;
                                    ParsingException parsingException3 = new ParsingException(e.getMessage());
                                    parsingException3.initCause(e);
                                    throw parsingException3;
                                } catch (Throwable th2) {
                                    th = th2;
                                    byteArrayInputStream = byteArrayInputStream2;
                                    if (byteArrayInputStream != null) {
                                        byteArrayInputStream.close();
                                    }
                                    throw th;
                                }
                            }
                            i2++;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                    }
                } catch (IOException e5) {
                    e = e5;
                } catch (CertificateException e6) {
                    e = e6;
                }
            }
            if (i2 != length2) {
                this.certificates = (X509Certificate[]) Arrays.copyOf(this.certificates, i2);
            }
        }
        if (((byte) derInputStream.peekByte()) == -95) {
            DerValue[] set3 = derInputStream.getSet(1, true);
            int length3 = set3.length;
            this.crls = new X509CRL[length3];
            for (int i4 = 0; i4 < length3; i4++) {
                if (certificateFactory == null) {
                    try {
                        try {
                            this.crls[i4] = new X509CRLImpl(set3[i4]);
                        } catch (CRLException e7) {
                            e = e7;
                            ParsingException parsingException4 = new ParsingException(e.getMessage());
                            parsingException4.initCause(e);
                            throw parsingException4;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        if (byteArrayInputStream != null) {
                            byteArrayInputStream.close();
                        }
                        throw th;
                    }
                } else {
                    ByteArrayInputStream byteArrayInputStream3 = new ByteArrayInputStream(set3[i4].toByteArray());
                    try {
                        this.crls[i4] = (X509CRL) certificateFactory.generateCRL(byteArrayInputStream3);
                        byteArrayInputStream3.close();
                    } catch (CRLException e8) {
                        e = e8;
                        byteArrayInputStream = byteArrayInputStream3;
                        ParsingException parsingException42 = new ParsingException(e.getMessage());
                        parsingException42.initCause(e);
                        throw parsingException42;
                    } catch (Throwable th5) {
                        th = th5;
                        byteArrayInputStream = byteArrayInputStream3;
                        if (byteArrayInputStream != null) {
                        }
                        throw th;
                    }
                }
            }
        }
        DerValue[] set4 = derInputStream.getSet(1);
        int length4 = set4.length;
        this.signerInfos = new SignerInfo[length4];
        for (int i5 = 0; i5 < length4; i5++) {
            this.signerInfos[i5] = new SignerInfo(set4[i5].toDerInputStream());
        }
    }

    private void parseOldSignedData(DerValue derValue) throws Throwable {
        CertificateFactory certificateFactory;
        DerInputStream derInputStream = derValue.toDerInputStream();
        this.version = derInputStream.getBigInteger();
        DerValue[] set = derInputStream.getSet(1);
        int length = set.length;
        this.digestAlgorithmIds = new AlgorithmId[length];
        for (int i = 0; i < length; i++) {
            try {
                this.digestAlgorithmIds[i] = AlgorithmId.parse(set[i]);
            } catch (IOException e) {
                throw new ParsingException("Error parsing digest AlgorithmId IDs");
            }
        }
        this.contentInfo = new ContentInfo(derInputStream, true);
        ByteArrayInputStream byteArrayInputStream = null;
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e2) {
            certificateFactory = null;
        }
        DerValue[] set2 = derInputStream.getSet(2, false, true);
        int length2 = set2.length;
        this.certificates = new X509Certificate[length2];
        for (int i2 = 0; i2 < length2; i2++) {
            try {
                try {
                    byte[] originalEncodedForm = set2[i2].getOriginalEncodedForm();
                    if (certificateFactory == null) {
                        this.certificates[i2] = new X509CertImpl(set2[i2], originalEncodedForm);
                    } else {
                        ByteArrayInputStream byteArrayInputStream2 = new ByteArrayInputStream(originalEncodedForm);
                        try {
                            this.certificates[i2] = new VerbatimX509Certificate((X509Certificate) certificateFactory.generateCertificate(byteArrayInputStream2), originalEncodedForm);
                            byteArrayInputStream2.close();
                        } catch (IOException e3) {
                            e = e3;
                            ParsingException parsingException = new ParsingException(e.getMessage());
                            parsingException.initCause(e);
                            throw parsingException;
                        } catch (CertificateException e4) {
                            e = e4;
                            ParsingException parsingException2 = new ParsingException(e.getMessage());
                            parsingException2.initCause(e);
                            throw parsingException2;
                        } catch (Throwable th) {
                            th = th;
                            byteArrayInputStream = byteArrayInputStream2;
                            if (byteArrayInputStream != null) {
                                byteArrayInputStream.close();
                            }
                            throw th;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (IOException e5) {
                e = e5;
            } catch (CertificateException e6) {
                e = e6;
            }
        }
        derInputStream.getSet(0);
        DerValue[] set3 = derInputStream.getSet(1);
        int length3 = set3.length;
        this.signerInfos = new SignerInfo[length3];
        for (int i3 = 0; i3 < length3; i3++) {
            this.signerInfos[i3] = new SignerInfo(set3[i3].toDerInputStream(), true);
        }
    }

    public void encodeSignedData(OutputStream outputStream) throws IOException {
        DerOutputStream derOutputStream = new DerOutputStream();
        encodeSignedData(derOutputStream);
        outputStream.write(derOutputStream.toByteArray());
    }

    public void encodeSignedData(DerOutputStream derOutputStream) throws IOException {
        DerOutputStream derOutputStream2 = new DerOutputStream();
        derOutputStream2.putInteger(this.version);
        derOutputStream2.putOrderedSetOf((byte) 49, this.digestAlgorithmIds);
        this.contentInfo.encode(derOutputStream2);
        if (this.certificates != null && this.certificates.length != 0) {
            X509CertImpl[] x509CertImplArr = new X509CertImpl[this.certificates.length];
            for (int i = 0; i < this.certificates.length; i++) {
                if (this.certificates[i] instanceof X509CertImpl) {
                    x509CertImplArr[i] = (X509CertImpl) this.certificates[i];
                } else {
                    try {
                        x509CertImplArr[i] = new X509CertImpl(this.certificates[i].getEncoded());
                    } catch (CertificateException e) {
                        throw new IOException(e);
                    }
                }
            }
            derOutputStream2.putOrderedSetOf((byte) -96, x509CertImplArr);
        }
        if (this.crls != null && this.crls.length != 0) {
            HashSet hashSet = new HashSet(this.crls.length);
            for (X509CRL x509crl : this.crls) {
                if (x509crl instanceof X509CRLImpl) {
                    hashSet.add((X509CRLImpl) x509crl);
                } else {
                    try {
                        hashSet.add(new X509CRLImpl(x509crl.getEncoded()));
                    } catch (CRLException e2) {
                        throw new IOException(e2);
                    }
                }
            }
            derOutputStream2.putOrderedSetOf((byte) -95, (DerEncoder[]) hashSet.toArray(new X509CRLImpl[hashSet.size()]));
        }
        derOutputStream2.putOrderedSetOf((byte) 49, this.signerInfos);
        new ContentInfo(ContentInfo.SIGNED_DATA_OID, new DerValue((byte) 48, derOutputStream2.toByteArray())).encode(derOutputStream);
    }

    public SignerInfo verify(SignerInfo signerInfo, byte[] bArr) throws NoSuchAlgorithmException, SignatureException {
        return signerInfo.verify(this, bArr);
    }

    public SignerInfo verify(SignerInfo signerInfo, InputStream inputStream) throws NoSuchAlgorithmException, SignatureException, IOException {
        return signerInfo.verify(this, inputStream);
    }

    public SignerInfo[] verify(byte[] bArr) throws NoSuchAlgorithmException, SignatureException {
        Vector vector = new Vector();
        for (int i = 0; i < this.signerInfos.length; i++) {
            SignerInfo signerInfoVerify = verify(this.signerInfos[i], bArr);
            if (signerInfoVerify != null) {
                vector.addElement(signerInfoVerify);
            }
        }
        if (!vector.isEmpty()) {
            SignerInfo[] signerInfoArr = new SignerInfo[vector.size()];
            vector.copyInto(signerInfoArr);
            return signerInfoArr;
        }
        return null;
    }

    public SignerInfo[] verify() throws NoSuchAlgorithmException, SignatureException {
        return verify(null);
    }

    public BigInteger getVersion() {
        return this.version;
    }

    public AlgorithmId[] getDigestAlgorithmIds() {
        return this.digestAlgorithmIds;
    }

    public ContentInfo getContentInfo() {
        return this.contentInfo;
    }

    public X509Certificate[] getCertificates() {
        if (this.certificates != null) {
            return (X509Certificate[]) this.certificates.clone();
        }
        return null;
    }

    public X509CRL[] getCRLs() {
        if (this.crls != null) {
            return (X509CRL[]) this.crls.clone();
        }
        return null;
    }

    public SignerInfo[] getSignerInfos() {
        return this.signerInfos;
    }

    public X509Certificate getCertificate(BigInteger bigInteger, X500Name x500Name) {
        if (this.certificates != null) {
            if (this.certIssuerNames == null) {
                populateCertIssuerNames();
            }
            for (int i = 0; i < this.certificates.length; i++) {
                X509Certificate x509Certificate = this.certificates[i];
                if (bigInteger.equals(x509Certificate.getSerialNumber()) && x500Name.equals(this.certIssuerNames[i])) {
                    return x509Certificate;
                }
            }
            return null;
        }
        return null;
    }

    private void populateCertIssuerNames() {
        Principal principal;
        if (this.certificates == null) {
            return;
        }
        this.certIssuerNames = new Principal[this.certificates.length];
        for (int i = 0; i < this.certificates.length; i++) {
            X509Certificate x509Certificate = this.certificates[i];
            Principal issuerDN = x509Certificate.getIssuerDN();
            if (!(issuerDN instanceof X500Name)) {
                try {
                    principal = (Principal) new X509CertInfo(x509Certificate.getTBSCertificate()).get("issuer.dname");
                } catch (Exception e) {
                    principal = issuerDN;
                }
            } else {
                principal = issuerDN;
            }
            this.certIssuerNames[i] = principal;
        }
    }

    public String toString() {
        String str = "" + ((Object) this.contentInfo) + "\n";
        if (this.version != null) {
            str = str + "PKCS7 :: version: " + Debug.toHexString(this.version) + "\n";
        }
        if (this.digestAlgorithmIds != null) {
            String str2 = str + "PKCS7 :: digest AlgorithmIds: \n";
            for (int i = 0; i < this.digestAlgorithmIds.length; i++) {
                str2 = str2 + "\t" + ((Object) this.digestAlgorithmIds[i]) + "\n";
            }
            str = str2;
        }
        if (this.certificates != null) {
            String str3 = str + "PKCS7 :: certificates: \n";
            for (int i2 = 0; i2 < this.certificates.length; i2++) {
                str3 = str3 + "\t" + i2 + ".   " + ((Object) this.certificates[i2]) + "\n";
            }
            str = str3;
        }
        if (this.crls != null) {
            String str4 = str + "PKCS7 :: crls: \n";
            for (int i3 = 0; i3 < this.crls.length; i3++) {
                str4 = str4 + "\t" + i3 + ".   " + ((Object) this.crls[i3]) + "\n";
            }
            str = str4;
        }
        if (this.signerInfos != null) {
            str = str + "PKCS7 :: signer infos: \n";
            for (int i4 = 0; i4 < this.signerInfos.length; i4++) {
                str = str + "\t" + i4 + ".  " + ((Object) this.signerInfos[i4]) + "\n";
            }
        }
        return str;
    }

    public boolean isOldStyle() {
        return this.oldStyle;
    }

    private static class VerbatimX509Certificate extends WrappedX509Certificate {
        private byte[] encodedVerbatim;

        public VerbatimX509Certificate(X509Certificate x509Certificate, byte[] bArr) {
            super(x509Certificate);
            this.encodedVerbatim = bArr;
        }

        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            return this.encodedVerbatim;
        }
    }

    private static class WrappedX509Certificate extends X509Certificate {
        private final X509Certificate wrapped;

        public WrappedX509Certificate(X509Certificate x509Certificate) {
            this.wrapped = x509Certificate;
        }

        @Override
        public Set<String> getCriticalExtensionOIDs() {
            return this.wrapped.getCriticalExtensionOIDs();
        }

        @Override
        public byte[] getExtensionValue(String str) {
            return this.wrapped.getExtensionValue(str);
        }

        @Override
        public Set<String> getNonCriticalExtensionOIDs() {
            return this.wrapped.getNonCriticalExtensionOIDs();
        }

        @Override
        public boolean hasUnsupportedCriticalExtension() {
            return this.wrapped.hasUnsupportedCriticalExtension();
        }

        @Override
        public void checkValidity() throws CertificateNotYetValidException, CertificateExpiredException {
            this.wrapped.checkValidity();
        }

        @Override
        public void checkValidity(Date date) throws CertificateNotYetValidException, CertificateExpiredException {
            this.wrapped.checkValidity(date);
        }

        @Override
        public int getVersion() {
            return this.wrapped.getVersion();
        }

        @Override
        public BigInteger getSerialNumber() {
            return this.wrapped.getSerialNumber();
        }

        @Override
        public Principal getIssuerDN() {
            return this.wrapped.getIssuerDN();
        }

        @Override
        public Principal getSubjectDN() {
            return this.wrapped.getSubjectDN();
        }

        @Override
        public Date getNotBefore() {
            return this.wrapped.getNotBefore();
        }

        @Override
        public Date getNotAfter() {
            return this.wrapped.getNotAfter();
        }

        @Override
        public byte[] getTBSCertificate() throws CertificateEncodingException {
            return this.wrapped.getTBSCertificate();
        }

        @Override
        public byte[] getSignature() {
            return this.wrapped.getSignature();
        }

        @Override
        public String getSigAlgName() {
            return this.wrapped.getSigAlgName();
        }

        @Override
        public String getSigAlgOID() {
            return this.wrapped.getSigAlgOID();
        }

        @Override
        public byte[] getSigAlgParams() {
            return this.wrapped.getSigAlgParams();
        }

        @Override
        public boolean[] getIssuerUniqueID() {
            return this.wrapped.getIssuerUniqueID();
        }

        @Override
        public boolean[] getSubjectUniqueID() {
            return this.wrapped.getSubjectUniqueID();
        }

        @Override
        public boolean[] getKeyUsage() {
            return this.wrapped.getKeyUsage();
        }

        @Override
        public int getBasicConstraints() {
            return this.wrapped.getBasicConstraints();
        }

        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            return this.wrapped.getEncoded();
        }

        @Override
        public void verify(PublicKey publicKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException, NoSuchProviderException {
            this.wrapped.verify(publicKey);
        }

        @Override
        public void verify(PublicKey publicKey, String str) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException, NoSuchProviderException {
            this.wrapped.verify(publicKey, str);
        }

        @Override
        public String toString() {
            return this.wrapped.toString();
        }

        @Override
        public PublicKey getPublicKey() {
            return this.wrapped.getPublicKey();
        }

        @Override
        public List<String> getExtendedKeyUsage() throws CertificateParsingException {
            return this.wrapped.getExtendedKeyUsage();
        }

        @Override
        public Collection<List<?>> getIssuerAlternativeNames() throws CertificateParsingException {
            return this.wrapped.getIssuerAlternativeNames();
        }

        @Override
        public X500Principal getIssuerX500Principal() {
            return this.wrapped.getIssuerX500Principal();
        }

        @Override
        public Collection<List<?>> getSubjectAlternativeNames() throws CertificateParsingException {
            return this.wrapped.getSubjectAlternativeNames();
        }

        @Override
        public X500Principal getSubjectX500Principal() {
            return this.wrapped.getSubjectX500Principal();
        }

        @Override
        public void verify(PublicKey publicKey, Provider provider) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException {
            this.wrapped.verify(publicKey, provider);
        }
    }
}
