package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import libcore.util.EmptyArray;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.utils.Array;
import org.apache.harmony.security.x509.AlgorithmIdentifier;
import org.apache.harmony.security.x509.CertificatePolicies;
import org.apache.harmony.security.x509.GeneralName;
import org.apache.harmony.security.x509.GeneralNames;
import org.apache.harmony.security.x509.NameConstraints;
import org.apache.harmony.security.x509.PolicyInformation;
import org.apache.harmony.security.x509.PrivateKeyUsagePeriod;
import org.apache.harmony.security.x509.SubjectPublicKeyInfo;

public class X509CertSelector implements CertSelector {
    private byte[] authorityKeyIdentifier;
    private X509Certificate certificateEquals;
    private Date certificateValid;
    private Set<String> extendedKeyUsage;
    private X500Principal issuer;
    private byte[] issuerBytes;
    private String issuerName;
    private boolean[] keyUsage;
    private NameConstraints nameConstraints;
    private ArrayList<GeneralName> pathToNames;
    private Set<String> policies;
    private Date privateKeyValid;
    private BigInteger serialNumber;
    private X500Principal subject;
    private List<GeneralName>[] subjectAltNames;
    private byte[] subjectKeyIdentifier;
    private byte[] subjectPublicKey;
    private String subjectPublicKeyAlgID;
    private PublicKey subjectPublicKeyImpl;
    private boolean matchAllNames = true;
    private int pathLen = -1;

    public void setCertificate(X509Certificate certificate) {
        this.certificateEquals = certificate;
    }

    public X509Certificate getCertificate() {
        return this.certificateEquals;
    }

    public void setSerialNumber(BigInteger serialNumber) {
        this.serialNumber = serialNumber;
    }

    public BigInteger getSerialNumber() {
        return this.serialNumber;
    }

    public void setIssuer(X500Principal issuer) {
        this.issuer = issuer;
        this.issuerName = null;
        this.issuerBytes = null;
    }

    public X500Principal getIssuer() {
        return this.issuer;
    }

    public void setIssuer(String issuerName) throws IOException {
        if (issuerName == null) {
            this.issuer = null;
            this.issuerName = null;
            this.issuerBytes = null;
        } else {
            try {
                this.issuer = new X500Principal(issuerName);
                this.issuerName = issuerName;
                this.issuerBytes = null;
            } catch (IllegalArgumentException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    public String getIssuerAsString() {
        if (this.issuer == null) {
            return null;
        }
        if (this.issuerName == null) {
            this.issuerName = this.issuer.getName();
        }
        return this.issuerName;
    }

    public void setIssuer(byte[] issuerDN) throws IOException {
        if (issuerDN == null) {
            this.issuer = null;
            return;
        }
        try {
            this.issuer = new X500Principal(issuerDN);
            this.issuerName = null;
            this.issuerBytes = new byte[issuerDN.length];
            System.arraycopy(issuerDN, 0, this.issuerBytes, 0, issuerDN.length);
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    public byte[] getIssuerAsBytes() throws IOException {
        if (this.issuer == null) {
            return null;
        }
        if (this.issuerBytes == null) {
            this.issuerBytes = this.issuer.getEncoded();
        }
        byte[] result = new byte[this.issuerBytes.length];
        System.arraycopy(this.issuerBytes, 0, result, 0, this.issuerBytes.length);
        return result;
    }

    public void setSubject(X500Principal subject) {
        this.subject = subject;
    }

    public X500Principal getSubject() {
        return this.subject;
    }

    public void setSubject(String subjectDN) throws IOException {
        if (subjectDN == null) {
            this.subject = null;
            return;
        }
        try {
            this.subject = new X500Principal(subjectDN);
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    public String getSubjectAsString() {
        if (this.subject == null) {
            return null;
        }
        return this.subject.getName();
    }

    public void setSubject(byte[] subjectDN) throws IOException {
        if (subjectDN == null) {
            this.subject = null;
            return;
        }
        try {
            this.subject = new X500Principal(subjectDN);
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }
    }

    public byte[] getSubjectAsBytes() throws IOException {
        if (this.subject == null) {
            return null;
        }
        return this.subject.getEncoded();
    }

    public void setSubjectKeyIdentifier(byte[] subjectKeyIdentifier) {
        if (subjectKeyIdentifier == null) {
            this.subjectKeyIdentifier = null;
        } else {
            this.subjectKeyIdentifier = new byte[subjectKeyIdentifier.length];
            System.arraycopy(subjectKeyIdentifier, 0, this.subjectKeyIdentifier, 0, subjectKeyIdentifier.length);
        }
    }

    public byte[] getSubjectKeyIdentifier() {
        if (this.subjectKeyIdentifier == null) {
            return null;
        }
        byte[] res = new byte[this.subjectKeyIdentifier.length];
        System.arraycopy(this.subjectKeyIdentifier, 0, res, 0, res.length);
        return res;
    }

    public void setAuthorityKeyIdentifier(byte[] authorityKeyIdentifier) {
        if (authorityKeyIdentifier == null) {
            this.authorityKeyIdentifier = null;
        } else {
            this.authorityKeyIdentifier = new byte[authorityKeyIdentifier.length];
            System.arraycopy(authorityKeyIdentifier, 0, this.authorityKeyIdentifier, 0, authorityKeyIdentifier.length);
        }
    }

    public byte[] getAuthorityKeyIdentifier() {
        if (this.authorityKeyIdentifier == null) {
            return null;
        }
        byte[] res = new byte[this.authorityKeyIdentifier.length];
        System.arraycopy(this.authorityKeyIdentifier, 0, res, 0, res.length);
        return res;
    }

    public void setCertificateValid(Date certificateValid) {
        this.certificateValid = certificateValid == null ? null : (Date) certificateValid.clone();
    }

    public Date getCertificateValid() {
        if (this.certificateValid == null) {
            return null;
        }
        return (Date) this.certificateValid.clone();
    }

    public void setPrivateKeyValid(Date privateKeyValid) {
        if (privateKeyValid == null) {
            this.privateKeyValid = null;
        } else {
            this.privateKeyValid = (Date) privateKeyValid.clone();
        }
    }

    public Date getPrivateKeyValid() {
        if (this.privateKeyValid != null) {
            return (Date) this.privateKeyValid.clone();
        }
        return null;
    }

    private void checkOID(String oid) throws IOException {
        int end = oid.indexOf(46, 0);
        try {
            int comp = Integer.parseInt(oid.substring(0, end));
            int beg = end + 1;
            if (comp < 0 || comp > 2) {
                throw new IOException("Bad OID: " + oid);
            }
            int comp2 = Integer.parseInt(oid.substring(beg, oid.indexOf(46, beg)));
            if (comp2 < 0 || comp2 > 39) {
                throw new IOException("Bad OID: " + oid);
            }
        } catch (IndexOutOfBoundsException e) {
            throw new IOException("Bad OID: " + oid);
        } catch (NumberFormatException e2) {
            throw new IOException("Bad OID: " + oid);
        }
    }

    public void setSubjectPublicKeyAlgID(String oid) throws IOException {
        if (oid == null) {
            this.subjectPublicKeyAlgID = null;
        } else {
            checkOID(oid);
            this.subjectPublicKeyAlgID = oid;
        }
    }

    public String getSubjectPublicKeyAlgID() {
        return this.subjectPublicKeyAlgID;
    }

    public void setSubjectPublicKey(PublicKey key) {
        this.subjectPublicKey = key == null ? null : key.getEncoded();
        this.subjectPublicKeyImpl = key;
    }

    public void setSubjectPublicKey(byte[] key) throws IOException {
        if (key == null) {
            this.subjectPublicKey = null;
            this.subjectPublicKeyImpl = null;
        } else {
            this.subjectPublicKey = new byte[key.length];
            System.arraycopy(key, 0, this.subjectPublicKey, 0, key.length);
            this.subjectPublicKeyImpl = ((SubjectPublicKeyInfo) SubjectPublicKeyInfo.ASN1.decode(key)).getPublicKey();
        }
    }

    public PublicKey getSubjectPublicKey() {
        return this.subjectPublicKeyImpl;
    }

    public void setKeyUsage(boolean[] keyUsage) {
        if (keyUsage == null) {
            this.keyUsage = null;
        } else {
            this.keyUsage = new boolean[keyUsage.length];
            System.arraycopy(keyUsage, 0, this.keyUsage, 0, keyUsage.length);
        }
    }

    public boolean[] getKeyUsage() {
        if (this.keyUsage == null) {
            return null;
        }
        boolean[] result = new boolean[this.keyUsage.length];
        System.arraycopy(this.keyUsage, 0, result, 0, this.keyUsage.length);
        return result;
    }

    public void setExtendedKeyUsage(Set<String> keyUsage) throws IOException {
        this.extendedKeyUsage = null;
        if (keyUsage != null && keyUsage.size() != 0) {
            HashSet<String> key_u = new HashSet<>();
            for (String usage : keyUsage) {
                checkOID(usage);
                key_u.add(usage);
            }
            this.extendedKeyUsage = Collections.unmodifiableSet(key_u);
        }
    }

    public Set<String> getExtendedKeyUsage() {
        return this.extendedKeyUsage;
    }

    public void setMatchAllSubjectAltNames(boolean matchAllNames) {
        this.matchAllNames = matchAllNames;
    }

    public boolean getMatchAllSubjectAltNames() {
        return this.matchAllNames;
    }

    public void setSubjectAlternativeNames(Collection<List<?>> names) throws IOException {
        this.subjectAltNames = null;
        if (names != null && names.size() != 0) {
            for (List<?> name : names) {
                int tag = ((Integer) name.get(0)).intValue();
                Object value = name.get(1);
                if (value instanceof String) {
                    addSubjectAlternativeName(tag, (String) value);
                } else if (value instanceof byte[]) {
                    addSubjectAlternativeName(tag, (byte[]) value);
                } else {
                    throw new IOException("name neither a String nor a byte[]");
                }
            }
        }
    }

    public void addSubjectAlternativeName(int tag, String name) throws IOException {
        GeneralName alt_name = new GeneralName(tag, name);
        if (this.subjectAltNames == null) {
            this.subjectAltNames = new ArrayList[9];
        }
        if (this.subjectAltNames[tag] == null) {
            this.subjectAltNames[tag] = new ArrayList();
        }
        this.subjectAltNames[tag].add(alt_name);
    }

    public void addSubjectAlternativeName(int tag, byte[] name) throws IOException {
        GeneralName alt_name = new GeneralName(tag, name);
        if (this.subjectAltNames == null) {
            this.subjectAltNames = new ArrayList[9];
        }
        if (this.subjectAltNames[tag] == null) {
            this.subjectAltNames[tag] = new ArrayList();
        }
        this.subjectAltNames[tag].add(alt_name);
    }

    public Collection<List<?>> getSubjectAlternativeNames() {
        if (this.subjectAltNames == null) {
            return null;
        }
        ArrayList<List<?>> result = new ArrayList<>();
        for (int tag = 0; tag < 9; tag++) {
            if (this.subjectAltNames[tag] != null) {
                for (int name = 0; name < this.subjectAltNames[tag].size(); name++) {
                    List<?> arrayList = new ArrayList<>(2);
                    arrayList.add(Integer.valueOf(tag));
                    arrayList.add(this.subjectAltNames[tag].get(name));
                    result.add(arrayList);
                }
            }
        }
        return result;
    }

    public void setNameConstraints(byte[] bytes) throws IOException {
        this.nameConstraints = bytes == null ? null : (NameConstraints) NameConstraints.ASN1.decode(bytes);
    }

    public byte[] getNameConstraints() {
        if (this.nameConstraints == null) {
            return null;
        }
        return this.nameConstraints.getEncoded();
    }

    public void setBasicConstraints(int pathLen) {
        if (pathLen < -2) {
            throw new IllegalArgumentException("pathLen < -2");
        }
        this.pathLen = pathLen;
    }

    public int getBasicConstraints() {
        return this.pathLen;
    }

    public void setPolicy(Set<String> policies) throws IOException {
        if (policies == null) {
            this.policies = null;
            return;
        }
        HashSet<String> pols = new HashSet<>(policies.size());
        for (String certPolicyId : policies) {
            checkOID(certPolicyId);
            pols.add(certPolicyId);
        }
        this.policies = Collections.unmodifiableSet(pols);
    }

    public Set<String> getPolicy() {
        return this.policies;
    }

    public void addPathToName(int type, String name) throws IOException {
        GeneralName path_name = new GeneralName(type, name);
        if (this.pathToNames == null) {
            this.pathToNames = new ArrayList<>();
        }
        this.pathToNames.add(path_name);
    }

    public void setPathToNames(Collection<List<?>> names) throws IOException {
        this.pathToNames = null;
        if (names != null && names.size() != 0) {
            for (List<?> name : names) {
                int tag = ((Integer) name.get(0)).intValue();
                Object value = name.get(1);
                if (value instanceof String) {
                    addPathToName(tag, (String) value);
                } else if (value instanceof byte[]) {
                    addPathToName(tag, (byte[]) value);
                } else {
                    throw new IOException("name neither a String nor a byte[]");
                }
            }
        }
    }

    public void addPathToName(int type, byte[] name) throws IOException {
        GeneralName path_name = new GeneralName(type, name);
        if (this.pathToNames == null) {
            this.pathToNames = new ArrayList<>();
        }
        this.pathToNames.add(path_name);
    }

    public Collection<List<?>> getPathToNames() {
        if (this.pathToNames == null) {
            return null;
        }
        Collection<List<?>> result = new ArrayList<>();
        for (GeneralName name : this.pathToNames) {
            result.add(name.getAsList());
        }
        return result;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("X509CertSelector: \n[");
        if (this.certificateEquals != null) {
            result.append("\n  certificateEquals: ").append(this.certificateEquals);
        }
        if (this.serialNumber != null) {
            result.append("\n  serialNumber: ").append(this.serialNumber);
        }
        if (this.issuer != null) {
            result.append("\n  issuer: ").append(this.issuer);
        }
        if (this.subject != null) {
            result.append("\n  subject: ").append(this.subject);
        }
        if (this.subjectKeyIdentifier != null) {
            result.append("\n  subjectKeyIdentifier: ").append(Array.getBytesAsString(this.subjectKeyIdentifier));
        }
        if (this.authorityKeyIdentifier != null) {
            result.append("\n  authorityKeyIdentifier: ").append(Array.getBytesAsString(this.authorityKeyIdentifier));
        }
        if (this.certificateValid != null) {
            result.append("\n  certificateValid: ").append(this.certificateValid);
        }
        if (this.subjectPublicKeyAlgID != null) {
            result.append("\n  subjectPublicKeyAlgID: ").append(this.subjectPublicKeyAlgID);
        }
        if (this.privateKeyValid != null) {
            result.append("\n  privateKeyValid: ").append(this.privateKeyValid);
        }
        if (this.subjectPublicKey != null) {
            result.append("\n  subjectPublicKey: ").append(Array.getBytesAsString(this.subjectPublicKey));
        }
        if (this.keyUsage != null) {
            result.append("\n  keyUsage: \n  [");
            String[] kuNames = {"digitalSignature", "nonRepudiation", "keyEncipherment", "dataEncipherment", "keyAgreement", "keyCertSign", "cRLSign", "encipherOnly", "decipherOnly"};
            for (int i = 0; i < 9; i++) {
                if (this.keyUsage[i]) {
                    result.append("\n    ").append(kuNames[i]);
                }
            }
            result.append("\n  ]");
        }
        if (this.extendedKeyUsage != null) {
            result.append("\n  extendedKeyUsage: ").append(this.extendedKeyUsage.toString());
        }
        result.append("\n  matchAllNames: ").append(this.matchAllNames);
        result.append("\n  pathLen: ").append(this.pathLen);
        if (this.subjectAltNames != null) {
            result.append("\n  subjectAltNames:  \n  [");
            for (int i2 = 0; i2 < 9; i2++) {
                List<GeneralName> names = this.subjectAltNames[i2];
                if (names != null) {
                    names.size();
                    for (GeneralName generalName : names) {
                        result.append("\n    ").append(generalName.toString());
                    }
                }
            }
            result.append("\n  ]");
        }
        if (this.nameConstraints != null) {
        }
        if (this.policies != null) {
            result.append("\n  policies: ").append(this.policies.toString());
        }
        if (this.pathToNames != null) {
            result.append("\n  pathToNames:  \n  [");
            for (GeneralName generalName2 : this.pathToNames) {
                result.append("\n    ").append(generalName2.toString());
            }
        }
        result.append("\n]");
        return result.toString();
    }

    private byte[] getExtensionValue(X509Certificate cert, String oid) {
        try {
            byte[] bytes = cert.getExtensionValue(oid);
            if (bytes == null) {
                return null;
            }
            return (byte[]) ASN1OctetString.getInstance().decode(bytes);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean match(Certificate certificate) {
        byte[] bytes;
        boolean[] ku;
        if (!(certificate instanceof X509Certificate)) {
            return false;
        }
        X509Certificate cert = (X509Certificate) certificate;
        if (this.certificateEquals != null && !this.certificateEquals.equals(cert)) {
            return false;
        }
        if (this.serialNumber != null && !this.serialNumber.equals(cert.getSerialNumber())) {
            return false;
        }
        if (this.issuer != null && !this.issuer.equals(cert.getIssuerX500Principal())) {
            return false;
        }
        if (this.subject != null && !this.subject.equals(cert.getSubjectX500Principal())) {
            return false;
        }
        if (this.subjectKeyIdentifier != null && !Arrays.equals(this.subjectKeyIdentifier, getExtensionValue(cert, "2.5.29.14"))) {
            return false;
        }
        if (this.authorityKeyIdentifier != null && !Arrays.equals(this.authorityKeyIdentifier, getExtensionValue(cert, "2.5.29.35"))) {
            return false;
        }
        if (this.certificateValid != null) {
            try {
                cert.checkValidity(this.certificateValid);
            } catch (CertificateExpiredException e) {
                return false;
            } catch (CertificateNotYetValidException e2) {
                return false;
            }
        }
        if (this.privateKeyValid != null) {
            try {
                byte[] bytes2 = getExtensionValue(cert, "2.5.29.16");
                if (bytes2 == null) {
                    return false;
                }
                PrivateKeyUsagePeriod pkup = (PrivateKeyUsagePeriod) PrivateKeyUsagePeriod.ASN1.decode(bytes2);
                Date notBefore = pkup.getNotBefore();
                Date notAfter = pkup.getNotAfter();
                if (notBefore == null && notAfter == null) {
                    return false;
                }
                if (notBefore != null && notBefore.compareTo(this.privateKeyValid) > 0) {
                    return false;
                }
                if (notAfter != null) {
                    if (notAfter.compareTo(this.privateKeyValid) < 0) {
                        return false;
                    }
                }
            } catch (IOException e3) {
                return false;
            }
        }
        if (this.subjectPublicKeyAlgID != null) {
            try {
                byte[] encoding = cert.getPublicKey().getEncoded();
                AlgorithmIdentifier ai = ((SubjectPublicKeyInfo) SubjectPublicKeyInfo.ASN1.decode(encoding)).getAlgorithmIdentifier();
                if (!this.subjectPublicKeyAlgID.equals(ai.getAlgorithm())) {
                    return false;
                }
            } catch (IOException e4) {
                e4.printStackTrace();
                return false;
            }
        }
        if (this.subjectPublicKey != null && !Arrays.equals(this.subjectPublicKey, cert.getPublicKey().getEncoded())) {
            return false;
        }
        if (this.keyUsage != null && (ku = cert.getKeyUsage()) != null) {
            int i = 0;
            int min_length = ku.length < this.keyUsage.length ? ku.length : this.keyUsage.length;
            while (i < min_length) {
                if (!this.keyUsage[i] || ku[i]) {
                    i++;
                } else {
                    return false;
                }
            }
            while (i < this.keyUsage.length) {
                if (!this.keyUsage[i]) {
                    i++;
                } else {
                    return false;
                }
            }
        }
        if (this.extendedKeyUsage != null) {
            try {
                List<String> extendedKeyUsage = cert.getExtendedKeyUsage();
                if (extendedKeyUsage != null) {
                    if (!extendedKeyUsage.containsAll(this.extendedKeyUsage)) {
                        return false;
                    }
                }
            } catch (CertificateParsingException e5) {
                return false;
            }
        }
        if (this.pathLen != -1) {
            int p_len = cert.getBasicConstraints();
            if (this.pathLen < 0 && p_len >= 0) {
                return false;
            }
            if (this.pathLen > 0 && this.pathLen > p_len) {
                return false;
            }
        }
        if (this.subjectAltNames != null) {
            try {
                byte[] bytes3 = getExtensionValue(cert, "2.5.29.17");
                if (bytes3 == null) {
                    return false;
                }
                List<GeneralName> sans = ((GeneralNames) GeneralNames.ASN1.decode(bytes3)).getNames();
                if (sans == null || sans.size() == 0) {
                    return false;
                }
                boolean[][] map = new boolean[9][];
                for (int i2 = 0; i2 < 9; i2++) {
                    map[i2] = this.subjectAltNames[i2] == null ? EmptyArray.BOOLEAN : new boolean[this.subjectAltNames[i2].size()];
                }
                Iterator<GeneralName> it = sans.iterator();
                loop4: while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    GeneralName name = it.next();
                    int tag = name.getTag();
                    for (int i3 = 0; i3 < map[tag].length; i3++) {
                        if (this.subjectAltNames[tag].get(i3).equals(name)) {
                            if (!this.matchAllNames) {
                                break loop4;
                            }
                            map[tag][i3] = true;
                        }
                    }
                }
            } catch (IOException e6) {
                e6.printStackTrace();
                return false;
            }
        }
        if (this.nameConstraints != null && !this.nameConstraints.isAcceptable(cert)) {
            return false;
        }
        if (this.policies != null) {
            byte[] bytes4 = getExtensionValue(cert, "2.5.29.32");
            if (bytes4 == null) {
                return false;
            }
            if (this.policies.size() == 0) {
                return true;
            }
            try {
                List<PolicyInformation> policyInformations = ((CertificatePolicies) CertificatePolicies.ASN1.decode(bytes4)).getPolicyInformations();
                for (PolicyInformation policyInformation : policyInformations) {
                    if (this.policies.contains(policyInformation.getPolicyIdentifier())) {
                    }
                }
                return false;
            } catch (IOException e7) {
                return false;
            }
        }
        if (this.pathToNames != null && (bytes = getExtensionValue(cert, "2.5.29.30")) != null) {
            try {
                NameConstraints nameConstraints = (NameConstraints) NameConstraints.ASN1.decode(bytes);
                if (!nameConstraints.isAcceptable(this.pathToNames)) {
                    return false;
                }
            } catch (IOException e8) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Object clone() {
        try {
            X509CertSelector result = (X509CertSelector) super.clone();
            if (this.subjectKeyIdentifier != null) {
                result.subjectKeyIdentifier = new byte[this.subjectKeyIdentifier.length];
                System.arraycopy(this.subjectKeyIdentifier, 0, result.subjectKeyIdentifier, 0, this.subjectKeyIdentifier.length);
            }
            if (this.authorityKeyIdentifier != null) {
                result.authorityKeyIdentifier = new byte[this.authorityKeyIdentifier.length];
                System.arraycopy(this.authorityKeyIdentifier, 0, result.authorityKeyIdentifier, 0, this.authorityKeyIdentifier.length);
            }
            if (this.subjectPublicKey != null) {
                result.subjectPublicKey = new byte[this.subjectPublicKey.length];
                System.arraycopy(this.subjectPublicKey, 0, result.subjectPublicKey, 0, this.subjectPublicKey.length);
            }
            if (this.keyUsage != null) {
                result.keyUsage = new boolean[this.keyUsage.length];
                System.arraycopy(this.keyUsage, 0, result.keyUsage, 0, this.keyUsage.length);
            }
            result.extendedKeyUsage = this.extendedKeyUsage == null ? null : new HashSet(this.extendedKeyUsage);
            if (this.subjectAltNames != null) {
                result.subjectAltNames = new ArrayList[9];
                for (int i = 0; i < 9; i++) {
                    if (this.subjectAltNames[i] != null) {
                        result.subjectAltNames[i] = new ArrayList(this.subjectAltNames[i]);
                    }
                }
            }
            result.policies = this.policies == null ? null : new HashSet(this.policies);
            result.pathToNames = this.pathToNames != null ? new ArrayList<>(this.pathToNames) : null;
            return result;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
