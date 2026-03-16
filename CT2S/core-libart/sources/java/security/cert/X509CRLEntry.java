package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import javax.security.auth.x500.X500Principal;
import org.apache.harmony.security.asn1.ASN1OctetString;
import org.apache.harmony.security.x509.ReasonCode;

public abstract class X509CRLEntry implements X509Extension {
    public abstract byte[] getEncoded() throws CRLException;

    public abstract Date getRevocationDate();

    public abstract BigInteger getSerialNumber();

    public abstract boolean hasExtensions();

    public abstract String toString();

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof X509CRLEntry)) {
            return false;
        }
        X509CRLEntry obj = (X509CRLEntry) other;
        try {
            return Arrays.equals(getEncoded(), obj.getEncoded());
        } catch (CRLException e) {
            return false;
        }
    }

    public int hashCode() {
        int res = 0;
        try {
            byte[] array = getEncoded();
            for (byte b : array) {
                res += b & Character.DIRECTIONALITY_UNDEFINED;
            }
        } catch (CRLException e) {
        }
        return res;
    }

    public X500Principal getCertificateIssuer() {
        return null;
    }

    public CRLReason getRevocationReason() {
        byte[] reasonBytes = getExtensionValue("2.5.29.21");
        if (reasonBytes == null) {
            return null;
        }
        try {
            byte[] rawBytes = (byte[]) ASN1OctetString.getInstance().decode(reasonBytes);
            return new ReasonCode(rawBytes).getReason();
        } catch (IOException e) {
            return null;
        }
    }
}
