package sun.security.x509;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CRLException;
import java.security.cert.CRLReason;
import java.security.cert.X509CRLEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.security.auth.x500.X500Principal;
import sun.misc.HexDumpEncoder;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class X509CRLEntryImpl extends X509CRLEntry implements Comparable<X509CRLEntryImpl> {
    private static final long YR_2050 = 2524636800000L;
    private static final boolean isExplicit = false;
    private X500Principal certIssuer;
    private CRLExtensions extensions;
    private Date revocationDate;
    private byte[] revokedCert;
    private SerialNumber serialNumber;

    public X509CRLEntryImpl(BigInteger bigInteger, Date date) {
        this.serialNumber = null;
        this.revocationDate = null;
        this.extensions = null;
        this.revokedCert = null;
        this.serialNumber = new SerialNumber(bigInteger);
        this.revocationDate = date;
    }

    public X509CRLEntryImpl(BigInteger bigInteger, Date date, CRLExtensions cRLExtensions) {
        this.serialNumber = null;
        this.revocationDate = null;
        this.extensions = null;
        this.revokedCert = null;
        this.serialNumber = new SerialNumber(bigInteger);
        this.revocationDate = date;
        this.extensions = cRLExtensions;
    }

    public X509CRLEntryImpl(byte[] bArr) throws CRLException {
        this.serialNumber = null;
        this.revocationDate = null;
        this.extensions = null;
        this.revokedCert = null;
        try {
            parse(new DerValue(bArr));
        } catch (IOException e) {
            this.revokedCert = null;
            throw new CRLException("Parsing error: " + e.toString());
        }
    }

    public X509CRLEntryImpl(DerValue derValue) throws CRLException {
        this.serialNumber = null;
        this.revocationDate = null;
        this.extensions = null;
        this.revokedCert = null;
        try {
            parse(derValue);
        } catch (IOException e) {
            this.revokedCert = null;
            throw new CRLException("Parsing error: " + e.toString());
        }
    }

    @Override
    public boolean hasExtensions() {
        if (this.extensions != null) {
            return true;
        }
        return isExplicit;
    }

    public void encode(DerOutputStream derOutputStream) throws CRLException {
        try {
            if (this.revokedCert == null) {
                DerOutputStream derOutputStream2 = new DerOutputStream();
                this.serialNumber.encode(derOutputStream2);
                if (this.revocationDate.getTime() < YR_2050) {
                    derOutputStream2.putUTCTime(this.revocationDate);
                } else {
                    derOutputStream2.putGeneralizedTime(this.revocationDate);
                }
                if (this.extensions != null) {
                    this.extensions.encode(derOutputStream2, isExplicit);
                }
                DerOutputStream derOutputStream3 = new DerOutputStream();
                derOutputStream3.write((byte) 48, derOutputStream2);
                this.revokedCert = derOutputStream3.toByteArray();
            }
            derOutputStream.write(this.revokedCert);
        } catch (IOException e) {
            throw new CRLException("Encoding error: " + e.toString());
        }
    }

    @Override
    public byte[] getEncoded() throws CRLException {
        return (byte[]) getEncoded0().clone();
    }

    private byte[] getEncoded0() throws CRLException {
        if (this.revokedCert == null) {
            encode(new DerOutputStream());
        }
        return this.revokedCert;
    }

    @Override
    public X500Principal getCertificateIssuer() {
        return this.certIssuer;
    }

    void setCertificateIssuer(X500Principal x500Principal, X500Principal x500Principal2) {
        if (x500Principal.equals(x500Principal2)) {
            this.certIssuer = null;
        } else {
            this.certIssuer = x500Principal2;
        }
    }

    @Override
    public BigInteger getSerialNumber() {
        return this.serialNumber.getNumber();
    }

    @Override
    public Date getRevocationDate() {
        return new Date(this.revocationDate.getTime());
    }

    @Override
    public CRLReason getRevocationReason() {
        Extension extension = getExtension(PKIXExtensions.ReasonCode_Id);
        if (extension == null) {
            return null;
        }
        return ((CRLReasonCodeExtension) extension).getReasonCode();
    }

    public static CRLReason getRevocationReason(X509CRLEntry x509CRLEntry) {
        try {
            byte[] extensionValue = x509CRLEntry.getExtensionValue("2.5.29.21");
            if (extensionValue == null) {
                return null;
            }
            return new CRLReasonCodeExtension(Boolean.FALSE, new DerValue(extensionValue).getOctetString()).getReasonCode();
        } catch (IOException e) {
            return null;
        }
    }

    public Integer getReasonCode() throws IOException {
        Extension extension = getExtension(PKIXExtensions.ReasonCode_Id);
        if (extension == null) {
            return null;
        }
        return ((CRLReasonCodeExtension) extension).get(CRLReasonCodeExtension.REASON);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.serialNumber.toString());
        sb.append("  On: " + this.revocationDate.toString());
        if (this.certIssuer != null) {
            sb.append("\n    Certificate issuer: " + ((Object) this.certIssuer));
        }
        if (this.extensions != null) {
            int i = 0;
            Extension[] extensionArr = (Extension[]) this.extensions.getAllExtensions().toArray(new Extension[0]);
            sb.append("\n    CRL Entry Extensions: " + extensionArr.length);
            while (i < extensionArr.length) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("\n    [");
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
        }
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public boolean hasUnsupportedCriticalExtension() {
        if (this.extensions == null) {
            return isExplicit;
        }
        return this.extensions.hasUnsupportedCriticalExtension();
    }

    @Override
    public Set<String> getCriticalExtensionOIDs() {
        if (this.extensions == null) {
            return null;
        }
        TreeSet treeSet = new TreeSet();
        for (Extension extension : this.extensions.getAllExtensions()) {
            if (extension.isCritical()) {
                treeSet.add(extension.getExtensionId().toString());
            }
        }
        return treeSet;
    }

    @Override
    public Set<String> getNonCriticalExtensionOIDs() {
        if (this.extensions == null) {
            return null;
        }
        TreeSet treeSet = new TreeSet();
        for (Extension extension : this.extensions.getAllExtensions()) {
            if (!extension.isCritical()) {
                treeSet.add(extension.getExtensionId().toString());
            }
        }
        return treeSet;
    }

    @Override
    public byte[] getExtensionValue(String str) {
        Extension extensionNextElement;
        byte[] extensionValue;
        if (this.extensions == null) {
            return null;
        }
        try {
            String name = OIDMap.getName(new ObjectIdentifier(str));
            if (name == null) {
                ObjectIdentifier objectIdentifier = new ObjectIdentifier(str);
                Enumeration<Extension> elements = this.extensions.getElements();
                while (true) {
                    if (elements.hasMoreElements()) {
                        extensionNextElement = elements.nextElement();
                        if (extensionNextElement.getExtensionId().equals((Object) objectIdentifier)) {
                            break;
                        }
                    } else {
                        extensionNextElement = null;
                        break;
                    }
                }
            } else {
                extensionNextElement = this.extensions.get(name);
            }
            if (extensionNextElement == null || (extensionValue = extensionNextElement.getExtensionValue()) == null) {
                return null;
            }
            DerOutputStream derOutputStream = new DerOutputStream();
            derOutputStream.putOctetString(extensionValue);
            return derOutputStream.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public Extension getExtension(ObjectIdentifier objectIdentifier) {
        if (this.extensions == null) {
            return null;
        }
        return this.extensions.get(OIDMap.getName(objectIdentifier));
    }

    private void parse(DerValue derValue) throws IOException, CRLException {
        if (derValue.tag != 48) {
            throw new CRLException("Invalid encoded RevokedCertificate, starting sequence tag missing.");
        }
        if (derValue.data.available() == 0) {
            throw new CRLException("No data encoded for RevokedCertificates");
        }
        this.revokedCert = derValue.toByteArray();
        this.serialNumber = new SerialNumber(derValue.toDerInputStream().getDerValue());
        byte bPeekByte = (byte) derValue.data.peekByte();
        if (bPeekByte == 23) {
            this.revocationDate = derValue.data.getUTCTime();
        } else if (bPeekByte == 24) {
            this.revocationDate = derValue.data.getGeneralizedTime();
        } else {
            throw new CRLException("Invalid encoding for revocation date");
        }
        if (derValue.data.available() == 0) {
            return;
        }
        this.extensions = new CRLExtensions(derValue.toDerInputStream());
    }

    public static X509CRLEntryImpl toImpl(X509CRLEntry x509CRLEntry) throws CRLException {
        if (x509CRLEntry instanceof X509CRLEntryImpl) {
            return (X509CRLEntryImpl) x509CRLEntry;
        }
        return new X509CRLEntryImpl(x509CRLEntry.getEncoded());
    }

    CertificateIssuerExtension getCertificateIssuerExtension() {
        return (CertificateIssuerExtension) getExtension(PKIXExtensions.CertificateIssuer_Id);
    }

    public Map<String, java.security.cert.Extension> getExtensions() {
        if (this.extensions == null) {
            return Collections.emptyMap();
        }
        Collection<Extension> allExtensions = this.extensions.getAllExtensions();
        TreeMap treeMap = new TreeMap();
        for (Extension extension : allExtensions) {
            treeMap.put(extension.getId(), extension);
        }
        return treeMap;
    }

    @Override
    public int compareTo(X509CRLEntryImpl x509CRLEntryImpl) {
        int iCompareTo = getSerialNumber().compareTo(x509CRLEntryImpl.getSerialNumber());
        if (iCompareTo != 0) {
            return iCompareTo;
        }
        try {
            byte[] encoded0 = getEncoded0();
            byte[] encoded02 = x509CRLEntryImpl.getEncoded0();
            for (int i = 0; i < encoded0.length && i < encoded02.length; i++) {
                int i2 = encoded0[i] & Character.DIRECTIONALITY_UNDEFINED;
                int i3 = encoded02[i] & Character.DIRECTIONALITY_UNDEFINED;
                if (i2 != i3) {
                    return i2 - i3;
                }
            }
            return encoded0.length - encoded02.length;
        } catch (CRLException e) {
            return -1;
        }
    }
}
