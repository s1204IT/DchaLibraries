package sun.security.provider.certpath;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.security.auth.x500.X500Principal;
import sun.misc.HexDumpEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.x509.AlgorithmId;
import sun.security.x509.SerialNumber;

public class CertId {
    private static final AlgorithmId SHA1_ALGID = new AlgorithmId(AlgorithmId.SHA_oid);
    private static final boolean debug = false;
    private final SerialNumber certSerialNumber;
    private final AlgorithmId hashAlgId;
    private final byte[] issuerKeyHash;
    private final byte[] issuerNameHash;
    private int myhash;

    public CertId(X509Certificate x509Certificate, SerialNumber serialNumber) throws IOException {
        this(x509Certificate.getSubjectX500Principal(), x509Certificate.getPublicKey(), serialNumber);
    }

    public CertId(X500Principal x500Principal, PublicKey publicKey, SerialNumber serialNumber) throws IOException {
        this.myhash = -1;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            this.hashAlgId = SHA1_ALGID;
            messageDigest.update(x500Principal.getEncoded());
            this.issuerNameHash = messageDigest.digest();
            DerValue derValue = new DerValue(publicKey.getEncoded());
            messageDigest.update(new DerValue[]{derValue.data.getDerValue(), derValue.data.getDerValue()}[1].getBitString());
            this.issuerKeyHash = messageDigest.digest();
            this.certSerialNumber = serialNumber;
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Unable to create CertId", e);
        }
    }

    public CertId(DerInputStream derInputStream) throws IOException {
        this.myhash = -1;
        this.hashAlgId = AlgorithmId.parse(derInputStream.getDerValue());
        this.issuerNameHash = derInputStream.getOctetString();
        this.issuerKeyHash = derInputStream.getOctetString();
        this.certSerialNumber = new SerialNumber(derInputStream);
    }

    public AlgorithmId getHashAlgorithm() {
        return this.hashAlgId;
    }

    public byte[] getIssuerNameHash() {
        return this.issuerNameHash;
    }

    public byte[] getIssuerKeyHash() {
        return this.issuerKeyHash;
    }

    public BigInteger getSerialNumber() {
        return this.certSerialNumber.getNumber();
    }

    public void encode(DerOutputStream derOutputStream) throws IOException {
        DerOutputStream derOutputStream2 = new DerOutputStream();
        this.hashAlgId.encode(derOutputStream2);
        derOutputStream2.putOctetString(this.issuerNameHash);
        derOutputStream2.putOctetString(this.issuerKeyHash);
        this.certSerialNumber.encode(derOutputStream2);
        derOutputStream.write((byte) 48, derOutputStream2);
    }

    public int hashCode() {
        if (this.myhash == -1) {
            this.myhash = this.hashAlgId.hashCode();
            for (int i = 0; i < this.issuerNameHash.length; i++) {
                this.myhash += this.issuerNameHash[i] * i;
            }
            for (int i2 = 0; i2 < this.issuerKeyHash.length; i2++) {
                this.myhash += this.issuerKeyHash[i2] * i2;
            }
            this.myhash += this.certSerialNumber.getNumber().hashCode();
        }
        return this.myhash;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof CertId)) {
            return debug;
        }
        CertId certId = (CertId) obj;
        if (this.hashAlgId.equals(certId.getHashAlgorithm()) && Arrays.equals(this.issuerNameHash, certId.getIssuerNameHash()) && Arrays.equals(this.issuerKeyHash, certId.getIssuerKeyHash()) && this.certSerialNumber.getNumber().equals(certId.getSerialNumber())) {
            return true;
        }
        return debug;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CertId \n");
        sb.append("Algorithm: " + this.hashAlgId.toString() + "\n");
        sb.append("issuerNameHash \n");
        HexDumpEncoder hexDumpEncoder = new HexDumpEncoder();
        sb.append(hexDumpEncoder.encode(this.issuerNameHash));
        sb.append("\nissuerKeyHash: \n");
        sb.append(hexDumpEncoder.encode(this.issuerKeyHash));
        sb.append("\n" + this.certSerialNumber.toString());
        return sb.toString();
    }
}
