package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import javax.security.auth.x500.X500Principal;
import sun.misc.HexDumpEncoder;
import sun.security.provider.X509Factory;
import sun.security.util.DerEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class X509CertImpl extends X509Certificate implements DerEncoder {
    public static final String ALG_ID = "algorithm";
    private static final String AUTH_INFO_ACCESS_OID = "1.3.6.1.5.5.7.1.1";
    private static final String BASIC_CONSTRAINT_OID = "2.5.29.19";
    private static final String DOT = ".";
    private static final String EXTENDED_KEY_USAGE_OID = "2.5.29.37";
    public static final String INFO = "info";
    private static final String ISSUER_ALT_NAME_OID = "2.5.29.18";
    public static final String ISSUER_DN = "x509.info.issuer.dname";
    private static final String KEY_USAGE_OID = "2.5.29.15";
    public static final String NAME = "x509";
    private static final int NUM_STANDARD_KEY_USAGE = 9;
    public static final String PUBLIC_KEY = "x509.info.key.value";
    public static final String SERIAL_ID = "x509.info.serialNumber.number";
    public static final String SIG = "x509.signature";
    public static final String SIGNATURE = "signature";
    public static final String SIGNED_CERT = "signed_cert";
    public static final String SIG_ALG = "x509.algorithm";
    private static final String SUBJECT_ALT_NAME_OID = "2.5.29.17";
    public static final String SUBJECT_DN = "x509.info.subject.dname";
    public static final String VERSION = "x509.info.version.number";
    private static final long serialVersionUID = -3457612960190864406L;
    protected AlgorithmId algId;
    private Set<AccessDescription> authInfoAccess;
    private List<String> extKeyUsage;
    private ConcurrentHashMap<String, String> fingerprints;
    protected X509CertInfo info;
    private Collection<List<?>> issuerAlternativeNames;
    private boolean readOnly;
    protected byte[] signature;
    private byte[] signedCert;
    private Collection<List<?>> subjectAlternativeNames;
    private boolean verificationResult;
    private String verifiedProvider;
    private PublicKey verifiedPublicKey;

    public X509CertImpl() {
        this.readOnly = false;
        this.signedCert = null;
        this.info = null;
        this.algId = null;
        this.signature = null;
        this.fingerprints = new ConcurrentHashMap<>(2);
    }

    public X509CertImpl(byte[] bArr) throws CertificateException {
        this.readOnly = false;
        this.signedCert = null;
        this.info = null;
        this.algId = null;
        this.signature = null;
        this.fingerprints = new ConcurrentHashMap<>(2);
        try {
            parse(new DerValue(bArr));
        } catch (IOException e) {
            this.signedCert = null;
            throw new CertificateException("Unable to initialize, " + ((Object) e), e);
        }
    }

    public X509CertImpl(X509CertInfo x509CertInfo) {
        this.readOnly = false;
        this.signedCert = null;
        this.info = null;
        this.algId = null;
        this.signature = null;
        this.fingerprints = new ConcurrentHashMap<>(2);
        this.info = x509CertInfo;
    }

    public X509CertImpl(DerValue derValue) throws CertificateException {
        this.readOnly = false;
        this.signedCert = null;
        this.info = null;
        this.algId = null;
        this.signature = null;
        this.fingerprints = new ConcurrentHashMap<>(2);
        try {
            parse(derValue);
        } catch (IOException e) {
            this.signedCert = null;
            throw new CertificateException("Unable to initialize, " + ((Object) e), e);
        }
    }

    public X509CertImpl(DerValue derValue, byte[] bArr) throws CertificateException {
        this.readOnly = false;
        this.signedCert = null;
        this.info = null;
        this.algId = null;
        this.signature = null;
        this.fingerprints = new ConcurrentHashMap<>(2);
        try {
            parse(derValue, bArr);
        } catch (IOException e) {
            this.signedCert = null;
            throw new CertificateException("Unable to initialize, " + ((Object) e), e);
        }
    }

    public void encode(OutputStream outputStream) throws CertificateEncodingException {
        if (this.signedCert == null) {
            throw new CertificateEncodingException("Null certificate to encode");
        }
        try {
            outputStream.write((byte[]) this.signedCert.clone());
        } catch (IOException e) {
            throw new CertificateEncodingException(e.toString());
        }
    }

    @Override
    public void derEncode(OutputStream outputStream) throws IOException {
        if (this.signedCert == null) {
            throw new IOException("Null certificate to encode");
        }
        outputStream.write((byte[]) this.signedCert.clone());
    }

    @Override
    public byte[] getEncoded() throws CertificateEncodingException {
        return (byte[]) getEncodedInternal().clone();
    }

    public byte[] getEncodedInternal() throws CertificateEncodingException {
        if (this.signedCert == null) {
            throw new CertificateEncodingException("Null certificate to encode");
        }
        return this.signedCert;
    }

    @Override
    public void verify(PublicKey publicKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException, NoSuchProviderException {
        verify(publicKey, "");
    }

    @Override
    public synchronized void verify(PublicKey publicKey, String str) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException, NoSuchProviderException {
        Signature signature;
        if (str == null) {
            str = "";
        }
        try {
            if (this.verifiedPublicKey != null && this.verifiedPublicKey.equals(publicKey) && str.equals(this.verifiedProvider)) {
                if (!this.verificationResult) {
                    throw new SignatureException("Signature does not match.");
                }
                return;
            }
            if (this.signedCert == null) {
                throw new CertificateEncodingException("Uninitialized certificate");
            }
            if (str.length() == 0) {
                signature = Signature.getInstance(this.algId.getName());
            } else {
                signature = Signature.getInstance(this.algId.getName(), str);
            }
            signature.initVerify(publicKey);
            byte[] encodedInfo = this.info.getEncodedInfo();
            signature.update(encodedInfo, 0, encodedInfo.length);
            this.verificationResult = signature.verify(this.signature);
            this.verifiedPublicKey = publicKey;
            this.verifiedProvider = str;
            if (!this.verificationResult) {
                throw new SignatureException("Signature does not match.");
            }
        } catch (Throwable th) {
            throw th;
        }
    }

    @Override
    public synchronized void verify(PublicKey publicKey, Provider provider) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException {
        Signature signature;
        if (this.signedCert == null) {
            throw new CertificateEncodingException("Uninitialized certificate");
        }
        if (provider == null) {
            signature = Signature.getInstance(this.algId.getName());
        } else {
            signature = Signature.getInstance(this.algId.getName(), provider);
        }
        signature.initVerify(publicKey);
        byte[] encodedInfo = this.info.getEncodedInfo();
        signature.update(encodedInfo, 0, encodedInfo.length);
        this.verificationResult = signature.verify(this.signature);
        this.verifiedPublicKey = publicKey;
        if (!this.verificationResult) {
            throw new SignatureException("Signature does not match.");
        }
    }

    public static void verify(X509Certificate x509Certificate, PublicKey publicKey, Provider provider) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException {
        x509Certificate.verify(publicKey, provider);
    }

    public void sign(PrivateKey privateKey, String str) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException, NoSuchProviderException {
        sign(privateKey, str, null);
    }

    public void sign(PrivateKey privateKey, String str, String str2) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException, NoSuchProviderException {
        Signature signature;
        try {
            if (this.readOnly) {
                throw new CertificateEncodingException("cannot over-write existing certificate");
            }
            if (str2 == null || str2.length() == 0) {
                signature = Signature.getInstance(str);
            } else {
                signature = Signature.getInstance(str, str2);
            }
            signature.initSign(privateKey);
            this.algId = AlgorithmId.get(signature.getAlgorithm());
            DerOutputStream derOutputStream = new DerOutputStream();
            DerOutputStream derOutputStream2 = new DerOutputStream();
            this.info.encode(derOutputStream2);
            byte[] byteArray = derOutputStream2.toByteArray();
            this.algId.encode(derOutputStream2);
            signature.update(byteArray, 0, byteArray.length);
            this.signature = signature.sign();
            derOutputStream2.putBitString(this.signature);
            derOutputStream.write((byte) 48, derOutputStream2);
            this.signedCert = derOutputStream.toByteArray();
            this.readOnly = true;
        } catch (IOException e) {
            throw new CertificateEncodingException(e.toString());
        }
    }

    @Override
    public void checkValidity() throws CertificateNotYetValidException, CertificateExpiredException {
        checkValidity(new Date());
    }

    @Override
    public void checkValidity(Date date) throws CertificateNotYetValidException, CertificateExpiredException {
        try {
            CertificateValidity certificateValidity = (CertificateValidity) this.info.get("validity");
            if (certificateValidity == null) {
                throw new CertificateNotYetValidException("Null validity period");
            }
            certificateValidity.valid(date);
        } catch (Exception e) {
            throw new CertificateNotYetValidException("Incorrect validity period");
        }
    }

    public Object get(String str) throws CertificateParsingException {
        X509AttributeName x509AttributeName = new X509AttributeName(str);
        String prefix = x509AttributeName.getPrefix();
        if (!prefix.equalsIgnoreCase(NAME)) {
            throw new CertificateParsingException("Invalid root of attribute name, expected [x509], received [" + prefix + "]");
        }
        X509AttributeName x509AttributeName2 = new X509AttributeName(x509AttributeName.getSuffix());
        String prefix2 = x509AttributeName2.getPrefix();
        if (prefix2.equalsIgnoreCase("info")) {
            if (this.info == null) {
                return null;
            }
            if (x509AttributeName2.getSuffix() != null) {
                try {
                    return this.info.get(x509AttributeName2.getSuffix());
                } catch (IOException e) {
                    throw new CertificateParsingException(e.toString());
                } catch (CertificateException e2) {
                    throw new CertificateParsingException(e2.toString());
                }
            }
            return this.info;
        }
        if (prefix2.equalsIgnoreCase("algorithm")) {
            return this.algId;
        }
        if (prefix2.equalsIgnoreCase(SIGNATURE)) {
            if (this.signature != null) {
                return this.signature.clone();
            }
            return null;
        }
        if (prefix2.equalsIgnoreCase(SIGNED_CERT)) {
            if (this.signedCert != null) {
                return this.signedCert.clone();
            }
            return null;
        }
        throw new CertificateParsingException("Attribute name not recognized or get() not allowed for the same: " + prefix2);
    }

    public void set(String str, Object obj) throws IOException, CertificateException {
        if (this.readOnly) {
            throw new CertificateException("cannot over-write existing certificate");
        }
        X509AttributeName x509AttributeName = new X509AttributeName(str);
        String prefix = x509AttributeName.getPrefix();
        if (!prefix.equalsIgnoreCase(NAME)) {
            throw new CertificateException("Invalid root of attribute name, expected [x509], received " + prefix);
        }
        X509AttributeName x509AttributeName2 = new X509AttributeName(x509AttributeName.getSuffix());
        String prefix2 = x509AttributeName2.getPrefix();
        if (prefix2.equalsIgnoreCase("info")) {
            if (x509AttributeName2.getSuffix() == null) {
                if (!(obj instanceof X509CertInfo)) {
                    throw new CertificateException("Attribute value should be of type X509CertInfo.");
                }
                this.info = (X509CertInfo) obj;
                this.signedCert = null;
                return;
            }
            this.info.set(x509AttributeName2.getSuffix(), obj);
            this.signedCert = null;
            return;
        }
        throw new CertificateException("Attribute name not recognized or set() not allowed for the same: " + prefix2);
    }

    public void delete(String str) throws IOException, CertificateException {
        if (this.readOnly) {
            throw new CertificateException("cannot over-write existing certificate");
        }
        X509AttributeName x509AttributeName = new X509AttributeName(str);
        String prefix = x509AttributeName.getPrefix();
        if (!prefix.equalsIgnoreCase(NAME)) {
            throw new CertificateException("Invalid root of attribute name, expected [x509], received " + prefix);
        }
        X509AttributeName x509AttributeName2 = new X509AttributeName(x509AttributeName.getSuffix());
        String prefix2 = x509AttributeName2.getPrefix();
        if (prefix2.equalsIgnoreCase("info")) {
            if (x509AttributeName2.getSuffix() != null) {
                this.info = null;
                return;
            } else {
                this.info.delete(x509AttributeName2.getSuffix());
                return;
            }
        }
        if (prefix2.equalsIgnoreCase("algorithm")) {
            this.algId = null;
            return;
        }
        if (prefix2.equalsIgnoreCase(SIGNATURE)) {
            this.signature = null;
        } else {
            if (prefix2.equalsIgnoreCase(SIGNED_CERT)) {
                this.signedCert = null;
                return;
            }
            throw new CertificateException("Attribute name not recognized or delete() not allowed for the same: " + prefix2);
        }
    }

    public Enumeration<String> getElements() {
        AttributeNameEnumeration attributeNameEnumeration = new AttributeNameEnumeration();
        attributeNameEnumeration.addElement(X509CertInfo.IDENT);
        attributeNameEnumeration.addElement(SIG_ALG);
        attributeNameEnumeration.addElement(SIG);
        attributeNameEnumeration.addElement("x509.signed_cert");
        return attributeNameEnumeration.elements();
    }

    public String getName() {
        return NAME;
    }

    @Override
    public String toString() {
        if (this.info == null || this.algId == null || this.signature == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        sb.append(this.info.toString() + "\n");
        sb.append("  Algorithm: [" + this.algId.toString() + "]\n");
        sb.append("  Signature:\n" + new HexDumpEncoder().encodeBuffer(this.signature));
        sb.append("\n]");
        return sb.toString();
    }

    @Override
    public PublicKey getPublicKey() {
        if (this.info == null) {
            return null;
        }
        try {
            return (PublicKey) this.info.get("key.value");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int getVersion() {
        if (this.info == null) {
            return -1;
        }
        try {
            return ((Integer) this.info.get("version.number")).intValue() + 1;
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public BigInteger getSerialNumber() {
        SerialNumber serialNumberObject = getSerialNumberObject();
        if (serialNumberObject != null) {
            return serialNumberObject.getNumber();
        }
        return null;
    }

    public SerialNumber getSerialNumberObject() {
        if (this.info == null) {
            return null;
        }
        try {
            return (SerialNumber) this.info.get("serialNumber.number");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Principal getSubjectDN() {
        if (this.info == null) {
            return null;
        }
        try {
            return (Principal) this.info.get("subject.dname");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public X500Principal getSubjectX500Principal() {
        if (this.info == null) {
            return null;
        }
        try {
            return (X500Principal) this.info.get("subject.x500principal");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Principal getIssuerDN() {
        if (this.info == null) {
            return null;
        }
        try {
            return (Principal) this.info.get("issuer.dname");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public X500Principal getIssuerX500Principal() {
        if (this.info == null) {
            return null;
        }
        try {
            return (X500Principal) this.info.get("issuer.x500principal");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Date getNotBefore() {
        if (this.info == null) {
            return null;
        }
        try {
            return (Date) this.info.get("validity.notBefore");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Date getNotAfter() {
        if (this.info == null) {
            return null;
        }
        try {
            return (Date) this.info.get("validity.notAfter");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public byte[] getTBSCertificate() throws CertificateEncodingException {
        if (this.info != null) {
            return this.info.getEncodedInfo();
        }
        throw new CertificateEncodingException("Uninitialized certificate");
    }

    @Override
    public byte[] getSignature() {
        if (this.signature == null) {
            return null;
        }
        return (byte[]) this.signature.clone();
    }

    @Override
    public String getSigAlgName() {
        if (this.algId == null) {
            return null;
        }
        return this.algId.getName();
    }

    @Override
    public String getSigAlgOID() {
        if (this.algId == null) {
            return null;
        }
        return this.algId.getOID().toString();
    }

    @Override
    public byte[] getSigAlgParams() {
        if (this.algId == null) {
            return null;
        }
        try {
            return this.algId.getEncodedParams();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean[] getIssuerUniqueID() {
        if (this.info == null) {
            return null;
        }
        try {
            UniqueIdentity uniqueIdentity = (UniqueIdentity) this.info.get(X509CertInfo.ISSUER_ID);
            if (uniqueIdentity == null) {
                return null;
            }
            return uniqueIdentity.getId();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean[] getSubjectUniqueID() {
        if (this.info == null) {
            return null;
        }
        try {
            UniqueIdentity uniqueIdentity = (UniqueIdentity) this.info.get(X509CertInfo.SUBJECT_ID);
            if (uniqueIdentity == null) {
                return null;
            }
            return uniqueIdentity.getId();
        } catch (Exception e) {
            return null;
        }
    }

    public KeyIdentifier getAuthKeyId() {
        AuthorityKeyIdentifierExtension authorityKeyIdentifierExtension = getAuthorityKeyIdentifierExtension();
        if (authorityKeyIdentifierExtension != null) {
            try {
                return (KeyIdentifier) authorityKeyIdentifierExtension.get("key_id");
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    public KeyIdentifier getSubjectKeyId() {
        SubjectKeyIdentifierExtension subjectKeyIdentifierExtension = getSubjectKeyIdentifierExtension();
        if (subjectKeyIdentifierExtension != null) {
            try {
                return subjectKeyIdentifierExtension.get("key_id");
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    public AuthorityKeyIdentifierExtension getAuthorityKeyIdentifierExtension() {
        return (AuthorityKeyIdentifierExtension) getExtension(PKIXExtensions.AuthorityKey_Id);
    }

    public BasicConstraintsExtension getBasicConstraintsExtension() {
        return (BasicConstraintsExtension) getExtension(PKIXExtensions.BasicConstraints_Id);
    }

    public CertificatePoliciesExtension getCertificatePoliciesExtension() {
        return (CertificatePoliciesExtension) getExtension(PKIXExtensions.CertificatePolicies_Id);
    }

    public ExtendedKeyUsageExtension getExtendedKeyUsageExtension() {
        return (ExtendedKeyUsageExtension) getExtension(PKIXExtensions.ExtendedKeyUsage_Id);
    }

    public IssuerAlternativeNameExtension getIssuerAlternativeNameExtension() {
        return (IssuerAlternativeNameExtension) getExtension(PKIXExtensions.IssuerAlternativeName_Id);
    }

    public NameConstraintsExtension getNameConstraintsExtension() {
        return (NameConstraintsExtension) getExtension(PKIXExtensions.NameConstraints_Id);
    }

    public PolicyConstraintsExtension getPolicyConstraintsExtension() {
        return (PolicyConstraintsExtension) getExtension(PKIXExtensions.PolicyConstraints_Id);
    }

    public PolicyMappingsExtension getPolicyMappingsExtension() {
        return (PolicyMappingsExtension) getExtension(PKIXExtensions.PolicyMappings_Id);
    }

    public PrivateKeyUsageExtension getPrivateKeyUsageExtension() {
        return (PrivateKeyUsageExtension) getExtension(PKIXExtensions.PrivateKeyUsage_Id);
    }

    public SubjectAlternativeNameExtension getSubjectAlternativeNameExtension() {
        return (SubjectAlternativeNameExtension) getExtension(PKIXExtensions.SubjectAlternativeName_Id);
    }

    public SubjectKeyIdentifierExtension getSubjectKeyIdentifierExtension() {
        return (SubjectKeyIdentifierExtension) getExtension(PKIXExtensions.SubjectKey_Id);
    }

    public CRLDistributionPointsExtension getCRLDistributionPointsExtension() {
        return (CRLDistributionPointsExtension) getExtension(PKIXExtensions.CRLDistributionPoints_Id);
    }

    @Override
    public boolean hasUnsupportedCriticalExtension() {
        if (this.info == null) {
            return false;
        }
        try {
            CertificateExtensions certificateExtensions = (CertificateExtensions) this.info.get("extensions");
            if (certificateExtensions == null) {
                return false;
            }
            return certificateExtensions.hasUnsupportedCriticalExtension();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Set<String> getCriticalExtensionOIDs() {
        if (this.info == null) {
            return null;
        }
        try {
            CertificateExtensions certificateExtensions = (CertificateExtensions) this.info.get("extensions");
            if (certificateExtensions == null) {
                return null;
            }
            TreeSet treeSet = new TreeSet();
            for (Extension extension : certificateExtensions.getAllExtensions()) {
                if (extension.isCritical()) {
                    treeSet.add(extension.getExtensionId().toString());
                }
            }
            return treeSet;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Set<String> getNonCriticalExtensionOIDs() {
        if (this.info == null) {
            return null;
        }
        try {
            CertificateExtensions certificateExtensions = (CertificateExtensions) this.info.get("extensions");
            if (certificateExtensions == null) {
                return null;
            }
            TreeSet treeSet = new TreeSet();
            for (Extension extension : certificateExtensions.getAllExtensions()) {
                if (!extension.isCritical()) {
                    treeSet.add(extension.getExtensionId().toString());
                }
            }
            treeSet.addAll(certificateExtensions.getUnparseableExtensions().keySet());
            return treeSet;
        } catch (Exception e) {
            return null;
        }
    }

    public Extension getExtension(ObjectIdentifier objectIdentifier) {
        if (this.info == null) {
            return null;
        }
        try {
            try {
                CertificateExtensions certificateExtensions = (CertificateExtensions) this.info.get("extensions");
                if (certificateExtensions == null) {
                    return null;
                }
                Extension extension = certificateExtensions.getExtension(objectIdentifier.toString());
                if (extension != null) {
                    return extension;
                }
                for (Extension extension2 : certificateExtensions.getAllExtensions()) {
                    if (extension2.getExtensionId().equals((Object) objectIdentifier)) {
                        return extension2;
                    }
                }
                return null;
            } catch (IOException e) {
                return null;
            }
        } catch (CertificateException e2) {
            return null;
        }
    }

    public Extension getUnparseableExtension(ObjectIdentifier objectIdentifier) {
        if (this.info == null) {
            return null;
        }
        try {
            try {
                CertificateExtensions certificateExtensions = (CertificateExtensions) this.info.get("extensions");
                if (certificateExtensions == null) {
                    return null;
                }
                return certificateExtensions.getUnparseableExtensions().get(objectIdentifier.toString());
            } catch (IOException e) {
                return null;
            }
        } catch (CertificateException e2) {
            return null;
        }
    }

    @Override
    public byte[] getExtensionValue(String str) {
        Extension extension;
        Extension next;
        try {
            ObjectIdentifier objectIdentifier = new ObjectIdentifier(str);
            String name = OIDMap.getName(objectIdentifier);
            CertificateExtensions certificateExtensions = (CertificateExtensions) this.info.get("extensions");
            if (name == null) {
                if (certificateExtensions == null) {
                    return null;
                }
                Iterator<Extension> it = certificateExtensions.getAllExtensions().iterator();
                while (true) {
                    if (it.hasNext()) {
                        next = it.next();
                        if (next.getExtensionId().equals((Object) objectIdentifier)) {
                            break;
                        }
                    } else {
                        next = null;
                        break;
                    }
                }
                extension = next;
            } else {
                try {
                    extension = (Extension) get(name);
                } catch (CertificateException e) {
                    extension = null;
                }
            }
            if (extension == null) {
                if (certificateExtensions != null) {
                    extension = certificateExtensions.getUnparseableExtensions().get(str);
                }
                if (extension == null) {
                    return null;
                }
            }
            byte[] extensionValue = extension.getExtensionValue();
            if (extensionValue == null) {
                return null;
            }
            DerOutputStream derOutputStream = new DerOutputStream();
            derOutputStream.putOctetString(extensionValue);
            return derOutputStream.toByteArray();
        } catch (Exception e2) {
            return null;
        }
    }

    @Override
    public boolean[] getKeyUsage() {
        KeyUsageExtension keyUsageExtension;
        try {
            String name = OIDMap.getName(PKIXExtensions.KeyUsage_Id);
            if (name == null || (keyUsageExtension = (KeyUsageExtension) get(name)) == null) {
                return null;
            }
            boolean[] bits = keyUsageExtension.getBits();
            if (bits.length >= 9) {
                return bits;
            }
            boolean[] zArr = new boolean[9];
            System.arraycopy((Object) bits, 0, (Object) zArr, 0, bits.length);
            return zArr;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public synchronized List<String> getExtendedKeyUsage() throws CertificateParsingException {
        if (this.readOnly && this.extKeyUsage != null) {
            return this.extKeyUsage;
        }
        ExtendedKeyUsageExtension extendedKeyUsageExtension = getExtendedKeyUsageExtension();
        if (extendedKeyUsageExtension == null) {
            return null;
        }
        this.extKeyUsage = Collections.unmodifiableList(extendedKeyUsageExtension.getExtendedKeyUsage());
        return this.extKeyUsage;
    }

    public static List<String> getExtendedKeyUsage(X509Certificate x509Certificate) throws CertificateParsingException {
        try {
            byte[] extensionValue = x509Certificate.getExtensionValue(EXTENDED_KEY_USAGE_OID);
            if (extensionValue == null) {
                return null;
            }
            return Collections.unmodifiableList(new ExtendedKeyUsageExtension(Boolean.FALSE, new DerValue(extensionValue).getOctetString()).getExtendedKeyUsage());
        } catch (IOException e) {
            throw new CertificateParsingException(e);
        }
    }

    @Override
    public int getBasicConstraints() {
        BasicConstraintsExtension basicConstraintsExtension;
        try {
            String name = OIDMap.getName(PKIXExtensions.BasicConstraints_Id);
            if (name == null || (basicConstraintsExtension = (BasicConstraintsExtension) get(name)) == null || !((Boolean) basicConstraintsExtension.get(BasicConstraintsExtension.IS_CA)).booleanValue()) {
                return -1;
            }
            return ((Integer) basicConstraintsExtension.get(BasicConstraintsExtension.PATH_LEN)).intValue();
        } catch (Exception e) {
            return -1;
        }
    }

    private static Collection<List<?>> makeAltNames(GeneralNames generalNames) {
        if (generalNames.isEmpty()) {
            return Collections.emptySet();
        }
        ArrayList arrayList = new ArrayList();
        Iterator<GeneralName> it = generalNames.names().iterator();
        while (it.hasNext()) {
            GeneralNameInterface name = it.next().getName();
            ArrayList arrayList2 = new ArrayList(2);
            arrayList2.add(Integer.valueOf(name.getType()));
            switch (name.getType()) {
                case 1:
                    arrayList2.add(((RFC822Name) name).getName());
                    break;
                case 2:
                    arrayList2.add(((DNSName) name).getName());
                    break;
                case 3:
                case 5:
                default:
                    DerOutputStream derOutputStream = new DerOutputStream();
                    try {
                        name.encode(derOutputStream);
                        arrayList2.add(derOutputStream.toByteArray());
                    } catch (IOException e) {
                        throw new RuntimeException("name cannot be encoded", e);
                    }
                    break;
                case 4:
                    arrayList2.add(((X500Name) name).getRFC2253Name());
                    break;
                case 6:
                    arrayList2.add(((URIName) name).getName());
                    break;
                case 7:
                    try {
                        arrayList2.add(((IPAddressName) name).getName());
                    } catch (IOException e2) {
                        throw new RuntimeException("IPAddress cannot be parsed", e2);
                    }
                    break;
                case 8:
                    arrayList2.add(((OIDName) name).getOID().toString());
                    break;
            }
            arrayList.add(Collections.unmodifiableList(arrayList2));
        }
        return Collections.unmodifiableCollection(arrayList);
    }

    private static Collection<List<?>> cloneAltNames(Collection<List<?>> collection) {
        Iterator<List<?>> it = collection.iterator();
        boolean z = false;
        while (it.hasNext()) {
            if (it.next().get(1) instanceof byte[]) {
                z = true;
            }
        }
        if (z) {
            ArrayList arrayList = new ArrayList();
            for (List<?> list : collection) {
                Object obj = list.get(1);
                if (obj instanceof byte[]) {
                    ArrayList arrayList2 = new ArrayList(list);
                    arrayList2.set(1, ((byte[]) obj).clone());
                    arrayList.add(Collections.unmodifiableList(arrayList2));
                } else {
                    arrayList.add(list);
                }
            }
            return Collections.unmodifiableCollection(arrayList);
        }
        return collection;
    }

    @Override
    public synchronized Collection<List<?>> getSubjectAlternativeNames() throws CertificateParsingException {
        if (this.readOnly && this.subjectAlternativeNames != null) {
            return cloneAltNames(this.subjectAlternativeNames);
        }
        SubjectAlternativeNameExtension subjectAlternativeNameExtension = getSubjectAlternativeNameExtension();
        if (subjectAlternativeNameExtension == null) {
            return null;
        }
        try {
            this.subjectAlternativeNames = makeAltNames(subjectAlternativeNameExtension.get(SubjectAlternativeNameExtension.SUBJECT_NAME));
            return this.subjectAlternativeNames;
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    public static Collection<List<?>> getSubjectAlternativeNames(X509Certificate x509Certificate) throws CertificateParsingException {
        try {
            byte[] extensionValue = x509Certificate.getExtensionValue(SUBJECT_ALT_NAME_OID);
            if (extensionValue == null) {
                return null;
            }
            try {
                return makeAltNames(new SubjectAlternativeNameExtension(Boolean.FALSE, new DerValue(extensionValue).getOctetString()).get(SubjectAlternativeNameExtension.SUBJECT_NAME));
            } catch (IOException e) {
                return Collections.emptySet();
            }
        } catch (IOException e2) {
            throw new CertificateParsingException(e2);
        }
    }

    @Override
    public synchronized Collection<List<?>> getIssuerAlternativeNames() throws CertificateParsingException {
        if (this.readOnly && this.issuerAlternativeNames != null) {
            return cloneAltNames(this.issuerAlternativeNames);
        }
        IssuerAlternativeNameExtension issuerAlternativeNameExtension = getIssuerAlternativeNameExtension();
        if (issuerAlternativeNameExtension == null) {
            return null;
        }
        try {
            this.issuerAlternativeNames = makeAltNames(issuerAlternativeNameExtension.get(IssuerAlternativeNameExtension.ISSUER_NAME));
            return this.issuerAlternativeNames;
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    public static Collection<List<?>> getIssuerAlternativeNames(X509Certificate x509Certificate) throws CertificateParsingException {
        try {
            byte[] extensionValue = x509Certificate.getExtensionValue(ISSUER_ALT_NAME_OID);
            if (extensionValue == null) {
                return null;
            }
            try {
                return makeAltNames(new IssuerAlternativeNameExtension(Boolean.FALSE, new DerValue(extensionValue).getOctetString()).get(IssuerAlternativeNameExtension.ISSUER_NAME));
            } catch (IOException e) {
                return Collections.emptySet();
            }
        } catch (IOException e2) {
            throw new CertificateParsingException(e2);
        }
    }

    public AuthorityInfoAccessExtension getAuthorityInfoAccessExtension() {
        return (AuthorityInfoAccessExtension) getExtension(PKIXExtensions.AuthInfoAccess_Id);
    }

    private void parse(DerValue derValue) throws IOException, CertificateException {
        parse(derValue, null);
    }

    private void parse(DerValue derValue, byte[] bArr) throws IOException, CertificateException {
        if (this.readOnly) {
            throw new CertificateParsingException("cannot over-write existing certificate");
        }
        if (derValue.data == null || derValue.tag != 48) {
            throw new CertificateParsingException("invalid DER-encoded certificate data");
        }
        if (bArr == null) {
            bArr = derValue.toByteArray();
        }
        this.signedCert = bArr;
        DerValue[] derValueArr = {derValue.data.getDerValue(), derValue.data.getDerValue(), derValue.data.getDerValue()};
        if (derValue.data.available() != 0) {
            throw new CertificateParsingException("signed overrun, bytes = " + derValue.data.available());
        }
        if (derValueArr[0].tag != 48) {
            throw new CertificateParsingException("signed fields invalid");
        }
        this.algId = AlgorithmId.parse(derValueArr[1]);
        this.signature = derValueArr[2].getBitString();
        if (derValueArr[1].data.available() != 0) {
            throw new CertificateParsingException("algid field overrun");
        }
        if (derValueArr[2].data.available() != 0) {
            throw new CertificateParsingException("signed fields overrun");
        }
        this.info = new X509CertInfo(derValueArr[0]);
        if (!this.algId.equals((AlgorithmId) this.info.get("algorithmID.algorithm"))) {
            throw new CertificateException("Signature algorithm mismatch");
        }
        this.readOnly = true;
    }

    private static X500Principal getX500Principal(X509Certificate x509Certificate, boolean z) throws Exception {
        DerInputStream derInputStream = new DerInputStream(x509Certificate.getEncoded()).getSequence(3)[0].data;
        if (derInputStream.getDerValue().isContextSpecific((byte) 0)) {
            derInputStream.getDerValue();
        }
        derInputStream.getDerValue();
        DerValue derValue = derInputStream.getDerValue();
        if (!z) {
            derInputStream.getDerValue();
            derValue = derInputStream.getDerValue();
        }
        return new X500Principal(derValue.toByteArray());
    }

    public static X500Principal getSubjectX500Principal(X509Certificate x509Certificate) {
        try {
            return getX500Principal(x509Certificate, false);
        } catch (Exception e) {
            throw new RuntimeException("Could not parse subject", e);
        }
    }

    public static X500Principal getIssuerX500Principal(X509Certificate x509Certificate) {
        try {
            return getX500Principal(x509Certificate, true);
        } catch (Exception e) {
            throw new RuntimeException("Could not parse issuer", e);
        }
    }

    public static byte[] getEncodedInternal(Certificate certificate) throws CertificateEncodingException {
        if (certificate instanceof X509CertImpl) {
            return ((X509CertImpl) certificate).getEncodedInternal();
        }
        return certificate.getEncoded();
    }

    public static X509CertImpl toImpl(X509Certificate x509Certificate) throws CertificateException {
        if (x509Certificate instanceof X509CertImpl) {
            return (X509CertImpl) x509Certificate;
        }
        return X509Factory.intern(x509Certificate);
    }

    public static boolean isSelfIssued(X509Certificate x509Certificate) {
        return x509Certificate.getSubjectX500Principal().equals(x509Certificate.getIssuerX500Principal());
    }

    public static boolean isSelfSigned(X509Certificate x509Certificate, String str) {
        if (isSelfIssued(x509Certificate)) {
            try {
                if (str == null) {
                    x509Certificate.verify(x509Certificate.getPublicKey());
                    return true;
                }
                x509Certificate.verify(x509Certificate.getPublicKey(), str);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public static String getFingerprint(String str, X509Certificate x509Certificate) {
        try {
            byte[] bArrDigest = MessageDigest.getInstance(str).digest(x509Certificate.getEncoded());
            StringBuffer stringBuffer = new StringBuffer();
            for (byte b : bArrDigest) {
                byte2hex(b, stringBuffer);
            }
            return stringBuffer.toString();
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            return "";
        }
    }

    private static void byte2hex(byte b, StringBuffer stringBuffer) {
        char[] cArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        stringBuffer.append(cArr[(b & 240) >> 4]);
        stringBuffer.append(cArr[b & 15]);
    }
}
