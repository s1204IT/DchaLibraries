package java.security.cert;

import java.io.IOException;
import org.apache.harmony.security.asn1.ObjectIdentifier;
import org.apache.harmony.security.utils.Array;

public class PolicyQualifierInfo {
    private final byte[] encoded;
    private final byte[] policyQualifier;
    private final String policyQualifierId;

    public PolicyQualifierInfo(byte[] encoded) throws IOException {
        if (encoded == null) {
            throw new NullPointerException("encoded == null");
        }
        if (encoded.length == 0) {
            throw new IOException("encoded.length == 0");
        }
        this.encoded = new byte[encoded.length];
        System.arraycopy(encoded, 0, this.encoded, 0, this.encoded.length);
        Object[] decoded = (Object[]) org.apache.harmony.security.x509.PolicyQualifierInfo.ASN1.decode(this.encoded);
        this.policyQualifierId = ObjectIdentifier.toString((int[]) decoded[0]);
        this.policyQualifier = (byte[]) decoded[1];
    }

    public final byte[] getEncoded() {
        byte[] ret = new byte[this.encoded.length];
        System.arraycopy(this.encoded, 0, ret, 0, this.encoded.length);
        return ret;
    }

    public final String getPolicyQualifierId() {
        return this.policyQualifierId;
    }

    public final byte[] getPolicyQualifier() {
        if (this.policyQualifier == null) {
            return null;
        }
        byte[] ret = new byte[this.policyQualifier.length];
        System.arraycopy(this.policyQualifier, 0, ret, 0, this.policyQualifier.length);
        return ret;
    }

    public String toString() {
        return "PolicyQualifierInfo: [\npolicyQualifierId: " + this.policyQualifierId + "\npolicyQualifier: \n" + Array.toString(this.policyQualifier, " ") + "]";
    }
}
