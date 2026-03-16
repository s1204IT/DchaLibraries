package javax.security.cert;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;

public abstract class Certificate {
    public abstract byte[] getEncoded() throws CertificateEncodingException;

    public abstract PublicKey getPublicKey();

    public abstract String toString();

    public abstract void verify(PublicKey publicKey) throws CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException;

    public abstract void verify(PublicKey publicKey, String str) throws CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException;

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Certificate)) {
            return false;
        }
        Certificate object = (Certificate) obj;
        try {
            return Arrays.equals(getEncoded(), object.getEncoded());
        } catch (CertificateEncodingException e) {
            return false;
        }
    }

    public int hashCode() {
        int res = 0;
        try {
            byte[] array = getEncoded();
            for (byte b : array) {
                res += b;
            }
        } catch (CertificateEncodingException e) {
        }
        return res;
    }
}
