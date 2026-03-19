package sun.security.timestamp;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;

public class TimestampToken {
    private Date genTime;
    private AlgorithmId hashAlgorithm;
    private byte[] hashedMessage;
    private BigInteger nonce;
    private ObjectIdentifier policy;
    private BigInteger serialNumber;
    private int version;

    public TimestampToken(byte[] bArr) throws IOException {
        if (bArr == null) {
            throw new IOException("No timestamp token info");
        }
        parse(bArr);
    }

    public Date getDate() {
        return this.genTime;
    }

    public AlgorithmId getHashAlgorithm() {
        return this.hashAlgorithm;
    }

    public byte[] getHashedMessage() {
        return this.hashedMessage;
    }

    public BigInteger getNonce() {
        return this.nonce;
    }

    public String getPolicyID() {
        return this.policy.toString();
    }

    public BigInteger getSerialNumber() {
        return this.serialNumber;
    }

    private void parse(byte[] bArr) throws IOException {
        DerValue derValue = new DerValue(bArr);
        if (derValue.tag != 48) {
            throw new IOException("Bad encoding for timestamp token info");
        }
        this.version = derValue.data.getInteger();
        this.policy = derValue.data.getOID();
        DerValue derValue2 = derValue.data.getDerValue();
        this.hashAlgorithm = AlgorithmId.parse(derValue2.data.getDerValue());
        this.hashedMessage = derValue2.data.getOctetString();
        this.serialNumber = derValue.data.getBigInteger();
        this.genTime = derValue.data.getGeneralizedTime();
        while (derValue.data.available() > 0) {
            DerValue derValue3 = derValue.data.getDerValue();
            if (derValue3.tag == 2) {
                this.nonce = derValue3.getBigInteger();
                return;
            }
        }
    }
}
