package java.security.cert;

import java.math.BigInteger;
import java.util.Date;
import javax.security.auth.x500.X500Principal;
import sun.security.x509.X509CRLEntryImpl;

public abstract class X509CRLEntry implements X509Extension {
    public abstract byte[] getEncoded() throws CRLException;

    public abstract Date getRevocationDate();

    public abstract BigInteger getSerialNumber();

    public abstract boolean hasExtensions();

    public abstract String toString();

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof X509CRLEntry)) {
            return false;
        }
        try {
            byte[] encoded = getEncoded();
            byte[] encoded2 = ((X509CRLEntry) obj).getEncoded();
            if (encoded.length != encoded2.length) {
                return false;
            }
            for (int i = 0; i < encoded.length; i++) {
                if (encoded[i] != encoded2[i]) {
                    return false;
                }
            }
            return true;
        } catch (CRLException e) {
            return false;
        }
    }

    public int hashCode() {
        int i = 0;
        try {
            byte[] encoded = getEncoded();
            for (int i2 = 1; i2 < encoded.length; i2++) {
                i += encoded[i2] * i2;
            }
            return i;
        } catch (CRLException e) {
            return i;
        }
    }

    public X500Principal getCertificateIssuer() {
        return null;
    }

    public CRLReason getRevocationReason() {
        if (!hasExtensions()) {
            return null;
        }
        return X509CRLEntryImpl.getRevocationReason(this);
    }
}
