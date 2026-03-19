package java.security;

import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Locale;
import javax.crypto.spec.SecretKeySpec;

public class KeyRep implements Serializable {
    private static final String PKCS8 = "PKCS#8";
    private static final String RAW = "RAW";
    private static final String X509 = "X.509";
    private static final long serialVersionUID = -4757683898830641853L;
    private String algorithm;
    private byte[] encoded;
    private String format;
    private Type type;

    public enum Type {
        SECRET,
        PUBLIC,
        PRIVATE
    }

    public KeyRep(Type type, String str, String str2, byte[] bArr) {
        if (type == null || str == null || str2 == null || bArr == null) {
            throw new NullPointerException("invalid null input(s)");
        }
        this.type = type;
        this.algorithm = str;
        this.format = str2.toUpperCase(Locale.ENGLISH);
        this.encoded = (byte[]) bArr.clone();
    }

    protected Object readResolve() throws ObjectStreamException {
        try {
            if (this.type == Type.SECRET && RAW.equals(this.format)) {
                return new SecretKeySpec(this.encoded, this.algorithm);
            }
            if (this.type == Type.PUBLIC && X509.equals(this.format)) {
                return KeyFactory.getInstance(this.algorithm).generatePublic(new X509EncodedKeySpec(this.encoded));
            }
            if (this.type == Type.PRIVATE && PKCS8.equals(this.format)) {
                return KeyFactory.getInstance(this.algorithm).generatePrivate(new PKCS8EncodedKeySpec(this.encoded));
            }
            throw new NotSerializableException("unrecognized type/format combination: " + ((Object) this.type) + "/" + this.format);
        } catch (NotSerializableException e) {
            throw e;
        } catch (Exception e2) {
            NotSerializableException notSerializableException = new NotSerializableException("java.security.Key: [" + ((Object) this.type) + "] [" + this.algorithm + "] [" + this.format + "]");
            notSerializableException.initCause(e2);
            throw notSerializableException;
        }
    }
}
