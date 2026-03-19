package sun.security.x509;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.security.auth.x500.X500Principal;
import sun.misc.HexDumpEncoder;
import sun.security.provider.X509Factory;
import sun.security.util.DerEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class X509CRLImpl extends X509CRL implements DerEncoder {
    private static final long YR_2050 = 2524636800000L;
    private static final boolean isExplicit = true;
    private CRLExtensions extensions;
    private AlgorithmId infoSigAlgId;
    private X500Name issuer;
    private X500Principal issuerPrincipal;
    private Date nextUpdate;
    private boolean readOnly;
    private List<X509CRLEntry> revokedList;
    private Map<X509IssuerSerial, X509CRLEntry> revokedMap;
    private AlgorithmId sigAlgId;
    private byte[] signature;
    private byte[] signedCRL;
    private byte[] tbsCertList;
    private Date thisUpdate;
    private String verifiedProvider;
    private PublicKey verifiedPublicKey;
    private int version;

    private X509CRLImpl() {
        this.signedCRL = null;
        this.signature = null;
        this.tbsCertList = null;
        this.sigAlgId = null;
        this.issuer = null;
        this.issuerPrincipal = null;
        this.thisUpdate = null;
        this.nextUpdate = null;
        this.revokedMap = new TreeMap();
        this.revokedList = new LinkedList();
        this.extensions = null;
        this.readOnly = false;
    }

    public X509CRLImpl(byte[] bArr) throws CRLException {
        this.signedCRL = null;
        this.signature = null;
        this.tbsCertList = null;
        this.sigAlgId = null;
        this.issuer = null;
        this.issuerPrincipal = null;
        this.thisUpdate = null;
        this.nextUpdate = null;
        this.revokedMap = new TreeMap();
        this.revokedList = new LinkedList();
        this.extensions = null;
        this.readOnly = false;
        try {
            parse(new DerValue(bArr));
        } catch (IOException e) {
            this.signedCRL = null;
            throw new CRLException("Parsing error: " + e.getMessage());
        }
    }

    public X509CRLImpl(DerValue derValue) throws CRLException {
        this.signedCRL = null;
        this.signature = null;
        this.tbsCertList = null;
        this.sigAlgId = null;
        this.issuer = null;
        this.issuerPrincipal = null;
        this.thisUpdate = null;
        this.nextUpdate = null;
        this.revokedMap = new TreeMap();
        this.revokedList = new LinkedList();
        this.extensions = null;
        this.readOnly = false;
        try {
            parse(derValue);
        } catch (IOException e) {
            this.signedCRL = null;
            throw new CRLException("Parsing error: " + e.getMessage());
        }
    }

    public X509CRLImpl(InputStream inputStream) throws CRLException {
        this.signedCRL = null;
        this.signature = null;
        this.tbsCertList = null;
        this.sigAlgId = null;
        this.issuer = null;
        this.issuerPrincipal = null;
        this.thisUpdate = null;
        this.nextUpdate = null;
        this.revokedMap = new TreeMap();
        this.revokedList = new LinkedList();
        this.extensions = null;
        this.readOnly = false;
        try {
            parse(new DerValue(inputStream));
        } catch (IOException e) {
            this.signedCRL = null;
            throw new CRLException("Parsing error: " + e.getMessage());
        }
    }

    public X509CRLImpl(X500Name x500Name, Date date, Date date2) {
        this.signedCRL = null;
        this.signature = null;
        this.tbsCertList = null;
        this.sigAlgId = null;
        this.issuer = null;
        this.issuerPrincipal = null;
        this.thisUpdate = null;
        this.nextUpdate = null;
        this.revokedMap = new TreeMap();
        this.revokedList = new LinkedList();
        this.extensions = null;
        this.readOnly = false;
        this.issuer = x500Name;
        this.thisUpdate = date;
        this.nextUpdate = date2;
    }

    public X509CRLImpl(X500Name x500Name, Date date, Date date2, X509CRLEntry[] x509CRLEntryArr) throws CRLException {
        this.signedCRL = null;
        this.signature = null;
        this.tbsCertList = null;
        this.sigAlgId = null;
        this.issuer = null;
        this.issuerPrincipal = null;
        this.thisUpdate = null;
        this.nextUpdate = null;
        this.revokedMap = new TreeMap();
        this.revokedList = new LinkedList();
        this.extensions = null;
        this.readOnly = false;
        this.issuer = x500Name;
        this.thisUpdate = date;
        this.nextUpdate = date2;
        if (x509CRLEntryArr != null) {
            X500Principal issuerX500Principal = getIssuerX500Principal();
            X500Principal certIssuer = issuerX500Principal;
            for (X509CRLEntry x509CRLEntry : x509CRLEntryArr) {
                X509CRLEntryImpl x509CRLEntryImpl = (X509CRLEntryImpl) x509CRLEntry;
                try {
                    certIssuer = getCertIssuer(x509CRLEntryImpl, certIssuer);
                    x509CRLEntryImpl.setCertificateIssuer(issuerX500Principal, certIssuer);
                    this.revokedMap.put(new X509IssuerSerial(certIssuer, x509CRLEntryImpl.getSerialNumber()), x509CRLEntryImpl);
                    this.revokedList.add(x509CRLEntryImpl);
                    if (x509CRLEntryImpl.hasExtensions()) {
                        this.version = 1;
                    }
                } catch (IOException e) {
                    throw new CRLException(e);
                }
            }
        }
    }

    public X509CRLImpl(X500Name x500Name, Date date, Date date2, X509CRLEntry[] x509CRLEntryArr, CRLExtensions cRLExtensions) throws CRLException {
        this(x500Name, date, date2, x509CRLEntryArr);
        if (cRLExtensions != null) {
            this.extensions = cRLExtensions;
            this.version = 1;
        }
    }

    public byte[] getEncodedInternal() throws CRLException {
        if (this.signedCRL == null) {
            throw new CRLException("Null CRL to encode");
        }
        return this.signedCRL;
    }

    @Override
    public byte[] getEncoded() throws CRLException {
        return (byte[]) getEncodedInternal().clone();
    }

    public void encodeInfo(OutputStream outputStream) throws CRLException {
        try {
            DerOutputStream derOutputStream = new DerOutputStream();
            DerOutputStream derOutputStream2 = new DerOutputStream();
            DerOutputStream derOutputStream3 = new DerOutputStream();
            if (this.version != 0) {
                derOutputStream.putInteger(this.version);
            }
            this.infoSigAlgId.encode(derOutputStream);
            if (this.version == 0 && this.issuer.toString() == null) {
                throw new CRLException("Null Issuer DN not allowed in v1 CRL");
            }
            this.issuer.encode(derOutputStream);
            if (this.thisUpdate.getTime() < YR_2050) {
                derOutputStream.putUTCTime(this.thisUpdate);
            } else {
                derOutputStream.putGeneralizedTime(this.thisUpdate);
            }
            if (this.nextUpdate != null) {
                if (this.nextUpdate.getTime() < YR_2050) {
                    derOutputStream.putUTCTime(this.nextUpdate);
                } else {
                    derOutputStream.putGeneralizedTime(this.nextUpdate);
                }
            }
            if (!this.revokedList.isEmpty()) {
                Iterator<X509CRLEntry> it = this.revokedList.iterator();
                while (it.hasNext()) {
                    ((X509CRLEntryImpl) it.next()).encode(derOutputStream2);
                }
                derOutputStream.write((byte) 48, derOutputStream2);
            }
            if (this.extensions != null) {
                this.extensions.encode(derOutputStream, isExplicit);
            }
            derOutputStream3.write((byte) 48, derOutputStream);
            this.tbsCertList = derOutputStream3.toByteArray();
            outputStream.write(this.tbsCertList);
        } catch (IOException e) {
            throw new CRLException("Encoding error: " + e.getMessage());
        }
    }

    @Override
    public void verify(PublicKey publicKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CRLException, NoSuchProviderException {
        verify(publicKey, "");
    }

    @Override
    public synchronized void verify(PublicKey publicKey, String str) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CRLException, NoSuchProviderException {
        Signature signature;
        if (str == null) {
            str = "";
        }
        try {
            if (this.verifiedPublicKey != null && this.verifiedPublicKey.equals(publicKey) && str.equals(this.verifiedProvider)) {
                return;
            }
            if (this.signedCRL == null) {
                throw new CRLException("Uninitialized CRL");
            }
            if (str.length() == 0) {
                signature = Signature.getInstance(this.sigAlgId.getName());
            } else {
                signature = Signature.getInstance(this.sigAlgId.getName(), str);
            }
            signature.initVerify(publicKey);
            if (this.tbsCertList == null) {
                throw new CRLException("Uninitialized CRL");
            }
            signature.update(this.tbsCertList, 0, this.tbsCertList.length);
            if (!signature.verify(this.signature)) {
                throw new SignatureException("Signature does not match.");
            }
            this.verifiedPublicKey = publicKey;
            this.verifiedProvider = str;
        } catch (Throwable th) {
            throw th;
        }
    }

    @Override
    public synchronized void verify(PublicKey publicKey, Provider provider) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CRLException {
        Signature signature;
        if (this.signedCRL == null) {
            throw new CRLException("Uninitialized CRL");
        }
        if (provider == null) {
            signature = Signature.getInstance(this.sigAlgId.getName());
        } else {
            signature = Signature.getInstance(this.sigAlgId.getName(), provider);
        }
        signature.initVerify(publicKey);
        if (this.tbsCertList == null) {
            throw new CRLException("Uninitialized CRL");
        }
        signature.update(this.tbsCertList, 0, this.tbsCertList.length);
        if (!signature.verify(this.signature)) {
            throw new SignatureException("Signature does not match.");
        }
        this.verifiedPublicKey = publicKey;
    }

    public void sign(PrivateKey privateKey, String str) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CRLException, NoSuchProviderException {
        sign(privateKey, str, null);
    }

    public void sign(PrivateKey privateKey, String str, String str2) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CRLException, NoSuchProviderException {
        Signature signature;
        try {
            if (this.readOnly) {
                throw new CRLException("cannot over-write existing CRL");
            }
            if (str2 == null || str2.length() == 0) {
                signature = Signature.getInstance(str);
            } else {
                signature = Signature.getInstance(str, str2);
            }
            signature.initSign(privateKey);
            this.sigAlgId = AlgorithmId.get(signature.getAlgorithm());
            this.infoSigAlgId = this.sigAlgId;
            DerOutputStream derOutputStream = new DerOutputStream();
            DerOutputStream derOutputStream2 = new DerOutputStream();
            encodeInfo(derOutputStream2);
            this.sigAlgId.encode(derOutputStream2);
            signature.update(this.tbsCertList, 0, this.tbsCertList.length);
            this.signature = signature.sign();
            derOutputStream2.putBitString(this.signature);
            derOutputStream.write((byte) 48, derOutputStream2);
            this.signedCRL = derOutputStream.toByteArray();
            this.readOnly = isExplicit;
        } catch (IOException e) {
            throw new CRLException("Error while encoding data: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        StringBuilder sb = new StringBuilder();
        sb.append("X.509 CRL v");
        int i = 1;
        sb.append(this.version + 1);
        sb.append("\n");
        stringBuffer.append(sb.toString());
        if (this.sigAlgId != null) {
            stringBuffer.append("Signature Algorithm: " + this.sigAlgId.toString() + ", OID=" + this.sigAlgId.getOID().toString() + "\n");
        }
        if (this.issuer != null) {
            stringBuffer.append("Issuer: " + this.issuer.toString() + "\n");
        }
        if (this.thisUpdate != null) {
            stringBuffer.append("\nThis Update: " + this.thisUpdate.toString() + "\n");
        }
        if (this.nextUpdate != null) {
            stringBuffer.append("Next Update: " + this.nextUpdate.toString() + "\n");
        }
        if (this.revokedList.isEmpty()) {
            stringBuffer.append("\nNO certificates have been revoked\n");
        } else {
            stringBuffer.append("\nRevoked Certificates: " + this.revokedList.size());
            Iterator<X509CRLEntry> it = this.revokedList.iterator();
            while (it.hasNext()) {
                stringBuffer.append("\n[" + i + "] " + it.next().toString());
                i++;
            }
        }
        if (this.extensions != null) {
            Object[] array = this.extensions.getAllExtensions().toArray();
            stringBuffer.append("\nCRL Extensions: " + array.length);
            int i2 = 0;
            while (i2 < array.length) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("\n[");
                int i3 = i2 + 1;
                sb2.append(i3);
                sb2.append("]: ");
                stringBuffer.append(sb2.toString());
                Extension extension = (Extension) array[i2];
                try {
                    if (OIDMap.getClass(extension.getExtensionId()) == null) {
                        stringBuffer.append(extension.toString());
                        byte[] extensionValue = extension.getExtensionValue();
                        if (extensionValue != null) {
                            DerOutputStream derOutputStream = new DerOutputStream();
                            derOutputStream.putOctetString(extensionValue);
                            byte[] byteArray = derOutputStream.toByteArray();
                            stringBuffer.append("Extension unknown: DER encoded OCTET string =\n" + new HexDumpEncoder().encodeBuffer(byteArray) + "\n");
                        }
                    } else {
                        stringBuffer.append(extension.toString());
                    }
                } catch (Exception e) {
                    stringBuffer.append(", Error parsing this extension");
                }
                i2 = i3;
            }
        }
        if (this.signature != null) {
            stringBuffer.append("\nSignature:\n" + new HexDumpEncoder().encodeBuffer(this.signature) + "\n");
        } else {
            stringBuffer.append("NOT signed yet\n");
        }
        return stringBuffer.toString();
    }

    @Override
    public boolean isRevoked(Certificate certificate) {
        if (this.revokedMap.isEmpty() || !(certificate instanceof X509Certificate)) {
            return false;
        }
        return this.revokedMap.containsKey(new X509IssuerSerial((X509Certificate) certificate));
    }

    @Override
    public int getVersion() {
        return this.version + 1;
    }

    @Override
    public Principal getIssuerDN() {
        return this.issuer;
    }

    @Override
    public X500Principal getIssuerX500Principal() {
        if (this.issuerPrincipal == null) {
            this.issuerPrincipal = this.issuer.asX500Principal();
        }
        return this.issuerPrincipal;
    }

    @Override
    public Date getThisUpdate() {
        return new Date(this.thisUpdate.getTime());
    }

    @Override
    public Date getNextUpdate() {
        if (this.nextUpdate == null) {
            return null;
        }
        return new Date(this.nextUpdate.getTime());
    }

    @Override
    public X509CRLEntry getRevokedCertificate(BigInteger bigInteger) {
        if (this.revokedMap.isEmpty()) {
            return null;
        }
        return this.revokedMap.get(new X509IssuerSerial(getIssuerX500Principal(), bigInteger));
    }

    @Override
    public X509CRLEntry getRevokedCertificate(X509Certificate x509Certificate) {
        if (this.revokedMap.isEmpty()) {
            return null;
        }
        return this.revokedMap.get(new X509IssuerSerial(x509Certificate));
    }

    @Override
    public Set<X509CRLEntry> getRevokedCertificates() {
        if (this.revokedList.isEmpty()) {
            return null;
        }
        return new TreeSet(this.revokedList);
    }

    @Override
    public byte[] getTBSCertList() throws CRLException {
        if (this.tbsCertList == null) {
            throw new CRLException("Uninitialized CRL");
        }
        return (byte[]) this.tbsCertList.clone();
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
        if (this.sigAlgId == null) {
            return null;
        }
        return this.sigAlgId.getName();
    }

    @Override
    public String getSigAlgOID() {
        if (this.sigAlgId == null) {
            return null;
        }
        return this.sigAlgId.getOID().toString();
    }

    @Override
    public byte[] getSigAlgParams() {
        if (this.sigAlgId == null) {
            return null;
        }
        try {
            return this.sigAlgId.getEncodedParams();
        } catch (IOException e) {
            return null;
        }
    }

    public AlgorithmId getSigAlgId() {
        return this.sigAlgId;
    }

    public KeyIdentifier getAuthKeyId() throws IOException {
        AuthorityKeyIdentifierExtension authKeyIdExtension = getAuthKeyIdExtension();
        if (authKeyIdExtension != null) {
            return (KeyIdentifier) authKeyIdExtension.get("key_id");
        }
        return null;
    }

    public AuthorityKeyIdentifierExtension getAuthKeyIdExtension() throws IOException {
        return (AuthorityKeyIdentifierExtension) getExtension(PKIXExtensions.AuthorityKey_Id);
    }

    public CRLNumberExtension getCRLNumberExtension() throws IOException {
        return (CRLNumberExtension) getExtension(PKIXExtensions.CRLNumber_Id);
    }

    public BigInteger getCRLNumber() throws IOException {
        CRLNumberExtension cRLNumberExtension = getCRLNumberExtension();
        if (cRLNumberExtension != null) {
            return cRLNumberExtension.get("value");
        }
        return null;
    }

    public DeltaCRLIndicatorExtension getDeltaCRLIndicatorExtension() throws IOException {
        return (DeltaCRLIndicatorExtension) getExtension(PKIXExtensions.DeltaCRLIndicator_Id);
    }

    public BigInteger getBaseCRLNumber() throws IOException {
        DeltaCRLIndicatorExtension deltaCRLIndicatorExtension = getDeltaCRLIndicatorExtension();
        if (deltaCRLIndicatorExtension != null) {
            return deltaCRLIndicatorExtension.get("value");
        }
        return null;
    }

    public IssuerAlternativeNameExtension getIssuerAltNameExtension() throws IOException {
        return (IssuerAlternativeNameExtension) getExtension(PKIXExtensions.IssuerAlternativeName_Id);
    }

    public IssuingDistributionPointExtension getIssuingDistributionPointExtension() throws IOException {
        return (IssuingDistributionPointExtension) getExtension(PKIXExtensions.IssuingDistributionPoint_Id);
    }

    @Override
    public boolean hasUnsupportedCriticalExtension() {
        if (this.extensions == null) {
            return false;
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

    public Object getExtension(ObjectIdentifier objectIdentifier) {
        if (this.extensions == null) {
            return null;
        }
        return this.extensions.get(OIDMap.getName(objectIdentifier));
    }

    private void parse(DerValue derValue) throws IOException, CRLException {
        if (this.readOnly) {
            throw new CRLException("cannot over-write existing CRL");
        }
        if (derValue.getData() == null || derValue.tag != 48) {
            throw new CRLException("Invalid DER-encoded CRL data");
        }
        this.signedCRL = derValue.toByteArray();
        DerValue[] derValueArr = {derValue.data.getDerValue(), derValue.data.getDerValue(), derValue.data.getDerValue()};
        if (derValue.data.available() != 0) {
            throw new CRLException("signed overrun, bytes = " + derValue.data.available());
        }
        if (derValueArr[0].tag != 48) {
            throw new CRLException("signed CRL fields invalid");
        }
        this.sigAlgId = AlgorithmId.parse(derValueArr[1]);
        this.signature = derValueArr[2].getBitString();
        if (derValueArr[1].data.available() != 0) {
            throw new CRLException("AlgorithmId field overrun");
        }
        if (derValueArr[2].data.available() != 0) {
            throw new CRLException("Signature field overrun");
        }
        this.tbsCertList = derValueArr[0].toByteArray();
        DerInputStream derInputStream = derValueArr[0].data;
        this.version = 0;
        if (((byte) derInputStream.peekByte()) == 2) {
            this.version = derInputStream.getInteger();
            if (this.version != 1) {
                throw new CRLException("Invalid version");
            }
        }
        AlgorithmId algorithmId = AlgorithmId.parse(derInputStream.getDerValue());
        if (!algorithmId.equals(this.sigAlgId)) {
            throw new CRLException("Signature algorithm mismatch");
        }
        this.infoSigAlgId = algorithmId;
        this.issuer = new X500Name(derInputStream);
        if (this.issuer.isEmpty()) {
            throw new CRLException("Empty issuer DN not allowed in X509CRLs");
        }
        byte bPeekByte = (byte) derInputStream.peekByte();
        if (bPeekByte == 23) {
            this.thisUpdate = derInputStream.getUTCTime();
        } else if (bPeekByte == 24) {
            this.thisUpdate = derInputStream.getGeneralizedTime();
        } else {
            throw new CRLException("Invalid encoding for thisUpdate (tag=" + ((int) bPeekByte) + ")");
        }
        if (derInputStream.available() == 0) {
            return;
        }
        byte bPeekByte2 = (byte) derInputStream.peekByte();
        if (bPeekByte2 == 23) {
            this.nextUpdate = derInputStream.getUTCTime();
        } else if (bPeekByte2 == 24) {
            this.nextUpdate = derInputStream.getGeneralizedTime();
        }
        if (derInputStream.available() == 0) {
            return;
        }
        byte bPeekByte3 = (byte) derInputStream.peekByte();
        if (bPeekByte3 == 48 && (bPeekByte3 & DerValue.TAG_PRIVATE) != 128) {
            DerValue[] sequence = derInputStream.getSequence(4);
            X500Principal issuerX500Principal = getIssuerX500Principal();
            X500Principal certIssuer = issuerX500Principal;
            for (DerValue derValue2 : sequence) {
                X509CRLEntryImpl x509CRLEntryImpl = new X509CRLEntryImpl(derValue2);
                certIssuer = getCertIssuer(x509CRLEntryImpl, certIssuer);
                x509CRLEntryImpl.setCertificateIssuer(issuerX500Principal, certIssuer);
                this.revokedMap.put(new X509IssuerSerial(certIssuer, x509CRLEntryImpl.getSerialNumber()), x509CRLEntryImpl);
                this.revokedList.add(x509CRLEntryImpl);
            }
        }
        if (derInputStream.available() == 0) {
            return;
        }
        DerValue derValue3 = derInputStream.getDerValue();
        if (derValue3.isConstructed() && derValue3.isContextSpecific((byte) 0)) {
            this.extensions = new CRLExtensions(derValue3.data);
        }
        this.readOnly = isExplicit;
    }

    public static X500Principal getIssuerX500Principal(X509CRL x509crl) {
        try {
            DerInputStream derInputStream = new DerInputStream(x509crl.getEncoded()).getSequence(3)[0].data;
            if (((byte) derInputStream.peekByte()) == 2) {
                derInputStream.getDerValue();
            }
            derInputStream.getDerValue();
            return new X500Principal(derInputStream.getDerValue().toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Could not parse issuer", e);
        }
    }

    public static byte[] getEncodedInternal(X509CRL x509crl) throws CRLException {
        if (x509crl instanceof X509CRLImpl) {
            return ((X509CRLImpl) x509crl).getEncodedInternal();
        }
        return x509crl.getEncoded();
    }

    public static X509CRLImpl toImpl(X509CRL x509crl) throws CRLException {
        if (x509crl instanceof X509CRLImpl) {
            return (X509CRLImpl) x509crl;
        }
        return X509Factory.intern(x509crl);
    }

    private X500Principal getCertIssuer(X509CRLEntryImpl x509CRLEntryImpl, X500Principal x500Principal) throws IOException {
        CertificateIssuerExtension certificateIssuerExtension = x509CRLEntryImpl.getCertificateIssuerExtension();
        if (certificateIssuerExtension != null) {
            return ((X500Name) certificateIssuerExtension.get("issuer").get(0).getName()).asX500Principal();
        }
        return x500Principal;
    }

    @Override
    public void derEncode(OutputStream outputStream) throws IOException {
        if (this.signedCRL == null) {
            throw new IOException("Null CRL to encode");
        }
        outputStream.write((byte[]) this.signedCRL.clone());
    }

    private static final class X509IssuerSerial implements Comparable<X509IssuerSerial> {
        volatile int hashcode;
        final X500Principal issuer;
        final BigInteger serial;

        X509IssuerSerial(X500Principal x500Principal, BigInteger bigInteger) {
            this.hashcode = 0;
            this.issuer = x500Principal;
            this.serial = bigInteger;
        }

        X509IssuerSerial(X509Certificate x509Certificate) {
            this(x509Certificate.getIssuerX500Principal(), x509Certificate.getSerialNumber());
        }

        X500Principal getIssuer() {
            return this.issuer;
        }

        BigInteger getSerial() {
            return this.serial;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return X509CRLImpl.isExplicit;
            }
            if (!(obj instanceof X509IssuerSerial)) {
                return false;
            }
            X509IssuerSerial x509IssuerSerial = (X509IssuerSerial) obj;
            if (this.serial.equals(x509IssuerSerial.getSerial()) && this.issuer.equals(x509IssuerSerial.getIssuer())) {
                return X509CRLImpl.isExplicit;
            }
            return false;
        }

        public int hashCode() {
            if (this.hashcode == 0) {
                this.hashcode = (37 * (629 + this.issuer.hashCode())) + this.serial.hashCode();
            }
            return this.hashcode;
        }

        @Override
        public int compareTo(X509IssuerSerial x509IssuerSerial) {
            int iCompareTo = this.issuer.toString().compareTo(x509IssuerSerial.issuer.toString());
            return iCompareTo != 0 ? iCompareTo : this.serial.compareTo(x509IssuerSerial.serial);
        }
    }
}
