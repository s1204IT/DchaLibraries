package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import sun.misc.HexDumpEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class X509CertInfo implements CertAttrSet<String> {
    public static final String ALGORITHM_ID = "algorithmID";
    private static final int ATTR_ALGORITHM = 3;
    private static final int ATTR_EXTENSIONS = 10;
    private static final int ATTR_ISSUER = 4;
    private static final int ATTR_ISSUER_ID = 8;
    private static final int ATTR_KEY = 7;
    private static final int ATTR_SERIAL = 2;
    private static final int ATTR_SUBJECT = 6;
    private static final int ATTR_SUBJECT_ID = 9;
    private static final int ATTR_VALIDITY = 5;
    private static final int ATTR_VERSION = 1;
    public static final String DN_NAME = "dname";
    public static final String EXTENSIONS = "extensions";
    public static final String IDENT = "x509.info";
    public static final String ISSUER = "issuer";
    public static final String ISSUER_ID = "issuerID";
    public static final String KEY = "key";
    public static final String NAME = "info";
    public static final String SERIAL_NUMBER = "serialNumber";
    public static final String SUBJECT = "subject";
    public static final String SUBJECT_ID = "subjectID";
    public static final String VALIDITY = "validity";
    public static final String VERSION = "version";
    private static final Map<String, Integer> map = new HashMap();
    protected CertificateVersion version = new CertificateVersion();
    protected CertificateSerialNumber serialNum = null;
    protected CertificateAlgorithmId algId = null;
    protected X500Name issuer = null;
    protected X500Name subject = null;
    protected CertificateValidity interval = null;
    protected CertificateX509Key pubKey = null;
    protected UniqueIdentity issuerUniqueId = null;
    protected UniqueIdentity subjectUniqueId = null;
    protected CertificateExtensions extensions = null;
    private byte[] rawCertInfo = null;

    static {
        map.put("version", 1);
        map.put("serialNumber", 2);
        map.put("algorithmID", 3);
        map.put("issuer", 4);
        map.put("validity", 5);
        map.put("subject", 6);
        map.put("key", 7);
        map.put(ISSUER_ID, 8);
        map.put(SUBJECT_ID, 9);
        map.put("extensions", 10);
    }

    public X509CertInfo() {
    }

    public X509CertInfo(byte[] bArr) throws CertificateParsingException {
        try {
            parse(new DerValue(bArr));
        } catch (IOException e) {
            throw new CertificateParsingException(e);
        }
    }

    public X509CertInfo(DerValue derValue) throws CertificateParsingException {
        try {
            parse(derValue);
        } catch (IOException e) {
            throw new CertificateParsingException(e);
        }
    }

    @Override
    public void encode(OutputStream outputStream) throws IOException, CertificateException {
        if (this.rawCertInfo == null) {
            DerOutputStream derOutputStream = new DerOutputStream();
            emit(derOutputStream);
            this.rawCertInfo = derOutputStream.toByteArray();
        }
        outputStream.write((byte[]) this.rawCertInfo.clone());
    }

    @Override
    public Enumeration<String> getElements() {
        AttributeNameEnumeration attributeNameEnumeration = new AttributeNameEnumeration();
        attributeNameEnumeration.addElement("version");
        attributeNameEnumeration.addElement("serialNumber");
        attributeNameEnumeration.addElement("algorithmID");
        attributeNameEnumeration.addElement("issuer");
        attributeNameEnumeration.addElement("validity");
        attributeNameEnumeration.addElement("subject");
        attributeNameEnumeration.addElement("key");
        attributeNameEnumeration.addElement(ISSUER_ID);
        attributeNameEnumeration.addElement(SUBJECT_ID);
        attributeNameEnumeration.addElement("extensions");
        return attributeNameEnumeration.elements();
    }

    @Override
    public String getName() {
        return "info";
    }

    public byte[] getEncodedInfo() throws CertificateEncodingException {
        try {
            if (this.rawCertInfo == null) {
                DerOutputStream derOutputStream = new DerOutputStream();
                emit(derOutputStream);
                this.rawCertInfo = derOutputStream.toByteArray();
            }
            return (byte[]) this.rawCertInfo.clone();
        } catch (IOException e) {
            throw new CertificateEncodingException(e.toString());
        } catch (CertificateException e2) {
            throw new CertificateEncodingException(e2.toString());
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof X509CertInfo) {
            return equals((X509CertInfo) obj);
        }
        return false;
    }

    public boolean equals(X509CertInfo x509CertInfo) {
        if (this == x509CertInfo) {
            return true;
        }
        if (this.rawCertInfo == null || x509CertInfo.rawCertInfo == null || this.rawCertInfo.length != x509CertInfo.rawCertInfo.length) {
            return false;
        }
        for (int i = 0; i < this.rawCertInfo.length; i++) {
            if (this.rawCertInfo[i] != x509CertInfo.rawCertInfo[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int i = 0;
        for (int i2 = 1; i2 < this.rawCertInfo.length; i2++) {
            i += this.rawCertInfo[i2] * i2;
        }
        return i;
    }

    @Override
    public String toString() {
        if (this.subject == null || this.pubKey == null || this.interval == null || this.issuer == null || this.algId == null || this.serialNum == null) {
            throw new NullPointerException("X.509 cert is incomplete");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        sb.append("  " + this.version.toString() + "\n");
        sb.append("  Subject: " + this.subject.toString() + "\n");
        sb.append("  Signature Algorithm: " + this.algId.toString() + "\n");
        sb.append("  Key:  " + this.pubKey.toString() + "\n");
        sb.append("  " + this.interval.toString() + "\n");
        sb.append("  Issuer: " + this.issuer.toString() + "\n");
        sb.append("  " + this.serialNum.toString() + "\n");
        if (this.issuerUniqueId != null) {
            sb.append("  Issuer Id:\n" + this.issuerUniqueId.toString() + "\n");
        }
        if (this.subjectUniqueId != null) {
            sb.append("  Subject Id:\n" + this.subjectUniqueId.toString() + "\n");
        }
        if (this.extensions != null) {
            int i = 0;
            Extension[] extensionArr = (Extension[]) this.extensions.getAllExtensions().toArray(new Extension[0]);
            sb.append("\nCertificate Extensions: " + extensionArr.length);
            while (i < extensionArr.length) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("\n[");
                int i2 = i + 1;
                sb2.append(i2);
                sb2.append("]: ");
                sb.append(sb2.toString());
                Extension extension = extensionArr[i];
                try {
                    if (OIDMap.getClass(extension.getExtensionId()) == null) {
                        sb.append(extension.toString());
                        byte[] extensionValue = extension.getExtensionValue();
                        if (extensionValue != null) {
                            DerOutputStream derOutputStream = new DerOutputStream();
                            derOutputStream.putOctetString(extensionValue);
                            byte[] byteArray = derOutputStream.toByteArray();
                            sb.append("Extension unknown: DER encoded OCTET string =\n" + new HexDumpEncoder().encodeBuffer(byteArray) + "\n");
                        }
                    } else {
                        sb.append(extension.toString());
                    }
                } catch (Exception e) {
                    sb.append(", Error parsing this extension");
                }
                i = i2;
            }
            Map<String, Extension> unparseableExtensions = this.extensions.getUnparseableExtensions();
            if (!unparseableExtensions.isEmpty()) {
                sb.append("\nUnparseable certificate extensions: " + unparseableExtensions.size());
                int i3 = 1;
                for (Extension extension2 : unparseableExtensions.values()) {
                    sb.append("\n[" + i3 + "]: ");
                    sb.append((Object) extension2);
                    i3++;
                }
            }
        }
        sb.append("\n]");
        return sb.toString();
    }

    @Override
    public void set(String str, Object obj) throws IOException, CertificateException {
        X509AttributeName x509AttributeName = new X509AttributeName(str);
        int iAttributeMap = attributeMap(x509AttributeName.getPrefix());
        if (iAttributeMap == 0) {
            throw new CertificateException("Attribute name not recognized: " + str);
        }
        this.rawCertInfo = null;
        String suffix = x509AttributeName.getSuffix();
        switch (iAttributeMap) {
            case 1:
                if (suffix == null) {
                    setVersion(obj);
                    return;
                } else {
                    this.version.set(suffix, obj);
                    return;
                }
            case 2:
                if (suffix == null) {
                    setSerialNumber(obj);
                    return;
                } else {
                    this.serialNum.set(suffix, obj);
                    return;
                }
            case 3:
                if (suffix == null) {
                    setAlgorithmId(obj);
                    return;
                } else {
                    this.algId.set(suffix, obj);
                    return;
                }
            case 4:
                setIssuer(obj);
                return;
            case 5:
                if (suffix == null) {
                    setValidity(obj);
                    return;
                } else {
                    this.interval.set(suffix, obj);
                    return;
                }
            case 6:
                setSubject(obj);
                return;
            case 7:
                if (suffix == null) {
                    setKey(obj);
                    return;
                } else {
                    this.pubKey.set(suffix, obj);
                    return;
                }
            case 8:
                setIssuerUniqueId(obj);
                return;
            case 9:
                setSubjectUniqueId(obj);
                return;
            case 10:
                if (suffix == null) {
                    setExtensions(obj);
                    return;
                }
                if (this.extensions == null) {
                    this.extensions = new CertificateExtensions();
                }
                this.extensions.set(suffix, obj);
                return;
            default:
                return;
        }
    }

    @Override
    public void delete(String str) throws IOException, CertificateException {
        X509AttributeName x509AttributeName = new X509AttributeName(str);
        int iAttributeMap = attributeMap(x509AttributeName.getPrefix());
        if (iAttributeMap == 0) {
            throw new CertificateException("Attribute name not recognized: " + str);
        }
        this.rawCertInfo = null;
        String suffix = x509AttributeName.getSuffix();
        switch (iAttributeMap) {
            case 1:
                if (suffix == null) {
                    this.version = null;
                    return;
                } else {
                    this.version.delete(suffix);
                    return;
                }
            case 2:
                if (suffix == null) {
                    this.serialNum = null;
                    return;
                } else {
                    this.serialNum.delete(suffix);
                    return;
                }
            case 3:
                if (suffix == null) {
                    this.algId = null;
                    return;
                } else {
                    this.algId.delete(suffix);
                    return;
                }
            case 4:
                this.issuer = null;
                return;
            case 5:
                if (suffix == null) {
                    this.interval = null;
                    return;
                } else {
                    this.interval.delete(suffix);
                    return;
                }
            case 6:
                this.subject = null;
                return;
            case 7:
                if (suffix == null) {
                    this.pubKey = null;
                    return;
                } else {
                    this.pubKey.delete(suffix);
                    return;
                }
            case 8:
                this.issuerUniqueId = null;
                return;
            case 9:
                this.subjectUniqueId = null;
                return;
            case 10:
                if (suffix == null) {
                    this.extensions = null;
                    return;
                } else {
                    if (this.extensions != null) {
                        this.extensions.delete(suffix);
                        return;
                    }
                    return;
                }
            default:
                return;
        }
    }

    @Override
    public Object get(String str) throws IOException, CertificateException {
        X509AttributeName x509AttributeName = new X509AttributeName(str);
        int iAttributeMap = attributeMap(x509AttributeName.getPrefix());
        if (iAttributeMap == 0) {
            throw new CertificateParsingException("Attribute name not recognized: " + str);
        }
        String suffix = x509AttributeName.getSuffix();
        switch (iAttributeMap) {
            case 1:
                if (suffix == null) {
                    return this.version;
                }
                return this.version.get(suffix);
            case 2:
                if (suffix == null) {
                    return this.serialNum;
                }
                return this.serialNum.get(suffix);
            case 3:
                if (suffix == null) {
                    return this.algId;
                }
                return this.algId.get(suffix);
            case 4:
                if (suffix == null) {
                    return this.issuer;
                }
                return getX500Name(suffix, true);
            case 5:
                if (suffix == null) {
                    return this.interval;
                }
                return this.interval.get(suffix);
            case 6:
                if (suffix == null) {
                    return this.subject;
                }
                return getX500Name(suffix, false);
            case 7:
                if (suffix == null) {
                    return this.pubKey;
                }
                return this.pubKey.get(suffix);
            case 8:
                return this.issuerUniqueId;
            case 9:
                return this.subjectUniqueId;
            case 10:
                if (suffix == null) {
                    return this.extensions;
                }
                if (this.extensions == null) {
                    return null;
                }
                return this.extensions.get(suffix);
            default:
                return null;
        }
    }

    private Object getX500Name(String str, boolean z) throws IOException {
        if (str.equalsIgnoreCase("dname")) {
            return z ? this.issuer : this.subject;
        }
        if (str.equalsIgnoreCase("x500principal")) {
            return z ? this.issuer.asX500Principal() : this.subject.asX500Principal();
        }
        throw new IOException("Attribute name not recognized.");
    }

    private void parse(DerValue derValue) throws CertificateParsingException, IOException {
        if (derValue.tag != 48) {
            throw new CertificateParsingException("signed fields invalid");
        }
        this.rawCertInfo = derValue.toByteArray();
        DerInputStream derInputStream = derValue.data;
        DerValue derValue2 = derInputStream.getDerValue();
        if (derValue2.isContextSpecific((byte) 0)) {
            this.version = new CertificateVersion(derValue2);
            derValue2 = derInputStream.getDerValue();
        }
        this.serialNum = new CertificateSerialNumber(derValue2);
        this.algId = new CertificateAlgorithmId(derInputStream);
        this.issuer = new X500Name(derInputStream);
        if (this.issuer.isEmpty()) {
            throw new CertificateParsingException("Empty issuer DN not allowed in X509Certificates");
        }
        this.interval = new CertificateValidity(derInputStream);
        this.subject = new X500Name(derInputStream);
        if (this.version.compare(0) == 0 && this.subject.isEmpty()) {
            throw new CertificateParsingException("Empty subject DN not allowed in v1 certificate");
        }
        this.pubKey = new CertificateX509Key(derInputStream);
        if (derInputStream.available() != 0) {
            if (this.version.compare(0) == 0) {
                throw new CertificateParsingException("no more data allowed for version 1 certificate");
            }
            DerValue derValue3 = derInputStream.getDerValue();
            if (derValue3.isContextSpecific((byte) 1)) {
                this.issuerUniqueId = new UniqueIdentity(derValue3);
                if (derInputStream.available() == 0) {
                    return;
                } else {
                    derValue3 = derInputStream.getDerValue();
                }
            }
            if (derValue3.isContextSpecific((byte) 2)) {
                this.subjectUniqueId = new UniqueIdentity(derValue3);
                if (derInputStream.available() == 0) {
                    return;
                } else {
                    derValue3 = derInputStream.getDerValue();
                }
            }
            if (this.version.compare(2) != 0) {
                throw new CertificateParsingException("Extensions not allowed in v2 certificate");
            }
            if (derValue3.isConstructed() && derValue3.isContextSpecific((byte) 3)) {
                this.extensions = new CertificateExtensions(derValue3.data);
            }
            verifyCert(this.subject, this.extensions);
        }
    }

    private void verifyCert(X500Name x500Name, CertificateExtensions certificateExtensions) throws CertificateParsingException, IOException {
        if (x500Name.isEmpty()) {
            if (certificateExtensions == null) {
                throw new CertificateParsingException("X.509 Certificate is incomplete: subject field is empty, and certificate has no extensions");
            }
            try {
                SubjectAlternativeNameExtension subjectAlternativeNameExtension = (SubjectAlternativeNameExtension) certificateExtensions.get(SubjectAlternativeNameExtension.NAME);
                GeneralNames generalNames = subjectAlternativeNameExtension.get(SubjectAlternativeNameExtension.SUBJECT_NAME);
                if (generalNames == null || generalNames.isEmpty()) {
                    throw new CertificateParsingException("X.509 Certificate is incomplete: subject field is empty, and SubjectAlternativeName extension is empty");
                }
                if (!subjectAlternativeNameExtension.isCritical()) {
                    throw new CertificateParsingException("X.509 Certificate is incomplete: SubjectAlternativeName extension MUST be marked critical when subject field is empty");
                }
            } catch (IOException e) {
                throw new CertificateParsingException("X.509 Certificate is incomplete: subject field is empty, and SubjectAlternativeName extension is absent");
            }
        }
    }

    private void emit(DerOutputStream derOutputStream) throws IOException, CertificateException {
        DerOutputStream derOutputStream2 = new DerOutputStream();
        this.version.encode(derOutputStream2);
        this.serialNum.encode(derOutputStream2);
        this.algId.encode(derOutputStream2);
        if (this.version.compare(0) == 0 && this.issuer.toString() == null) {
            throw new CertificateParsingException("Null issuer DN not allowed in v1 certificate");
        }
        this.issuer.encode(derOutputStream2);
        this.interval.encode(derOutputStream2);
        if (this.version.compare(0) == 0 && this.subject.toString() == null) {
            throw new CertificateParsingException("Null subject DN not allowed in v1 certificate");
        }
        this.subject.encode(derOutputStream2);
        this.pubKey.encode(derOutputStream2);
        if (this.issuerUniqueId != null) {
            this.issuerUniqueId.encode(derOutputStream2, DerValue.createTag((byte) -128, false, (byte) 1));
        }
        if (this.subjectUniqueId != null) {
            this.subjectUniqueId.encode(derOutputStream2, DerValue.createTag((byte) -128, false, (byte) 2));
        }
        if (this.extensions != null) {
            this.extensions.encode(derOutputStream2);
        }
        derOutputStream.write((byte) 48, derOutputStream2);
    }

    private int attributeMap(String str) {
        Integer num = map.get(str);
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }

    private void setVersion(Object obj) throws CertificateException {
        if (!(obj instanceof CertificateVersion)) {
            throw new CertificateException("Version class type invalid.");
        }
        this.version = (CertificateVersion) obj;
    }

    private void setSerialNumber(Object obj) throws CertificateException {
        if (!(obj instanceof CertificateSerialNumber)) {
            throw new CertificateException("SerialNumber class type invalid.");
        }
        this.serialNum = (CertificateSerialNumber) obj;
    }

    private void setAlgorithmId(Object obj) throws CertificateException {
        if (!(obj instanceof CertificateAlgorithmId)) {
            throw new CertificateException("AlgorithmId class type invalid.");
        }
        this.algId = (CertificateAlgorithmId) obj;
    }

    private void setIssuer(Object obj) throws CertificateException {
        if (!(obj instanceof X500Name)) {
            throw new CertificateException("Issuer class type invalid.");
        }
        this.issuer = (X500Name) obj;
    }

    private void setValidity(Object obj) throws CertificateException {
        if (!(obj instanceof CertificateValidity)) {
            throw new CertificateException("CertificateValidity class type invalid.");
        }
        this.interval = (CertificateValidity) obj;
    }

    private void setSubject(Object obj) throws CertificateException {
        if (!(obj instanceof X500Name)) {
            throw new CertificateException("Subject class type invalid.");
        }
        this.subject = (X500Name) obj;
    }

    private void setKey(Object obj) throws CertificateException {
        if (!(obj instanceof CertificateX509Key)) {
            throw new CertificateException("Key class type invalid.");
        }
        this.pubKey = (CertificateX509Key) obj;
    }

    private void setIssuerUniqueId(Object obj) throws CertificateException {
        if (this.version.compare(1) < 0) {
            throw new CertificateException("Invalid version");
        }
        if (!(obj instanceof UniqueIdentity)) {
            throw new CertificateException("IssuerUniqueId class type invalid.");
        }
        this.issuerUniqueId = (UniqueIdentity) obj;
    }

    private void setSubjectUniqueId(Object obj) throws CertificateException {
        if (this.version.compare(1) < 0) {
            throw new CertificateException("Invalid version");
        }
        if (!(obj instanceof UniqueIdentity)) {
            throw new CertificateException("SubjectUniqueId class type invalid.");
        }
        this.subjectUniqueId = (UniqueIdentity) obj;
    }

    private void setExtensions(Object obj) throws CertificateException {
        if (this.version.compare(2) < 0) {
            throw new CertificateException("Invalid version");
        }
        if (!(obj instanceof CertificateExtensions)) {
            throw new CertificateException("Extensions class type invalid.");
        }
        this.extensions = (CertificateExtensions) obj;
    }
}
