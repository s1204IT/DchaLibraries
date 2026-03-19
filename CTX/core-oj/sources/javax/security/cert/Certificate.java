package javax.security.cert;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;

public abstract class Certificate {
    public abstract byte[] getEncoded() throws CertificateEncodingException;

    public abstract PublicKey getPublicKey();

    public abstract String toString();

    public abstract void verify(PublicKey publicKey) throws CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException;

    public abstract void verify(PublicKey publicKey, String str) throws CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException;

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Certificate)) {
            return false;
        }
        try {
            byte[] encoded = getEncoded();
            byte[] encoded2 = ((Certificate) obj).getEncoded();
            if (encoded.length != encoded2.length) {
                return false;
            }
            for (int i = 0; i < encoded.length; i++) {
                if (encoded[i] != encoded2[i]) {
                    return false;
                }
            }
            return true;
        } catch (CertificateException e) {
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
        } catch (CertificateException e) {
            return i;
        }
    }
}
